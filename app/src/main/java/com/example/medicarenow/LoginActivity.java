package com.example.medicarenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private FirebaseFirestore db;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting LoginActivity");
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "onCreate: Firestore instance initialized");

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        Log.d(TAG, "onCreate: UI elements initialized");

        loginButton.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            loginUser();
        });
        registerButton.setOnClickListener(v -> {
            Log.d(TAG, "Register button clicked");
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences("MediCareNow", MODE_PRIVATE);
        String loggedInUser = prefs.getString("user_email", "");
        Log.d(TAG, "onCreate: Checking existing session for user: " + loggedInUser);

        if (!loggedInUser.isEmpty()) {
            Log.d(TAG, "onCreate: User already logged in, redirecting to Dashboard");
            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
            finish();
        } else {
            Log.d(TAG, "onCreate: No existing session found");
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        Log.d(TAG, "loginUser: Attempting login for email: " + email);
        Log.d(TAG, "loginUser: Password length: " + password.length());

        if (email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "loginUser: Empty email or password fields");
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "loginUser: Starting Firestore query for email: " + email);

        // Query Firestore for user with matching email
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        Log.d(TAG, "loginUser: Firestore query completed");

                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            Log.d(TAG, "loginUser: Query successful, found " + result.size() + " documents");

                            if (result.isEmpty()) {
                                Log.w(TAG, "loginUser: No user found with email: " + email);
                                Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            for (QueryDocumentSnapshot document : result) {
                                Log.d(TAG, "loginUser: Processing document ID: " + document.getId());

                                String storedPassword = document.getString("password");
                                String firstName = document.getString("firstName");
                                String lastName = document.getString("lastName");
                                String role = document.getString("role");

                                Log.d(TAG, "loginUser: Retrieved user data - firstName: " + firstName + ", lastName: "
                                        + lastName + ", role: " + role);
                                Log.d(TAG, "loginUser: Stored password hash length: "
                                        + (storedPassword != null ? storedPassword.length() : "null"));

                                // Verify password using BCrypt
                                Log.d(TAG, "loginUser: Starting password verification");
                                BCrypt.Result bcryptResult = BCrypt.verifyer().verify(password.toCharArray(),
                                        storedPassword);
                                Log.d(TAG, "loginUser: Password verification result: " + bcryptResult.verified);

                                if (bcryptResult.verified) {
                                    Log.i(TAG, "loginUser: Login successful for user: " + email);
                                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                                    // Save user session
                                    Log.d(TAG, "loginUser: Saving user session");
                                    SharedPreferences prefs = getSharedPreferences("MediCareNow", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("user_email", email);
                                    editor.putString("user_first_name", firstName);
                                    editor.putString("user_last_name", lastName);
                                    editor.putString("user_role", role);
                                    editor.putString("user_id", document.getId());
                                    editor.apply();
                                    Log.d(TAG, "loginUser: User session saved successfully");

                                    Log.d(TAG, "loginUser: Redirecting to Dashboard");
                                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                    finish();
                                } else {
                                    Log.w(TAG, "loginUser: Password verification failed for user: " + email);
                                    Toast.makeText(LoginActivity.this, "Invalid password", Toast.LENGTH_SHORT).show();
                                }
                                return; // Only process first matching user
                            }
                        } else {
                            Log.e(TAG, "loginUser: Firestore query failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}