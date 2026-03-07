package com.danieletoniolo.Chatastic.api;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "ChatasticSession";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_USERNAME = "username";

    private static final String KEY_SERVER_IP = "server_ip";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public void saveServerIp(String ip) {
        prefs.edit().putString(KEY_SERVER_IP, ip).apply();
    }

    public String getServerIp() {
        return prefs.getString(KEY_SERVER_IP, "10.0.2.2");
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
