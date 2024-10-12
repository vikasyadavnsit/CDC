package com.vikasyadavnsit.cdc.receiver;


import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.vikasyadavnsit.cdc.activities.PasswordActivity;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
        // Launch the password activity when device admin is disabled
        Intent passwordIntent = new Intent(context, PasswordActivity.class);
        passwordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(passwordIntent);
    }
}

