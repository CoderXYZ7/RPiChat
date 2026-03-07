package com.danieletoniolo.Chatastic.api;

import com.danieletoniolo.Chatastic.model.AuthResponse;
import com.danieletoniolo.Chatastic.model.Chat;
import com.danieletoniolo.Chatastic.model.ChatRequest;
import com.danieletoniolo.Chatastic.model.LoginRequest;
import com.danieletoniolo.Chatastic.model.Message;
import com.danieletoniolo.Chatastic.model.RegisterRequest;
import com.danieletoniolo.Chatastic.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Header;

public interface ApiService {
    @POST("/api/auth/register")
    Call<User> register(@Body RegisterRequest request);

    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("/api/users/list")
    Call<List<User>> getUsers(@Header("Authorization") String token);

    @GET("/api/chats")
    Call<List<Chat>> getChats(@Header("Authorization") String token);

    @POST("/api/chats")
    Call<Chat> createChat(@Header("Authorization") String token, @Body ChatRequest request);

    @GET("/api/chats/{chat_id}/messages")
    Call<List<Message>> getMessages(@Header("Authorization") String token, @retrofit2.http.Path("chat_id") int chatId);
}
