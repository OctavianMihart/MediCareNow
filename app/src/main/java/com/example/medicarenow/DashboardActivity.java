package com.example.medicarenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private Button viewHealthDataButton, logoutButton;
    private TextView welcomeTextView;
    private static final String TAG = "DashboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting DashboardActivity");
        setContentView(R.layout.activity_dashboard);

        welcomeTextView = findViewById(R.id.welcomeTextView);
        viewHealthDataButton = findViewById(R.id.viewHealthDataButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Check if user is logged in
        SharedPreferences prefs = getSharedPreferences("MediCareNow", MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        String firstName = prefs.getString("user_first_name", "");
        String lastName = prefs.getString("user_last_name", "");
        String userRole = prefs.getString("user_role", "");

        Log.d(TAG,
                "onCreate: User session - email: " + userEmail + ", firstName: " + firstName + ", role: " + userRole);

        // If no user session, redirect to login
        if (userEmail.isEmpty()) {
            Log.w(TAG, "onCreate: No user session found, redirecting to login");
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Set welcome message with user's name
        String welcomeMessage;
        if (!firstName.isEmpty() && !lastName.isEmpty()) {
            welcomeMessage = "Welcome, " + firstName + " " + lastName;
        } else if (!firstName.isEmpty()) {
            welcomeMessage = "Welcome, " + firstName;
        } else {
            welcomeMessage = "Welcome, " + userEmail;
        }
        welcomeTextView.setText(welcomeMessage);
        Log.d(TAG, "onCreate: Welcome message set: " + welcomeMessage);

        viewHealthDataButton.setOnClickListener(v -> {
            Log.d(TAG, "View Health Data button clicked");
            startActivity(new Intent(DashboardActivity.this, HealthDataActivity.class));
        });

        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            logout();
        });
    }

    private void logout() {
        Log.d(TAG, "logout: Starting logout process");

        // Clear user session from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MediCareNow", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Clear all stored user data
        editor.apply();

        Log.d(TAG, "logout: User session cleared");
        Log.d(TAG, "logout: Redirecting to LoginActivity");

        // Redirect to login activity
        Intent loginIntent = new Intent(DashboardActivity.this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
        startActivity(loginIntent);
        finish();
    }
}