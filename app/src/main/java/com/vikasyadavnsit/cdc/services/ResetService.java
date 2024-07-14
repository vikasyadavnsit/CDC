package com.vikasyadavnsit.cdc.services;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
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
                    FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, "Device unlocked at: " + LocalDateTime.now() + ". Total unlocks: " + unlockCount);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    lockCount++;
                    FileUtils.appendDataToFile(FileMap.APPLICATION_USAGE, "Device locked at: " + LocalDateTime.now() + ". Total locks: " + lockCount);
                    break;
                case AppConstants.ACTION_APPLICATION_RESET_USAGE:
                    // Print daily usage and reset data
                    MyAccessibilityService.printDailyUsageStatic();
                    MyAccessibilityService.resetUsageDataStatic();
                    // Scheduling for next cycle
                    CommonUtil.scheduleDailyReset(this);
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


}

