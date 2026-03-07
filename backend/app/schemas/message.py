"""Message schemas."""

from datetime import datetime
from pydantic import BaseModel, Field


class MessageCreate(BaseModel):
    """Create message request body."""

    content: str = Field(..., min_length=1, max_length=4000)


class MessageResponse(BaseModel):
    """Message response."""

    id: int
    chat_id: int
    sender_username: str | None
    content: str
    sent_at: datetime

    class Config:
        from_attributes = True
