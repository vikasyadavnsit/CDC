package com.vikasyadavnsit.cdc.utils;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for logging messages with different logging levels.
 */
public class LoggerUtils {


    private static void log(String tag, String message, LoggingLevel loggingLevel) {
        String logTag = StringUtils.isNotBlank(tag) ? tag + ": " : "Logger: ";
        switch (loggingLevel) {
            case DEBUG:
            case INFO:
            case WARN:
            case ERROR:
                Log.d(logTag, message);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                    FileUtils.appendDataToFile(FileMap.LOG, loggingLevel + " " + logTag + message);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid logging level: " + loggingLevel);
        }
    }

    public static void d(String tag, String message) {
        log(tag, message, LoggingLevel.DEBUG);
    }

    public static void e(String tag, String message) {
        log(tag, message, LoggingLevel.ERROR);
    }
}
