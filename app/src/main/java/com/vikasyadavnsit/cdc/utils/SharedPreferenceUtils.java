package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.constants.AppConstants.DEFAULT_MESSAGE_TEXT;
import static com.vikasyadavnsit.cdc.constants.AppConstants.DEFAULT_SHAYARI_LAUNCHER_DATA;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceUtils {

    private static final String PREFS_NAME = "CDC_LAUNCH_PREF";
    private static final String FIRST_LAUNCH_KEY = "FIRST_LAUNCH";
    private static final String RESET_DONE = "RESET_DONE";
    private static final String LAUCNH_SHAYARI_DATA = "LAUCNH_SHAYARI_DATA";
    private static final String LAUNCH_MESSAGE_TEXT = "LAUNCH_MESSAGE_TEXT";
    private static final String ADMIN_SETTINGS_USER_ANDROID_ID = "ADMIN_SETTINGS_USER_ANDROID_ID";

    public static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(FIRST_LAUNCH_KEY, true);
    }

    public static boolean isResetDone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(RESET_DONE, false);
    }

    public static String getShayariData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAUCNH_SHAYARI_DATA, DEFAULT_SHAYARI_LAUNCHER_DATA);
    }

    public static String getMessageText(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAUNCH_MESSAGE_TEXT, DEFAULT_MESSAGE_TEXT);
    }

    public static String getAdminSettingsUserAndroidId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(ADMIN_SETTINGS_USER_ANDROID_ID, null);
    }

    // Helper method to get SharedPreferences.Editor
    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.edit();
    }

    // Method to reset the first launch flag
    public static void resetFirstLauncher(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(FIRST_LAUNCH_KEY);
        editor.putBoolean(RESET_DONE, true);
        editor.apply();
    }

    // Method to set the first launch flag as completed
    public static void setFirstLaunchCompleted(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(FIRST_LAUNCH_KEY, false);
        editor.putBoolean(RESET_DONE, false);
        editor.apply();
    }

    public static void updateShayariData(Context context, String data) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(LAUCNH_SHAYARI_DATA, data);
        editor.apply();
    }

    public static void updateMessageText(Context context, String data) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(LAUNCH_MESSAGE_TEXT, data);
        editor.apply();
    }

    public static void updateAdminSettingsUserAndroidId(Context context, String data) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(ADMIN_SETTINGS_USER_ANDROID_ID, data);
        editor.apply();
    }

    public static void resetAllData(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.clear();
        editor.apply();
    }

}

