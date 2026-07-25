package com.vikasyadavnsit.cdc.utils;

import com.vikasyadavnsit.cdc.services.ScheduledWorker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorUtils {

    public static void executeTaskPeriodically(Long timeInterval, TimeUnit timeUnit) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new ScheduledWorker(),
                0, timeInterval, timeUnit);
    }

}
