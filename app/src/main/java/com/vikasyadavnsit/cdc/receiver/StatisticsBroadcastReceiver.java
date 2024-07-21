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

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
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
                    // Handle application installed
                    // String packageName = intent.getData().getSchemeSpecificPart();
                    LoggerUtils.d("TAG", intent.getAction() + " received update");
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    // Handle application uninstalled
                    String packageNameRemoved = intent.getData().getSchemeSpecificPart();
                    LoggerUtils.d("TAG", "Uninstalled: " + packageNameRemoved);
                    break;


            }
        }

    }
}
