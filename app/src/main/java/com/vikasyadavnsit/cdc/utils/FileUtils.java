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
import com.vikasyadavnsit.cdc.services.CDCOrganisedFileAppender;
import com.vikasyadavnsit.cdc.services.CDCUnorganisedFileAppender;

import java.io.File;

/**
 * Utility class for file operations.
 */
public class FileUtils {

    /**
     * Opens the system settings UI needed to grant broad file access so the app can read/write external storage.
     *
     * <p>On Android 11+ (R+), this opens the "All files access" page for the app. On older versions,
     * it requests {@link PermissionType#WRITE_EXTERNAL_STORAGE} via {@link PermissionManager}.</p>
     *
     * @param context The current {@link Activity}, used to start settings intents and request runtime permissions.
     */
    public static void startFileAccessSettings(Activity context) {
        // If you have access to the external storage, do whatever you need
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            // If you don't have access, launch a new activity to show the user the system's dialog
            // to allow access to the external storage
        } else {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }

    /**
     * Appends data to a file based on the specified FileMap.
     *
     * @param fileMap The file map.
     * @param data    The data to append.
     */
    public static void appendDataToFile(FileMap fileMap, Object data) {
        if (fileMap.isOrganized()) {
            CDCOrganisedFileAppender.checkAndAppendDataInOrganizedFile(fileMap, data);
        } else {
            CDCUnorganisedFileAppender.add(fileMap, data);
        }
    }

    public static void clearFile(FileMap fileMap) {
        try {
            File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
            File file = new File(directory, fileMap.getFileName());
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }
        } catch (Exception e) {
            Log.e("FileUtils", "Error clearing file: " + fileMap.name(), e);
        }
    }

    /**
     * Appends data to an unorganized file.
     *
     * <p>This delegates to {@link CDCUnorganisedFileAppender} which buffers writes and flushes when
     * size thresholds are reached.</p>
     *
     * @param fileMap File type configuration (used for routing and encryption behavior).
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
