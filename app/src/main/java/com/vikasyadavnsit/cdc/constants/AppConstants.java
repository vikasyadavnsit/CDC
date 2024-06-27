package com.vikasyadavnsit.cdc.constants;

import android.os.Environment;

public class AppConstants {
    public static final int ALL_PERMISSIONS_REQUEST_CODE = 1000;
    public static final int FILE_APPENDER_BUFFER_SIZE = 64;
    public static final String CRYPTO_AES_SECRET_KEY = "lgr2sIXLgsywblgrNvqI0m7FJfcJZon6xKQ0ixhPbJw=";
    public static final String CRYTPO_AES_IV = "suXmtxjpt3IQtIYUtb3/3A==";
    public static final String CDC_DATABASE_NAME = "cdc.db";
    public static final String CDC_DATABASE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS) + "/CDC/db";


    // Activity Request Code
    public static final int MEDIA_PROJECTION_REQUEST_CODE = 3000;


    //Intent Actions
    public static final String ACTION_APPLICATION_RESET_USAGE = "com.vikasyadavnsit.cdc.action.APPLICAION_RESET_USAGE";
}
