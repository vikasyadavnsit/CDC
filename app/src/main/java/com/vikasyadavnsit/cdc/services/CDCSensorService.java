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
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.vikasyadavnsit.cdc.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CDCSensorService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "CDCSensorServiceChannel";
    StringBuilder sb = new StringBuilder();
    private SensorManager sensorManager;
    private Map<Integer, String> sensorFiles;
    private boolean isListening = false;


    //Todo: Use a SQLLite Database to store data and snapshots locally

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
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        startListeningToSensors();
        createSensorNotificationChannel();
        startForeground(1, getSensorNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("STOP_SENSOR".equals(action)) {
                stopListeningToSensors();
            } else {  //("START_SENSOR".equals(action))
                startListeningToSensors();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListeningToSensors();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String fileName = sensorFiles.get(event.sensor.getType());
        if (Objects.nonNull(fileName)) {
            sb.setLength(0);
            sb.append("Timestamp: ").append(event.timestamp).append(", Values: ");
            for (float value : event.values) {
                sb.append(value).append(", ");
            }
            sb.append("\n");
            CDCUnorganisedFileAppender.appendDataToFile(fileName, sb.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something if sensor accuracy changes
    }

    private @NonNull Notification getSensorNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Congratulations! Everything is up to date.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void startListeningToSensors() {
        if (sensorManager != null && !isListening) {
            String ignoreUncalibratedSensors = "uncalibrated";
            sensorFiles = new HashMap<>();
            List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : deviceSensors) {
                if (!sensor.getName().toLowerCase().contains(ignoreUncalibratedSensors)) {
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    sensorFiles.put(sensor.getType(), sensor.getName() + ".txt");
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
