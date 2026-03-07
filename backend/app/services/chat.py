"""Chat service (Chat Manager)."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models import Chat, Message, ChatParticipant, User
from app.core.exceptions import NotFoundException, ForbiddenException
from app.schemas import ChatCreate, ChatUpdate, MessageCreate


class ChatService:
    """Handles chat operations."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_chat(self, request: ChatCreate, creator_id: int) -> Chat:
        """Create a new chat with participants."""
        chat = Chat(name=request.name)
        self.db.add(chat)
        await self.db.flush()

        # Add creator as participant
        self.db.add(ChatParticipant(chat_id=chat.id, user_id=creator_id))

        # Add other participants by username
        for username in request.participants:
            result = await self.db.execute(
                select(User).where(User.username == username)
            )
            user = result.scalar_one_or_none()
            if user and user.id != creator_id:
                self.db.add(ChatParticipant(chat_id=chat.id, user_id=user.id))

        await self.db.commit()
        await self.db.refresh(chat)
        return chat

    async def get_user_chats(self, user_id: int) -> list[Chat]:
        """Get all chats a user is part of."""
        result = await self.db.execute(
            select(Chat)
            .join(ChatParticipant)
            .where(ChatParticipant.user_id == user_id)
            .options(selectinload(Chat.participants))
        )
        return list(result.scalars().all())

    async def get_chat(self, chat_id: int, user_id: int) -> Chat:
        """Get a specific chat (must be participant)."""
        chat = await self._get_chat_if_participant(chat_id, user_id)
        return chat

    async def update_chat(self, chat_id: int, request: ChatUpdate, user_id: int) -> Chat:
        """Update chat details."""
        chat = await self._get_chat_if_participant(chat_id, user_id)
        if request.name is not None:
            chat.name = request.name
        await self.db.commit()
        await self.db.refresh(chat)
        return chat

    async def add_participant(self, chat_id: int, username: str, user_id: int) -> None:
        """Add a participant to a chat."""
        await self._get_chat_if_participant(chat_id, user_id)

        result = await self.db.execute(
            select(User).where(User.username == username)
        )
        new_user = result.scalar_one_or_none()
        if not new_user:
            raise NotFoundException(detail=f"User '{username}' not found")

        # Check if already a participant
        existing = await self.db.execute(
            select(ChatParticipant).where(
                ChatParticipant.chat_id == chat_id,
                ChatParticipant.user_id == new_user.id
            )
        )
        if existing.scalar_one_or_none():
            return  # Already a participant

        self.db.add(ChatParticipant(chat_id=chat_id, user_id=new_user.id))
        await self.db.commit()

    async def remove_participant(self, chat_id: int, username: str, user_id: int) -> None:
        """Remove a participant from a chat."""
        await self._get_chat_if_participant(chat_id, user_id)

        result = await self.db.execute(
            select(User).where(User.username == username)
        )
        target_user = result.scalar_one_or_none()
        if not target_user:
            raise NotFoundException(detail=f"User '{username}' not found")

        result = await self.db.execute(
            select(ChatParticipant).where(
                ChatParticipant.chat_id == chat_id,
                ChatParticipant.user_id == target_user.id
            )
        )
        participant = result.scalar_one_or_none()
        if participant:
            await self.db.delete(participant)
            await self.db.commit()

    async def get_chat_participants(self, chat_id: int, user_id: int) -> list[User]:
        """Get all participants in a chat."""
        await self._get_chat_if_participant(chat_id, user_id)

        result = await self.db.execute(
            select(User)
            .join(ChatParticipant)
            .where(ChatParticipant.chat_id == chat_id)
        )
        return list(result.scalars().all())

    async def get_messages(self, chat_id: int, user_id: int, limit: int = 50) -> list[Message]:
        """Get messages for a chat."""
        await self._get_chat_if_participant(chat_id, user_id)

        result = await self.db.execute(
            select(Message)
            .where(Message.chat_id == chat_id)
            .options(selectinload(Message.sender))
            .order_by(Message.sent_at.desc())
            .limit(limit)
        )
        messages = list(result.scalars().all())
        messages.reverse()  # Return in chronological order
        return messages

    async def send_message(self, chat_id: int, request: MessageCreate, user_id: int) -> Message:
        """Send a message to a chat."""
        await self._get_chat_if_participant(chat_id, user_id)

        message = Message(
            chat_id=chat_id,
            sender_id=user_id,
            content=request.content,
        )
        self.db.add(message)
        await self.db.commit()
        await self.db.refresh(message)

        # Load sender relationship
        result = await self.db.execute(
            select(Message)
            .where(Message.id == message.id)
            .options(selectinload(Message.sender))
        )
        return result.scalar_one()

    async def is_participant(self, chat_id: int, user_id: int) -> bool:
        """Check if a user is a participant in a chat."""
        result = await self.db.execute(
            select(ChatParticipant).where(
                ChatParticipant.chat_id == chat_id,
                ChatParticipant.user_id == user_id
            )
        )
        return result.scalar_one_or_none() is not None

    async def _get_chat_if_participant(self, chat_id: int, user_id: int, is_admin: bool = False) -> Chat:
        """Get chat if user is a participant or admin, else raise exception."""
        result = await self.db.execute(
            select(Chat).where(Chat.id == chat_id)
        )
        chat = result.scalar_one_or_none()
        if not chat:
            raise NotFoundException(detail="Chat not found")

        # Admins bypass participant check
        if not is_admin and not await self.is_participant(chat_id, user_id):
            raise ForbiddenException(detail="You are not a participant in this chat")

        return chat

    async def get_all_chats(self) -> list[Chat]:
        """Get all chats (admin only)."""
        result = await self.db.execute(
            select(Chat).options(selectinload(Chat.participants))
        )
        return list(result.scalars().all())

    async def get_chat_admin(self, chat_id: int) -> Chat:
        """Get any chat by ID (admin only)."""
        result = await self.db.execute(
            select(Chat).where(Chat.id == chat_id)
        )
        chat = result.scalar_one_or_none()
        if not chat:
            raise NotFoundException(detail="Chat not found")
        return chat

