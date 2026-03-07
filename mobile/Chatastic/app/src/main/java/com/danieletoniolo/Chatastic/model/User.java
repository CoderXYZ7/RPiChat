package com.danieletoniolo.Chatastic.model;

public class User {
    private String username;
    private boolean is_admin;

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return is_admin;
    }
}
