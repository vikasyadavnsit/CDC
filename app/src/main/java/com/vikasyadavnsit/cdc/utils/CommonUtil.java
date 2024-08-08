package com.vikasyadavnsit.cdc.utils;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.receiver.ResetBroadcastReceiver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CommonUtil {

    // Method to load a fragment into the FrameLayout
    public static void loadFragment(FragmentManager fragmentManager, Fragment fragment) {
        loadFragment(R.id.main_frame_layout, fragmentManager, fragment);
    }

    public static void loadFragment(int frameLayout, FragmentManager fragmentManager, Fragment fragment) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame_layout, fragment);
        //fragmentTransaction.addToBackStack(fragment.getClass().getName());
        fragmentTransaction.commit();
    }


    // When DataType is List<Map<String, String>>
    public static boolean isDataTypeListOfMap(Object data) {
        if (data instanceof List<?>) {
            List<?> dataList = (List<?>) data;
            return !dataList.isEmpty() && dataList.get(0) instanceof Map<?, ?>;
        }
        return false;
    }

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

    public static boolean checkAndCreateDirectory(String directoryPath) {
        return checkAndCreateDirectory(new File(directoryPath));
    }


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

    public static String getAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

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


}
