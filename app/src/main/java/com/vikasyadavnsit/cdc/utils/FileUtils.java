package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;
import com.vikasyadavnsit.cdc.service.FileAppender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for file operations.
 */
public class FileUtils {

    /**
     * Appends data to a file based on the specified FileMap.
     *
     * @param fileMap The file map.
     * @param data    The data to append.
     */
    public static void appendDataToFile(FileMap fileMap, Object data) {
        if (fileMap.isOrganized()) {
            checkAndAppendDataInOrganizedFile(fileMap, data);
        } else {
            appendDataInUnorganizedFile(fileMap, data);
        }
    }

    /**
     * Appends data to an unorganized file.
     *
     * @param data The data to append.
     */
    private static void appendDataInUnorganizedFile(FileMap fileMap, Object data) {
        FileAppender.add(fileMap, data);
    }


    /**
     * Checks and appends data in an organized file.
     *
     * @param fileMap The file map.
     * @param data    The data to append.
     */
    private static void checkAndAppendDataInOrganizedFile(FileMap fileMap, Object data) {
        try {
            LoggerUtils.log("FileUtil", "Checking and appending data to organized file", LoggingLevel.DEBUG);
            File directory = getExternalStoragePublicDirectory(fileMap);
            File file = new File(directory, fileMap.getFileName());

            if (checkAndCreateDirectory(directory) || checkAndCreateFile(file)) {
                LoggerUtils.log("FileUtil", "Directory or file does not exist, creation failed", LoggingLevel.ERROR);
                return;
            }

            LoggerUtils.log("FileUtil", "Appending to file " + file.getAbsolutePath(), LoggingLevel.DEBUG);

            Set<String> uniqueIds = new HashSet<>();
            if (fileMap.isCheckForDuplication()) {
                uniqueIds = getAllUniqueIdsInFile(file, fileMap);
            }

            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true))) {
                appendToBufferWriterInOrganizedFile(fileMap, data, uniqueIds, bufferedWriter);
                bufferedWriter.flush();
            }

            LoggerUtils.log("FileUtil", "Data appended to " + file.getAbsolutePath() + " successfully", LoggingLevel.DEBUG);
        } catch (Exception e) {
            LoggerUtils.log("FileUtil", "Failed to append data to file: " + e.toString(), LoggingLevel.ERROR);
        }
    }

    /**
     * Appends data to a BufferedWriter for an organized file.
     *
     * @param fileMap        The file map.
     * @param data           The data to append.
     * @param uniqueIds      Set of unique IDs to check for duplication.
     * @param bufferedWriter The BufferedWriter to write data to.
     * @throws IOException If an I/O error occurs.
     */
    private static void appendToBufferWriterInOrganizedFile(FileMap fileMap, Object data, Set<String> uniqueIds, BufferedWriter bufferedWriter) throws IOException {
        if (CommonUtil.isDataTypeListOfMap(data)) {
            List<Map<String, String>> tableData = (List<Map<String, String>>) data;
            StringBuilder buffer = new StringBuilder();
            int recordCount = 0;

            // Write data rows
            for (Map<String, String> record : tableData) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    buffer.append("Record:").append(++recordCount).append(" DateTime:").append(Instant.now()).append("\n");
                }
                if (fileMap.isCheckForDuplication() && uniqueIds.contains(record.get(fileMap.getUniqueIdKey()))) {
                    continue;
                }
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    buffer.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                buffer.append("\n"); // Add an empty line between records
            }

            bufferedWriter.write(buffer.toString());
        }
    }

    /**
     * Retrieves all unique IDs from a file.
     *
     * @param file    The file to read from.
     * @param fileMap The file map.
     * @return A set of unique IDs.
     */
    private static Set<String> getAllUniqueIdsInFile(File file, FileMap fileMap) {
        LoggerUtils.log("FileUtil", "Retrieving all unique IDs from file", LoggingLevel.DEBUG);
        Set<String> idSet = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(fileMap.getUniqueIdKey() + ":")) {
                    String id = line.substring(fileMap.getUniqueIdKey().length() + 1).trim(); // Extract the ID value
                    idSet.add(id);
                }
            }
        } catch (IOException e) {
            LoggerUtils.log("FileUtil", "Failed to read file for duplicate records: " + e.toString(), LoggingLevel.ERROR);
        }
        return idSet;
    }

    /**
     * Checks if the file exists, and if not, creates it.
     *
     * @param file The file to check and create.
     * @return True if the file creation failed, otherwise false.
     * @throws IOException If an I/O error occurs.
     */
    public static boolean checkAndCreateFile(File file) throws IOException {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                LoggerUtils.log("FileUtil", "Failed to create file", LoggingLevel.ERROR);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the directory exists, and if not, creates it.
     *
     * @param directory The directory to check and create.
     * @return True if the directory creation failed, otherwise false.
     */
    public static boolean checkAndCreateDirectory(File directory) {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                LoggerUtils.log("FileUtil", "Failed to create directory", LoggingLevel.ERROR);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if internal storage is available.
     *
     * @param context The context.
     * @return True if internal storage is available, otherwise false.
     */
    public static boolean isInternalStorageAvailable(Context context) {
        File internalStorageDir = context.getFilesDir();
        boolean isAvailable = internalStorageDir != null;
        LoggerUtils.log("FileUtil", "Internal storage is " + (isAvailable ? "available: " + internalStorageDir.getAbsolutePath() : "not available"), isAvailable ? LoggingLevel.DEBUG : LoggingLevel.ERROR);
        return isAvailable;
    }

    /**
     * Checks if external storage is available.
     *
     * @param context The context.
     * @return True if external storage is available, otherwise false.
     */
    public static boolean isExternalStorageAvailable(Context context) {
        String externalStorageState = Environment.getExternalStorageState();
        boolean isAvailable = Environment.MEDIA_MOUNTED.equals(externalStorageState);

        if (isAvailable) {
            File externalStorageDir = context.getExternalFilesDir(null);
            LoggerUtils.log("FileUtil", "External storage is available: " + externalStorageDir.getAbsolutePath(), LoggingLevel.DEBUG);
        } else {
            LoggerUtils.log("FileUtil", "External storage is not available", LoggingLevel.ERROR);
        }
        return isAvailable;
    }

    /**
     * Retrieves the public directory for external storage.
     *
     * @param fileMap The file map.
     * @return The public directory file.
     */
    private static File getExternalStoragePublicDirectory(FileMap fileMap) {
        return Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
    }
}
