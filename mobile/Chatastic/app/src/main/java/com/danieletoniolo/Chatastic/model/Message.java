package com.danieletoniolo.Chatastic.model;

import com.google.gson.annotations.SerializedName;

public class Message {
    private int id;
    private int chat_id;
    @SerializedName("sender_username")
    private String sender_username;
    @SerializedName("sender")
    private String sender;
    private String content;
    private String sent_at;
    private String type; // For WS messages: "message", "user_joined", "user_left"

    public int getId() {
        return id;
    }

    public String getSenderUsername() {
        return sender_username != null ? sender_username : sender;
    }

    public String getContent() {
        return content;
    }

    public String getSentAt() {
        return sent_at;
    }

    public String getType() {
        return type;
    }
}
