package com.danieletoniolo.Chatastic;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danieletoniolo.Chatastic.api.ApiClient;
import com.danieletoniolo.Chatastic.api.ApiService;
import com.danieletoniolo.Chatastic.api.SessionManager;
import com.danieletoniolo.Chatastic.model.Chat;
import com.danieletoniolo.Chatastic.model.ChatRequest;
import android.widget.ImageButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private ImageButton buttonClose;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        sessionManager = new SessionManager(this);
        token = sessionManager.getAuthToken();

        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiService = ApiClient.getClient();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        recyclerView.setAdapter(adapter);

        buttonClose = findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(v -> {
            sessionManager.clear();
            startActivity(new Intent(ChatListActivity.this, LoginActivity.class));
            finish();
        });

        loadChats();
    }

    private void loadChats() {
        apiService.getChats(token).enqueue(new Callback<List<Chat>>() {
            @Override
            public void onResponse(Call<List<Chat>> call, Response<List<Chat>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setChats(response.body());
                } else {
                    Toast.makeText(ChatListActivity.this, "Failed to load chats", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Chat>> call, Throwable t) {
                Toast.makeText(ChatListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateChatDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_chat, null);
        TextInputEditText nameInput = view.findViewById(R.id.chatNameInput);
        TextInputEditText participantsInput = view.findViewById(R.id.participantsInput);

        new AlertDialog.Builder(this)
                .setTitle("Create New Chat")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String participantsStr = participantsInput.getText().toString().trim();
                    if (!name.isEmpty() && !participantsStr.isEmpty()) {
                        List<String> participants = Arrays.asList(participantsStr.split(","));
                        createChat(name, participants);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createChat(String name, List<String> participants) {
        ChatRequest request = new ChatRequest(name, participants);
        apiService.createChat(token, request).enqueue(new Callback<Chat>() {
            @Override
            public void onResponse(Call<Chat> call, Response<Chat> response) {
                if (response.isSuccessful()) {
                    loadChats(); // Refresh list
                    Toast.makeText(ChatListActivity.this, "Chat created", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatListActivity.this, "Failed to create chat", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Chat> call, Throwable t) {
                Toast.makeText(ChatListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Inner Adapter Class
    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private List<Chat> chats = new ArrayList<>();

        public void setChats(List<Chat> chats) {
            this.chats = chats;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Chat chat = chats.get(position);
            holder.nameText.setText(chat.getName());
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                intent.putExtra("CHAT_ID", chat.getId());
                intent.putExtra("CHAT_NAME", chat.getName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;

            public ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.chatName);
            }
        }
    }
}
