"""Chat participant association model."""

from datetime import datetime
from sqlalchemy import DateTime, ForeignKey
from typing import TYPE_CHECKING
from sqlalchemy.orm import Mapped, mapped_column, relationship

if TYPE_CHECKING:
    from app.models.chat import Chat
    from app.models.user import User

from app.database import Base


class ChatParticipant(Base):
    """Association table for chat participants."""

    __tablename__ = "chat_participants"

    chat_id: Mapped[int] = mapped_column(ForeignKey("chats.id", ondelete="CASCADE"), primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    joined_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    # Relationships
    chat: Mapped["Chat"] = relationship(back_populates="participants")
    user: Mapped["User"] = relationship(back_populates="chat_participations")

    def __repr__(self) -> str:
        return f"<ChatParticipant(chat_id={self.chat_id}, user_id={self.user_id})>"
