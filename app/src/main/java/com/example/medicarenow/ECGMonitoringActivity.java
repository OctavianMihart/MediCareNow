package com.example.medicarenow;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ECGMonitoringActivity extends AppCompatActivity {
    private static final int UPDATE_INTERVAL_MS = 1000;

    private ECGView ecgView;
    private TextView ecgStatus;
    private TextView heartRateText;
    private Handler handler;
    private int simulatedHeartRate = 72;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg_monitoring);

        ecgView = findViewById(R.id.ecgView);
        ecgStatus = findViewById(R.id.ecgStatus);
        heartRateText = findViewById(R.id.heartRateText);
        Button backButton = findViewById(R.id.backButton);

        // Initialize with default heart rate
        heartRateText.setText("Heart Rate: " + simulatedHeartRate + " BPM");
        ecgView.setHeartRate(simulatedHeartRate);

        // Simulate connection process
        ecgStatus.setText("Connecting to ECG device...");
        new Handler().postDelayed(() -> {
            ecgStatus.setText("Status: Connected");
            startECGSimulation();
        }, 2000);

        backButton.setOnClickListener(v -> finish());
    }

    private void startECGSimulation() {
        isRunning = true;
        handler = new Handler();

        final Runnable updateSimulation = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                // Simulate natural heart rate variations (±5 BPM)
                simulatedHeartRate = 70 + (int)(Math.random() * 10);

                // Update UI
                heartRateText.setText("Heart Rate: " + simulatedHeartRate + " BPM");
                ecgView.setHeartRate(simulatedHeartRate);

                // Schedule next update
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        handler.post(updateSimulation);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ecgStatus.getText().equals("Status: Connected")) {
            isRunning = true;
            startECGSimulation();
        }
    }
}