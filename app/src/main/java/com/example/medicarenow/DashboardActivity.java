package com.example.medicarenow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private Button viewHealthDataButton, logoutButton;
    private TextView welcomeTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();

        welcomeTextView = findViewById(R.id.welcomeTextView);
        viewHealthDataButton = findViewById(R.id.viewHealthDataButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Set welcome message
        if (mAuth.getCurrentUser() != null) {
            welcomeTextView.setText("Welcome, " + mAuth.getCurrentUser().getEmail());
        }

        viewHealthDataButton.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, HealthDataActivity.class));
        });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
            finish();
        });
    }
}