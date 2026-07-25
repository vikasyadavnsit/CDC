package com.vikasyadavnsit.cdc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vikasyadavnsit.cdc.services.ResetService;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class StatisticsBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            LoggerUtils.d("StatisticsReceiver", "Broadcast Received: " + action);
            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    context.startService(new Intent(context, ResetService.class).setAction(Intent.ACTION_SCREEN_ON));
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    context.startService(new Intent(context, ResetService.class).setAction(Intent.ACTION_SCREEN_OFF));
                    break;
                case Intent.ACTION_SHUTDOWN:
                case Intent.ACTION_REBOOT:
                case Intent.ACTION_BOOT_COMPLETED:
                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                case Intent.ACTION_POWER_CONNECTED:
                case Intent.ACTION_POWER_DISCONNECTED:
                case Intent.ACTION_PACKAGE_ADDED:
                case Intent.ACTION_USER_PRESENT:
                    LoggerUtils.i("StatisticsReceiver", "System Event Triggered: " + action);
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    String pkg = intent.getData() != null ? intent.getData().getSchemeSpecificPart() : "unknown";
                    LoggerUtils.i("StatisticsReceiver", "Application Uninstalled: " + pkg);
                    break;
            }
        }
    }
}
