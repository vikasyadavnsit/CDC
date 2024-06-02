package com.vikasyadavnsit.cdc.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.dialog.MessageDialog;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;
import com.vikasyadavnsit.cdc.enums.PermissionType;
import com.vikasyadavnsit.cdc.utils.LoggerUtil;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;


public class PermissionManager implements PermissionHandler {
    @Override
    public boolean hasPermission(Context context, PermissionType permissionType) {
        boolean hasAllPermissions = true;
        for (String permission : permissionType.getPermissions()) {
            if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                LoggerUtil.log("PermissionManager", "Permission : " + permission + " already granted", LoggingLevel.DEBUG);
            } else {
                LoggerUtil.log("PermissionManager", "Permission : " + permission + " doesn't exist, taking permission", LoggingLevel.DEBUG);
                hasAllPermissions = false;
                break;
            }
        }
        return hasAllPermissions;
    }

    @Override
    public void requestPermission(Activity activity, PermissionType permissionType) {
        if (!hasPermission(activity, permissionType)) {
            StringJoiner stringJoiner = new StringJoiner(", ");
            LoggerUtil.log("PermissionManager", "Requesting Permission : " + String.join(", ", permissionType.getPermissions()), LoggingLevel.DEBUG);
            ActivityCompat.requestPermissions(activity, permissionType.getPermissions(), permissionType.getRequestCode());
        }
    }

    public void requestDirectPermissionInOneGo(Activity activity) {
        LoggerUtil.log("PermissionManager", "Requesting All Permissions in one Go", LoggingLevel.DEBUG);
        ActivityCompat.requestPermissions(activity, Stream.of(PermissionType.values()).flatMap(permissionType -> Stream.of(permissionType.getPermissions())).toArray(String[]::new), AppConstants.ALL_PERMISSIONS_REQUEST_CODE);
    }


    @Override
    public void requestAllPermissions(Activity activity) {
        LoggerUtil.log("PermissionManager", "Requesting All Permissions", LoggingLevel.DEBUG);
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
                    LoggerUtil.log("PermissionManager", "Permission : " + permissions[i] + " granted", LoggingLevel.DEBUG);
                } else {
                    LoggerUtil.log("PermissionManager", "Permission : " + permissions[i] + " not granted", LoggingLevel.DEBUG);
                    //Toast.makeText(context, "Permission : " + permissions[i] + " not granted", Toast.LENGTH_SHORT).show();
                    MessageDialog.showCustomDialog(context, "Permission Status", "Permission : " + permissions[i] + " not granted");
                }
            }
        }

    }


    @Override
    public void resetAllPermissionManually(Context context) {
        LoggerUtil.log("PermissionManager", "Resetting All Permissions Manually, So opening Settings", LoggingLevel.DEBUG);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }
}


