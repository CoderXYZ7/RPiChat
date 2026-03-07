package com.danieletoniolo.Chatastic.model;

import java.util.List;

public class ChatRequest {
    private String name;
    private List<String> participants;

    public ChatRequest(String name, List<String> participants) {
        this.name = name;
        this.participants = participants;
    }
}
