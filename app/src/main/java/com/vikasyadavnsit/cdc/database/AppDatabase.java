package com.vikasyadavnsit.cdc.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.DeviceData;
import com.vikasyadavnsit.cdc.database.dao.ApplicationDataDao;
import com.vikasyadavnsit.cdc.database.dao.DeviceDataDao;

@Database(entities = {ApplicationData.class, DeviceData.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ApplicationDataDao applicationDataDao();

    public abstract DeviceDataDao deviceDataDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, getDbPath())
                            .fallbackToDestructiveMigration() // Handle migration
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static @NonNull String getDbPath() {
        if (com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess()) {
            String dbDir = AppConstants.CDC_DATABASE_PATH;
            java.io.File dir = new java.io.File(dbDir);
            if (dir.exists() || dir.mkdirs()) {
                return dbDir + "/" + AppConstants.CDC_DATABASE_NAME;
            }
        }
        // Fallback to internal storage if "All Files Access" is not granted or directory creation fails
        return AppConstants.CDC_DATABASE_NAME;
    }
}
