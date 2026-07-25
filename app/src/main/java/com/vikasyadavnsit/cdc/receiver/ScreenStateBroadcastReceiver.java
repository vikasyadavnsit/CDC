package com.vikasyadavnsit.cdc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScreenStateBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenStateBroadcastReceiver";
    private static final int BATCH_INTERVAL_MS = 30_000;

    // Daily counters — reset when the date changes
    private static int screenOnCount  = 0;
    private static int screenOffCount = 0;
    private static int unlockCount    = 0;
    private static String currentDate = "";

    // Batch buffer — all accessed on main thread (onReceive runs on main)
    private static final List<Map<String, Object>> pendingBatch = new ArrayList<>();
    private static boolean flushScheduled = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        long ts = System.currentTimeMillis();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(ts));

        // Reset counters at midnight
        if (!today.equals(currentDate)) {
            currentDate    = today;
            screenOnCount  = 0;
            screenOffCount = 0;
            unlockCount    = 0;
        }

        String event;
        switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_ON:
                screenOnCount++;
                event = "SCREEN_ON";
                break;
            case Intent.ACTION_SCREEN_OFF:
                screenOffCount++;
                event = "SCREEN_OFF";
                break;
            case Intent.ACTION_USER_PRESENT:
                unlockCount++;
                event = "UNLOCKED";
                break;
            default:
                return;
        }

        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(ts));
        LoggerUtils.i(TAG, "Screen Event: " + event + " (Total Today: ON=" + screenOnCount + ", OFF=" + screenOffCount + ", UNLOCK=" + unlockCount + ")");

        // Write to local file immediately
        FileUtils.appendDataToFile(FileMap.SCREEN_STATE, event + " at " + dateStr);

        // Buffer event for batched Firebase upload
        Map<String, Object> data = new HashMap<>();
        data.put("event", event);
        data.put("timestamp", ts);
        data.put("date", today);
        data.put("screenOnCount", screenOnCount);
        data.put("screenOffCount", screenOffCount);
        data.put("unlockCount", unlockCount);
        pendingBatch.add(data);

        // Schedule flush once; subsequent events within the window reuse the same scheduled call
        if (!flushScheduled) {
            flushScheduled = true;
            LoggerUtils.d(TAG, "Scheduling batch upload flush in " + (BATCH_INTERVAL_MS / 1000) + "s");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                flushScheduled = false;
                if (!pendingBatch.isEmpty()) {
                    List<Map<String, Object>> batch = new ArrayList<>(pendingBatch);
                    pendingBatch.clear();
                    LoggerUtils.i(TAG, "Flushing " + batch.size() + " screen state events to Firebase");
                    FirebaseUtils.uploadScreenStateBatch(batch);
                }
            }, BATCH_INTERVAL_MS);
        }
    }
}
