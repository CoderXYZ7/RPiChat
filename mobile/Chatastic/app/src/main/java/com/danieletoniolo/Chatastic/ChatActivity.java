package com.danieletoniolo.Chatastic;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danieletoniolo.Chatastic.api.ApiClient;
import com.danieletoniolo.Chatastic.api.ApiService;
import com.danieletoniolo.Chatastic.api.SessionManager;
import com.danieletoniolo.Chatastic.api.WebSocketClient;
import com.danieletoniolo.Chatastic.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView chatTitleText;
    private ImageButton buttonClose;

    private ApiService apiService;
    private SessionManager sessionManager;
    private WebSocketClient webSocketClient;
    private Gson gson = new Gson();

    private int chatId;
    private String chatName;
    private String token;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getIntExtra("CHAT_ID", -1);
        chatName = getIntent().getStringExtra("CHAT_NAME");
        if (chatId == -1) {
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        token = sessionManager.getAuthToken();
        username = sessionManager.getUsername();
        apiService = ApiClient.getClient();
        webSocketClient = new WebSocketClient();

        chatTitleText = findViewById(R.id.chatTitleText);
        chatTitleText.setText(chatName);

        buttonClose = findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> sendMessage());

        // Bottom nav
        ImageButton navChatList = findViewById(R.id.navChatList);
        ImageButton navSettings = findViewById(R.id.navSettings);
        navChatList.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, ChatListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        navSettings.setOnClickListener(v -> startActivity(new Intent(ChatActivity.this, SettingsActivity.class)));

        loadMessages();
        connectWebSocket();
    }

    private void loadMessages() {
        apiService.getMessages(token, chatId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, retrofit2.Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMessages(response.body());
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectWebSocket() {
        // Use saved server IP
        String serverIp = sessionManager.getServerIp();
        if (serverIp == null || serverIp.isEmpty()) {
            serverIp = "10.0.2.2"; // Default fallback
        }

        // Robust parsing: Remove scheme and trailing slash
        String host = serverIp.replace("http://", "").replace("https://", "");
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        // If no port is specified, assume 8000 (default for this app backend)
        // Check for colon NOT at the start (in case of weird input)
        if (!host.contains(":")) {
            host = host + ":8000";
        }

        // Ensure clean token
        String plainToken = token.replace("Bearer ", "").trim();
        String wsUrl = "ws://" + host + "/ws/" + chatId + "?token=" + plainToken;

        android.util.Log.d("ChatActivity", "Connecting to WebSocket: " + wsUrl);

        webSocketClient.connect(wsUrl, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connected", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JsonObject json = gson.fromJson(text, JsonObject.class);
                        if (json.has("type") && json.get("type").getAsString().equals("message")) {
                            Message message = gson.fromJson(json, Message.class);
                            adapter.addMessage(message);
                            scrollToBottom();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                String errorMsg = t.getMessage();
                runOnUiThread(() -> Toast
                        .makeText(ChatActivity.this, "Connection Error: " + errorMsg, Toast.LENGTH_LONG).show());
                t.printStackTrace();
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Closed: " + reason, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty())
            return;

        JsonObject json = new JsonObject();
        json.addProperty("content", content);
        webSocketClient.send(json.toString());
        messageInput.setText("");
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocketClient.close();
    }

    // Inner Adapter Class
    public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        private List<Message> messages = new ArrayList<>();

        public void setMessages(List<Message> messages) {
            this.messages = messages;
            notifyDataSetChanged();
        }

        public void addMessage(Message message) {
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Message message = messages.get(position);
            boolean isMe = message.getSenderUsername() != null && message.getSenderUsername().equals(username);

            holder.contentText.setText(message.getContent());

            // Show timestamp formatted as HH:MM if available
            String sentAt = message.getSentAt();
            if (sentAt != null && sentAt.length() >= 16) {
                holder.timeText.setText(sentAt.substring(11, 16));
            } else {
                holder.timeText.setText(sentAt != null ? sentAt : "");
            }

            if (isMe) {
                holder.container.setBackgroundResource(R.drawable.bg_message_me);
                holder.senderText.setVisibility(View.GONE);
                ((LinearLayout) holder.itemView).setGravity(Gravity.END);
                holder.avatarIncoming.setVisibility(View.GONE);
                holder.avatarOutgoing.setVisibility(View.VISIBLE);
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_message_other);
                holder.senderText.setVisibility(View.VISIBLE);
                holder.senderText.setText(message.getSenderUsername());
                ((LinearLayout) holder.itemView).setGravity(Gravity.START);
                holder.avatarIncoming.setVisibility(View.VISIBLE);
                holder.avatarOutgoing.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView senderText, contentText, timeText;
            LinearLayout container;
            View avatarIncoming, avatarOutgoing;

            public ViewHolder(View itemView) {
                super(itemView);
                senderText = itemView.findViewById(R.id.senderText);
                contentText = itemView.findViewById(R.id.contentText);
                timeText = itemView.findViewById(R.id.timeText);
                container = itemView.findViewById(R.id.messageContainer);
                avatarIncoming = itemView.findViewById(R.id.avatarIncoming);
                avatarOutgoing = itemView.findViewById(R.id.avatarOutgoing);
            }
        }
    }
}
