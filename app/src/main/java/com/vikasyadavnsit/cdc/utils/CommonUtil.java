package com.vikasyadavnsit.cdc.utils;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.vikasyadavnsit.cdc.R;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CommonUtil {

    // Method to load a fragment into the FrameLayout
    public static void loadFragment(FragmentManager fragmentManager, Fragment fragment) {
        loadFragment(R.id.main_frame_layout, fragmentManager, fragment);
    }

    public static void loadFragment(int frameLayout, FragmentManager fragmentManager, Fragment fragment) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame_layout, fragment);
        //fragmentTransaction.addToBackStack(fragment.getClass().getName());
        fragmentTransaction.commit();
    }





    // When DataType is List<Map<String, String>>
    public static boolean isDataTypeListOfMap(Object data) {
        if (data instanceof List<?>) {
            List<?> dataList = (List<?>) data;
            return !dataList.isEmpty() && dataList.get(0) instanceof Map<?, ?>;
        }
        return false;
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
                LoggerUtils.e("FileUtil", "Failed to create file");
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
                LoggerUtils.e("FileUtil", "Failed to create directory");
                return true;
            }
        }
        return false;
    }

    public static boolean checkAndCreateDirectory(String directoryPath) {
        return checkAndCreateDirectory(new File(directoryPath));
    }


}
