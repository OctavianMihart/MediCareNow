package com.example.medicarenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText;
    private Button registerButton;
    private FirebaseFirestore db;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting RegisterActivity");
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "onCreate: Firestore instance initialized");

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        Log.d(TAG, "onCreate: UI elements initialized");

        registerButton.setOnClickListener(v -> {
            Log.d(TAG, "Register button clicked");
            registerUser();
        });
    }

    private void registerUser() {
        final String fullName = nameEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        Log.d(TAG, "registerUser: Attempting registration for email: " + email);
        Log.d(TAG, "registerUser: Full name: " + fullName);
        Log.d(TAG, "registerUser: Password length: " + password.length());

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "registerUser: Empty fields detected");
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Log.w(TAG, "registerUser: Password too short");
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split full name into first and last name
        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Log.d(TAG, "registerUser: Parsed name - firstName: " + firstName + ", lastName: " + lastName);

        // Check if email already exists
        Log.d(TAG, "registerUser: Checking if email already exists");
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (!task.getResult().isEmpty()) {
                            Log.w(TAG, "registerUser: Email already exists: " + email);
                            Toast.makeText(RegisterActivity.this, "Email already registered", Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        // Email doesn't exist, proceed with registration
                        Log.d(TAG, "registerUser: Email available, proceeding with registration");
                        createNewUser(firstName, lastName, email, password);
                    } else {
                        Log.e(TAG, "registerUser: Error checking email existence", task.getException());
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewUser(String firstName, String lastName, String email, String password) {
        Log.d(TAG, "createNewUser: Creating new user with email: " + email);

        // Hash the password using BCrypt
        Log.d(TAG, "createNewUser: Hashing password");
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        Log.d(TAG, "createNewUser: Password hashed successfully, length: " + hashedPassword.length());

        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("password", hashedPassword);
        user.put("role", "USER"); // Default role
        user.put("createdAt", System.currentTimeMillis());

        Log.d(TAG, "createNewUser: User data prepared, saving to Firestore");

        // Save to Firestore
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "createNewUser: User created successfully with ID: " + documentReference.getId());
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();

                    // Save user session
                    Log.d(TAG, "createNewUser: Saving user session");
                    SharedPreferences prefs = getSharedPreferences("MediCareNow", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_email", email);
                    editor.putString("user_first_name", firstName);
                    editor.putString("user_last_name", lastName);
                    editor.putString("user_role", "USER");
                    editor.putString("user_id", documentReference.getId());
                    editor.apply();
                    Log.d(TAG, "createNewUser: User session saved successfully");

                    // Redirect to Dashboard
                    Log.d(TAG, "createNewUser: Redirecting to Dashboard");
                    startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createNewUser: Error creating user", e);
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }
}