package com.vikasyadavnsit.cdc.database.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.vikasyadavnsit.cdc.data.ApplicationData;

import java.util.List;

@Dao
public interface ApplicationDataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ApplicationData applicationData);

    @Query("INSERT OR REPLACE INTO application_data (active, 'key', value) VALUES (:active, :key, :value)")
    void upsert(boolean active, @NonNull String key, String value);

    @Query("SELECT * FROM application_data WHERE id = :id")
    ApplicationData getRecord(long id);

    @Query("SELECT * FROM application_data WHERE key = :key")
    ApplicationData getRecordByKey(String key);

    @Query("SELECT * FROM application_data")
    List<ApplicationData> getAllRecords();

    @Query("DELETE FROM application_data WHERE id = :id")
    void deleteRecord(long id);

    @Query("DELETE FROM application_data")
    void deleteAllRecords();

    @Query("UPDATE application_data SET active = :active, value = :value WHERE `key` = :key")
    void updateRecord(boolean active, String value, String key);
}

