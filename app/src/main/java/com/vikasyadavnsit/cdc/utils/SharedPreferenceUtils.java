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

    /**
     * Checks whether this is the first launch of the application (defaulting to {@code true}).
     *
     * @param context Android {@link Context} used to access {@link SharedPreferences}.
     * @return {@code true} if the first-launch flag is unset or true; {@code false} if launch was previously completed.
     */
    public static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(FIRST_LAUNCH_KEY, true);
    }

    /**
     * Indicates whether a "reset" flow has been completed (used to gate certain initialization behavior).
     *
     * @param context Android {@link Context} used to access {@link SharedPreferences}.
     * @return {@code true} if reset has been marked done; {@code false} otherwise.
     */
    public static boolean isResetDone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(RESET_DONE, false);
    }

    /**
     * Returns the cached shayari launcher state string.
     *
     * <p>The value typically encodes an index, date, and message text separated by {@code ":"}.</p>
     *
     * @param context Android {@link Context} used to access {@link SharedPreferences}.
     * @return Cached shayari data, or {@link com.vikasyadavnsit.cdc.constants.AppConstants#DEFAULT_SHAYARI_LAUNCHER_DATA} if absent.
     */
    public static String getShayariData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAUCNH_SHAYARI_DATA, DEFAULT_SHAYARI_LAUNCHER_DATA);
    }

    /**
     * Returns the cached message text shown on the message screen.
     *
     * @param context Android {@link Context} used to access {@link SharedPreferences}.
     * @return Cached message text, or {@link com.vikasyadavnsit.cdc.constants.AppConstants#DEFAULT_MESSAGE_TEXT} if absent.
     */
    public static String getMessageText(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAUNCH_MESSAGE_TEXT, DEFAULT_MESSAGE_TEXT);
    }

    /**
     * Returns the Android ID of the currently selected "admin target" device (if any).
     *
     * @param context Android {@link Context} used to access {@link SharedPreferences}.
     * @return Selected target Android ID, or {@code null} if not selected.
     */
    public static String getAdminSettingsUserAndroidId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(ADMIN_SETTINGS_USER_ANDROID_ID, null);
    }

    // Helper method to get SharedPreferences.Editor
    /**
     * Creates a {@link SharedPreferences.Editor} for this app's preferences file.
     *
     * @param context Android {@link Context}.
     * @return Editor for {@link #PREFS_NAME}.
     */
    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.edit();
    }

    // Method to reset the first launch flag
    /**
     * Resets the first-launch state and marks reset as done.
     *
     * @param context Android {@link Context}.
     */
    public static void resetFirstLauncher(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(FIRST_LAUNCH_KEY);
        editor.putBoolean(RESET_DONE, true);
        editor.apply();
    }

    // Method to set the first launch flag as completed
    /**
     * Marks the first launch as completed and clears the reset-done flag.
     *
     * @param context Android {@link Context}.
     */
    public static void setFirstLaunchCompleted(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(FIRST_LAUNCH_KEY, false);
        editor.putBoolean(RESET_DONE, false);
        editor.apply();
    }

    /**
     * Stores the cached shayari launcher state string.
     *
     * @param context Android {@link Context}.
     * @param data    Shayari state string to store.
     */
    public static void updateShayariData(Context context, String data) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(LAUCNH_SHAYARI_DATA, data);
        editor.apply();
    }

    /**
     * Stores the cached message text shown on the message screen.
     *
     * @param context Android {@link Context}.
     * @param data    Message text to store.
     */
    public static void updateMessageText(Context context, String data) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(LAUNCH_MESSAGE_TEXT, data);
        editor.apply();
    }

    /**
     * Stores the Android ID of the selected "admin target" device.
     *
     * @param context Android {@link Context}.
     * @param data    Android ID to store (or {@code null} to clear selection).
     */
    public static void updateAdminSettingsUserAndroidId(Context context, String data) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(ADMIN_SETTINGS_USER_ANDROID_ID, data);
        editor.apply();
    }

    /**
     * Clears all persisted values in the preferences file.
     *
     * @param context Android {@link Context}.
     */
    public static void resetAllData(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.clear();
        editor.apply();
    }

}

