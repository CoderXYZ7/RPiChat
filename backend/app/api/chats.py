"""Chat endpoints."""

from fastapi import APIRouter

from app.api.deps import DbSession, CurrentUser
from app.schemas import (
    ChatCreate,
    ChatUpdate,
    ChatResponse,
    ParticipantAdd,
    MessageCreate,
    MessageResponse,
)
from app.services import ChatService

router = APIRouter(prefix="/api/chats", tags=["Chats"])


@router.get("", response_model=list[ChatResponse])
async def list_chats(db: DbSession, current_user: CurrentUser):
    """List all chats the user is part of (or all chats for admins)."""
    chat_service = ChatService(db)
    if current_user.is_admin:
        return await chat_service.get_all_chats()
    return await chat_service.get_user_chats(current_user.id)


@router.post("", response_model=ChatResponse, status_code=201)
async def create_chat(request: ChatCreate, db: DbSession, current_user: CurrentUser):
    """Create a new chat."""
    chat_service = ChatService(db)
    return await chat_service.create_chat(request, current_user.id)


@router.get("/{chat_id}", response_model=ChatResponse)
async def get_chat(chat_id: int, db: DbSession, current_user: CurrentUser):
    """Get details of a specific chat."""
    chat_service = ChatService(db)
    return await chat_service.get_chat(chat_id, current_user.id)


@router.patch("/{chat_id}", response_model=ChatResponse)
async def update_chat(chat_id: int, request: ChatUpdate, db: DbSession, current_user: CurrentUser):
    """Update chat details (e.g., name)."""
    chat_service = ChatService(db)
    return await chat_service.update_chat(chat_id, request, current_user.id)


@router.post("/{chat_id}/participants", status_code=204)
async def add_participant(
    chat_id: int, request: ParticipantAdd, db: DbSession, current_user: CurrentUser
):
    """Add a participant to a chat."""
    chat_service = ChatService(db)
    await chat_service.add_participant(chat_id, request.username, current_user.id)


@router.delete("/{chat_id}/participants/{username}", status_code=204)
async def remove_participant(
    chat_id: int, username: str, db: DbSession, current_user: CurrentUser
):
    """Remove a participant from a chat."""
    chat_service = ChatService(db)
    await chat_service.remove_participant(chat_id, username, current_user.id)


@router.get("/{chat_id}/messages", response_model=list[MessageResponse])
async def get_messages(chat_id: int, db: DbSession, current_user: CurrentUser):
    """Get message history for a chat."""
    chat_service = ChatService(db)
    messages = await chat_service.get_messages(chat_id, current_user.id)
    return [
        MessageResponse(
            id=msg.id,
            chat_id=msg.chat_id,
            sender_username=msg.sender.username if msg.sender else None,
            content=msg.content,
            sent_at=msg.sent_at,
        )
        for msg in messages
    ]


@router.post("/{chat_id}/messages", response_model=MessageResponse, status_code=201)
async def send_message(
    chat_id: int, request: MessageCreate, db: DbSession, current_user: CurrentUser
):
    """Send a message to a chat."""
    chat_service = ChatService(db)
    msg = await chat_service.send_message(chat_id, request, current_user.id)
    return MessageResponse(
        id=msg.id,
        chat_id=msg.chat_id,
        sender_username=msg.sender.username if msg.sender else None,
        content=msg.content,
        sent_at=msg.sent_at,
    )
