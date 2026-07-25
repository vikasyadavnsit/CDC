package com.vikasyadavnsit.cdc.utils;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@SuppressWarnings({"MissingPermission", "deprecation"})
public class DeviceInfoUtils {

    public static Map<String, Object> collectAll(Context context) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("wifi",      safe(() -> collectWifi(context)));
        info.put("bluetooth", safe(() -> collectBluetooth(context)));
        info.put("network",   safe(() -> collectNetwork(context)));
        info.put("battery",   safe(() -> collectBattery(context)));
        info.put("storage",   safe(DeviceInfoUtils::collectStorage));
        info.put("system",    safe(() -> collectSystem(context)));
        info.put("capturedAt", System.currentTimeMillis());
        return info;
    }

    private static Map<String, Object> safe(Callable<Map<String, Object>> fn) {
        try { return fn.call(); }
        catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    // ── WiFi ─────────────────────────────────────────────────────────────────

    private static Map<String, Object> collectWifi(Context context) {
        Map<String, Object> map = new LinkedHashMap<>();
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        boolean wifiEnabled = wm.isWifiEnabled();
        map.put("enabled", wifiEnabled);

        // Diagnostic: check location permission and services (required for getScanResults)
        boolean fineLocationGranted = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean locationEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? lm.isLocationEnabled()
                : lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        map.put("locationPermission", fineLocationGranted);
        map.put("locationEnabled", locationEnabled);

        WifiInfo info = wm.getConnectionInfo();
        String connectedBssid = null;
        if (info != null && info.getNetworkId() != -1) {
            String ssid = info.getSSID();
            if (ssid != null) ssid = ssid.replace("\"", "");
            map.put("ssid", ssid);
            map.put("bssid", info.getBSSID());
            connectedBssid = info.getBSSID();
            map.put("rssi", info.getRssi());
            map.put("signalBars", WifiManager.calculateSignalLevel(info.getRssi(), 5));
            map.put("linkSpeedMbps", info.getLinkSpeed());
            int freq = info.getFrequency();
            map.put("frequencyMHz", freq);
            map.put("band", freq > 0 && freq < 3000 ? "2.4 GHz" : "5 GHz");
            int ip = info.getIpAddress();
            map.put("ipAddress", String.format(Locale.US, "%d.%d.%d.%d",
                    ip & 0xff, ip >> 8 & 0xff, ip >> 16 & 0xff, ip >> 24 & 0xff));
        }

        // Trigger a fresh scan so next cycle has up-to-date results
        if (wifiEnabled) wm.startScan();

        List<Map<String, Object>> nearby = new ArrayList<>();
        if (!fineLocationGranted) {
            map.put("scanError", "ACCESS_FINE_LOCATION not granted");
        } else if (!locationEnabled) {
            map.put("scanError", "Location services disabled");
        } else if (!wifiEnabled) {
            map.put("scanError", "WiFi disabled");
        } else {
            try {
                List<ScanResult> results = wm.getScanResults();
                map.put("scanResultCount", results.size());
                for (ScanResult sr : results) {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("ssid", sr.SSID.isEmpty() ? "(hidden)" : sr.SSID);
                    n.put("level", sr.level);
                    n.put("band", sr.frequency < 3000 ? "2.4G" : "5G");
                    n.put("connected", connectedBssid != null && connectedBssid.equals(sr.BSSID));
                    nearby.add(n);
                }
            } catch (Exception e) {
                map.put("scanError", e.getMessage());
            }
        }
        map.put("nearbyNetworks", nearby);
        return map;
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────

    private static Map<String, Object> collectBluetooth(Context context) {
        Map<String, Object> map = new LinkedHashMap<>();
        BluetoothAdapter adapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = bm != null ? bm.getAdapter() : null;
        } else {
            adapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (adapter == null) { map.put("available", false); return map; }
        map.put("available", true);
        map.put("enabled", adapter.isEnabled());
        map.put("name", adapter.getName());

        List<Map<String, Object>> paired = new ArrayList<>();
        try {
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
            for (BluetoothDevice d : devices) {
                Map<String, Object> dev = new LinkedHashMap<>();
                dev.put("name", d.getName() != null ? d.getName() : "Unknown");
                dev.put("address", d.getAddress());
                dev.put("type", btDeviceType(d.getBluetoothClass()));
                dev.put("connected", isBluetoothConnected(d));
                paired.add(dev);
            }
        } catch (Exception ignored) {}
        map.put("pairedDevices", paired);
        return map;
    }

    private static boolean isBluetoothConnected(BluetoothDevice device) {
        try {
            Method m = BluetoothDevice.class.getDeclaredMethod("isConnected");
            m.setAccessible(true);
            return Boolean.TRUE.equals(m.invoke(device));
        } catch (Exception e) {
            return false;
        }
    }

    private static String btDeviceType(BluetoothClass c) {
        if (c == null) return "Unknown";
        switch (c.getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO: return "Audio/Video";
            case BluetoothClass.Device.Major.PHONE:       return "Phone";
            case BluetoothClass.Device.Major.COMPUTER:    return "Computer";
            case BluetoothClass.Device.Major.WEARABLE:    return "Wearable";
            case BluetoothClass.Device.Major.PERIPHERAL:  return "Peripheral";
            default: return "Other";
        }
    }

    // ── Network / SIM ─────────────────────────────────────────────────────────

    private static Map<String, Object> collectNetwork(Context context) {
        Map<String, Object> map = new LinkedHashMap<>();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo active = cm.getActiveNetworkInfo();
        map.put("connected", active != null && active.isConnected());
        map.put("connectionType", active != null ? active.getTypeName() : "None");
        map.put("mobileDataEnabled", isMobileDataEnabled(cm));
        map.put("roaming", tm.isNetworkRoaming());
        map.put("networkOperator", tm.getNetworkOperatorName());
        map.put("networkType", networkTypeName(tm.getNetworkType()));

        List<Map<String, Object>> sims = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
                if (subs != null) {
                    for (SubscriptionInfo sub : subs) {
                        Map<String, Object> sim = new LinkedHashMap<>();
                        sim.put("slot", sub.getSimSlotIndex() + 1);
                        sim.put("carrier", sub.getCarrierName().toString());
                        sim.put("number", sub.getNumber() != null ? sub.getNumber() : "");
                        sim.put("country", sub.getCountryIso().toUpperCase(Locale.US));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            TelephonyManager tmSub = tm.createForSubscriptionId(sub.getSubscriptionId());
                            sim.put("networkType", networkTypeName(tmSub.getNetworkType()));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                SignalStrength ss = tmSub.getSignalStrength();
                                if (ss != null) sim.put("signalLevel", ss.getLevel());
                            }
                        }
                        sims.add(sim);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (sims.isEmpty()) {
            Map<String, Object> sim = new LinkedHashMap<>();
            sim.put("slot", 1);
            sim.put("carrier", tm.getSimOperatorName());
            sim.put("country", tm.getSimCountryIso().toUpperCase(Locale.US));
            sims.add(sim);
        }
        map.put("sims", sims);
        return map;
    }

    private static boolean isMobileDataEnabled(ConnectivityManager cm) {
        try {
            java.lang.reflect.Method m = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            m.setAccessible(true);
            return Boolean.TRUE.equals(m.invoke(cm));
        } catch (Exception e) { return false; }
    }

    private static String networkTypeName(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR:     return "5G NR";
            case TelephonyManager.NETWORK_TYPE_LTE:    return "4G LTE";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:   return "3G HSPA";
            case TelephonyManager.NETWORK_TYPE_UMTS:   return "3G UMTS";
            case TelephonyManager.NETWORK_TYPE_EDGE:   return "2G EDGE";
            case TelephonyManager.NETWORK_TYPE_GPRS:   return "2G GPRS";
            default: return "Unknown";
        }
    }

    // ── Battery / USB ─────────────────────────────────────────────────────────

    private static Map<String, Object> collectBattery(Context context) {
        Map<String, Object> map = new LinkedHashMap<>();
        Intent b = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return map;

        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level >= 0 && scale > 0) map.put("percent", (int) (level * 100f / scale));

        int plugged = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String chargingType;
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_USB:      chargingType = "USB";      break;
            case BatteryManager.BATTERY_PLUGGED_AC:       chargingType = "AC";       break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: chargingType = "Wireless"; break;
            default:                                      chargingType = "None";
        }
        map.put("usbConnected", plugged == BatteryManager.BATTERY_PLUGGED_USB);
        map.put("chargingType", chargingType);
        map.put("charging", plugged > 0);

        int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        map.put("status", status == BatteryManager.BATTERY_STATUS_FULL ? "Full" :
                status == BatteryManager.BATTERY_STATUS_CHARGING ? "Charging" :
                status == BatteryManager.BATTERY_STATUS_DISCHARGING ? "Discharging" : "Unknown");

        int health = b.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        String healthName;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:     healthName = "Good";     break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: healthName = "Overheat"; break;
            case BatteryManager.BATTERY_HEALTH_DEAD:     healthName = "Dead";     break;
            case BatteryManager.BATTERY_HEALTH_COLD:     healthName = "Cold";     break;
            default:                                     healthName = "Unknown";
        }
        map.put("health", healthName);
        int temp = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        map.put("tempC", temp / 10.0f);
        return map;
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    private static Map<String, Object> collectStorage() {
        Map<String, Object> map = new LinkedHashMap<>();
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long bs = stat.getBlockSizeLong();
        long total = stat.getBlockCountLong() * bs;
        long free  = stat.getAvailableBlocksLong() * bs;
        map.put("internalTotalBytes",     total);
        map.put("internalFreeBytes",      free);
        map.put("internalUsedBytes",      total - free);
        map.put("internalTotalFormatted", formatBytes(total));
        map.put("internalFreeFormatted",  formatBytes(free));
        map.put("internalUsedFormatted",  formatBytes(total - free));

        boolean extMounted = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        map.put("externalAvailable", extMounted);
        if (extMounted) {
            StatFs extStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long ebs = extStat.getBlockSizeLong();
            long etotal = extStat.getBlockCountLong() * ebs;
            long efree  = extStat.getAvailableBlocksLong() * ebs;
            map.put("externalTotalBytes",     etotal);
            map.put("externalFreeBytes",      efree);
            map.put("externalTotalFormatted", formatBytes(etotal));
            map.put("externalFreeFormatted",  formatBytes(efree));
        }
        return map;
    }

    // ── System ────────────────────────────────────────────────────────────────

    private static Map<String, Object> collectSystem(Context context) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("manufacturer",     Build.MANUFACTURER);
        map.put("brand",            Build.BRAND);
        map.put("model",            Build.MODEL);
        map.put("device",           Build.DEVICE);
        map.put("androidVersion",   Build.VERSION.RELEASE);
        map.put("sdkInt",           Build.VERSION.SDK_INT);
        map.put("buildId",          Build.ID);
        map.put("hardware",         Build.HARDWARE);
        map.put("cpuAbi",           Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                                        ? Build.SUPPORTED_ABIS[0] : "Unknown");
        map.put("board",            Build.BOARD);
        map.put("bootloader",       Build.BOOTLOADER);
        map.put("securityPatch",    Build.VERSION.SECURITY_PATCH);
        map.put("fingerprint",      Build.FINGERPRINT);
        map.put("accessibilityEnabled", AccessibilityUtils.isAccessibilityServiceEnabled(context, 
                com.vikasyadavnsit.cdc.services.CDCAccessibilityService.class));

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mem);
        map.put("totalRamBytes",     mem.totalMem);
        map.put("availRamBytes",     mem.availMem);
        map.put("totalRamFormatted", formatBytes(mem.totalMem));
        map.put("availRamFormatted", formatBytes(mem.availMem));
        map.put("lowMemory",         mem.lowMemory);

        long uptimeMs = SystemClock.elapsedRealtime();
        map.put("uptimeHours", String.format(Locale.US, "%.1f h", uptimeMs / 3_600_000.0));
        return map;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static String formatBytes(long bytes) {
        if (bytes < 1024)            return bytes + " B";
        if (bytes < 1_048_576)       return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1_073_741_824)   return String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0);
        return String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0);
    }
}
