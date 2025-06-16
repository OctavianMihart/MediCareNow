package com.example.medicarenow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HealthDataActivity extends AppCompatActivity
        implements BluetoothService.BluetoothDataListener {

    private static final String ARDUINO_MAC_ADDRESS = "58:56:00:00:2C:BE"; // Replace with your device's MAC

    private TextView pulseTextView, tempTextView, humidityTextView, statusTextView;
    private Button saveDataButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private BluetoothService bluetoothService;
    private boolean isBound = false;

    // Current health data
    private HealthData currentHealthData;

    // Threshold values
    private static final int MAX_PULSE = 100;
    private static final int MIN_PULSE = 60;
    private static final float MAX_TEMP = 37.5f;
    private static final float MIN_TEMP = 36.0f;
    private static final float MAX_HUMIDITY = 70.0f;
    private static final float MIN_HUMIDITY = 30.0f;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            bluetoothService.setDataListener(HealthDataActivity.this);
            isBound = true;
            connectToDevice();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

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
        saveDataButton = findViewById(R.id.saveDataButton);

        // Initialize save button
        saveDataButton.setOnClickListener(v -> {
            if (currentHealthData != null) {
                saveToDatabase(currentHealthData);
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show();
            }
        });

        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void connectToDevice() {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(ARDUINO_MAC_ADDRESS);
        if (device != null && bluetoothService != null) {
            bluetoothService.connectToDevice(String.valueOf(device));
            statusTextView.setText("Connecting to device...");
        } else {
            statusTextView.setText("Device not found");
        }
    }

    @Override
    public void onDataReceived(String data) {
        try {
            currentHealthData = new Gson().fromJson(data, HealthData.class);
            runOnUiThread(() -> {
                updateUI(currentHealthData);
                checkThresholds(currentHealthData);
                // Automatic saving removed - now only manual save via button
            });
        } catch (JsonSyntaxException e) {
            runOnUiThread(() -> statusTextView.setText("Data format error"));
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected, String message) {

    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                statusTextView.setText("Connected to device");
            } else {
                statusTextView.setText("Disconnected");
                Toast.makeText(this, "Device disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(HealthData data) {
        pulseTextView.setText(String.format(Locale.getDefault(), "Pulse: %d bpm", data.pulse));
        tempTextView.setText(String.format(Locale.getDefault(), "Temp: %.1f°C", data.temperature));
        humidityTextView.setText(String.format(Locale.getDefault(), "Humidity: %.1f%%", data.humidity));
    }

    private void checkThresholds(HealthData data) {
        StringBuilder alerts = new StringBuilder();

        if (data.pulse > MAX_PULSE) alerts.append("High pulse! ");
        else if (data.pulse < MIN_PULSE) alerts.append("Low pulse! ");

        if (data.temperature > MAX_TEMP) alerts.append("High temp! ");
        else if (data.temperature < MIN_TEMP) alerts.append("Low temp! ");

        if (data.humidity > MAX_HUMIDITY) alerts.append("High humidity! ");
        else if (data.humidity < MIN_HUMIDITY) alerts.append("Low humidity! ");

        if (alerts.length() > 0) {
            Toast.makeText(this, alerts.toString(), Toast.LENGTH_LONG).show();
            statusTextView.setText("ALERT: " + alerts);
        }
    }

    private void saveToDatabase(HealthData data) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        DatabaseReference newRecordRef = mDatabase.child("health_data").child(userId).push();

        newRecordRef.child("pulse").setValue(data.pulse);
        newRecordRef.child("temperature").setValue(data.temperature);
        newRecordRef.child("humidity").setValue(data.humidity);
        newRecordRef.child("timestamp").setValue(timestamp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private static class HealthData {
        int pulse;
        float temperature;
        float humidity;
    }
}