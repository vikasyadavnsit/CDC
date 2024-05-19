package com.vikasyadavnsit.cdc.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class CameraPermissionManager implements PermissionHandler {
    private static final int REQUEST_CODE = 123;

    @Override
    public boolean hasPermission() {
//        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_GRANTED;
        return true;
    }

    @Override
    public void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);
    }
}

