"""Authentication service (Account Manager)."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import User
from app.core.security import verify_password, get_password_hash, create_access_token
from app.core.exceptions import AuthException
from app.schemas import RegisterRequest, LoginRequest, TokenResponse


class AuthService:
    """Handles user authentication and registration."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def register(self, request: RegisterRequest) -> User:
        """Register a new user."""
        # Check if username exists
        existing = await self.db.execute(
            select(User).where(User.username == request.username)
        )
        if existing.scalar_one_or_none():
            raise AuthException(detail="Username already registered")

        # Create user
        user = User(
            username=request.username,
            password_hash=get_password_hash(request.password),
        )
        self.db.add(user)
        await self.db.commit()
        await self.db.refresh(user)
        return user

    async def login(self, request: LoginRequest) -> TokenResponse:
        """Authenticate user and return JWT token."""
        result = await self.db.execute(
            select(User).where(User.username == request.username)
        )
        user = result.scalar_one_or_none()

        if not user or not verify_password(request.password, user.password_hash):
            raise AuthException(detail="Invalid username or password")

        token = create_access_token(data={
            "sub": user.username,
            "user_id": user.id,
            "is_admin": user.is_admin
        })
        return TokenResponse(access_token=token)

    async def get_user_by_username(self, username: str) -> User | None:
        """Get user by username."""
        result = await self.db.execute(
            select(User).where(User.username == username)
        )
        return result.scalar_one_or_none()

    async def get_user_by_id(self, user_id: int) -> User | None:
        """Get user by ID."""
        result = await self.db.execute(
            select(User).where(User.id == user_id)
        )
        return result.scalar_one_or_none()

    async def search_users(self, query: str) -> list[User]:
        """Search users by username prefix."""
        result = await self.db.execute(
            select(User).where(User.username.ilike(f"{query}%")).limit(20)
        )
        return list(result.scalars().all())

    async def get_all_users(self) -> list[User]:
        """Get all users."""
        result = await self.db.execute(select(User))
        return list(result.scalars().all())
