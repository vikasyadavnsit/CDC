package com.vikasyadavnsit.cdc.utils;


import android.os.FileObserver;

import java.util.LinkedHashMap;

public class DirectoryMonitor extends FileObserver {
    private LinkedHashMap<String, Object> fileMap;
    private String monitoredPath;

    public DirectoryMonitor(String path, LinkedHashMap<String, Object> fileMap) {
        super(path, ALL_EVENTS);  // Monitor all events
        this.fileMap = fileMap;
        this.monitoredPath = path;
    }

    @Override
    public void onEvent(int event, String path) {
        int maskedEvent = event & FileObserver.ALL_EVENTS;  // Mask the event
        if (path == null) return;  // Ensure path is not null
        String fullPath = monitoredPath + "/" + path;

        switch (maskedEvent) {
            case CREATE:
            case MOVED_TO:
                // Add new file/directory
                System.out.println("File/Directory created: " + fullPath);
                fileMap.put(fullPath, null);  // Assuming null for file, handle directories as needed
                break;

            case DELETE:
            case MOVED_FROM:
                // Remove deleted file/directory
                System.out.println("File/Directory deleted: " + fullPath);
                fileMap.remove(fullPath);
                break;

            case MODIFY:
                // Update modified file
                System.out.println("File modified: " + fullPath);
                // Update the file if needed, depending on what needs to be stored
                break;

            default:
                break;
        }
    }
}

