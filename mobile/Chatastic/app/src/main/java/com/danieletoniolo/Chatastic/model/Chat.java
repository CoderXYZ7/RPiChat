package com.danieletoniolo.Chatastic.model;

import java.util.List;

public class Chat {
    private int id;
    private String name;
    private List<User> participants;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<User> getParticipants() {
        return participants;
    }
}
