package com.vikasyadavnsit.cdc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.services.DeltaSyncWorker;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class CaptureAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        LoggerUtils.d("CaptureAlarmReceiver", "Scheduled alarm triggered for " + type);
        
        // For scheduled sync, we trigger the DeltaSyncWorker
        DeltaSyncWorker.schedule(context);
        
        // Re-schedule for next day by starting the service to refresh alarms
        CommonUtil.startCaptureService(context);
    }
}
