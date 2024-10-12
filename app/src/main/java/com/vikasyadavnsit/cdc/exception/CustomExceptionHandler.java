package com.vikasyadavnsit.cdc.exception;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

import com.vikasyadavnsit.cdc.activities.MainActivity;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler defaultUEH;
    private Context context;

    public CustomExceptionHandler(Context context) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Display a toast message to the user
        new Thread(() -> {
            Looper.prepare();
            Toast.makeText(context, "An unexpected error occurred. The app will restart.", Toast.LENGTH_LONG).show();
            Looper.loop();
        }).start();

        LoggerUtils.d("CustomExceptionHandler", "An unexpected error occurred. The app will restart." + throwable);

        // Wait for a short period to show the toast message
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Restart the app
//        Intent intent = new Intent(context, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);

        // Kill the process
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(2);

        // Call the default handler (optional, if you want to let the system handle the exception after custom handling)
        // defaultUEH.uncaughtException(thread, throwable);
    }
}

