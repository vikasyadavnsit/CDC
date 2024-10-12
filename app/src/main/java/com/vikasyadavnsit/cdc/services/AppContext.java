package com.vikasyadavnsit.cdc.services;


import android.app.Activity;

public class AppContext {
    private static Activity activity;

    public static void init(Activity activity) {
        AppContext.activity = activity;
    }

    public static Activity getContext() {
        return activity;
    }
}

