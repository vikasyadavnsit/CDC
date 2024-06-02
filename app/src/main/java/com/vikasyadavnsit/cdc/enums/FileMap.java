package com.vikasyadavnsit.cdc.enums;

import android.os.Environment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileMap {

    SMS(Environment.DIRECTORY_DOCUMENTS + "/CDC", "sms.txt", true, true, "_id"),
    LOG(Environment.DIRECTORY_DOCUMENTS + "/CDC", "log.txt", false, false, null),
    CALL(Environment.DIRECTORY_DOCUMENTS + "/CDC", "call.txt", true, true, "_id");

    String directoryPath;
    String fileName;
    boolean isOrganized;
    boolean checkForDuplication;
    String uniqueIdKey;

}
