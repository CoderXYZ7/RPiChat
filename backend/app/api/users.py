"""User endpoints."""

from fastapi import APIRouter

from app.api.deps import DbSession, CurrentUser
from app.schemas import UserResponse, UserSearchRequest
from app.services import AuthService, ChatService

router = APIRouter(prefix="/api/users", tags=["Users"])


@router.get("/list", response_model=list[UserResponse])
async def list_users(db: DbSession, current_user: CurrentUser):
    """Get all users."""
    auth_service = AuthService(db)
    users = await auth_service.get_all_users()
    return users


@router.get("/list/{chat_id}", response_model=list[UserResponse])
async def list_chat_users(chat_id: int, db: DbSession, current_user: CurrentUser):
    """Get all users in a specific chat."""
    chat_service = ChatService(db)
    users = await chat_service.get_chat_participants(chat_id, current_user.id)
    return users


@router.post("/search", response_model=list[UserResponse])
async def search_users(request: UserSearchRequest, db: DbSession, current_user: CurrentUser):
    """Search users by username."""
    auth_service = AuthService(db)
    users = await auth_service.search_users(request.query)
    return users
