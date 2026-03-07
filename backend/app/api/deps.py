"""API dependencies for dependency injection."""

from typing import Annotated

from fastapi import Depends, Header
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.security import decode_token
from app.core.exceptions import AuthException
from app.models import User
from app.services import AuthService


async def get_current_user(
    authorization: Annotated[str | None, Header()] = None,
    db: AsyncSession = Depends(get_db),
) -> User:
    """
    Extract and validate JWT from Authorization header.
    Returns the current authenticated user.
    """
    if not authorization:
        raise AuthException(detail="Authorization header missing")

    # Expect "Bearer <token>"
    parts = authorization.split(" ")
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise AuthException(detail="Invalid authorization header format")

    token = parts[1]
    payload = decode_token(token)

    if not payload:
        raise AuthException(detail="Invalid or expired token")

    username = payload.get("sub")
    if not username:
        raise AuthException(detail="Invalid token payload")

    auth_service = AuthService(db)
    user = await auth_service.get_user_by_username(username)

    if not user:
        raise AuthException(detail="User not found")

    return user


# Type alias for cleaner route signatures
CurrentUser = Annotated[User, Depends(get_current_user)]
DbSession = Annotated[AsyncSession, Depends(get_db)]
