"""User schemas."""

from pydantic import BaseModel, Field



class UserResponse(BaseModel):
    """User response (public info only)."""

    username: str
    is_admin: bool = False

    class Config:
        from_attributes = True


class UserSearchRequest(BaseModel):
    """User search request body."""

    query: str = Field(..., min_length=1, max_length=50)

