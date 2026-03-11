package com.vikasyadavnsit.cdc.constants;

import android.os.Environment;

import java.time.LocalDate;

public class AppConstants {

    public static final String BLANK_STRING = "";

    //File Constants
    public static final int ALL_PERMISSIONS_REQUEST_CODE = 1000;
    public static final int FILE_APPENDER_BUFFER_SIZE = 64;

    // Crypto Constants
    public static final String CRYPTO_AES_SECRET_KEY = "lgr2sIXLgsywblgrNvqI0m7FJfcJZon6xKQ0ixhPbJw=";
    public static final String CRYTPO_AES_IV = "suXmtxjpt3IQtIYUtb3/3A==";

    //Cursor Constants
    public static final String CURSOR_UNIQUE_ID_KEY_STRING = "_id";

    //Database Constants
    public static final String CDC_DATABASE_NAME = "cdc.db";
    public static final String CDC_DATABASE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS) + "/CDC/db";

    // Activity Request Code
    public static final int MEDIA_PROJECTION_REQUEST_CODE = 3000;
    public static final int ENABLE_ADMIN_REQUEST_CODE = 3001;


    //Intent Actions
    public static final String ACTION_APPLICATION_RESET_USAGE = "com.vikasyadavnsit.cdc.action.APPLICAION_RESET_USAGE";

    //Firebase Properties
    public static final String FIREBASE_DATABASE_REGION_URL = "https://android-cdc-5357e-default-rtdb.asia-southeast1.firebasedatabase.app";
    public static final String FIREBASE_RTDB_BASE_PATH = "cdc/users/";
    public static final String FIREBASE_RTDB_FLAT_USERDETAILS_BASE_PATH = "cdc/flatUserDetails/";
    public static final String FIREBASE_SHAYARI_PATH = "cdc/shayari/";
    public static final String DEFAULT_SHAYARI_TEXT = "वो प्यार जो हकीकत में प्यार होता है # जिन्दगी में सिर्फ एक बार होता है # निगाहों के मिलते मिलते दिल मिल जाये # ऐसा इतेफाक सिर्फ एक बार होता है#";
    public static final String DEFAULT_SHAYARI_LAUNCHER_DATA = "0:" + LocalDate.now() + ":" + AppConstants.DEFAULT_SHAYARI_TEXT;
    public static final String DEFAULT_MESSAGE_TEXT = "Hello Sweetheart ! ## I hope you always remain in good health !  ## Stay Cute and Sweet.";
}
