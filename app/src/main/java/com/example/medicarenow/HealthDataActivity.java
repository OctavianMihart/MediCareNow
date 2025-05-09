package com.example.medicarenow;

import android.Manifest;
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

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HealthDataActivity extends AppCompatActivity implements BluetoothService.BluetoothDataListener {

    private TextView pulseTextView, tempTextView, humidityTextView, statusTextView;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private Button saveDataButton;
    private HealthData currentHealthData;

    // Threshold values for alerts
    private static final int MAX_PULSE = 100;
    private static final int MIN_PULSE = 60;
    private static final float MAX_TEMP = 37.5f; // Celsius
    private static final float MIN_TEMP = 36.0f;
    private static final float MAX_HUMIDITY = 70.0f;
    private static final float MIN_HUMIDITY = 30.0f;

    // Bluetooth service
    private BluetoothService bluetoothService;
    private boolean isBound = false;
    // ecg
    private Button ecgButton;

    // Hardcoded Bluetooth device address
    private static final String ARDUINO_BLUETOOTH_ADDRESS = "00:11:22:33:44:55"; // Replace with your Arduino's address

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            bluetoothService.setDataListener(HealthDataActivity.this);
            isBound = true;

            // Connect to the hardcoded device
            connectToArduinoDevice();
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
        ecgButton = findViewById(R.id.ecgButton);

        ecgButton.setOnClickListener(v -> {
            Intent intent = new Intent(HealthDataActivity.this, ECGMonitoringActivity.class);
            startActivity(intent);
        });


        // Start and bind to BluetoothService
        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToArduinoDevice() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            statusTextView.setText("Bluetooth not supported");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            statusTextView.setText("Please enable Bluetooth");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(ARDUINO_BLUETOOTH_ADDRESS);
        if (device != null && bluetoothService != null) {
            bluetoothService.connectToDevice(device);
            statusTextView.setText("Connecting to device...");
        } else {
            statusTextView.setText("Device not found");
        }
    }

    private void updateUI(HealthData data) {
        pulseTextView.setText(String.format(Locale.getDefault(), "Pulse: %d bpm", data.pulse));
        tempTextView.setText(String.format(Locale.getDefault(), "Temperature: %.1f°C", data.temperature));
        humidityTextView.setText(String.format(Locale.getDefault(), "Humidity: %.1f%%", data.humidity));
    }

    private void checkThresholds(HealthData data) {
        StringBuilder alertMessage = new StringBuilder();

        if (data.pulse > MAX_PULSE) {
            alertMessage.append("High pulse! ");
        } else if (data.pulse < MIN_PULSE) {
            alertMessage.append("Low pulse! ");
        }

        if (data.temperature > MAX_TEMP) {
            alertMessage.append("High temperature! ");
        } else if (data.temperature < MIN_TEMP) {
            alertMessage.append("Low temperature! ");
        }

        if (data.humidity > MAX_HUMIDITY) {
            alertMessage.append("High humidity! ");
        } else if (data.humidity < MIN_HUMIDITY) {
            alertMessage.append("Low humidity! ");
        }

        if (alertMessage.length() > 0) {
            Toast.makeText(this, alertMessage.toString(), Toast.LENGTH_LONG).show();
            statusTextView.setText("ALERT: " + alertMessage.toString());
        }
    }

    private void saveToDatabase(HealthData data) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> healthRecord = new HashMap<>();
        healthRecord.put("pulse", data.pulse);
        healthRecord.put("temperature", data.temperature);
        healthRecord.put("humidity", data.humidity);
        healthRecord.put("timestamp", timestamp);

        mDatabase.child("health_data").child(userId).child(timestamp.replace(" ", "_")).setValue(healthRecord);
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                statusTextView.setText("Connected to Arduino device");
            } else {
                statusTextView.setText("Disconnected - attempting to reconnect");
                // Attempt to reconnect
                if (bluetoothService != null) {
                    connectToArduinoDevice();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    // Data model class
    private static class HealthData {
        int pulse;
        float temperature;
        float humidity;
    }

}