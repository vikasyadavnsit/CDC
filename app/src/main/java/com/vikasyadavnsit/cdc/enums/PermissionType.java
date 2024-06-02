package com.vikasyadavnsit.cdc.enums;

import android.Manifest;

import com.vikasyadavnsit.cdc.utils.LoggerUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionType {

    CAMERA(new String[]{Manifest.permission.CAMERA}, 1001),
    STORAGE(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1002),
    LOCATION(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1003),
    READ_SMS(new String[]{Manifest.permission.READ_SMS}, 1004),
    READ_CALL_LOG(new String[]{Manifest.permission.READ_CALL_LOG}, 1005),
    WRITE_CALL_LOG(new String[]{Manifest.permission.WRITE_CALL_LOG}, 1006);

    private final String[] permissions;
    private final int requestCode;

    public static PermissionType getPermissionTypeByRequestCode(int requestCode) {
        for (PermissionType permissionType : PermissionType.values()) {
            if (permissionType.getRequestCode() == requestCode) {
                return permissionType;
            }
        }
        LoggerUtil.log("PermissionTye", "getPermissionTypeByRequestCode: " + requestCode + " not found", LoggingLevel.ERROR);
        return null;
    }

}
