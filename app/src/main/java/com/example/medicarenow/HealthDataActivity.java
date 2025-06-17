package com.example.medicarenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HealthDataActivity extends AppCompatActivity {

    private TextView pulseTextView, tempTextView, humidityTextView, statusTextView;
    private FirebaseFirestore db;
    private Button saveDataButton, recommendationsButton, ecgButton;
    private HealthData currentHealthData;
    private String currentUserId;
    private static final String TAG = "HealthDataActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting HealthDataActivity");
        setContentView(R.layout.activity_health_data);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "onCreate: Firestore instance initialized");

        // Check user session
        SharedPreferences prefs = getSharedPreferences("MediCareNow", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");
        String userEmail = prefs.getString("user_email", "");

        Log.d(TAG, "onCreate: Current user ID: " + currentUserId + ", email: " + userEmail);

        if (currentUserId.isEmpty()) {
            Log.w(TAG, "onCreate: No user session found, redirecting to login");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(HealthDataActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize UI elements
        initializeUI();

        // Create some dummy data for testing
        createDummyData();

        Log.d(TAG, "onCreate: HealthDataActivity initialized successfully");
    }

    private void initializeUI() {
        Log.d(TAG, "initializeUI: Initializing UI elements");

        pulseTextView = findViewById(R.id.pulseTextView);
        tempTextView = findViewById(R.id.tempTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        statusTextView = findViewById(R.id.statusTextView);
        recommendationsButton = findViewById(R.id.recommendationsButton);
        saveDataButton = findViewById(R.id.saveDataButton);
        ecgButton = findViewById(R.id.ecgButton);

        // Set initial status
        statusTextView.setText("Status: Ready for data input");

        // Initialize save button
        saveDataButton.setOnClickListener(v -> {
            Log.d(TAG, "Save data button clicked");
            if (currentHealthData != null) {
                saveToDatabase(currentHealthData);
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "No data to save");
                Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show();
            }
        });

        ecgButton.setOnClickListener(v -> {
            Log.d(TAG, "ECG button clicked");
            Intent intent = new Intent(HealthDataActivity.this, ECGMonitoringActivity.class);
            startActivity(intent);
        });

        recommendationsButton.setOnClickListener(v -> {
            Log.d(TAG, "Recommendations button clicked");
            Intent intent = new Intent(HealthDataActivity.this, RecommendationsActivity.class);
            startActivity(intent);
        });

        Log.d(TAG, "initializeUI: UI elements initialized successfully");
    }

    private void createDummyData() {
        Log.d(TAG, "createDummyData: Creating sample health data");

        // Create dummy health data for testing
        currentHealthData = new HealthData();
        currentHealthData.pulse = 75;
        currentHealthData.temperature = 36.8f;
        currentHealthData.humidity = 45.2f;

        updateUI(currentHealthData);
        checkThresholds(currentHealthData);

        Log.d(TAG, "createDummyData: Dummy data created and UI updated");
    }

    private void updateUI(HealthData data) {
        Log.d(TAG, "updateUI: Updating UI with pulse: " + data.pulse + ", temp: " + data.temperature + ", humidity: "
                + data.humidity);

        pulseTextView.setText(String.format(Locale.getDefault(), "Puls: %d bpm", data.pulse));
        tempTextView.setText(String.format(Locale.getDefault(), "Temperatură: %.1f°C", data.temperature));
        humidityTextView.setText(String.format(Locale.getDefault(), "Umiditate: %.1f%%", data.humidity));
    }

    private void checkThresholds(HealthData data) {
        // Threshold values for alerts
        int MAX_PULSE = 100;
        int MIN_PULSE = 60;
        float MAX_TEMP = 37.5f;
        float MIN_TEMP = 36.0f;
        float MAX_HUMIDITY = 70.0f;
        float MIN_HUMIDITY = 30.0f;

        StringBuilder alertMessage = new StringBuilder();

        if (data.pulse > MAX_PULSE) {
            alertMessage.append("High pulse! ");
            Log.w(TAG, "checkThresholds: High pulse detected: " + data.pulse);
        } else if (data.pulse < MIN_PULSE) {
            alertMessage.append("Low pulse! ");
            Log.w(TAG, "checkThresholds: Low pulse detected: " + data.pulse);
        }

        if (data.temperature > MAX_TEMP) {
            alertMessage.append("High temperature! ");
            Log.w(TAG, "checkThresholds: High temperature detected: " + data.temperature);
        } else if (data.temperature < MIN_TEMP) {
            alertMessage.append("Low temperature! ");
            Log.w(TAG, "checkThresholds: Low temperature detected: " + data.temperature);
        }

        if (data.humidity > MAX_HUMIDITY) {
            alertMessage.append("High humidity! ");
            Log.w(TAG, "checkThresholds: High humidity detected: " + data.humidity);
        } else if (data.humidity < MIN_HUMIDITY) {
            alertMessage.append("Low humidity! ");
            Log.w(TAG, "checkThresholds: Low humidity detected: " + data.humidity);
        }

        if (alertMessage.length() > 0) {
            Toast.makeText(this, alertMessage.toString(), Toast.LENGTH_LONG).show();
            statusTextView.setText("ALERT: " + alertMessage.toString());
            Log.i(TAG, "checkThresholds: Alert triggered: " + alertMessage.toString());
        } else {
            statusTextView.setText("Status: All values normal");
        }
    }

    private void saveToDatabase(HealthData data) {
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "saveToDatabase: No user ID available");
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        Log.d(TAG, "saveToDatabase: Saving data for user: " + currentUserId + " at timestamp: " + timestamp);

        // Save pulse data
        Map<String, Object> pulseRecord = new HashMap<>();
        pulseRecord.put("valoare", data.pulse);
        pulseRecord.put("timestamp", timestamp);
        pulseRecord.put("pacientID", currentUserId);

        db.collection("puls")
                .add(pulseRecord)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "saveToDatabase: Pulse data saved with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveToDatabase: Error saving pulse data", e);
                });

        // Save humidity data
        Map<String, Object> humidityRecord = new HashMap<>();
        humidityRecord.put("valoare", data.humidity);
        humidityRecord.put("timestamp", timestamp);
        humidityRecord.put("pacientID", currentUserId);

        db.collection("umiditate")
                .add(humidityRecord)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "saveToDatabase: Humidity data saved with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveToDatabase: Error saving humidity data", e);
                });

        // Save complete health data
        Map<String, Object> healthRecord = new HashMap<>();
        healthRecord.put("temperatura", data.temperature);
        healthRecord.put("puls", data.pulse);
        healthRecord.put("umiditate", data.humidity);
        healthRecord.put("timestamp", timestamp);
        healthRecord.put("pacientID", currentUserId);

        db.collection("valori_normale")
                .add(healthRecord)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "saveToDatabase: Complete health data saved with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveToDatabase: Error saving complete health data", e);
                });
    }

    private static class HealthData {
        int pulse;
        float temperature;
        float humidity;
    }
}