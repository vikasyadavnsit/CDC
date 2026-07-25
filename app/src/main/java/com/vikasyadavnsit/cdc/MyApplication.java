package com.vikasyadavnsit.cdc;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;
import com.vikasyadavnsit.cdc.exception.CustomExceptionHandler;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.d(TAG, "Application onCreate - System Bootstrapping");
        
        initializeFirebase();
        
        LoggerUtils.d(TAG, "Scheduling initial background DeltaSyncWorker");
        com.vikasyadavnsit.cdc.services.DeltaSyncWorker.schedule(this);
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
    }

    public void initializeFirebase() {
        FirebaseOptions customOptions = getFirebaseOptions();
        
        try {
            FirebaseApp existingApp = null;
            try {
                existingApp = FirebaseApp.getInstance();
            } catch (IllegalStateException ignored) {}

            if (customOptions != null) {
                if (existingApp != null) {
                    // Check if current initialization matches custom config
                    String currentAppId = existingApp.getOptions().getApplicationId();
                    if (!currentAppId.equals(customOptions.getApplicationId())) {
                        LoggerUtils.w(TAG, "Default FirebaseApp mismatch. Overriding with custom config.");
                        existingApp.delete();
                        FirebaseApp.initializeApp(this, customOptions);
                    } else {
                        LoggerUtils.i(TAG, "Firebase initialized with correct custom config.");
                    }
                } else {
                    LoggerUtils.i(TAG, "Initializing Firebase with custom config.");
                    FirebaseApp.initializeApp(this, customOptions);
                }
            } else {
                if (existingApp == null) {
                    LoggerUtils.w(TAG, "No Firebase config found in SharedPrefs or resources!");
                } else {
                    LoggerUtils.i(TAG, "Firebase initialized using default google-services.json");
                }
            }
        } catch (Exception e) {
            LoggerUtils.e(TAG, "Error during Firebase initialization override: " + e.getMessage());
        }
    }

    private FirebaseOptions getFirebaseOptions() {
        String customConfig = SharedPreferenceUtils.getFirebaseConfig(this);
        if (customConfig != null) {
            try {
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(customConfig).getAsJsonObject();
                com.google.gson.JsonObject projectInfo = root.getAsJsonObject("project_info");
                com.google.gson.JsonObject client = root.getAsJsonArray("client").get(0).getAsJsonObject();
                
                String apiKey = client.getAsJsonArray("api_key").get(0).getAsJsonObject().get("current_key").getAsString();
                String appId = client.getAsJsonObject("client_info").get("mobilesdk_app_id").getAsString();
                String dbUrl = projectInfo.get("firebase_url").getAsString();
                String projectId = projectInfo.get("project_id").getAsString();
                String projectNumber = projectInfo.get("project_number").getAsString();

                return new FirebaseOptions.Builder()
                        .setApplicationId(appId)
                        .setApiKey(apiKey)
                        .setDatabaseUrl(dbUrl)
                        .setProjectId(projectId)
                        .setGcmSenderId(projectNumber)
                        .build();
            } catch (Exception e) {
                LoggerUtils.e(TAG, "Failed to parse stored Firebase config: " + e.getMessage());
            }
        }
        return null;
    }
}
