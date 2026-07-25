package com.vikasyadavnsit.cdc.database.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.AppDatabase;
import com.vikasyadavnsit.cdc.database.dao.ApplicationDataDao;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationDataRepository {


    private static volatile ApplicationDataRepository INSTANCE;

    private static ApplicationDataDao applicationDataDao;
    private static final Map<String, ApplicationData> memoryCache = new HashMap<>();

    private ApplicationDataRepository(Context context) {
        try {
            AppDatabase db = AppDatabase.getDatabase(context);
            applicationDataDao = db.applicationDataDao();
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "Failed to initialize Room database: " + e.getMessage());
        }
    }

    public static void initialize(final Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ApplicationDataRepository(context);
        }
    }

    public static long insert(ApplicationData applicationData) {
        if (applicationData != null && applicationData.getKey() != null) {
            memoryCache.put(applicationData.getKey(), applicationData);
        }
        if (applicationDataDao == null) return -1;
        try {
            return applicationDataDao.insert(applicationData);
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "Insert failed: " + e.getMessage());
            return -1;
        }
    }

    public static ApplicationData getRecord(long id) {
        if (applicationDataDao == null) return null;
        try {
            return applicationDataDao.getRecord(id);
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "getRecord failed: " + e.getMessage());
            return null;
        }
    }

    public static ApplicationData getRecordByKey(String key) {
        if (memoryCache.containsKey(key)) {
            return memoryCache.get(key);
        }
        if (applicationDataDao == null) return null;
        try {
            ApplicationData data = applicationDataDao.getRecordByKey(key);
            if (data != null) {
                memoryCache.put(key, data);
            }
            return data;
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "getRecordByKey failed for " + key + ": " + e.getMessage());
            return null;
        }
    }

    public static List<ApplicationData> getAllRecords() {
        if (applicationDataDao == null) return new java.util.ArrayList<>(memoryCache.values());
        try {
            return applicationDataDao.getAllRecords();
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "getAllRecords failed: " + e.getMessage());
            return new java.util.ArrayList<>(memoryCache.values());
        }
    }

    public static void deleteRecord(long id) {
        if (applicationDataDao == null) return;
        try {
            applicationDataDao.deleteRecord(id);
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "deleteRecord failed: " + e.getMessage());
        }
    }

    public static void deleteAllRecords() {
        memoryCache.clear();
        if (applicationDataDao == null) return;
        try {
            applicationDataDao.deleteAllRecords();
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "deleteAllRecords failed: " + e.getMessage());
        }
    }

    public static void updateRecord(boolean active, String value, String key) {
        memoryCache.put(key, ApplicationData.builder().active(active).value(value).key(key).build());
        if (applicationDataDao == null) return;
        try {
            applicationDataDao.updateRecord(active, value, key);
        } catch (Exception e) {
            LoggerUtils.e("ApplicationDataRepository", "updateRecord failed: " + e.getMessage());
        }
    }

    public static void insertAllRecords(Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        appTriggerSettingsDataMap.forEach((key, value) -> {
            LoggerUtils.d("ApplicationDataRepository", "Adding Record with key : " + key);
            ApplicationData data = ApplicationData.builder()
                    .active(value.isEnabled())
                    .key(key)
                    .value(new Gson().toJson(value))
                    .build();
            memoryCache.put(key, data);
            
            if (applicationDataDao != null) {
                try {
                    long id = applicationDataDao.insert(data);
                    if (id > 0) {
                        LoggerUtils.d("ApplicationDataRepository", "added Record with key : " + key + " id : " + id);
                    } else {
                        LoggerUtils.d("ApplicationDataRepository", "Record already exists with key : " + key);
                    }
                } catch (Exception e) {
                    LoggerUtils.e("ApplicationDataRepository", "insertAllRecords failed for " + key + ": " + e.getMessage());
                }
            }
        });
    }

    public static void updateAllRecords(Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        LoggerUtils.d("DatabaseUtil", "updating all records");
        appTriggerSettingsDataMap.forEach((key, value) -> {
            LoggerUtils.d("DatabaseUtil", "upserting a record with key : " + key);
            memoryCache.put(key, ApplicationData.builder().active(value.isEnabled()).key(key).value(new Gson().toJson(value)).build());
            if (applicationDataDao != null) {
                try {
                    applicationDataDao.upsert(true, key, new Gson().toJson(value));
                } catch (Exception e) {
                    LoggerUtils.e("ApplicationDataRepository", "updateAllRecords failed for " + key + ": " + e.getMessage());
                }
            }
        });
    }

}
