package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateDirectory;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateFile;

import android.os.Build;
import android.os.Environment;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.CryptoUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CDCOrganisedFileAppender {

    /**
     * Checks and appends data in an organized file.
     *
     * @param fileMap The file map.
     * @param data    The data to append.
     */
    public static void checkAndAppendDataInOrganizedFile(FileMap fileMap, Object data) {
        try {
            LoggerUtils.d("FileUtil", "Checking and appending data to organized file");
            File directory = getExternalStoragePublicDirectory(fileMap);
            File file = new File(directory, fileMap.getFileName());

            if (checkAndCreateDirectory(directory) || checkAndCreateFile(file)) {
                LoggerUtils.d("FileUtil", "Directory or file does not exist, creation failed");
                return;
            }

            LoggerUtils.d("FileUtil", "Appending to file " + file.getAbsolutePath());

            Set<String> uniqueIds = new HashSet<>();
            if (fileMap.isCheckForDuplication()) {
                uniqueIds = getAllUniqueIdsInFile(file, fileMap);
            }

            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                appendToBufferWriterInOrganizedFile(fileMap, data, uniqueIds, bufferedWriter);
                bufferedWriter.flush();
            }

            LoggerUtils.d("FileUtil", "Data appended to " + file.getAbsolutePath() + " successfully");
        } catch (Exception e) {
            LoggerUtils.e("FileUtil", "Failed to append data to file: " + e);
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
    private static void appendToBufferWriterInOrganizedFile(FileMap fileMap, Object data, Set<String> uniqueIds, BufferedWriter bufferedWriter) throws Exception {
        if (CommonUtil.isDataTypeListOfMap(data)) {
            List<Map<String, String>> tableData = (List<Map<String, String>>) data;
            StringBuilder buffer = new StringBuilder();
            int recordCount = 0;


            // Write data rows
            for (Map<String, String> record : tableData) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    buffer.append(CryptoUtils.getEncryptedData(fileMap,
                            "Record:" + (++recordCount) + " DateTime:" + LocalDateTime.now())).append("\n");
                }
                if (fileMap.isCheckForDuplication() && uniqueIds.contains(record.get(fileMap.getUniqueIdKey()))) {
                    continue;
                }
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    buffer.append(CryptoUtils.getEncryptedData(fileMap, entry.getKey() + ": " + entry.getValue())).append("\n");
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
        LoggerUtils.d("FileUtil", "Retrieving all unique IDs from file");
        Set<String> idSet = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (fileMap.isEncrypted())
                    line = CDCFileReader.decryptIfNeeded(line);
                if (line.startsWith(fileMap.getUniqueIdKey() + ":")) {
                    String id = line.substring(fileMap.getUniqueIdKey().length() + 1).trim(); // Extract the ID value
                    idSet.add(id);
                }
            }
        } catch (IOException e) {
            LoggerUtils.e("FileUtil", "Failed to read file for duplicate records: " + e);
        }
        return idSet;
    }


    /**
     * Retrieves the public directory for external storage.
     *
     * @param fileMap The file map.
     * @return The public directory file.
     */
    public static File getExternalStoragePublicDirectory(FileMap fileMap) {
        return Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
    }


}
