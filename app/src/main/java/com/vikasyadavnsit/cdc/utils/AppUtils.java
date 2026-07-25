package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppUtils {

    public static List<Map<String, String>> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<Map<String, String>> result = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("_id", app.packageName);
            entry.put("packageName", app.packageName);
            entry.put("name", app.loadLabel(pm).toString());
            entry.put("isSystem", String.valueOf((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0));

            try {
                PackageInfo pi = pm.getPackageInfo(app.packageName, 0);
                entry.put("versionName", pi.versionName != null ? pi.versionName : "");
                long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        ? pi.getLongVersionCode()
                        : pi.versionCode;
                entry.put("versionCode", String.valueOf(versionCode));
                entry.put("firstInstallTime", String.valueOf(pi.firstInstallTime));
                entry.put("lastUpdateTime", String.valueOf(pi.lastUpdateTime));
            } catch (PackageManager.NameNotFoundException e) {
                entry.put("versionName", "");
                entry.put("versionCode", "0");
                entry.put("firstInstallTime", "0");
                entry.put("lastUpdateTime", "0");
            }

            result.add(entry);
        }

        result.sort((a, b) -> {
            String nameA = a.getOrDefault("name", "");
            String nameB = b.getOrDefault("name", "");
            return nameA.compareToIgnoreCase(nameB);
        });

        return result;
    }

    public static String getAppName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}
