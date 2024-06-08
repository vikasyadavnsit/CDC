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
//    FOREGROUND_SERVICE(new String[]{Manifest.permission.FOREGROUND_SERVICE}, 1010),
//    SYSTEM_ALERT_WINDOW(new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, 1011);
    POST_NOTIFICATIONS(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1012);

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
