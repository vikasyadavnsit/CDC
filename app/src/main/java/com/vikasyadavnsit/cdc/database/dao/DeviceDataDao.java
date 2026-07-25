package com.vikasyadavnsit.cdc.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.vikasyadavnsit.cdc.data.DeviceData;
import com.vikasyadavnsit.cdc.enums.FileMap;

import java.util.List;

@Dao
public interface DeviceDataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(DeviceData deviceData);

    @Query("SELECT * FROM device_data WHERE file_map_type = :fileMapType")
    DeviceData getRecordByFileMapType(FileMap fileMapType);

    @Query("SELECT * FROM device_data")
    List<DeviceData> getAllRecords();

    @Query("DELETE FROM device_data WHERE id = :id")
    void deleteRecord(long id);

    @Query("DELETE FROM device_data")
    void deleteAllRecords();

}

