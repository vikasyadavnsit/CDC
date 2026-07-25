package com.vikasyadavnsit.cdc.services;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.data.AppSession;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.DeviceData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AppUsageStats {

    public static boolean hasUsageStatsPermission(Context context) {
        return new com.vikasyadavnsit.cdc.permissions.PermissionManager().hasPermission(context, com.vikasyadavnsit.cdc.enums.PermissionType.PACKAGE_USAGE_STATS);
    }

    public static void getDailyUsageStats(Context context) {
        getDailyUsageStats(context, false);
    }

    public static void getDailyUsageStats(Context context, boolean forceUpload) {
        android.app.usage.UsageStatsManager usageStatsManager = (android.app.usage.UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            LoggerUtils.e("AppUsageStats", "UsageStatsManager is null");
            return;
        }

        // Set startTime to midnight of the current day
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis(); // Current time
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis(); // Midnight of the current day

        // If it's early in the morning, query at least 6 hours of data to ensure we see SOMETHING
        if (endTime - startTime < 6 * 3600000) {
            startTime = endTime - 6 * 3600000;
        }

        // Attempt to capture events regardless of permission check result
        UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);
        Map<String, AppUsageReportData> appUsageDataMap = new HashMap<>();
        int eventCount = 0;

        if (usageEvents != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                eventCount++;

                String packageName = event.getPackageName();
                if (packageName == null) continue;
                
                String modifiedPackageName = packageName.replace('.', '-');
                AppUsageReportData appUsageData = appUsageDataMap.getOrDefault(modifiedPackageName, 
                        AppUsageReportData.builder().packageName(packageName).build());

                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    appUsageData.incrementOpenCount();
                    appUsageData.setLastOpenTime(event.getTimeStamp());
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (appUsageData.getLastOpenTime() != 0) {
                        long usageDuration = event.getTimeStamp() - appUsageData.getLastOpenTime();
                        appUsageData.addUsageTime(usageDuration);
                        appUsageData.addSession(new AppSession(appUsageData.getLastOpenTime(), event.getTimeStamp()));
                    }
                }
                appUsageDataMap.put(modifiedPackageName, appUsageData);
            }
        }

        LoggerUtils.d("AppUsageStats", "Processed " + eventCount + " usage events.");

        if (eventCount == 0 && !hasUsageStatsPermission(context)) {
            LoggerUtils.w("AppUsageStats", "No events found and permission check failed.");
            if (forceUpload) {
                Map<String, AppUsageReportData> errorMap = new HashMap<>();
                errorMap.put("error", AppUsageReportData.builder()
                        .packageName("Usage Access permission missing. Please ensure it is enabled for CDC in Settings. Mode: " + getMode(context))
                        .totalTimeUsed(-1)
                        .build());
                String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(new java.util.Date());
                FirebaseUtils.getDbRef(FirebaseUtils.getPath("/userDeviceData/appStats/" + today)).setValue(errorMap);
            }
            return;
        }

        LoggerUtils.d("AppUsageStats", "Captured usage data for " + appUsageDataMap.size() + " apps");
        collectAppUsageReportData(appUsageDataMap, forceUpload);
    }

    private static int getMode(Context context) {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return -2;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName());
        } else {
            return appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName());
        }
    }

    private static void collectAppUsageReportData(Map<String, AppUsageReportData> appUsageDataMap, boolean forceUpload) {
        if (forceUpload) {
            FirebaseUtils.uploadApplicationUsageReportDataSnapshot(appUsageDataMap);
        }
        
        ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.GET_APP_USAGE_STATISTICS_REPORT.name());
        if (appData != null && appData.getValue() != null) {
            User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

            if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {
                if (!forceUpload && appTriggerSettingsData.isUploadDataSnapshot())
                    FirebaseUtils.uploadApplicationUsageReportDataSnapshot(appUsageDataMap);

                DeviceDataRepository.insert(DeviceData.builder().value(new Gson().toJson(appUsageDataMap)).fileMapType(FileMap.APPLICATION_USAGE).build());
            }
        }
    }

}
