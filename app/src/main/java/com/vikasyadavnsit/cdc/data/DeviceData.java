package com.vikasyadavnsit.cdc.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.vikasyadavnsit.cdc.enums.FileMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(tableName = "device_data")
public class DeviceData {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "file_map_type")
    @NonNull
    public FileMap fileMapType;

    @ColumnInfo(name = "value")
    @NonNull
    public String value;

    @ColumnInfo(name = "created_at")
    public String createdAt;

}
