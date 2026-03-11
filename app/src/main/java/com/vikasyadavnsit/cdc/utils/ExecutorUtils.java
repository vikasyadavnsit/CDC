package com.vikasyadavnsit.cdc.utils;

import com.vikasyadavnsit.cdc.services.ScheduledWorker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorUtils {

    /**
     * Schedules {@link ScheduledWorker} to run periodically on a single-threaded scheduler.
     *
     * <p>This uses {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
     * with an initial delay of 0.</p>
     *
     * @param timeInterval The fixed period between task executions.
     * @param timeUnit     The time unit for {@code timeInterval}.
     */
    public static void executeTaskPeriodically(Long timeInterval, TimeUnit timeUnit) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new ScheduledWorker(),
                0, timeInterval, timeUnit);
    }

}
