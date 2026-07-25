package com.vikasyadavnsit.cdc.exception;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.vikasyadavnsit.cdc.activities.MainActivity;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String PREFS_NAME = "crash_prefs";
    private static final String KEY_LAST_CRASH_TIME = "last_crash_time";
    // If two crashes occur within this window, it's a crash loop — don't restart.
    private static final long CRASH_LOOP_THRESHOLD_MS = 3000;

    private final Thread.UncaughtExceptionHandler defaultUEH;
    private final Context context;

    public CustomExceptionHandler(Context context) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.context = context.getApplicationContext();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String crashMsg = sw.toString();
        
        LoggerUtils.e("FATAL_CRASH", "Uncaught Exception in thread [" + thread.getName() + "]:\n" + crashMsg);
        
        // Push a dedicated high-priority log for critical failures
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.pushRemoteLog(
                com.vikasyadavnsit.cdc.enums.LoggingLevel.ERROR, 
                "CRASH_REPORTER", 
                "Application crashed! Thread: " + thread.getName() + " Error: " + throwable.getMessage());

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCrashTime = prefs.getLong(KEY_LAST_CRASH_TIME, 0);
        long now = System.currentTimeMillis();

        if (now - lastCrashTime < CRASH_LOOP_THRESHOLD_MS) {
            // Crash loop detected — let the system handle it instead of restarting again.
            LoggerUtils.e("CustomExceptionHandler", "Crash loop detected, delegating to system handler.");
            defaultUEH.uncaughtException(thread, throwable);
            return;
        }

        // Use commit() (synchronous) so the write lands before we kill the process.
        prefs.edit().putLong(KEY_LAST_CRASH_TIME, now).commit();

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(2);
    }
}

