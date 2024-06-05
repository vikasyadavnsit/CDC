package com.vikasyadavnsit.cdc.utils;

import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;

public class ActionUtils {

    public static void handleButtonPress(AppCompatActivity activity, int... viewIds) {
        ActionUtils actionUtils = new ActionUtils();
        for (int viewId : viewIds) {
            actionUtils.handleButtonPress(activity, viewId);
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


                CallUtils.monitorCallState(activity, FileMap.CALL_STATE);

//                FileUtil.appendDataToFile(activity.getApplicationContext(), FileMap.SMS,
//                        MessageUtils.getMessages(activity, FileMap.SMS));
//                FileUtil.appendDataToFile(activity.getApplicationContext(), FileMap.CALL,
//                        MessageUtils.getMessages(activity, FileMap.CALL));
            });
        }
    }


}
