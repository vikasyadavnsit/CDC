package com.vikasyadavnsit.cdc.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.dialog.MessageDialog;
import com.vikasyadavnsit.cdc.enums.PermissionType;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;


public class PermissionManager implements PermissionHandler {
    @Override
    public boolean hasPermission(Context context, PermissionType permissionType) {
        if (permissionType == PermissionType.MANAGE_EXTERNAL_STORAGE) {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
        }
        if (permissionType == PermissionType.SYSTEM_ALERT_WINDOW) {
            return Settings.canDrawOverlays(context);
        }
        if (permissionType == PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE) {
            String listeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return listeners != null && listeners.contains(context.getPackageName());
        }
        if (permissionType == PermissionType.PACKAGE_USAGE_STATS) {
            android.app.AppOpsManager appOps = (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName());
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        }
        if (permissionType == PermissionType.ACCESSIBILITY_SERVICE) {
            return com.vikasyadavnsit.cdc.utils.AccessibilityUtils.isAccessibilityServiceEnabled(context, 
                    com.vikasyadavnsit.cdc.services.CDCAccessibilityService.class);
        }
        if (permissionType == PermissionType.BATTERY_OPTIMIZATION) {
            android.os.PowerManager powerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }

        boolean hasAllPermissions = true;
        for (String permission : permissionType.getPermissions()) {
            if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                LoggerUtils.d("PermissionManager", "Permission : " + permission + " already granted");
            } else {
                LoggerUtils.d("PermissionManager", "Permission : " + permission + " doesn't exist, taking permission");
                hasAllPermissions = false;
                break;
            }
        }
        return hasAllPermissions;
    }

    @Override
    public void requestPermission(Activity activity, PermissionType permissionType) {
        if (hasPermission(activity, permissionType)) return;

        if (permissionType == PermissionType.MANAGE_EXTERNAL_STORAGE) {
            com.vikasyadavnsit.cdc.utils.FileUtils.startFileAccessSettings(activity);
            return;
        }
        if (permissionType == PermissionType.SYSTEM_ALERT_WINDOW) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
            return;
        }
        if (permissionType == PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE) {
            activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return;
        }
        if (permissionType == PermissionType.PACKAGE_USAGE_STATS) {
            activity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }
        if (permissionType == PermissionType.ACCESSIBILITY_SERVICE) {
            activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        if (permissionType == PermissionType.BATTERY_OPTIMIZATION) {
            activity.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            return;
        }

        LoggerUtils.d("PermissionManager", "Requesting Permission : " + String.join(", ", permissionType.getPermissions()));
        ActivityCompat.requestPermissions(activity, permissionType.getPermissions(), permissionType.getRequestCode());
    }

    public void requestDirectPermissionInOneGo(Activity activity) {
        LoggerUtils.d("PermissionManager", "Requesting All Permissions in one Go");
        String[] allPermissions = Stream.of(PermissionType.values())
                .filter(type -> type != PermissionType.MANAGE_EXTERNAL_STORAGE 
                        && type != PermissionType.SYSTEM_ALERT_WINDOW 
                        && type != PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE
                        && type != PermissionType.PACKAGE_USAGE_STATS
                        && type != PermissionType.ACCESSIBILITY_SERVICE
                        && type != PermissionType.BATTERY_OPTIMIZATION)
                .flatMap(permissionType -> Stream.of(permissionType.getPermissions()))
                .toArray(String[]::new);
        ActivityCompat.requestPermissions(activity, allPermissions, AppConstants.ALL_PERMISSIONS_REQUEST_CODE);
    }


    @Override
    public void requestAllPermissions(Activity activity) {
        LoggerUtils.d("PermissionManager", "Requesting All Permissions");
        PermissionType[] permissionTypes = PermissionType.values();
//        for (PermissionType permissionType : permissionTypes) {
//            requestPermission(activity, permissionType);
//        }
        requestDirectPermissionInOneGo(activity);
    }

    @Override
    public void handlePermissionResult(Context context, int requestCode, String[] permissions, int[] grantResults) {

        PermissionType permissionType = PermissionType.getPermissionTypeByRequestCode(requestCode);
        if (Objects.nonNull(permissionType)) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    LoggerUtils.d("PermissionManager", "Permission : " + permissions[i] + " granted");
                } else {
                    LoggerUtils.d("PermissionManager", "Permission : " + permissions[i] + " not granted");
                    //Toast.makeText(context, "Permission : " + permissions[i] + " not granted", Toast.LENGTH_SHORT).show();
                    MessageDialog.showCustomDialog(context, "Permission Status", "Permission : " + permissions[i] + " not granted");
                }
            }
        }

    }

    @Override
    public void resetAllPermissionManually(Context context) {
        LoggerUtils.d("PermissionManager", "Resetting All Permissions Manually, So opening Settings");
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

}


