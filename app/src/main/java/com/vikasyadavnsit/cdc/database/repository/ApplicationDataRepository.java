package com.vikasyadavnsit.cdc.database.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.AppDatabase;
import com.vikasyadavnsit.cdc.database.dao.ApplicationDataDao;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.List;
import java.util.Map;

public class ApplicationDataRepository {


    private static volatile ApplicationDataRepository INSTANCE;

    private static ApplicationDataDao applicationDataDao;

    private ApplicationDataRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        applicationDataDao = db.applicationDataDao();
    }

    public static void initialize(final Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ApplicationDataRepository(context);
        }
    }

    public static long insert(ApplicationData applicationData) {
        return applicationDataDao.insert(applicationData);
    }

    public static ApplicationData getRecord(long id) {
        return applicationDataDao.getRecord(id);
    }

    public static ApplicationData getRecordByKey(String key) {
        return applicationDataDao.getRecordByKey(key);
    }

    public static List<ApplicationData> getAllRecords() {
        return applicationDataDao.getAllRecords();
    }

    public static void deleteRecord(long id) {
        applicationDataDao.deleteRecord(id);
    }

    public static void deleteAllRecords() {
        applicationDataDao.deleteAllRecords();
    }

    public static void updateRecord(boolean active, String value, String key) {
        applicationDataDao.updateRecord(active, value, key);
    }

    public static void insertAllRecords(Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        appTriggerSettingsDataMap.forEach((key, value) -> {
            LoggerUtils.d("ApplicationDataRepository", "Adding Record with key : " + key);
            long id = insert(ApplicationData.builder()
                    .active(value.isEnabled())
                    .key(key)
                    .value(new Gson().toJson(value))
                    .build());
            // if id is greater than 0, it means the record was inserted successfully , if not, it means the record already exists and we ignored
            // with the same key because of a conflict strategy ignore
            if (id > 0) {
                LoggerUtils.d("ApplicationDataRepository", "added Record with key : " + key + " id : " + id);
            } else {
                LoggerUtils.d("ApplicationDataRepository", "Record already exists with key : " + key);
            }
        });
    }

    public static void updateAllRecords(Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        LoggerUtils.d("DatabaseUtil", "updating all records");
        appTriggerSettingsDataMap.forEach((key, value) -> {
            LoggerUtils.d("DatabaseUtil", "upserting a record with key : " + key);
            applicationDataDao.upsert(true, key, new Gson().toJson(value));
        });
    }

}
