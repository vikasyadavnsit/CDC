package com.vikasyadavnsit.cdc.database.repository;

import android.content.Context;

import com.vikasyadavnsit.cdc.data.DeviceData;
import com.vikasyadavnsit.cdc.database.AppDatabase;
import com.vikasyadavnsit.cdc.database.dao.DeviceDataDao;

public class DeviceDataRepository {

    private static volatile DeviceDataRepository INSTANCE;
    private static DeviceDataDao deviceDataDao;

    public static void initialize(final Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DeviceDataRepository(context);
        }
    }

    private DeviceDataRepository(Context context) {
        try {
            AppDatabase db = AppDatabase.getDatabase(context);
            deviceDataDao = db.deviceDataDao();
        } catch (Exception e) {
            com.vikasyadavnsit.cdc.utils.LoggerUtils.e("DeviceDataRepository", "Failed to initialize Room database: " + e.getMessage());
        }
    }

    public static long insert(DeviceData deviceData) {
        if (deviceDataDao == null) {
            // Fallback: If we can't save to DB, we should probably upload to Firebase directly if it's important
            // For now, just log and return error
            com.vikasyadavnsit.cdc.utils.LoggerUtils.w("DeviceDataRepository", "Database not available, skipping insert for: " + deviceData.getFileMapType());
            return -1;
        }
        try {
            return deviceDataDao.insert(deviceData);
        } catch (Exception e) {
            com.vikasyadavnsit.cdc.utils.LoggerUtils.e("DeviceDataRepository", "Insert failed: " + e.getMessage());
            return -1;
        }
    }

}