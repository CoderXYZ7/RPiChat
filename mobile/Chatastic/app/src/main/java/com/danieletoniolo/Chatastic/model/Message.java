package com.danieletoniolo.Chatastic.model;

public class Message {
    private int id;
    private int chat_id;
    private String sender_username;
    private String content;
    private String sent_at;
    private String type; // For WS messages: "message", "user_joined", "user_left"

    public int getId() {
        return id;
    }

    public String getSenderUsername() {
        return sender_username;
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
