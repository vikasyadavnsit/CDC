package com.vikasyadavnsit.cdc.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class CDCNotificationListenerService extends NotificationListenerService {

    @Override
    public void onCreate() {
        super.onCreate();
        // Start the service as a foreground service to keep it running reliably
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "YourChannelId",
                    "Notification Listener Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "YourChannelId")
                    .setContentTitle("Notification Listener")
                    .setContentText("Listening for notifications")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .build();

            startForeground(1, notification);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String notificationText = "";
        if (sbn.getNotification().extras != null) {
            CharSequence charSequence = sbn.getNotification().extras.getCharSequence("android.text");
            if (charSequence != null) {
                notificationText = charSequence.toString();
            }
        }
        LoggerUtils.d("NotificationListener", "Notification posted: " + notificationText);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        LoggerUtils.d("NotificationListener", "Notification removed: " + sbn.getPackageName());
    }
}

