package com.vikasyadavnsit.cdc.utils;

import android.util.Log;

import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import org.apache.commons.lang3.StringUtils;

public class LoggerUtil {


    public static void log(String tag, String message, LoggingLevel loggingLevel) {
        String logTag = StringUtils.isNotBlank(tag) ? tag : "Logger";
        switch (loggingLevel) {
            case DEBUG:
                Log.d(logTag, message);
                break;
            case INFO:
                Log.i(logTag, message);
                break;
            case WARN:
                Log.w(logTag, message);
                break;
            case ERROR:
                Log.e(logTag, message);
                break;
        }
    }
}
