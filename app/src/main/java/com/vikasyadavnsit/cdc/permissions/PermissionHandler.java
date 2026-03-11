package com.vikasyadavnsit.cdc.permissions;


import android.app.Activity;
import android.content.Context;

import com.vikasyadavnsit.cdc.enums.PermissionType;

public interface PermissionHandler {
    boolean hasPermission(Context context, PermissionType permissionType);

    void requestPermission(Activity activity, PermissionType permissionType);

    void requestAllPermissions(Activity activity);

    void handlePermissionResult(Context context, int requestCode, String[] permissions, int[] grantResults);

    void resetAllPermissionManually(Context context);
}

