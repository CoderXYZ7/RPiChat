# RPiChat Backend

This directory contains the FastAPI-based backend for the RPiChat application.

## Architecture

The backend follows a standard multi-layer architecture to separate concerns, making the code easy to maintain and test.

```text
backend/
├── app/
│   ├── api/         # API Routers (Controllers) - Handle HTTP requests and responses
│   ├── core/        # Core utilities (Security, JWT, Orchestrator)
│   ├── models/      # SQLAlchemy Database ORM Models
│   ├── schemas/     # Pydantic Schemas (Data validation & serialization)
│   ├── services/    # Business Logic layer
│   ├── config.py    # Environment configuration & settings
│   ├── database.py  # SQLAlchemy engine & session setup
│   └── main.py      # FastAPI entry point
├── deploy.sh        # Server deployment script
├── pyproject.toml   # Python project metadata
└── requirements.txt # Python dependencies
```

### 1. API Layer (`app/api/`)
The API routers define the available endpoints (e.g., `/auth`, `/chats`, `/users`, `/ws`). Their main job is to accept HTTP requests, validate input via Pydantic schemas, pass the data to the appropriate *Service*, and return the result. They contain minimal business logic.

### 2. Business Logic Layer (`app/services/`)
This layer handles the core functionality. For example, `services/chat.py` contains the logic for creating a chat, verifying if a user can join, and retrieving messages. Abstracting this away from the API layer allows you to call these services from other places (like background tasks) without needing an HTTP request.

### 3. Database Models (`app/models/`)
These are SQLAlchemy classes that define the structure of the database tables (`users`, `chats`, `messages`, `chat_participants`) and their relationships. 

### 4. Data Validation (`app/schemas/`)
Pydantic models are used to rigorously type-check incoming JSON bodies and format outgoing responses. For instance, `UserCreate` ensures a password and username are provided, while `UserResponse` strips out the password hash before sending the user data back to the client.

### 5. WebSockets (`app/api/websocket.py` & `app/core/orchestrator.py`)
Real-time communication is handled through WebSockets. When the mobile app sends a message, it traverses through `api/chats.py` into the `core/orchestrator.py`, which is responsible for taking the message and broadcasting it live to any connected WebSocket clients associated with that specific chat room.

## Detailed File Breakdown

### `/backend/` Scope
- **`deploy.sh`**: The management script used to start the server via Uvicorn (with `nohup`), stop the server safely by its written PID, restart, and create/manage the python `.venv`. 
- **`pyproject.toml`** and **`requirements.txt`**: Package management files containing the necessary FastAPI and SQLAlchemy dependencies for this project.
- **`rpichat.db`**: The local SQLite database storing all persistent state.

### `app/` Directory Main Components
- **`main.py`**: The application's entry point where the FastAPI instance is instantiated, CORS is configured, and all routers (`@app.include_router()`) are mounted.
- **`database.py`**: Configures the `aiosqlite` asynchronous engine and `sessionmaker` mapped to the SQLite database.
- **`config.py`**: Uses `pydantic-settings` to load and validate environment variables into a structured configuration class (like the JWT `secret_key` and server port).

### `app/api/` (Routers)
- **`auth.py`**: Exposes `/api/auth/register` and `/api/auth/login` to give users access tokens. 
- **`chats.py`**: Handles all the group chat and direct message REST APIs (e.g., retrieving history, posting new messages to an inactive chat room). 
- **`users.py`**: Provides endpoints for searching users and looking up usernames.
- **`websocket.py`**: Exposes the single `/ws/{token}/{chat_id}` endpoint that the mobile app maintains an active connection to for live incoming/outgoing messaging.

### `app/core/` (Under-the-hood Utilities)
- **`exceptions.py`**: Standardized HTTP exceptions (401 Unauthorized, 404 Not Found, 403 Forbidden) so that error returns are uniform. 
- **`security.py`**: A specialized file that uses `bcrypt` to hash/verify passwords and `PyJWT` to encode/decode user access tokens.
- **`orchestrator.py`**: The "brain" of real-time communication. As WebSocket sessions open, it stores them in-memory. When a user sends a message, the orchestrator loops over active socket connections in that room and instantly relays the new message.

### `app/models/` (SQLAlchemy Entities)
- **`user.py`**: The `User` table representation, storing usernames and securely hashed passwords.
- **`chat.py`**: The `Chat` room representation (public flag, created date, etc). 
- **`message.py`**: Represents a single sent message, holding the sender's ID, the chat ID, and the text payload.
- **`participant.py`**: The pivot/associative table handling the many-to-many relationship mapping *which Users* are registered inside *which Chats*.

### `app/schemas/` (Pydantic Models)
- **`auth.py`**: defines the `Token` return type and `UserLogin` POST bodies.
- **`chat.py`**: defines responses like `ChatListResponse` and inputs like `ChatCreate`.
- **`message.py`**: defines outgoing formats such as `MessageResponse`.
- **`user.py`**: defines shapes for users (stripping `password_hash` to ensure it never leaks via JSON output).

### `app/services/` (Services layer)
- **`auth.py`**: Contains the robust business logic to register a new user or authenticate an existing one before handing success back to `api/auth.py`. 
- **`chat.py`**: Controls database insertions and verifications for rooms (such as checking if User A is allowed to read Chat B's history) and storing new message texts.

## Running the Server

Because the environment relies on specific dependencies, you should use the provided bash script to run the server.

From this directory (`backend/`):

- **Start:** `./deploy.sh start`
- **Stop:** `./deploy.sh stop` 
- **Restart:** `./deploy.sh restart`
- **Status:** `./deploy.sh status`

By default, the API will run on `http://0.0.0.0:8000`. You can test endpoints interactively by going to `http://<your-ip>:8000/docs` in a web browser.
