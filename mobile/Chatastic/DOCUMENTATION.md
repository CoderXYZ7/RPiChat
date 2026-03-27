# Chatastic — Mobile App Documentation

Full explanation of the Android client from design, logic, and development perspectives.

---

## Table of Contents

1. [Project at a Glance](#1-project-at-a-glance)
2. [Design Perspective](#2-design-perspective)
   - 2.1 Visual Language
   - 2.2 Screen-by-Screen Breakdown
   - 2.3 Component Library
3. [Logic Perspective](#3-logic-perspective)
   - 3.1 Activity Flow & Navigation
   - 3.2 Authentication Flow
   - 3.3 Chat List Flow
   - 3.4 Real-time Messaging Flow
   - 3.5 Data Models
4. [Development Perspective](#4-development-perspective)
   - 4.1 Project Structure
   - 4.2 Build Configuration
   - 4.3 Dependencies
   - 4.4 API Layer
   - 4.5 Session Management
   - 4.6 WebSocket Client
   - 4.7 RecyclerView Adapters
5. [Communication Protocol](#5-communication-protocol)
   - 5.1 REST Endpoints
   - 5.2 WebSocket Protocol

---

## 1. Project at a Glance

**App name:** Chatastic
**Package:** `com.danieletoniolo.Chatastic`
**Language:** Java (source); Kotlin DSL for Gradle build files
**Min SDK:** 24 (Android 7.0)
**Target/Compile SDK:** 35 (Android 15)
**Java compatibility:** 1.8

Chatastic is the Android client for the RPiChat system. It connects to a self-hosted FastAPI backend (typically running on a Raspberry Pi) over HTTP for REST calls and WebSocket for live messaging.

---

## 2. Design Perspective

### 2.1 Visual Language

The app follows a **brutalist dark aesthetic**: high-contrast black/white on dark grey backgrounds, square/rectangular shapes, bold typography, and no decorative elements. This keeps the UI legible on small screens and intentionally avoids the rounded, colourful conventions of mainstream messaging apps.

#### Colour palette (`res/values/colors.xml`)

| Name | Hex | Usage |
|------|-----|-------|
| `dark_bg` | `#303030` | Main screen background |
| `darker_bg` | `#212121` | Message input bar background |
| `white` | `#FFFFFF` | Text, icons, borders, avatars |
| `black` | `#000000` | Text on light bubbles |
| `light_gray` | `#BDBDBD` | Default message bubble fill |
| `mid_gray` | `#757575` | Placeholder/hint text |

Message bubbles override the default grey:
- Outgoing (me): `#DCF8C6` — soft green
- Incoming (other): `#FFFFFF` with a `#E0E0E0` border

#### Typography

All text is rendered in the system default font. Sizes used:
- Screen titles: `24sp`
- Input fields / action buttons: `20sp`
- Chat/message body text: `16–18sp`
- Sender label: `11sp` bold
- Timestamp: `10sp`

#### Theme (`res/values/themes.xml`)

The theme extends `Theme.AppCompat.NoActionBar` — no system action bar is shown on any screen. The window background defaults to `dark_bg`. All drawables and colours are set to match the dark palette.

---

### 2.2 Screen-by-Screen Breakdown

#### Login Screen (`activity_login.xml`)

```
┌─────────────────────────────┐
│                             │
│    [A New Way to Chat img]  │
│                             │
│  ________________________   │
│  Username                   │
│  ________________________   │
│  Password             [👁]  │
│  ________________________   │
│  Server url                 │
│                             │
│  [ LOGIN ]   [ REGISTER ]   │
└─────────────────────────────┘
```

- Background: wavy blob PNG (`bg_login.png`) sourced from the Haikei design tool.
- Title: PNG image (`title_login.png`) containing the "A New Way to Chat" lettering.
- Three input fields with a white bottom-underline border (`bg_input_underline.xml`), no visible box.
- Password field has an eye (`ic_eye`) `ImageButton` overlaid in the right end of a `RelativeLayout` wrapper; tapping it toggles `PasswordTransformationMethod` / `HideReturnsTransformationMethod`.
- Two action buttons side by side, styled with `bg_button_square.xml` (transparent fill, 3dp white stroke).

#### Chat List Screen (`activity_chat_list.xml`)

```
┌─────────────────────────────┐
│     [✕ logo]          [+]   │  ← 64dp top bar
│ ┌─────────────────────────┐ │
│ │  Search                 │ │  ← rounded white input
│ └─────────────────────────┘ │
│                             │
│ [▪] Chat Name               │  ← RecyclerView
│     Latest Message          │
│ [▪] Chat Name               │
│     Latest Message          │
│           ...               │
│                             │
│ [≡]      [ⓘ]      [⚙]      │  ← 64dp bottom nav
└─────────────────────────────┘
```

- Dark background (`dark_bg`).
- Top bar: the centred `✕` icon is a non-navigating brand logo. The `+` button in the top-right opens the "Create New Chat" dialog.
- Search bar: white fill with 4dp rounded corners (`bg_search.xml`), black text. Currently a visual-only element — filtering is not implemented.
- Each chat row (item_chat.xml): 56×56dp white square avatar + chat name in 20sp white + "Latest Message" subtitle in 14sp mid-grey.
- Bottom nav (`layout_bottom_nav.xml`): three equal-weight `ImageButton`s for Chat List (disabled/dimmed on this screen), Info, and Settings.

#### Chat Screen (`activity_chat.xml`)

```
┌─────────────────────────────┐
│ [✕]  Chat Name              │  ← 64dp top bar
│                             │
│         [▪] Hello!          │  ← incoming msg
│             Alice    12:30  │
│                             │
│  Hi there! [▪]              │  ← outgoing msg
│  12:31                      │
│                             │
│ [ Type a message...  ] [➤] │  ← input + send
│ [≡]      [ⓘ]      [⚙]      │  ← bottom nav
└─────────────────────────────┘
```

- `✕` button closes the activity (returns to the previous screen in the back-stack).
- Messages in `RecyclerView` with two bubble styles. Bubble width is `wrap_content` with a `maxWidth` of `280dp` — short messages stay compact; the `Gravity` of the parent `LinearLayout` is set per-item to `END` or `START` to align bubbles.
- Incoming: white bubble with border, 40×40dp white avatar on the left, sender name in 11sp bold above the content, timestamp bottom-right.
- Outgoing: green bubble, 40×40dp white avatar on the right, no sender label, timestamp bottom-right.
- Input bar has a dark background (`darker_bg`) and sits above the bottom nav.
- Send button (`ic_send`) styled with the same `bg_button_square` outline.

#### Settings Screen (`activity_settings.xml`)

```
┌─────────────────────────────┐
│ [✕]  Settings               │  ← 64dp top bar
│                             │
│  (wavy layered background)  │
│                             │
│  ________________________   │
│  Server url       [~65% ↓]  │
│                             │
│  [ LOG OUT ]  [ IP CHANGE ] │
│ [≡]      [ⓘ]      [⚙]      │
└─────────────────────────────┘
```

- Background: layered waves PNG (`bg_settings.png`).
- `✕` button goes back.
- Server URL input vertically positioned at 65% of the space between the top bar and the bottom nav (using `constraintVertical_bias="0.65"`), so it sits in the lower half of the screen as shown in the reference design.
- Two side-by-side buttons: **Log out** clears the session and goes to `LoginActivity`; **Ip change** saves the new IP and rebuilds the Retrofit client.
- Bottom nav: Settings icon is dimmed/disabled on this screen.

---

### 2.3 Component Library

#### `bg_button_square.xml`
Transparent rectangle with a 3dp white stroke. Used for all action buttons (Login, Register, Log out, Ip change, Send).

#### `bg_input_underline.xml`
Layer-list that shows only a bottom border (white, 2dp) by using negative left/right/top offsets. Used on all text inputs.

#### `bg_search.xml`
Solid white rectangle with 4dp corner radius. Used on the search bar in Chat List.

#### `bg_message_me.xml`
Light green (`#DCF8C6`) rectangle, rounded on three corners (top-left, bottom-left, bottom-right = 16dp; top-right = 0dp).

#### `bg_message_other.xml`
White rectangle with a 1dp grey border, rounded on three corners (top-right, bottom-left, bottom-right = 16dp; top-left = 0dp).

#### `layout_bottom_nav.xml`
Horizontal `LinearLayout` of height 64dp with background `#202020`. Three equal-weight `ImageButton` children using `ic_chat_list`, `ic_info`, and `ic_settings`. The active screen disables and dims its own icon programmatically.

---

## 3. Logic Perspective

### 3.1 Activity Flow & Navigation

```
[App Launch]
     │
     ▼
LoginActivity ──(token exists)──► MainActivity ──► ChatListActivity
     │                                                    │
     │(login success)                             [tap chat row]
     ▼                                                    │
MainActivity ──────────────────────────────────► ChatListActivity
                                                          │
                                            [tap chat]   │
                                                   ▼     │
                                             ChatActivity │
                                                          │
                               [nav: settings] ◄──────────┘
                                       │
                                 SettingsActivity
```

- `MainActivity` is a transparent relay: it immediately starts `ChatListActivity` and calls `finish()`. Its only purpose is to be the post-login target, separating login routing from the chat list.
- All navigation uses explicit `Intent`s. There is no fragment-based navigation or NavGraph.
- Back-stack: `ChatActivity` → back → `ChatListActivity`. `SettingsActivity` is layered on top of whatever called it.
- Bottom nav in `ChatActivity` uses `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` when going back to `ChatListActivity`, preventing duplicate instances from stacking.

### 3.2 Authentication Flow

```
User enters username + password + server IP
        │
        ▼
validateInputs()
  ├── empty check → Toast + abort
  └── ApiClient.setBaseUrl("http://<ip>:8000/")  ← rebuilds Retrofit
        │
        ▼
  handleLogin()                   handleRegister()
  POST /api/auth/login            POST /api/auth/register
        │                               │
  200 OK → AuthResponse           200 OK → User
  saveAuthToken("Bearer " + token)      │
  saveUsername(username)          Toast "Registered! Please log in."
        │
        ▼
  startActivity(MainActivity)
  finish()
```

- The server IP is stored without scheme in `SessionManager`. On app restart, `LoginActivity.onCreate()` reads it and calls `ApiClient.setBaseUrl()` before routing to `ChatListActivity`, so the Retrofit instance is always pointing at the correct server.
- The token is stored as the full `Authorization` header value (`"Bearer <jwt>"`), so it can be passed directly to `@Header("Authorization")` in `ApiService` calls without any further manipulation.
- Registration does **not** auto-login — the user must press Login after registering.

### 3.3 Chat List Flow

```
ChatListActivity.onCreate()
  │
  ├── Check token → null → redirect to LoginActivity
  │
  ├── GET /api/chats (with token)
  │     └── adapter.setChats(list) → RecyclerView renders
  │
  ├── buttonNewChat → showCreateChatDialog()
  │     ├── user fills name + comma-separated participants
  │     └── POST /api/chats → on success → loadChats() (refresh)
  │
  └── navSettings → startActivity(SettingsActivity)
```

- The search `EditText` is a UI-only element — there is no listener attached to it. It does not filter the list.
- `ChatAdapter` is an inner class of `ChatListActivity`. Each row click passes `CHAT_ID` and `CHAT_NAME` as intent extras to `ChatActivity`.

### 3.4 Real-time Messaging Flow

```
ChatActivity.onCreate()
  │
  ├── loadMessages()
  │     GET /api/chats/{chatId}/messages
  │     └── adapter.setMessages(history) → scrollToBottom()
  │
  └── connectWebSocket()
        │
        URL: ws://<host>:8000/ws/<chatId>?token=<jwt>
        │
        onOpen  → Toast "Connected"
        onMessage → parse JSON
          │
          ├── type == "message" → Message msg = gson.fromJson(...)
          │     adapter.addMessage(msg)
          │     scrollToBottom()
          │
          └── other types (user_joined, etc.) → ignored
        │
        onFailure → Toast error
        onClosing → webSocket.close(1000, null)

sendMessage() [send button]
  │
  content = messageInput.getText()
  json = { "content": "<text>" }
  webSocketClient.send(json.toString())
  messageInput.setText("")
```

**Important dual-source handling in `Message.java`:**
The REST history endpoint returns messages with a `sender_username` field; the WebSocket broadcast uses `sender` instead. The `Message` model uses `@SerializedName` on both fields and `getSenderUsername()` returns whichever is non-null, so the `MessageAdapter` works identically for both sources.

**Message rendering logic (in `onBindViewHolder`):**
```
isMe = getSenderUsername().equals(sessionManager.getUsername())

if isMe:
    background = bg_message_me (green)
    senderText = GONE
    gravity = END (right-align)
    avatarIncoming = GONE, avatarOutgoing = VISIBLE

else:
    background = bg_message_other (white)
    senderText = VISIBLE (set to sender name)
    gravity = START (left-align)
    avatarIncoming = VISIBLE, avatarOutgoing = GONE
```

**Timestamp formatting:** ISO-8601 strings from the server (e.g. `"2026-03-27T14:30:00"`) are sliced at characters 11–16 to extract `HH:MM`.

### 3.5 Data Models

All model classes are plain POJOs in `model/`. Gson uses field names for deserialization unless overridden by `@SerializedName`.

| Class | Fields | Notes |
|-------|--------|-------|
| `User` | `username`, `is_admin` | Returned from register and user-list endpoints |
| `AuthResponse` | `access_token`, `token_type` | Returned from login |
| `Chat` | `id`, `name`, `participants` (List\<String\>) | |
| `Message` | `id`, `chat_id`, `sender_username` (@SerializedName), `sender` (@SerializedName), `content`, `sent_at`, `type` | Dual-field for REST vs WS |
| `LoginRequest` | `username`, `password` | Sent to `/api/auth/login` |
| `RegisterRequest` | `username`, `password` | Sent to `/api/auth/register` |
| `ChatRequest` | `name`, `participants` (List\<String\>) | Sent to `POST /api/chats` |
| `MessageRequest` | `content` | Defined but unused — messages are sent via WebSocket JSON, not REST |

---

## 4. Development Perspective

### 4.1 Project Structure

```
mobile/Chatastic/
├── app/
│   ├── src/main/
│   │   ├── java/com/danieletoniolo/Chatastic/
│   │   │   ├── MainActivity.java          ← splash/redirect
│   │   │   ├── LoginActivity.java         ← login + register
│   │   │   ├── ChatListActivity.java      ← chat list + inner ChatAdapter
│   │   │   ├── ChatActivity.java          ← chat view + inner MessageAdapter
│   │   │   ├── SettingsActivity.java      ← server IP + logout
│   │   │   ├── api/
│   │   │   │   ├── ApiClient.java         ← Retrofit singleton builder
│   │   │   │   ├── ApiService.java        ← Retrofit interface (REST endpoints)
│   │   │   │   ├── SessionManager.java    ← SharedPreferences wrapper
│   │   │   │   └── WebSocketClient.java   ← OkHttp WebSocket wrapper
│   │   │   └── model/
│   │   │       ├── User.java
│   │   │       ├── AuthResponse.java
│   │   │       ├── Chat.java
│   │   │       ├── Message.java
│   │   │       ├── LoginRequest.java
│   │   │       ├── RegisterRequest.java
│   │   │       ├── ChatRequest.java
│   │   │       └── MessageRequest.java
│   │   ├── res/
│   │   │   ├── layout/                    ← XML screen and item layouts
│   │   │   ├── drawable/                  ← Vector icons + shape backgrounds + PNGs
│   │   │   └── values/                    ← colors.xml, strings.xml, themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

### 4.2 Build Configuration

**`app/build.gradle.kts`** (key settings):

```kotlin
compileSdk = 35
minSdk = 24
targetSdk = 35

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```

Build commands (run from `mobile/Chatastic/`):

```bash
./gradlew assembleDebug            # Build debug APK → app/build/outputs/apk/debug/
./gradlew installDebug             # Build + install on connected device/emulator
./gradlew test                     # Run unit tests
./gradlew connectedAndroidTest     # Run instrumented tests (requires device/emulator)
```

**`AndroidManifest.xml`** — key declarations:

```xml
<uses-permission android:name="android.permission.INTERNET" />

<!-- Allows plain HTTP (needed for local Raspberry Pi without TLS) -->
<application android:usesCleartextTraffic="true" ...>

<!-- Entry point -->
<activity android:name=".LoginActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### 4.3 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Retrofit | 2.9.0 | HTTP client with annotation-based API definition |
| OkHttp | 4.12.0 | Underlying HTTP client; also used directly for WebSocket |
| OkHttp Logging Interceptor | 4.12.0 | Logs HTTP request/response bodies (debug only) |
| Gson | 2.10.1 | JSON serialisation/deserialisation |
| Gson Converter (Retrofit) | 2.9.0 | Bridges Retrofit responses to Gson |
| Material Design | 1.11.0 | `TextInputEditText`, `AlertDialog.Builder` themes |
| ConstraintLayout | 2.1.4 | Flexible XML layouts |
| AppCompat | — | `AppCompatActivity` base class |

### 4.4 API Layer

#### `ApiClient.java`

A static singleton that holds a `Retrofit` instance. The base URL is mutable — calling `setBaseUrl()` nulls the existing instance, forcing a rebuild on the next `getClient()` call. This is how the app supports dynamic server addresses.

```java
// Force a new Retrofit instance pointing at a different server
ApiClient.setBaseUrl("http://192.168.1.50:8000/");
ApiService service = ApiClient.getClient();
```

The OkHttp client attached to Retrofit has a `BODY`-level logging interceptor. In production this should be downgraded to `NONE` or gated behind `BuildConfig.DEBUG`.

Default base URL is `http://10.0.2.2:8000/` — the AVD (emulator) alias for `localhost` on the host machine, which points at a backend running on the development PC.

#### `ApiService.java`

Retrofit interface declaring all REST calls:

```java
@POST("/api/auth/register")   Call<User>         register(@Body RegisterRequest);
@POST("/api/auth/login")      Call<AuthResponse> login(@Body LoginRequest);
@GET("/api/users/list")       Call<List<User>>   getUsers(@Header("Authorization") String);
@GET("/api/chats")            Call<List<Chat>>   getChats(@Header("Authorization") String);
@POST("/api/chats")           Call<Chat>         createChat(@Header("Authorization") String, @Body ChatRequest);
@GET("/api/chats/{chat_id}/messages") Call<List<Message>> getMessages(@Header("Authorization") String, @Path("chat_id") int);
```

All authenticated endpoints receive the token as the full `Authorization` header string (`"Bearer <jwt>"`).

All calls are executed asynchronously with `.enqueue(Callback<T>)` on the calling thread's Looper; Retrofit dispatches the callback on the main thread.

### 4.5 Session Management

`SessionManager` wraps a `SharedPreferences` file named `"ChatasticSession"`. It is the single source of truth for the logged-in state.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `access_token` | String | null | Full `"Bearer <jwt>"` value |
| `username` | String | null | Logged-in user's username |
| `server_ip` | String | `"10.0.2.2"` | IP (no scheme, no port) of the backend |

`clear()` removes all keys — this is the logout mechanism called from `SettingsActivity`.

The server IP is stored **without** scheme or port. The scheme (`http://`) and port (`:8000`) are always appended programmatically when constructing URLs, keeping the stored value clean and reusable for both HTTP (`ApiClient`) and WebSocket (`connectWebSocket()`) connection strings.

### 4.6 WebSocket Client

`WebSocketClient` is a thin wrapper around OkHttp's `WebSocket` API. It holds a single socket instance and exposes three methods:

```java
connect(String url, WebSocketListener listener)  // Opens the connection
send(String text)                                // Sends a text frame
close()                                          // Closes with code 1000
```

The listener is implemented inline in `ChatActivity.connectWebSocket()`. All listener callbacks (except `onMessage` text parsing) are dispatched to the main thread via `runOnUiThread()`.

The WebSocket URL is built at connection time:

```java
// serverIp = "192.168.1.50" (from SessionManager)
// token    = "Bearer eyJ..." (from SessionManager, prefix stripped)
"ws://192.168.1.50:8000/ws/<chatId>?token=<jwt>"
```

The socket is closed in `ChatActivity.onDestroy()`, preventing leaks when the activity is destroyed.

### 4.7 RecyclerView Adapters

Both adapters are **inner classes** of their parent Activity, giving them direct access to the activity's fields (token, username, context) without needing constructor injection.

#### `ChatAdapter` (inside `ChatListActivity`)

- `setChats(List<Chat>)` replaces the entire list and calls `notifyDataSetChanged()`.
- Each item view inflates `item_chat.xml`. The `ViewHolder` holds only `chatName` (TextView).
- Click listener on each row starts `ChatActivity` with `CHAT_ID` and `CHAT_NAME` extras.

#### `MessageAdapter` (inside `ChatActivity`)

- `setMessages(List<Message>)` — loads message history (called after REST fetch).
- `addMessage(Message)` — appends a single new message (called per WebSocket event) and calls `notifyItemInserted()` for efficient incremental updates.
- `onBindViewHolder` applies all visual differentiation (bubble colour, gravity, avatar visibility, sender label) for each message.

---

## 5. Communication Protocol

### 5.1 REST Endpoints

All HTTP calls go to `http://<server-ip>:8000`.

| Method | Path | Auth | Request body | Response |
|--------|------|------|-------------|----------|
| POST | `/api/auth/register` | No | `{username, password}` | `User` |
| POST | `/api/auth/login` | No | `{username, password}` | `{access_token, token_type}` |
| GET | `/api/users/list` | Bearer | — | `List<User>` |
| GET | `/api/chats` | Bearer | — | `List<Chat>` |
| POST | `/api/chats` | Bearer | `{name, participants[]}` | `Chat` |
| GET | `/api/chats/{id}/messages` | Bearer | — | `List<Message>` |

### 5.2 WebSocket Protocol

**Connection URL:** `ws://<host>:8000/ws/<chat_id>?token=<jwt>`

The JWT is passed as a query parameter because WebSocket handshakes cannot carry custom headers in browser/Android clients.

**Incoming frame (server → client):**

```json
{
  "type": "message",
  "id": 42,
  "sender": "alice",
  "content": "Hello!",
  "sent_at": "2026-03-27T14:30:00"
}
```

`type` can also be `"user_joined"` or `"user_left"` — the client currently ignores these.

**Outgoing frame (client → server):**

```json
{
  "content": "Hello!"
}
```

The backend associates the message with the authenticated user derived from the JWT, so no sender field needs to be sent.

**Field name discrepancy:**
REST responses use `sender_username`; WebSocket broadcasts use `sender`. `Message.java` handles both via `@SerializedName` annotations and a fallback getter.
