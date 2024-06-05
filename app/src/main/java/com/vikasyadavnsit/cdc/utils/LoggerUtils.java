package com.vikasyadavnsit.cdc.utils;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import org.apache.commons.lang3.StringUtils;

public class LoggerUtils {


    public static void log(String tag, String message, LoggingLevel loggingLevel) {
        String logTag = StringUtils.isNotBlank(tag) ? tag + ": " : "Logger";
        switch (loggingLevel) {
            case DEBUG:
            case INFO:
            case WARN:
            case ERROR:
                FileUtils.appendDataToFile(FileMap.LOG, loggingLevel + " " + logTag + message);
                break;
        }
    }
}
