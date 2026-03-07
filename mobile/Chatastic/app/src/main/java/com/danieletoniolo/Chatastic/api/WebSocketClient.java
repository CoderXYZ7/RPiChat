package com.danieletoniolo.Chatastic.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient {
    private WebSocket webSocket;
    private OkHttpClient client;

    public WebSocketClient() {
        client = new OkHttpClient();
    }

    public void connect(String url, WebSocketListener listener) {
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, listener);
    }

    public void send(String text) {
        if (webSocket != null) {
            webSocket.send(text);
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
        }
    }
}
