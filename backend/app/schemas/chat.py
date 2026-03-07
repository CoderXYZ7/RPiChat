"""Chat schemas."""

from datetime import datetime
from pydantic import BaseModel, Field


class ChatCreate(BaseModel):
    """Create chat request body."""

    name: str | None = Field(None, max_length=100)
    participants: list[str] = Field(default_factory=list)


class ChatUpdate(BaseModel):
    """Update chat request body."""

    name: str | None = Field(None, max_length=100)


class ChatResponse(BaseModel):
    """Chat response."""

    id: int
    name: str | None
    is_public: bool
    created_at: datetime

    class Config:
        from_attributes = True


class ParticipantAdd(BaseModel):
    """Add participant request body."""

    username: str = Field(..., min_length=3, max_length=50)
