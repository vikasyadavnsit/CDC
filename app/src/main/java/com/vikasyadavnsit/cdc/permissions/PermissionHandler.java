package com.vikasyadavnsit.cdc.permissions;


import android.app.Activity;

public interface PermissionHandler {
    boolean hasPermission();

    void requestPermission(Activity activity);
}

