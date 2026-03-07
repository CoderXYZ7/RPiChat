package com.danieletoniolo.Chatastic;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.danieletoniolo.Chatastic.api.ApiClient;
import com.danieletoniolo.Chatastic.api.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText serverIpInput;
    private Button buttonLogout, buttonSaveIp;
    private ImageButton buttonClose;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sessionManager = new SessionManager(this);

        serverIpInput = findViewById(R.id.serverIpInput);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonSaveIp = findViewById(R.id.buttonSaveIp);
        buttonClose = findViewById(R.id.buttonClose);

        // Pre-fill
        serverIpInput.setText(sessionManager.getServerIp());

        buttonClose.setOnClickListener(v -> finish());

        buttonSaveIp.setOnClickListener(v -> {
            String ip = serverIpInput.getText().toString().trim();
            if (!ip.isEmpty()) {
                sessionManager.saveServerIp(ip);
                ApiClient.setBaseUrl("http://" + ip + ":8000/");
                Toast.makeText(SettingsActivity.this, "IP Updated", Toast.LENGTH_SHORT).show();
            }
        });

        buttonLogout.setOnClickListener(v -> {
            sessionManager.clear();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Setup Bottom Nav
        ImageButton navChatList = findViewById(R.id.navChatList);
        ImageButton navSettings = findViewById(R.id.navSettings);

        navChatList.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChatListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Disable settings button on settings screen
        navSettings.setEnabled(false);
        navSettings.setAlpha(0.5f);
    }
}
