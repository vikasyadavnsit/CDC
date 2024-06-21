package com.vikasyadavnsit.cdc.services;

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
    private SensorManager sensorManager;
    private static final String CHANNEL_ID = "CDCSensorServiceChannel";
    private Map<Integer, String> sensorFiles;

    //Todo: Use a SQLLite Database to store data and snapshots locally

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorFiles = new HashMap<>();
        String ignoreUncalibratedSensors = "uncalibrated";
        if (sensorManager != null) {
            List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : deviceSensors) {
                if (!sensor.getName().toLowerCase().contains(ignoreUncalibratedSensors)) {
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    sensorFiles.put(sensor.getType(), "sensor_" + sensor.getName() + ".txt");
                }
            }
        }

        createSensorNotificationChannel();
        startForeground(1, getSensorNotification());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String data = "Timestamp: " + event.timestamp + ", Values: ";
        for (float value : event.values) {
            data += value + " ";
        }
        data += "\n";

        String fileName = sensorFiles.get(event.sensor.getType());
        if (Objects.nonNull(fileName)) {
            CDCUnorganisedFileAppender.appendDataToFile(fileName, data);
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

    private void createSensorNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "CDCSensor Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
