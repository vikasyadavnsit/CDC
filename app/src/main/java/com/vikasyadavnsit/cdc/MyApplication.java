package com.vikasyadavnsit.cdc;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialization tasks
        // e.g., Initialize dependency injection framework
        // e.g., Initialize analytics or crash reporting libraries
    }
}

