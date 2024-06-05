package com.vikasyadavnsit.cdc.service;

import android.os.Build;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

public class FileUtilsFileWriter {

    public static FileUtilsFileWriter instance;
    private Queue<String> queue;
    private int byteCount;
    private int byteLimit;
    private Object lock;

    public FileUtilsFileWriter(int byteLimit) {
        this.queue = new LinkedList<>();
        this.byteCount = 0;
        this.byteLimit = byteLimit;
        this.lock = new Object();
    }

    public static synchronized FileUtilsFileWriter getInstance(int byteLimit) {
        if (instance == null) {
            instance = new FileUtilsFileWriter(byteLimit);
        }
        return instance;
    }

    public void add(Object data, BufferedWriter bufferedWriter) throws IOException {
        synchronized (lock) {
            String dataString = data.toString();
            int dataBytes = dataString.getBytes(StandardCharsets.UTF_8).length;
            if (byteCount + dataBytes > byteLimit) {
                writeToFile(bufferedWriter);
            }
            queue.add(dataString);
            byteCount += dataBytes;
        }
    }

    private void writeToFile(BufferedWriter bufferedWriter) throws IOException {
        synchronized (lock) {
            StringBuilder buffer = new StringBuilder();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bufferedWriter.write(buffer.toString());
                while (!queue.isEmpty()) {
                    buffer.append(Instant.now()).append(queue.poll()).append("\n");
                }
                byteCount = 0;
            }
            bufferedWriter.write(buffer.toString());
        }
    }


}
