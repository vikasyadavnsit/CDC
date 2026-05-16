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
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3));
    }

    public static void displaySystemAppUsageStatistics(
            Activity activity,
            TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap) {
        initializeSpinner(activity, reportMap);
        setupSpinnerListener(activity, reportMap);
    }

    private static void initializeSpinner(Activity activity,
                                           TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap) {
        SpinnerItem[] items = new SpinnerItem[reportMap.size() + 1];
        items[0] = new SpinnerItem("Select a date", null);
        int index = 1;
        for (String date : reportMap.keySet()) items[index++] = new SpinnerItem(date, null);
        spinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownSpinner.setAdapter(spinnerArrayAdapter);
    }

    private static void setupSpinnerListener(Activity activity,
                                              TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap) {
        dropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fragmentLayout.removeAllViews();
                if (position != 0) {
                    displayForDate(activity, reportMap,
                            (SpinnerItem) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private static void displayForDate(Activity activity,
                                        TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap,
                                        SpinnerItem selectedItem) {
        String selectedDate = selectedItem.getLabel();
        TreeMap<String, AppUsageReportData> appData = reportMap.get(selectedDate);
        LinearLayout group = createGroupLayout(activity, selectedDate);
        for (Map.Entry<String, AppUsageReportData> entry : appData.entrySet()) {
            if (entry.getValue().getOpenCount() > 0) {
                addUsageRow(group, entry.getKey(), entry.getValue());
            }
        }
        fragmentLayout.addView(group);
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

    private static void addUsageRow(LinearLayout group, String appPackage,
                                     AppUsageReportData data) {
        Activity activity = (Activity) group.getContext();
        float density = activity.getResources().getDisplayMetrics().density;

        TextView appName = new TextView(activity);
        appName.setText(appPackage);
        appName.setTextColor(activity.getColor(R.color.text_secondary));
        appName.setTextSize(12f);
        appName.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        np.setMargins(0, dp(6, density), 0, dp(2, density));
        appName.setLayoutParams(np);
        group.addView(appName);

        TextView detail = new TextView(activity);
        detail.setText("Opens: " + data.getOpenCount()
                + "  |  Duration: " + formatDuration(data.getTotalTimeUsed()));
        detail.setTextColor(activity.getColor(R.color.text_hint));
        detail.setTextSize(11f);
        detail.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        group.addView(detail);

        View sep = new View(activity);
        sep.setBackgroundColor(activity.getColor(R.color.divider));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sp.setMargins(0, dp(6, density), 0, 0);
        sep.setLayoutParams(sp);
        group.addView(sep);
    }

    private static String formatDuration(long millis) {
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        return minutes + "m " + seconds + "s";
    }

    private static int dp(int v, float density) {
        return (int) (v * density);
    }
}
