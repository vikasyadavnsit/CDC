package com.vikasyadavnsit.cdc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class InstallStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        switch (status) {
            case PackageInstaller.STATUS_SUCCESS:
                LoggerUtils.d("InstallStatusReceiver", "App updated successfully");
                break;
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                // System needs user confirmation (first-time install or restricted policy)
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(confirmIntent);
                }
                break;
            default:
                LoggerUtils.e("InstallStatusReceiver",
                        "Install failed (status=" + status + "): " + message);
        }
    }
}
