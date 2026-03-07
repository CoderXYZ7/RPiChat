"""WebSocket endpoint for real-time messaging."""

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.security import decode_token
from app.core.orchestrator import manager
from app.services import AuthService, ChatService
from app.schemas import MessageCreate

router = APIRouter(tags=["WebSocket"])


@router.websocket("/ws/{chat_id}")
async def websocket_endpoint(
    websocket: WebSocket,
    chat_id: int,
    token: str = Query(...),
    db: AsyncSession = Depends(get_db),
):
    """
    WebSocket endpoint for real-time chat messaging.
    
    Connect with: ws://host/ws/{chat_id}?token={jwt_token}
    
    Messages are JSON with format:
    - Incoming (from client): {"content": "message text"}
    - Outgoing (to client): {"type": "message", "id": 1, "sender": "username", "content": "text", "sent_at": "..."}
    """
    # Validate token
    payload = decode_token(token)
    if not payload:
        await websocket.close(code=4001, reason="Invalid token")
        return

    username = payload.get("sub")
    user_id = payload.get("user_id")

    if not username or not user_id:
        await websocket.close(code=4001, reason="Invalid token payload")
        return

    # Verify user exists
    auth_service = AuthService(db)
    user = await auth_service.get_user_by_id(user_id)
    if not user:
        await websocket.close(code=4001, reason="User not found")
        return

    # Verify user is participant in chat
    chat_service = ChatService(db)
    if not await chat_service.is_participant(chat_id, user_id):
        await websocket.close(code=4003, reason="Not a participant in this chat")
        return

    # Accept connection
    await manager.connect(websocket, chat_id, user_id)

    try:
        # Notify others of join
        await manager.broadcast(
            chat_id,
            {"type": "user_joined", "username": username},
            exclude_user=user_id,
        )

        while True:
            # Receive message from client
            data = await websocket.receive_json()
            content = data.get("content", "").strip()

            if not content:
                continue

            # Save message to database
            msg = await chat_service.send_message(
                chat_id, MessageCreate(content=content), user_id
            )

            # Broadcast to all participants (including sender for confirmation)
            await manager.broadcast(
                chat_id,
                {
                    "type": "message",
                    "id": msg.id,
                    "sender": username,
                    "content": msg.content,
                    "sent_at": msg.sent_at.isoformat(),
                },
            )

    except WebSocketDisconnect:
        manager.disconnect(websocket, chat_id, user_id)
        await manager.broadcast(
            chat_id,
            {"type": "user_left", "username": username},
        )
