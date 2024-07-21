package com.vikasyadavnsit.cdc.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class CDCNotificationListenerService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String notificationText = "";
        if (sbn.getNotification().extras != null) {
            notificationText = sbn.getNotification().extras.getCharSequence("android.text").toString();
        }
        Log.d("NotificationListener", "Notification posted: " + notificationText);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("NotificationListener", "Notification removed: " + sbn.getPackageName());
    }
}

