package com.vikasyadavnsit.cdc.services;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.receiver.ResetBroadcastReceiver;
import com.vikasyadavnsit.cdc.utils.FileUtils;

import java.time.LocalDateTime;

public class ResetService extends Service {

    private static final String TAG = "ResetService";

    private static int lockCount = 0;
    private static int unlockCount = 0;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ResetService started");


        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    unlockCount++;
                    Log.d(TAG, "Unlock count: " + unlockCount);
                    FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, "Device unlocked at: " + LocalDateTime.now() + ". Total unlocks: " + unlockCount);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    lockCount++;
                    Log.d(TAG, "Lock count: " + lockCount);
                    FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, "Device locked at: " + LocalDateTime.now() + ". Total locks: " + lockCount);
                    break;
                case AppConstants.ACTION_APPLICATION_RESET_USAGE:
                    // Print daily usage and reset data
                    MyAccessibilityService.printDailyUsageStatic();
                    MyAccessibilityService.resetUsageDataStatic();

                    // Schedule next reset
                    if (canScheduleExactAlarms()) {
                        scheduleDailyReset(this);
                    } else {
                        requestExactAlarmPermission();
                    }
            }
        }

        // Stop service after tasks are done
        stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleDailyReset(Context context) {
        Intent intent = new Intent(context, ResetBroadcastReceiver.class);
        intent.setAction(AppConstants.ACTION_APPLICATION_RESET_USAGE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        long currentTime = System.currentTimeMillis();
        long midnight = currentTime + (24 * 60 * 60 * 1000) - (currentTime % (24 * 60 * 60 * 1000));

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, midnight, pendingIntent);
        } catch (SecurityException e) {
            Log.d(TAG, "Failed to schedule exact alarm: " + e.getMessage());
        }
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // For older versions, exact alarms are always allowed
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}

