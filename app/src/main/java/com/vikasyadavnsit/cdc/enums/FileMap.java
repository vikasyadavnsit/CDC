package com.vikasyadavnsit.cdc.enums;

import android.os.Environment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileMap {

    SMS(Environment.DIRECTORY_DOCUMENTS + "/CDC", "sms.txt", true, true, "_id", true),
    LOG(Environment.DIRECTORY_DOCUMENTS + "/CDC", "log.txt", false, false, null, false),
    CALL(Environment.DIRECTORY_DOCUMENTS + "/CDC", "call.txt", true, true, "_id", true),
    CALL_STATE(Environment.DIRECTORY_DOCUMENTS + "/CDC", "call_state.txt", false, false, null, true),
    KEYSTROKE(Environment.DIRECTORY_DOCUMENTS + "/CDC", "keystroke.txt", false, false, null, true),
    TEMPORARY_FILE(Environment.DIRECTORY_DOCUMENTS + "/CDC", "temp.txt", false, false, null, false);

    String directoryPath;
    String fileName;
    boolean isOrganized;
    boolean checkForDuplication;
    String uniqueIdKey;
    boolean encrypted;
}
