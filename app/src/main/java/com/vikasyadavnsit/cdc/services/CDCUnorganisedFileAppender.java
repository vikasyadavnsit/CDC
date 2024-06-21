package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateDirectory;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateFile;

import android.os.Environment;
import android.util.Log;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.CryptoUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Singleton class for writing data to files.
 * This class provides methods to add data to a buffer and write it to files when it exceeds a specified byte limit.
 */
public class CDCUnorganisedFileAppender {

    /**
     * Lock object for synchronization.
     */
    private static final Object lock = new Object();

    /**
     * The singleton instance of the FileAppender class.
     */
    private static volatile CDCUnorganisedFileAppender instance;

    /**
     * Map to store queues for different file types.
     */
    private static Map<String, Queue<String>> queueMap;

    /**
     * Map to store current byte counts for different file types.
     */
    private static Map<String, Integer> positionMap;

    /**
     * Initializes the singleton instance and sets up the buffer maps.
     */
    static {
        getInstance(AppConstants.FILE_APPENDER_BUFFER_SIZE);
    }

    /**
     * Private constructor to initialize the file writer with a byte limit.
     *
     * @param byteLimit The byte limit for the buffer.
     */
    private CDCUnorganisedFileAppender(int byteLimit) {
        queueMap = new HashMap<>();
        positionMap = new HashMap<>();
        for (FileMap fileMap : FileMap.values()) {
            queueMap.putIfAbsent(fileMap.name(), new LinkedList<>());
            positionMap.putIfAbsent(getCurrentByteCountName(fileMap), 0);
            positionMap.putIfAbsent(getByteLimitName(fileMap), byteLimit);
        }
    }

    /**
     * Gets the singleton instance of the file writer.
     *
     * @param byteLimit The byte limit for the buffer.
     * @return The singleton instance.
     */
    public static synchronized CDCUnorganisedFileAppender getInstance(int byteLimit) {
        if (instance == null) {
            instance = new CDCUnorganisedFileAppender(byteLimit);
        }
        return instance;
    }

    /**
     * Adds data to the buffer. If the buffer exceeds the byte limit, writes the data to a file.
     *
     * @param fileMap The type of file to which data is added.
     * @param data    The data to add.
     */
    public static void add(FileMap fileMap, Object data) {
        synchronized (lock) {
            String dataString = data.toString();
            int dataBytes = dataString.getBytes(StandardCharsets.UTF_8).length;
            int currentByteCount = positionMap.get(getCurrentByteCountName(fileMap));
            if (currentByteCount + dataBytes > positionMap.get(getByteLimitName(fileMap))) {
                writeToFile(fileMap);
            }
            queueMap.get(fileMap.name()).add(dataString);
            positionMap.put(getCurrentByteCountName(fileMap), currentByteCount + dataBytes);
        }
    }


    /**
     * Writes the buffered data to a file.
     *
     * @param fileMap The type of file to write data to.
     */
    private static void writeToFile(FileMap fileMap) {
        synchronized (lock) {
            try {
                File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
                File file = new File(directory, fileMap.getFileName());
                if (checkAndCreateDirectory(directory) || checkAndCreateFile(file)) {
                    return;
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
                        StringBuilder buffer = new StringBuilder();
                        Queue<String> queue = queueMap.get(fileMap.name());
                        while (!queue.isEmpty()) {
                            buffer.append(CryptoUtils.getEncryptedData(fileMap,
                                    LocalDateTime.now() + " :: " + queue.poll())).append("\n");
                        }
                        positionMap.put(getCurrentByteCountName(fileMap), 0);
                        bufferedWriter.write(buffer.toString());
                    }
                }
            } catch (Exception e) {
                Log.e("FileUtilsFileWriter", "Error writing to file", e);
            }
        }
    }


    /**
     * Constructs the name for the byte limit attribute in positionMap.
     *
     * @param fileMap The type of file.
     * @return The name of the byte limit attribute.
     */
    private static String getByteLimitName(FileMap fileMap) {
        return fileMap.name() + "byteLimit";
    }

    /**
     * Constructs the name for the current byte count attribute in positionMap.
     *
     * @param fileMap The type of file.
     * @return The name of the current byte count attribute.
     */
    private static String getCurrentByteCountName(FileMap fileMap) {
        return fileMap.name() + "byteCount";
    }


    public static void appendDataToFile(String fileName, String data) {
        try {
            File directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS + "/CDC/Sensors");
            File file = new File(directory, fileName);
            if (checkAndCreateDirectory(directory) || checkAndCreateFile(file)) {
                return;
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(data);
            }
        } catch (Exception e) {
            Log.e("CDCUnorganisedFileAppender", "Error writing to file");
        }
    }

}
