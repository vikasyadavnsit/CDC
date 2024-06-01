package com.vikasyadavnsit.cdc.enums;

import android.os.Environment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileMap {

    SMS(Environment.DIRECTORY_DOCUMENTS + "/CDC", "sms.txt"),
    LOG(Environment.DIRECTORY_DOCUMENTS + "/CDC", "log.txt");

    String directoryPath;
    String fileName;

}
