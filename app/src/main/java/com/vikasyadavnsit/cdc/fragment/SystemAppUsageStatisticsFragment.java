package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.Map;
import java.util.TreeMap;

public class SystemAppUsageStatisticsFragment extends Fragment {

    private static GridLayout fragmentLayout;
    private static Spinner dropdownSpinner;
    private static ArrayAdapter<SpinnerItem> spinnerArrayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_system_app_usage_statistics, container, false);
        fragmentLayout = view.findViewById(R.id.system_app_usage_statistics_fragment_layout);
        dropdownSpinner = view.findViewById(R.id.system_app_usage_statistics_fragment_dropdown_spinner);

        fragmentLayout.setColumnCount(calculateNoOfColumns());
        FirebaseUtils.getAndroidUserSystemAppUsageStatistics();

        return view;
    }

    private int calculateNoOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3)); // Minimum 1, maximum 3 columns
    }

    public static void displaySystemAppUsageStatistics(Activity activity, TreeMap<String, TreeMap<String, AppUsageReportData>> appUsageStatisticsReportDataMap) {
        initializeSpinner(activity, appUsageStatisticsReportDataMap);
        setupSpinnerListener(activity, appUsageStatisticsReportDataMap);
    }

    private static void initializeSpinner(Activity activity, TreeMap<String, TreeMap<String, AppUsageReportData>> appUsageStatisticsReportDataMap) {
        SpinnerItem[] items = new SpinnerItem[appUsageStatisticsReportDataMap.size() + 1];
        items[0] = new SpinnerItem("Select a date", null);

        int index = 1;
        for (String appPackage : appUsageStatisticsReportDataMap.keySet()) {
            items[index++] = new SpinnerItem(appPackage, null);
        }

        spinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownSpinner.setAdapter(spinnerArrayAdapter);
    }

    private static void setupSpinnerListener(Activity activity, TreeMap<String, TreeMap<String, AppUsageReportData>> appUsageStatisticsReportDataMap) {
        dropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fragmentLayout.removeAllViews();

                if (position == 0) {
                    // Do nothing for the placeholder
                } else {
                    displaySelectedAppData(activity, appUsageStatisticsReportDataMap, (SpinnerItem) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private static void displaySelectedAppData(Activity activity, TreeMap<String, TreeMap<String, AppUsageReportData>> appUsageStatisticsReportDataMap, SpinnerItem selectedItem) {
        String selectedDate = selectedItem.getLabel();
        TreeMap<String, AppUsageReportData> appUsageData = appUsageStatisticsReportDataMap.get(selectedDate);

        LinearLayout groupLayout = createGroupLayout(activity, "Date : " + selectedDate);
        for (Map.Entry<String, AppUsageReportData> entry : appUsageData.entrySet()) {
            if (entry.getValue().getOpenCount() > 0) {
                addAppUsageDataToGroup(groupLayout, entry.getValue());
            }
        }
        fragmentLayout.addView(groupLayout);
    }


    private static LinearLayout createGroupLayout(Activity activity, String dateText) {
        LinearLayout groupLayout = new LinearLayout(activity);
        groupLayout.setOrientation(LinearLayout.VERTICAL);
        groupLayout.setLayoutParams(createGroupLayoutParams());
        groupLayout.setPadding(16, 24, 16, 16);
        groupLayout.setBackgroundResource(R.drawable.group_border);

        TextView dateTextView = new TextView(activity);
        dateTextView.setText(dateText);
        dateTextView.setLayoutParams(createLabelLayoutParams());
        dateTextView.setTextSize(16);
        dateTextView.setTypeface(null, Typeface.BOLD);
        groupLayout.addView(dateTextView);

        return groupLayout;
    }

    private static void addAppUsageDataToGroup(LinearLayout groupLayout, AppUsageReportData usageReportData) {
        TextView textView = new TextView(groupLayout.getContext());
        StringBuilder sb = new StringBuilder();
        sb.append(usageReportData);

        textView.setText(sb);
        textView.setLayoutParams(createLabelLayoutParams());
        groupLayout.addView(textView);
    }


    private static GridLayout.LayoutParams createGroupLayoutParams() {
        GridLayout.LayoutParams groupParams = new GridLayout.LayoutParams();
        groupParams.width = 0;
        groupParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        groupParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        groupParams.setMargins(8, 16, 8, 16);
        return groupParams;
    }

    private static LinearLayout.LayoutParams createLabelLayoutParams() {
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(8, 8, 8, 8);
        return labelParams;
    }
}
