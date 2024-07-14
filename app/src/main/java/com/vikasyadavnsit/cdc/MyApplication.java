package com.vikasyadavnsit.cdc;

import android.app.Application;

import com.vikasyadavnsit.cdc.exception.CustomExceptionHandler;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialization tasks
        // e.g., Initialize dependency injection framework
        // e.g., Initialize analytics or crash reporting libraries

        // Set the custom exception handler
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
    }
}

