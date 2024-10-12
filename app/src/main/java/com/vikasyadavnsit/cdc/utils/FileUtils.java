package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.PermissionType;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.services.CDCOrganisedFileAppender;
import com.vikasyadavnsit.cdc.services.CDCUnorganisedFileAppender;

import java.io.File;

/**
 * Utility class for file operations.
 */
public class FileUtils {

    public static void startFileAccessSettings(Activity context) {
        // If you have access to the external storage, do whatever you need
        if (!hasFileAccess()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11 and above
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
            } else {
                new PermissionManager().requestPermission(context, PermissionType.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * Appends data to a file based on the specified FileMap.
     *
     * @param fileMap The file map.
     * @param data    The data to append.
     */
    public static void appendDataToFile(FileMap fileMap, Object data) {

        if (hasFileAccess()) {
            if (fileMap.isOrganized()) {
                CDCOrganisedFileAppender.checkAndAppendDataInOrganizedFile(fileMap, data);
            } else {
                appendDataInUnorganizedFile(fileMap, data);
            }
        } else {
            Log.d("FileUtil", "No file access to peform " + fileMap.name() + " operation");
        }
    }

    /**
     * Appends data to an unorganized file.
     *
     * @param data The data to append.
     */
    private static void appendDataInUnorganizedFile(FileMap fileMap, Object data) {
        CDCUnorganisedFileAppender.add(fileMap, data);
    }


    /**
     * Checks if internal storage is available.
     *
     * @param context The context.
     * @return True if internal storage is available, otherwise false.
     */
    public static boolean isInternalStorageAvailable(Context context) {
        File internalStorageDir = context.getFilesDir();
        boolean isAvailable = internalStorageDir != null;
        LoggerUtils.d("FileUtil", "Internal storage is " + (isAvailable ? "available: " + internalStorageDir.getAbsolutePath() : "not available"));
        return isAvailable;
    }

    /**
     * Checks if external storage is available.
     *
     * @param context The context.
     * @return True if external storage is available, otherwise false.
     */
    public static boolean isExternalStorageAvailable(Context context) {
        String externalStorageState = Environment.getExternalStorageState();
        boolean isAvailable = Environment.MEDIA_MOUNTED.equals(externalStorageState);

        if (isAvailable) {
            File externalStorageDir = context.getExternalFilesDir(null);
            LoggerUtils.d("FileUtil", "External storage is available: " + externalStorageDir.getAbsolutePath());
        } else {
            LoggerUtils.d("FileUtil", "External storage is not available");
        }
        return isAvailable;
    }

}
