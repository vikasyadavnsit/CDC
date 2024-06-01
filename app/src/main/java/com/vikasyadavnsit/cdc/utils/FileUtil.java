package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.os.Environment;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FileUtil {

    public static void createFile(Context context, FileMap fileMap, Object data) {
        createAndWriteToFile(context, fileMap, data);
    }


    public static void createAndWriteToFile(Context context, FileMap fileMap, Object data) {

        try {
            File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
            File file = new File(directory, fileMap.getFileName());

            if (checkAndCreateDirectory(directory)) return;
            if (checkAndCreateFile(file)) return;


            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));

            if (fileMap.isOrganized()) {

                // When DataType is List<Map<String, String>>
                if (data instanceof List<?>) {
                    List<?> dataList = (List<?>) data;
                    if (!dataList.isEmpty() && dataList.get(0) instanceof Map<?, ?>) {
                        List<Map<String, String>> tableData = (List<Map<String, String>>) data;
                        StringBuilder buffer = new StringBuilder();

                        buffer.append(IntStream.range(1, 50).mapToObj(i -> "#").collect(Collectors.joining())).append("\n");

                        // Write data rows
                        for (Map<String, String> record : tableData) {
                            for (Map.Entry<String, String> entry : record.entrySet()) {
                                buffer.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                            }
                            buffer.append("\n"); // Add an empty line between records
                        }

                        buffer.append(IntStream.range(1, 50).mapToObj(i -> "#").collect(Collectors.joining())).append("\n");

                        // Write buffer to file
                        bufferedWriter.write(buffer.toString());
                    }

                }
            } else {
                bufferedWriter.write(data.toString());
            }


            bufferedWriter.flush();
            bufferedWriter.close();
            LoggerUtil.log("FileUtil", "Directory : " + directory.getAbsolutePath() + " File : " + fileMap.getFileName() + " created or appended successfully", LoggingLevel.DEBUG);
        } catch (
                IOException e) {
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
