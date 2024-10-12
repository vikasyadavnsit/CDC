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
        AppDatabase db = AppDatabase.getDatabase(context);
        deviceDataDao = db.deviceDataDao();
    }

    public static void deleteAllRecords() {
        deviceDataDao.deleteAllRecords();
    }

    public static long insert(DeviceData deviceData) {
        return deviceDataDao.insert(deviceData);
    }

}