package com.vikasyadavnsit.cdc.utils;

import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;

public class ActionUtil {

    public static void handleButtonPress(AppCompatActivity activity, int... viewIds) {
        ActionUtil actionUtil = new ActionUtil();
        for (int viewId : viewIds) {
            actionUtil.handleButtonPress(activity, viewId);
        }
    }

    public static void handleButtonPress(AppCompatActivity activity, int viewId) {

        Button actionButton = activity.findViewById(viewId);

        if (R.id.main_navigation_request_home_button == viewId) {
            actionButton.setOnClickListener(view -> {
                CommonUtil.loadFragment(activity.getSupportFragmentManager(), new HomeFragment());
            });
        } else if (R.id.main_navigation_request_settings_button == viewId) {
            actionButton.setOnClickListener(view -> {
                CommonUtil.loadFragment(activity.getSupportFragmentManager(), new SettingsFragment());
                new PermissionManager().requestAllPermissions(activity);
                //permissionHandler.resetAllPermissionManually(this);
            });
        }
    }
}
