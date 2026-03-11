package com.vikasyadavnsit.cdc.database;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

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
import com.vikasyadavnsit.cdc.utils.CommonUtil;

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
        String dbPath = AppConstants.CDC_DATABASE_PATH;
        if (hasFileAccess() && !CommonUtil.checkAndCreateDirectory(dbPath)) {
            // if path exists
            dbPath += "/" + AppConstants.CDC_DATABASE_NAME;
        } else {
            // if app doesn't have file permission
            dbPath = AppConstants.CDC_DATABASE_NAME;
        }
        return dbPath;
    }

    public static void deleteAllRecords() {
        if (hasFileAccess()) {
            INSTANCE.applicationDataDao().deleteAllRecords();
            INSTANCE.deviceDataDao().deleteAllRecords();
        }
    }
}
