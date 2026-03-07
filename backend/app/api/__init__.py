"""API routes."""

from app.api.deps import get_current_user
from app.api import auth, users, chats, websocket

__all__ = ["get_current_user", "auth", "users", "chats", "websocket"]
