package com.example.medicarenow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class HealthDataActivity extends AppCompatActivity {

    private TextView pulseTextView, tempTextView, humidityTextView, statusTextView;
    private Button refreshButton, recommendationsButton;
    private Handler handler = new Handler();
    private Random random = new Random();
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Threshold values
    private static final int MAX_PULSE = 100;
    private static final int MIN_PULSE = 60;
    private static final float MAX_TEMP = 37.5f;
    private static final float MIN_TEMP = 36.0f;
    private static final float MAX_HUMIDITY = 70.0f;
    private static final float MIN_HUMIDITY = 30.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_data);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        pulseTextView = findViewById(R.id.pulseTextView);
        tempTextView = findViewById(R.id.tempTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        statusTextView = findViewById(R.id.statusTextView);

        // Generate initial data
        generateAndDisplayData();
        mDatabase.child("test").setValue("test");



        // Auto-refresh every 5 seconds
        handler.postDelayed(autoRefreshRunnable, 5000);
    }


    private void generateAndDisplayData() {
        // Generate random health data
        int pulse = MIN_PULSE + random.nextInt(MAX_PULSE - MIN_PULSE);
        float temperature = MIN_TEMP + random.nextFloat() * (MAX_TEMP - MIN_TEMP);
        float humidity = MIN_HUMIDITY + random.nextFloat() * (MAX_HUMIDITY - MIN_HUMIDITY);

        // Update UI
        pulseTextView.setText(String.format(Locale.getDefault(), "Puls: %d bpm", pulse));
        tempTextView.setText(String.format(Locale.getDefault(), "Temperatură: %.1f°C", temperature));
        humidityTextView.setText(String.format(Locale.getDefault(), "Umiditate: %.1f%%", humidity));

        // Check thresholds and show alerts
        checkThresholds(pulse, temperature, humidity);

        // Save to Firebase
        saveToDatabase(pulse, temperature, humidity);
    }

    private void checkThresholds(int pulse, float temperature, float humidity) {
        StringBuilder alertMessage = new StringBuilder();

        if (pulse > MAX_PULSE) alertMessage.append("Puls ridicat! ");
        else if (pulse < MIN_PULSE) alertMessage.append("Puls scăzut! ");

        if (temperature > MAX_TEMP) alertMessage.append("Febră! ");
        else if (temperature < MIN_TEMP) alertMessage.append("Hipotermie! ");

        if (humidity > MAX_HUMIDITY) alertMessage.append("Umiditate ridicată! ");
        else if (humidity < MIN_HUMIDITY) alertMessage.append("Umiditate scăzută! ");

        if (alertMessage.length() > 0) {
            statusTextView.setText("Atenție: " + alertMessage.toString());
        } else {
            statusTextView.setText("Toate valorile sunt normale");
        }
    }

    private void saveToDatabase(int pulse, float temperature, float humidity) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        HealthData healthData = new HealthData(pulse, temperature, humidity, timestamp);
        mDatabase.child("health_data").child(userId).push().setValue(healthData);
    }



    private Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            generateAndDisplayData();
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoRefreshRunnable);
    }

    // Health data model class
    public static class HealthData {
        public int pulse;
        public float temperature;
        public float humidity;
        public String timestamp;

        public HealthData(int pulse, float temperature, float humidity, String timestamp) {
            this.pulse = pulse;
            this.temperature = temperature;
            this.humidity = humidity;
            this.timestamp = timestamp;

        }
    }
}