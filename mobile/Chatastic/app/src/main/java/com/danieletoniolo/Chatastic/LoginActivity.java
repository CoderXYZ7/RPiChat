package com.danieletoniolo.Chatastic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.danieletoniolo.Chatastic.api.ApiClient;
import com.danieletoniolo.Chatastic.api.ApiService;
import com.danieletoniolo.Chatastic.api.SessionManager;
import com.danieletoniolo.Chatastic.model.AuthResponse;
import com.danieletoniolo.Chatastic.model.LoginRequest;
import com.danieletoniolo.Chatastic.model.RegisterRequest;
import com.danieletoniolo.Chatastic.model.User;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, passwordInput, serverIpInput;
    private Button actionButton;
    private TextView toggleText, titleText;
    private boolean isLoginMode = true;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Check if already logged in
        if (sessionManager.getAuthToken() != null) {
            // Ensure client is initialized with saved IP
            String savedIp = sessionManager.getServerIp();
            ApiClient.setBaseUrl("http://" + savedIp + ":8000/");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        serverIpInput = findViewById(R.id.serverIpInput);
        actionButton = findViewById(R.id.actionButton);
        toggleText = findViewById(R.id.toggleText);
        titleText = findViewById(R.id.titleText);

        // Pre-fill IP
        serverIpInput.setText(sessionManager.getServerIp());

        actionButton.setOnClickListener(v -> handleAuth());
        toggleText.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            titleText.setText("Welcome Back");
            actionButton.setText("Login");
            toggleText.setText("Don't have an account? Sign up");
        } else {
            titleText.setText("Create Account");
            actionButton.setText("Register");
            toggleText.setText("Already have an account? Login");
        }
    }

    private void handleAuth() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String ip = serverIpInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || ip.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configure API Client
        ApiClient.setBaseUrl("http://" + ip + ":8000/");
        apiService = ApiClient.getClient();
        sessionManager.saveServerIp(ip);

        if (isLoginMode) {
            login(username, password);
        } else {
            register(username, password);
        }
    }

    private void login(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveAuthToken("Bearer " + response.body().getAccessToken());
                    sessionManager.saveUsername(username);
                    Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    // Navigate to Main
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void register(String username, String password) {
        RegisterRequest request = new RegisterRequest(username, password);
        apiService.register(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Registration Successful! Please Login.", Toast.LENGTH_SHORT)
                            .show();
                    toggleMode(); // Switch to login
                } else {
                    Toast.makeText(LoginActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
