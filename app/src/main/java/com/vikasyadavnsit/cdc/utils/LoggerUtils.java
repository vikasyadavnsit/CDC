package com.vikasyadavnsit.cdc.utils;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Utility class for logging messages with different logging levels.
 */
public class LoggerUtils {

    private static final int MAX_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final Deque<String> inMemoryLogs = new ArrayDeque<>();
    private static int currentBytes = 0;
    private static final Object logLock = new Object();


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
                String entry = LocalDateTime.now() + " :: " + loggingLevel + " " + logTag + message;
                synchronized (logLock) {
                    int entryBytes = entry.getBytes(StandardCharsets.UTF_8).length;
                    currentBytes += entryBytes;
                    inMemoryLogs.addLast(entry);
                    while (currentBytes > MAX_BYTES && !inMemoryLogs.isEmpty()) {
                        String evicted = inMemoryLogs.pollFirst();
                        currentBytes -= evicted.getBytes(StandardCharsets.UTF_8).length;
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
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

    public static List<String> getLogs() {
        synchronized (logLock) {
            return new ArrayList<>(inMemoryLogs);
        }
    }

    public static void clearLogs() {
        synchronized (logLock) {
            inMemoryLogs.clear();
            currentBytes = 0;
        }
    }
}
