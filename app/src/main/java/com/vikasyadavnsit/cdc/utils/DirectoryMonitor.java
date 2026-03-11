package com.vikasyadavnsit.cdc.utils;


import android.os.FileObserver;

import java.util.LinkedHashMap;

public class DirectoryMonitor extends FileObserver {
    private LinkedHashMap<String, Object> fileMap;
    private String monitoredPath;

    /**
     * Creates a {@link FileObserver} that monitors a directory and mutates an in-memory map of paths.
     *
     * <p>The observer is configured with {@link #ALL_EVENTS}. On create/move-in it adds entries to
     * {@code fileMap}; on delete/move-out it removes entries.</p>
     *
     * @param path    Absolute path to the directory to monitor.
     * @param fileMap Backing map to update as events are received.
     */
    public DirectoryMonitor(String path, LinkedHashMap<String, Object> fileMap) {
        super(path, ALL_EVENTS);  // Monitor all events
        this.fileMap = fileMap;
        this.monitoredPath = path;
    }

    /**
     * Receives filesystem events for the monitored directory and updates the backing map.
     *
     * @param event Event bitmask (masked against {@link FileObserver#ALL_EVENTS}).
     * @param path  Relative path within the monitored directory; may be {@code null}.
     */
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

