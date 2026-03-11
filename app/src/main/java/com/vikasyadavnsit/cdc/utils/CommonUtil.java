package com.vikasyadavnsit.cdc.utils;

import static android.content.Context.ALARM_SERVICE;
import static com.vikasyadavnsit.cdc.activities.MainActivity.progressLoader;
import static com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.getShayariData;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.receiver.ResetBroadcastReceiver;
import com.vikasyadavnsit.cdc.services.AppContext;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CommonUtil {

    /**
     * Replaces the main frame layout with the provided fragment (without adding to back stack).
     *
     * @param fragmentManager Fragment manager used to perform the transaction.
     * @param fragment        Fragment instance to display.
     */
    public static void loadFragment(FragmentManager fragmentManager, Fragment fragment) {
        loadFragment(R.id.main_frame_layout, fragmentManager, fragment, false);
    }

    /**
     * Replaces the main frame layout with the provided fragment and adds the transaction to the back stack.
     *
     * @param fragmentManager Fragment manager used to perform the transaction.
     * @param fragment        Fragment instance to display.
     */
    public static void loadFragmentWithBackStack(FragmentManager fragmentManager, Fragment fragment) {
        loadFragment(R.id.main_frame_layout, fragmentManager, fragment, true);
    }

    /**
     * Replaces a frame layout with the provided fragment, optionally adding the transaction to back stack.
     *
     * @param frameLayout     Layout resource ID where the fragment should be placed.
     * @param fragmentManager Fragment manager used to perform the transaction.
     * @param fragment        Fragment instance to display.
     * @param loadBackStack   If true, the transaction is added to the back stack.
     */
    public static void loadFragment(int frameLayout, FragmentManager fragmentManager, Fragment fragment, boolean loadBackStack) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame_layout, fragment);
        if (loadBackStack)
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        fragmentTransaction.commit();
    }

    /**
     * Checks whether the given object is a non-empty {@code List} whose first element is a {@code Map}.
     *
     * @param data Arbitrary object to inspect.
     * @return {@code true} if {@code data} looks like {@code List<Map<...>>}; {@code false} otherwise.
     */
    public static boolean isDataTypeListOfMap(Object data) {
        if (data instanceof List<?>) {
            List<?> dataList = (List<?>) data;
            return !dataList.isEmpty() && dataList.get(0) instanceof Map<?, ?>;
        }
        return false;
    }

    /**
     * Converts various container types into a human-readable string representation.
     *
     * <p>Handles:</p>
     * <ul>
     *   <li>{@code CharSequence[]} → {@code List} string</li>
     *   <li>{@link Iterable} → join elements with {@code ", "}</li>
     *   <li>Fallback → {@link Object#toString()}</li>
     * </ul>
     *
     * @param data Object to convert.
     * @return A string-like representation (may be a {@link java.util.List} for {@code CharSequence[]} case).
     */
    public static Object convertToString(Object data) {
        if (data instanceof CharSequence[]) {
            CharSequence[] charSequences = (CharSequence[]) data;
            return Arrays.stream(charSequences).collect(Collectors.toList());
        } else if (data instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) data;
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        } else {
            return data.toString();
        }
    }

    /**
     * Checks if the file exists, and if not, creates it.
     *
     * @param file The file to check and create.
     * @return True if the file creation failed, otherwise false.
     * @throws IOException If an I/O error occurs.
     */
    public static boolean checkAndCreateFile(File file) throws IOException {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                LoggerUtils.e("FileUtil", "Failed to create file");
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the directory exists, and if not, creates it.
     *
     * @param directory The directory to check and create.
     * @return True if the directory creation failed, otherwise false.
     */
    public static boolean checkAndCreateDirectory(File directory) {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                LoggerUtils.e("FileUtil", "Failed to create directory");
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience overload for {@link #checkAndCreateDirectory(File)} using a string path.
     *
     * @param directoryPath Directory path string.
     * @return True if directory creation failed; otherwise false.
     */
    public static boolean checkAndCreateDirectory(String directoryPath) {
        return checkAndCreateDirectory(new File(directoryPath));
    }


    /**
     * Schedules a daily reset broadcast using an exact alarm (best-effort).
     *
     * <p>This sets an exact RTC_WAKEUP alarm intended for the next "midnight" boundary and sends
     * {@link AppConstants#ACTION_APPLICATION_RESET_USAGE} to {@link ResetBroadcastReceiver}.</p>
     *
     * @param context Android {@link Context} used to schedule the alarm.
     */
    public static void scheduleDailyReset(Context context) {
        Intent intent = new Intent(context, ResetBroadcastReceiver.class);
        intent.setAction(AppConstants.ACTION_APPLICATION_RESET_USAGE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        long currentTime = System.currentTimeMillis();
        long clockHours = 24 * 60 * 60 * 1000;
        long midnight = (currentTime + clockHours) - (currentTime % clockHours);

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, midnight, pendingIntent);
        } catch (SecurityException e) {
            LoggerUtils.d("CommonUtil", "Failed to schedule exact alarm: " + e.getMessage());
        }
    }

    /**
     * Returns the device's Android ID (SSAID).
     *
     * @param context Android {@link Context} used to access secure settings.
     * @return Android ID string (may be null on some devices/profiles).
     */
    public static String getAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Collects a set of basic device identifiers and environment fields for reporting/storage.
     *
     * <p>Includes build fields (brand/model/version/etc.) and Wi‑Fi identifiers (SSID/BSSID/MAC when available).</p>
     *
     * @param context Android {@link Context}.
     * @return Map of device detail keys to values.
     */
    public static Map<String, Object> getDeviceDetails(Context context) {
        HashMap<String, Object> deviceDetails = new HashMap<>();

        // Basic Device Info
        deviceDetails.put("brand", Build.BRAND);
        deviceDetails.put("model", Build.MODEL);
        deviceDetails.put("androidVersion", Build.VERSION.RELEASE);
        deviceDetails.put("buildId", Build.ID);
        deviceDetails.put("androidId", getAndroidID(context));
        deviceDetails.put("manufacturer", Build.MANUFACTURER);
        deviceDetails.put("hardware", Build.HARDWARE);
        deviceDetails.put("device", Build.DEVICE);
        deviceDetails.put("product", Build.PRODUCT);
        deviceDetails.put("serial", Build.SERIAL);

        // Telephony Info
//        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//        deviceDetails.put("imei", telephonyManager.getDeviceId());
//        deviceDetails.put("networkOperator", telephonyManager.getNetworkOperatorName());
//        deviceDetails.put("simOperator", telephonyManager.getSimOperatorName());

        // Wi-Fi Info
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            deviceDetails.put("macAddress", wifiInfo.getMacAddress());
            deviceDetails.put("ssid", wifiInfo.getSSID());
            deviceDetails.put("bssid", wifiInfo.getBSSID());
        }
        return deviceDetails;
    }

    /**
     * Checks whether the app currently has broad external storage access required by several features.
     *
     * <p>On pre-R devices this checks {@code WRITE_EXTERNAL_STORAGE}. On R+ this checks
     * {@link Environment#isExternalStorageManager()}.</p>
     *
     * @return {@code true} if file access is available; {@code false} otherwise.
     */
    public static boolean hasFileAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(AppContext.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager();
        }

    }

    /**
     * Updates the Home UI with the cached "shayari of the day", refreshing from Firebase when the day changes.
     *
     * <p>This reads the cached shayari launcher state from SharedPreferences and compares the cached
     * date with {@link LocalDate#now()}. If the cached date is stale, it requests the next shayari
     * from Firebase; otherwise it updates the UI immediately.</p>
     *
     * @param context Android {@link Context}.
     */
    public static void setShayari(Context context) {
        // Get the current shayari data from SharedPreferences
        String currentShayariData = getShayariData(context);
        String[] shayariDataParts = currentShayariData.split(":");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!LocalDate.now().equals(LocalDate.parse(shayariDataParts[1], DateTimeFormatter.ISO_LOCAL_DATE))) {
                FirebaseUtils.getShayariData(String.valueOf(Integer.parseInt(shayariDataParts[0]) + 1));
            } else {
                HomeFragment.updateShayariText(shayariDataParts[2]);
            }
        }
    }

    /**
     * Shows the global progress loader (if present) in {@link com.vikasyadavnsit.cdc.activities.MainActivity}.
     */
    public static void showLoader() {
        if (progressLoader != null) {
            progressLoader.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides the global progress loader (if present) in {@link com.vikasyadavnsit.cdc.activities.MainActivity}.
     */
    public static void hideLoader() {
        if (progressLoader != null) {
            progressLoader.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the current date formatted as {@code yyyy-MM-dd}.
     *
     * @return Current date string.
     */
    public static String getCurrentDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

}
