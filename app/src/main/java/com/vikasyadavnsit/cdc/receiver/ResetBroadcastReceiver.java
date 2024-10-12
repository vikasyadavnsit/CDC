package com.vikasyadavnsit.cdc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.services.ResetService;

public class ResetBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case AppConstants.ACTION_APPLICATION_RESET_USAGE:
                    context.startService(new Intent(context, ResetService.class)
                            .setAction(AppConstants.ACTION_APPLICATION_RESET_USAGE));
                    break;
            }
        }

    }
}
