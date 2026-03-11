package com.vikasyadavnsit.cdc.data;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(tableName = "application_data", indices = {@Index(value = "key", unique = true)})
public class ApplicationData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "active")
    public boolean active;

    @ColumnInfo(name = "key")
    @NonNull
    public String key;

    @ColumnInfo(name = "value")
    @NonNull
    public String value;

    @ColumnInfo(name = "created_at" )
    public String createdAt;

    @ColumnInfo(name = "updated_at")
    public String updatedAt;

}


