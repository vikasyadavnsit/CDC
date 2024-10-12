package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

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
            case INFO:
            case WARN:
            case DEBUG:
                Log.d(logTag, message);
                if (hasFileAccess()) {
                    FileUtils.appendDataToFile(FileMap.LOG, loggingLevel + " " + logTag + message);
                }
                break;
            case ERROR:
                Log.e(logTag, message);
                if (hasFileAccess()) {
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
