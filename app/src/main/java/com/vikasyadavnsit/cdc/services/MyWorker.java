package com.vikasyadavnsit.cdc.services;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class MyWorker extends Worker {

    public MyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Perform your background task here
        LoggerUtils.d("MyWorker", "Background task is running." + this.getApplicationContext().getPackageName());
        return Result.success();
    }
}

