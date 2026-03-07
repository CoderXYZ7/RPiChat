package com.danieletoniolo.Chatastic;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Redirect to Login if not logged in, otherwise ChatList
        // For now, simpler flow: default to LoginActivity as launcher,
        // but if we land here, go to ChatList
        startActivity(new Intent(this, ChatListActivity.class));
        finish();
    }
}