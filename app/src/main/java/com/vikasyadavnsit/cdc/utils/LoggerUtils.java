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


    /**
     * Logs a message to Logcat and optionally appends it to the app's log file.
     *
     * <p>When external file access is available, this also writes a line to {@link FileMap#LOG}
     * via {@link FileUtils#appendDataToFile(FileMap, Object)}.</p>
     *
     * @param tag          Logical tag/category for the message.
     * @param message      Message content.
     * @param loggingLevel The severity/level to use.
     */
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

    /**
     * Convenience wrapper for debug-level logging.
     *
     * @param tag     Logical tag/category for the message.
     * @param message Message content.
     */
    public static void d(String tag, String message) {
        log(tag, message, LoggingLevel.DEBUG);
    }

    /**
     * Convenience wrapper for error-level logging.
     *
     * @param tag     Logical tag/category for the message.
     * @param message Message content.
     */
    public static void e(String tag, String message) {
        log(tag, message, LoggingLevel.ERROR);
    }
}
