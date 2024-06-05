package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;
import com.vikasyadavnsit.cdc.service.FileUtilsFileWriter;

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


public class FileUtils {


    public static void appendDataToFile(FileMap fileMap, Object data) {
        checkCreateAndWriteToFileInADirectory(fileMap, data);
    }

    public static void checkCreateAndWriteToFileInADirectory(FileMap fileMap, Object data) {

        File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
        File file = new File(directory, fileMap.getFileName());

        try {

            if (checkAndCreateDirectory(directory)) return;
            if (checkAndCreateFile(file)) return;

            Set<String> uniqueIds = new HashSet<>();
            if (fileMap.isCheckForDuplication()) uniqueIds = getAllUniqueIdsInFile(file, fileMap);

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            if (fileMap.isOrganized()) {
                checkAndAppendDataInOrganisedFile(fileMap, data, uniqueIds, bufferedWriter);
            } else {
                appendDataInUnOrganisedFile(data, bufferedWriter);
            }

            bufferedWriter.flush();
            bufferedWriter.close();
           // LoggerUtils.log("FileUtil", "Directory : " + directory.getAbsolutePath() + " File : " + fileMap.getFileName() + " created or appended successfully", LoggingLevel.DEBUG);
        } catch (IOException e) {
            e.printStackTrace();
            //LoggerUtils.log("FileUtil", "Failed to create file" + e.toString(), LoggingLevel.ERROR);
        }
    }

    private static void appendDataInUnOrganisedFile(Object data, BufferedWriter bufferedWriter) throws IOException {
        //writing 20kb data in a go
        FileUtilsFileWriter.getInstance(1024 * 20).add(data, bufferedWriter);
    }

    private static void checkAndAppendDataInOrganisedFile(FileMap fileMap, Object data, Set<String> uniqueIds, BufferedWriter bufferedWriter) throws IOException {
        if (CommonUtil.isDataTypeListOfMap(data)) {
            List<Map<String, String>> tableData = (List<Map<String, String>>) data;
            StringBuilder buffer = new StringBuilder();
            int recordCount = 0;
            //buffer.append(IntStream.range(1, 50).mapToObj(i -> "#").collect(Collectors.joining())).append("\n");

            // Write data rows
            for (Map<String, String> record : tableData) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    buffer.append("Record:").append(++recordCount).append(" DateTime:").append(Instant.now()).append("\n");
                }
                if (fileMap.isCheckForDuplication() && uniqueIds.contains(record.get(fileMap.getUniqueIdKey()))) {
                    break;
                }
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    buffer.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                buffer.append("\n"); // Add an empty line between records
            }

            //buffer.append(IntStream.range(1, 50).mapToObj(i -> "#").collect(Collectors.joining())).append("\n");
            // Write buffer to file
            bufferedWriter.write(buffer.toString());
        }
    }

    private static Set<String> getAllUniqueIdsInFile(File file, FileMap fileMap) {
        Set<String> idSet = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(fileMap.getUniqueIdKey() + ":")) {
                    String id = line.substring(4).trim(); // Extract the ID value
                    idSet.add(id);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
           // LoggerUtils.log("FileUtil", "Failed to read file for duplicate records", LoggingLevel.ERROR);
        }
        return idSet;
    }

    private static boolean checkAndCreateFile(File file) throws IOException {
        // Check if the file exists
        if (!file.exists()) {
            // If the file doesn't exist, create it
            if (!file.createNewFile()) {
                // File creation failed
               // LoggerUtils.log("FileAction", "Failed to create file", LoggingLevel.ERROR);
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
              //  LoggerUtils.log("FileUtil", "Failed to create directory", LoggingLevel.ERROR);
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
          //  LoggerUtils.log("FileUtil", "Internal storage is available: " + internalStorageDir.getAbsolutePath(), LoggingLevel.DEBUG);
        } else {
            //LoggerUtils.log("FileUtil", "Internal storage is not available", LoggingLevel.ERROR);
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
           // LoggerUtils.log("FileUtil", "External storage is available: " + externalStorageDir.getAbsolutePath(), LoggingLevel.DEBUG);
        } else {
           // LoggerUtils.log("FileUtil", "External storage is not available", LoggingLevel.DEBUG);
        }
        return isExternalStorageAvailable;
    }

}
