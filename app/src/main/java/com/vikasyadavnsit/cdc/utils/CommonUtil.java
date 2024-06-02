package com.vikasyadavnsit.cdc.utils;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

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


    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        LoggerUtil.log("CommonUtil", "onActivityResult : requestCode : " + requestCode + " resultCode : " + resultCode, LoggingLevel.DEBUG);
    }


    // When DataType is List<Map<String, String>>
    public static boolean isDataTypeListOfMap(Object data) {
        if (data instanceof List<?>) {
            List<?> dataList = (List<?>) data;
            if (!dataList.isEmpty() && dataList.get(0) instanceof Map<?, ?>) {
                return true;
            }
        }
        return false;
    }
}
