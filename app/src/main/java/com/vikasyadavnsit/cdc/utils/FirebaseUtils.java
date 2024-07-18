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
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.User;

import java.util.Map;

public class FirebaseUtils {

    // Get a reference to the database
    public static FirebaseDatabase getDatabase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                AppConstants.FIREBASE_DATABASE_REGION_URL);
        database.setPersistenceEnabled(true);
        return database;
    }

    public static DatabaseReference getDbRef(String path) {
        DatabaseReference reference = getDatabase().getReference(path);
        reference.keepSynced(true);
        //reference.onDisconnect().setValue("I disconnected!");
        return reference;
    }

    private String getPath(Context context, String path) {
        return getBasePath(context) + path;
    }

    private static String getBasePath(Context context) {
        return AppConstants.FIREBASE_RTDB_BASE_PATH + getAndroidID(context);
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
                        User.AppSettings
                                .builder()
                                .appTriggerSettingsDataMap(
                                        Map.of("",
                                                User.AppTriggerSettingsData.builder()
                                                        .build())
                                )
                                .appSettingsMap(Map.of())
                                .build()
                )
                .userSettings(Map.of())
                .build();
    }

}
