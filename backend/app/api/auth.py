"""Authentication endpoints."""

from fastapi import APIRouter
from sqlalchemy import select

from app.api.deps import DbSession, CurrentUser
from app.schemas import RegisterRequest, LoginRequest, TokenResponse, UserResponse
from app.services import AuthService
from app.models import User
from app.core.security import get_password_hash
from app.core.exceptions import ForbiddenException

router = APIRouter(prefix="/api/auth", tags=["Authentication"])

# Default admin credentials
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "admin123"


@router.post("/register", response_model=UserResponse, status_code=201)
async def register(request: RegisterRequest, db: DbSession):
    """Register a new user."""
    auth_service = AuthService(db)
    user = await auth_service.register(request)
    return user


@router.post("/login", response_model=TokenResponse)
async def login(request: LoginRequest, db: DbSession):
    """Login and get access token."""
    auth_service = AuthService(db)
    return await auth_service.login(request)


@router.post("/init-admin", response_model=UserResponse, status_code=201)
async def init_admin(db: DbSession):
    """Initialize default admin account if it doesn't exist."""
    result = await db.execute(select(User).where(User.username == ADMIN_USERNAME))
    existing = result.scalar_one_or_none()
    
    if existing:
        return existing
    
    admin = User(
        username=ADMIN_USERNAME,
        password_hash=get_password_hash(ADMIN_PASSWORD),
        is_admin=True
    )
    db.add(admin)
    await db.commit()
    await db.refresh(admin)
    return admin


@router.post("/make-admin/{username}", status_code=204)
async def make_admin(username: str, db: DbSession, current_user: CurrentUser):
    """Make a user an admin (only admins can do this)."""
    if not current_user.is_admin:
        raise ForbiddenException(detail="Only admins can promote users")
    
    result = await db.execute(select(User).where(User.username == username))
    user = result.scalar_one_or_none()
    if not user:
        raise ForbiddenException(detail="User not found")
    
    user.is_admin = True
    await db.commit()

