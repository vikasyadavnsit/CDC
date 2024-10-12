package com.vikasyadavnsit.cdc.constants;

public class DBConstants {

    public static final String APPLICATION_TABLE_NAME = "app_data";
    public static final String CREATE_APPLICATION_DATA_TABLE = "CREATE TABLE IF NOT EXISTS " + APPLICATION_TABLE_NAME + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "active BOOLEAN DEFAULT TRUE, "
            + "key VARCHAR(100) NOT NULL UNIQUE, "
            + "json_data TEXT NOT NULL, "
            + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)";

}
