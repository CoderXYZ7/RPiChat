package com.danieletoniolo.Chatastic;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.ImageButton;
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
    private Button buttonLogin, buttonRegister;
    private ImageButton togglePassword;
    private boolean passwordVisible = false;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Check if already logged in
        if (sessionManager.getAuthToken() != null) {
            String savedIp = sessionManager.getServerIp();
            ApiClient.setBaseUrl("http://" + savedIp + ":8000/");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        serverIpInput = findViewById(R.id.serverIpInput);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        togglePassword = findViewById(R.id.togglePassword);

        serverIpInput.setText(sessionManager.getServerIp());

        buttonLogin.setOnClickListener(v -> handleLogin());
        buttonRegister.setOnClickListener(v -> handleRegister());

        togglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // Keep cursor at end
            passwordInput.setSelection(passwordInput.getText() != null ? passwordInput.getText().length() : 0);
        });
    }

    private boolean validateInputs() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String ip = serverIpInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || ip.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        ApiClient.setBaseUrl("http://" + ip + ":8000/");
        apiService = ApiClient.getClient();
        sessionManager.saveServerIp(ip);
        return true;
    }

    private void handleLogin() {
        if (!validateInputs()) return;
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        LoginRequest request = new LoginRequest(username, password);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveAuthToken("Bearer " + response.body().getAccessToken());
                    sessionManager.saveUsername(username);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRegister() {
        if (!validateInputs()) return;
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        RegisterRequest request = new RegisterRequest(username, password);
        apiService.register(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Registered! Please log in.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
