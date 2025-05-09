package com.example.medicarenow;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ECGMonitoringActivity extends AppCompatActivity {

    private TextView ecgStatus;
    private ECGView ecgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg_monitoring);

        ecgView = findViewById(R.id.ecgView);
        ecgStatus = findViewById(R.id.ecgStatus);
        Button backButton = findViewById(R.id.backButton);

        // Simulate connection status
        ecgStatus.postDelayed(() -> ecgStatus.setText("Status: Connected"), 2000);

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ecgView.invalidate(); // Start the animation
    }

    @Override
    protected void onPause() {
        super.onPause();
        ecgView.clearAnimation();
    }
}