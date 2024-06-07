package com.vikasyadavnsit.cdc.utils;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for logging messages with different logging levels.
 */
public class LoggerUtils {

    /**
     * Logs a message with a specific tag and logging level.
     *
     * @param tag          The tag for the log message, typically used to indicate the source of the log.
     * @param message      The message to log.
     * @param loggingLevel The level of the log (DEBUG, INFO, WARN, ERROR).
     */
    public static void log(String tag, String message, LoggingLevel loggingLevel) {
        String logTag = StringUtils.isNotBlank(tag) ? tag + ": " : "Logger: ";
        switch (loggingLevel) {
            case DEBUG:
            case INFO:
            case WARN:
            case ERROR:
                FileUtils.appendDataToFile(FileMap.LOG, loggingLevel + " " + logTag + message);
                break;
            default:
                throw new IllegalArgumentException("Invalid logging level: " + loggingLevel);
        }
    }
}
