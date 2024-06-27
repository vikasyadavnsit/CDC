package com.vikasyadavnsit.cdc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.services.ResetService;

public class ResetBroadcastReceiver extends BroadcastReceiver {
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
                case AppConstants.ACTION_APPLICATION_RESET_USAGE:
                    context.startService(new Intent(context, ResetService.class)
                            .setAction(AppConstants.ACTION_APPLICATION_RESET_USAGE));
                    break;
                case Intent.ACTION_SHUTDOWN:
                case Intent.ACTION_REBOOT:
                case Intent.ACTION_BOOT_COMPLETED:
                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                case Intent.ACTION_POWER_CONNECTED:
                case Intent.ACTION_POWER_DISCONNECTED:
                case Intent.ACTION_PACKAGE_ADDED:
                    Log.d("ResetBroadcastReceiver", "onReceive:" + AppConstants.ACTION_APPLICATION_RESET_USAGE);
                    // Handle application installed
                    String packageName = intent.getData().getSchemeSpecificPart();
                    //Log.d("TAG", "Installed: " + packageName);
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    // Handle application uninstalled
                    String packageNameRemoved = intent.getData().getSchemeSpecificPart();
                    Log.d("TAG", "Uninstalled: " + packageNameRemoved);
                    break;


            }
        }

//        LoggerUtils.d("ResetBroadcastReceiver", "onReceive called");
//        Intent serviceIntent = new Intent(context, ResetService.class);
//        context.startService(serviceIntent);
    }
}
