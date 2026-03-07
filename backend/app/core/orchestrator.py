"""WebSocket connection manager (Orchestrator)."""

from fastapi import WebSocket
from typing import Any
import json


class ConnectionManager:
    """
    Manages WebSocket connections for real-time messaging.
    
    This is the Orchestrator component that handles:
    - Connection lifecycle management
    - Message broadcasting to chat participants
    - User-specific messaging
    """

    def __init__(self):
        # chat_id -> list of (user_id, websocket) tuples
        self.active_connections: dict[int, list[tuple[int, WebSocket]]] = {}

    async def connect(self, websocket: WebSocket, chat_id: int, user_id: int) -> None:
        """Accept a WebSocket connection and register it for a chat."""
        await websocket.accept()
        if chat_id not in self.active_connections:
            self.active_connections[chat_id] = []
        self.active_connections[chat_id].append((user_id, websocket))

    def disconnect(self, websocket: WebSocket, chat_id: int, user_id: int) -> None:
        """Remove a WebSocket connection."""
        if chat_id in self.active_connections:
            self.active_connections[chat_id] = [
                (uid, ws) for uid, ws in self.active_connections[chat_id]
                if ws != websocket
            ]
            # Clean up empty chat rooms
            if not self.active_connections[chat_id]:
                del self.active_connections[chat_id]

    async def broadcast(self, chat_id: int, message: dict[str, Any], exclude_user: int | None = None) -> None:
        """Broadcast a message to all connected users in a chat."""
        if chat_id not in self.active_connections:
            return
        
        message_json = json.dumps(message)
        disconnected = []
        
        for user_id, websocket in self.active_connections[chat_id]:
            if exclude_user is not None and user_id == exclude_user:
                continue
            try:
                await websocket.send_text(message_json)
            except Exception:
                disconnected.append((user_id, websocket))
        
        # Clean up disconnected clients
        for uid, ws in disconnected:
            self.disconnect(ws, chat_id, uid)

    async def send_personal(self, websocket: WebSocket, message: dict[str, Any]) -> None:
        """Send a message to a specific WebSocket connection."""
        await websocket.send_text(json.dumps(message))

    def get_user_count(self, chat_id: int) -> int:
        """Get the number of connected users in a chat."""
        return len(self.active_connections.get(chat_id, []))


# Global connection manager instance
manager = ConnectionManager()
