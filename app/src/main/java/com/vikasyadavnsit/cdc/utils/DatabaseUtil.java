package com.vikasyadavnsit.cdc.utils;


import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateDirectory;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateFile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class DatabaseUtil extends SQLiteOpenHelper {

    private String dbPath;

    DatabaseUtil(Context context, String dbName, String dbPath) {
        super(context, dbName, null, 1);
        this.dbPath = (StringUtils.isNoneBlank(dbPath) ? dbPath : context.getDatabasePath(dbName).getPath());
        CommonUtil.checkAndCreateDirectory(this.dbPath);
        this.dbPath += "/" + dbName;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // No default tables; tables will be created dynamically
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrade as needed
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return SQLiteDatabase.openOrCreateDatabase(new File(dbPath), null);
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return SQLiteDatabase.openOrCreateDatabase(new File(dbPath), null);
    }

    // Method to create a table dynamically
    public void createTable(String createTableQuery) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(createTableQuery);
    }

    // Method to delete a table
    public void deleteTable(String tableName) {
        SQLiteDatabase db = getWritableDatabase();
        String deleteTableQuery = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(deleteTableQuery);
    }

    // Method to add a record to a table
    public long addRecord(String tableName, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(tableName, null, values);
    }

    // Method to get a record from a table
    public Cursor getRecord(String tableName, long id) {
        SQLiteDatabase db = getReadableDatabase();
        String selectQuery = "SELECT * FROM " + tableName + " WHERE id = " + id;
        return db.rawQuery(selectQuery, null);
    }

    // Method to delete a record from a table
    public void deleteRecord(String tableName, long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(tableName, "id = ?", new String[]{String.valueOf(id)});
    }

    public long insertIntoApplicationData(String key, String value) {
        ContentValues appValues = new ContentValues();
        appValues.put("active", true);
        appValues.put("key", key);
        appValues.put("value", value);
        return addRecord("app_data", appValues);
    }
}
