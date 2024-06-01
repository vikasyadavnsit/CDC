package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.os.Environment;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileUtil {

    public static void createFile(Context context, FileMap fileMap, Object data) {
        createAndWriteToFile(context, fileMap.getDirectoryPath(), fileMap.getFileName(), data);
    }


    public static void createAndWriteToFile(Context context, String directoryPath, String fileName, Object data) {

        try {
            File directory = Environment.getExternalStoragePublicDirectory(directoryPath);
            File file = new File(directory, fileName);

            if (checkAndCreateDirectory(directory)) return;
            if (checkAndCreateFile(file)) return;


            PrintWriter writer = new PrintWriter(new FileWriter(file, true));

//            // Write data rows
//            for (String[] record : tableData) {
//                writer.println("Name: " + record[0]);
//                writer.println("Age: " + record[1]);
//                writer.println("Country: " + record[2]);
//                writer.println(); // Add an empty line between records
//            }

            writer.flush();
            writer.close();
            LoggerUtil.log("FileUtil", "Directory : " + directory.getAbsolutePath() + " File : " + fileName + " created or appended successfully", LoggingLevel.DEBUG);
        } catch (IOException e) {
            LoggerUtil.log("FileUtil", "Failed to create file" + e.toString(), LoggingLevel.ERROR);
        }
    }

    private static boolean checkAndCreateFile(File file) throws IOException {
        // Check if the file exists
        if (!file.exists()) {
            // If the file doesn't exist, create it
            if (!file.createNewFile()) {
                // File creation failed
                LoggerUtil.log("FileAction", "Failed to create file", LoggingLevel.ERROR);
                return true;
            }
        }
        return false;
    }

    private static boolean checkAndCreateDirectory(File directory) {
        // Make sure the directory exists or create it if it doesn't
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                // Directory creation failed
                LoggerUtil.log("FileUtil", "Failed to create directory", LoggingLevel.ERROR);
                return true;
            }
        }
        return false;
    }


    // Check internal storage availability
    private static boolean isInternalStorageAvailable(Context context) {
        File internalStorageDir = context.getFilesDir();
        boolean isInternalStorageAvailable = internalStorageDir != null;
        // Display the results
        if (isInternalStorageAvailable) {
            LoggerUtil.log("FileUtil", "Internal storage is available: " + internalStorageDir.getAbsolutePath(), LoggingLevel.DEBUG);
        } else {
            LoggerUtil.log("FileUtil", "Internal storage is not available", LoggingLevel.ERROR);
        }
        return isInternalStorageAvailable;
    }

    // Check external storage availability
    private static boolean isExternalStorageAvailable(Context context) {
        String externalStorageState = Environment.getExternalStorageState();
        boolean isExternalStorageAvailable = Environment.MEDIA_MOUNTED.equals(externalStorageState);

        // Display the results
        if (isExternalStorageAvailable) {
            File externalStorageDir = context.getExternalFilesDir(null);
            LoggerUtil.log("FileUtil", "External storage is available: " + externalStorageDir.getAbsolutePath(), LoggingLevel.DEBUG);
        } else {
            LoggerUtil.log("FileUtil", "External storage is not available", LoggingLevel.DEBUG);
        }
        return isExternalStorageAvailable;
    }

}
