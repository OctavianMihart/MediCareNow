package com.example.medicarenow;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String deviceAddress;

    public interface BluetoothDataListener {
        void onDataReceived(String data);
        void onConnectionStatusChanged(boolean isConnected, String message);

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        void onConnectionStatusChanged(boolean isConnected);
    }

    private BluetoothDataListener dataListener;

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setDataListener(BluetoothDataListener listener) {
        this.dataListener = listener;
    }

    public void connectToDevice(String address) {
        this.deviceAddress = address;

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connectThread = new ConnectThread(device);
        connectThread.start();

        handler.post(() -> {
            if (dataListener != null) {
                dataListener.onConnectionStatusChanged(false, "Se încearcă conectarea...");
            }
        });
    }

    public void disconnect() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        handler.post(() -> {
            if (dataListener != null) {
                dataListener.onConnectionStatusChanged(false, "Deconectat");
            }
        });
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Eroare creare socket", e);
                handler.post(() -> {
                    if (dataListener != null) {
                        dataListener.onConnectionStatusChanged(false, "Eroare creare socket");
                    }
                });
            }
            mmSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();

                handler.post(() -> {
                    if (dataListener != null) {
                        dataListener.onConnectionStatusChanged(true, "Conectat la " + deviceAddress);
                    }
                });

                connectedThread = new ConnectedThread(mmSocket);
                connectedThread.start();

            } catch (IOException connectException) {
                Log.e(TAG, "Eroare conectare", connectException);
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Eroare închidere socket", closeException);
                }

                handler.post(() -> {
                    if (dataListener != null) {
                        dataListener.onConnectionStatusChanged(false, "Eroare conectare: " + connectException.getMessage());
                    }
                });
            }
        }

        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Eroare închidere socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Eroare obținere stream-uri", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);

                    handler.post(() -> {
                        if (dataListener != null) {
                            dataListener.onDataReceived(incomingMessage);
                        }
                    });

                } catch (IOException e) {
                    Log.d(TAG, "Conexiune pierdută", e);
                    handler.post(() -> {
                        if (dataListener != null) {
                            dataListener.onConnectionStatusChanged(false, "Conexiune pierdută");
                        }
                    });
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Eroare trimitere date", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Eroare închidere socket", e);
            }
        }
    }
}