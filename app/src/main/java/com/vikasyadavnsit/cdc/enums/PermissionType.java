package com.vikasyadavnsit.cdc.enums;

import android.Manifest;

import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionType {

    CAMERA(new String[]{Manifest.permission.CAMERA}, 1001),
    WRITE_EXTERNAL_STORAGE(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1002),
    READ_EXTERNAL_STORAGE(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1003),
    LOCATION(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1004),
    READ_SMS(new String[]{Manifest.permission.READ_SMS}, 1005),
    READ_CALL_LOG(new String[]{Manifest.permission.READ_CALL_LOG}, 1006),
    WRITE_CALL_LOG(new String[]{Manifest.permission.WRITE_CALL_LOG}, 1007),
    READ_PHONE_STATE(new String[]{Manifest.permission.READ_PHONE_STATE}, 1008),
    MANAGE_EXTERNAL_STORAGE(new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1009),
    FOREGROUND_SERVICE(new String[]{Manifest.permission.FOREGROUND_SERVICE}, 1010),
    //    SYSTEM_ALERT_WINDOW(new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, 1011);
    POST_NOTIFICATIONS(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1012),
    FOREGROUND_SERVICE_HEALTH(new String[]{Manifest.permission.FOREGROUND_SERVICE_HEALTH}, 1013),
    ACTIVITY_RECOGNITION(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1014),
    BODY_SENSORS(new String[]{Manifest.permission.BODY_SENSORS}, 1015),
    HIGH_SAMPLING_RATE_SENSORS(new String[]{Manifest.permission.HIGH_SAMPLING_RATE_SENSORS}, 1016),
    FOREGROUND_SERVICE_MEDIA_PROJECTION(new String[]{Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION}, 1017),
    READ_CONTACTS(new String[]{Manifest.permission.READ_CONTACTS}, 1018),
    SCHEDULE_EXACT_ALARM(new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, 1019),
    RECEIVE_BOOT_COMPLETED(new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED}, 1020),
    SET_ALARM(new String[]{Manifest.permission.SET_ALARM}, 1021),
    ACCESS_WIFI_STATE(new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1022),
    BIND_NOTIFICATION_LISTENER_SERVICE(new String[]{Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE}, 1023);

    private final String[] permissions;
    private final int requestCode;

    public static PermissionType getPermissionTypeByRequestCode(int requestCode) {
        for (PermissionType permissionType : PermissionType.values()) {
            if (permissionType.getRequestCode() == requestCode) {
                return permissionType;
            }
        }
        LoggerUtils.d("PermissionTye", "getPermissionTypeByRequestCode: " + requestCode + " not found");
        return null;
    }

}
