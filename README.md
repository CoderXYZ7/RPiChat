```mermaid
---
config:
  layout: elk
---
flowchart LR
 subgraph Database["Database"]
        chat1["Chat A B"]
        chat2["Chat B C"]
        chat3["Chat A B C"]
        chatp["Public Chat"]
        createchat>"Create New Chat"]
        accountt["Account Table"]
  end
 subgraph Server["Server"]
        orc["Orchestrator"]
        san["Sanificator"]
        api["API"]
        accountm["Account Manager"]
        chatmgr["Chat Manager"]
        Database
  end
    orc --> chatmgr & api & accountm
    chatmgr --> chat1 & chat2 & chat3 & chatp & createchat
    api --> san
    san --> orc
    accountm --> accountt
    device1["User A"] <--> api
    device2["User B"] <--> api
    device3["User C"] <--> api



    orc@{ shape: lean-l}
    san@{ shape: lean-l}
    api@{ shape: rect}
    accountm@{ shape: lean-l}
    chatmgr@{ shape: lean-l}
    device1@{ shape: rounded}
    device2@{ shape: rounded}
    device3@{ shape: rounded}
```

## API Endpoints

### Authentication
- `POST /api/auth/register`: Register a new user.
  - **Body**: `{ "username": "user", "password": "password" }`
- `POST /api/auth/login`: Login and retrieve a token.
  - **Body**: `{ "username": "user", "password": "password" }`
- `GET /api/users/list`: Get all users.
  - **Response**: `[{"username": "user1"}, {"username": "user2"}, ...]`
- `GET /api/users/list/{chat_id}`: Get all users in a chat.
  - **Response**: `[{"username": "user1"}, {"username": "user2"}, ...]`
- `POST /api/users/search`: Search for users.
  - **Body**: `{ "query": "user" }`
  - **Response**: `[{"username": "user1"}, {"username": "user2"}, ...]`

### Chat Management
- `GET /api/chats`: List all chats the user is part of.
- `POST /api/chats`: Create a new chat.
  - **Body**: `{ "name": "Group Chat", "participants": ["user1", "user2"] }`
- `GET /api/chats/{chat_id}`: Get details of a specific chat.

### Chat Editing
- `PATCH /api/chats/{chat_id}`: Update chat details (e.g., name).
  - **Body**: `{ "name": "New Name" }`
- `POST /api/chats/{chat_id}/participants`: Add a participant.
  - **Body**: `{ "username": "new_user" }`
- `DELETE /api/chats/{chat_id}/participants/{username}`: Remove a participant.

### Messaging
- `GET /api/chats/{chat_id}/messages`: Get message history for a chat.
- `POST /api/chats/{chat_id}/messages`: Send a message to a chat.
  - **Body**: `{ "content": "Hello world" }`