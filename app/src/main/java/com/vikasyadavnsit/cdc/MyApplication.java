package com.vikasyadavnsit.cdc;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.exception.CustomExceptionHandler;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:513321643745:android:20967ee64ef34f332491a0")
                    .setApiKey("AIzaSyDO8EwzPzVClp2cK8nQg_QoYqYAh9nUNio")
                    .setDatabaseUrl(AppConstants.FIREBASE_DATABASE_REGION_URL)
                    .setProjectId("android-cdc-5357e")
                    .setStorageBucket("android-cdc-5357e.appspot.com")
                    .setGcmSenderId("513321643745")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
    }
}

