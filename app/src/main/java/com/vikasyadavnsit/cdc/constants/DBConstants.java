package com.vikasyadavnsit.cdc.constants;

public class DBConstants {

    public static final String CREATE_APPLICATION_DATA_TABLE = "CREATE TABLE IF NOT EXISTS app_data ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "active BOOLEAN DEFAULT TRUE, "
            + "key VARCHAR(100) NOT NULL UNIQUE, "
            + "value VARCHAR(200) NOT NULL, "
            + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)";

}
