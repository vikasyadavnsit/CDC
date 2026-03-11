package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

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
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = System.currentTimeMillis();
        return !usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 600000, currentTime).isEmpty();
    }

    public static void getDailyUsageStats(Context context) {
        if (!hasUsageStatsPermission(context)) {
            LoggerUtils.d("AppUsageStats", "No Usage Stats Permission");
        }
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // Set startTime to midnight of the current day
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis(); // Current time
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis(); // Midnight of the current day

        // To capture detailed events (open/close times)
        UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);

        // Map to store app usage data
        Map<String, AppUsageReportData> appUsageDataMap = new HashMap<>();

        UsageEvents.Event event = new UsageEvents.Event();
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);

            String packageName = event.getPackageName();
            String modifiedPackageName = packageName.replace('.', '-');
            AppUsageReportData appUsageData = appUsageDataMap.getOrDefault(modifiedPackageName, AppUsageReportData.builder().packageName(packageName).build());

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
        collectAppUsageReportData(appUsageDataMap);
    }

    private static void collectAppUsageReportData(Map<String, AppUsageReportData> appUsageDataMap) {
        if (hasFileAccess()) {
            ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.GET_APP_USAGE_STATISTICS_REPORT.name());
            User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

            if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {

                if (appTriggerSettingsData.isUploadDataSnapshot())
                    FirebaseUtils.uploadApplicationUsageReportDataSnapshot(appUsageDataMap);

                DeviceDataRepository.insert(DeviceData.builder().value(new Gson().toJson(appUsageDataMap)).fileMapType(FileMap.APPLICATION_USAGE).build());
            } else {
                LoggerUtils.d("AccessibilityUtils", "GET_APP_USAGE_STATISTICS_REPORT setting is disabled");
            }
        } else {
            LoggerUtils.d("AccessibilityUtils", "No File Access for Application Report Usage logging decision");
        }
    }

}
