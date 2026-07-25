package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.AppSession;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SystemAppUsageStatisticsFragment extends Fragment {

    private static WeakReference<SystemAppUsageStatisticsFragment> activeInstance;
    private static final int[] BAR_COLORS = {
            0xFF9C6DFE, 0xFF5EAEFF, 0xFF5EE7D0, 0xFFFFB347,
            0xFFFF6B9C, 0xFF72EFB9, 0xFFFFD166, 0xFFC77DFF,
    };
    
    // ... rest of imports ...

    private static final Set<String> SKIP_PARTS = new HashSet<>(Arrays.asList(
            "android", "mobile", "app", "lite", "client", "service", "com", "org", "net"));

    private LinearLayout fragmentLayout;
    private Spinner dateSpinner;
    private Spinner sortSpinner;
    private EditText searchInput;
    private static TreeMap<String, TreeMap<String, AppUsageReportData>> lastReportMap;
    
    private String currentSearch = "";
    private SortType currentSort = SortType.TIME_USED;
    private String selectedDate = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);
    }

    private enum SortType {
        TIME_USED("By Usage Time"),
        OPEN_COUNT("By Launch Count"),
        NAME("Alphabetical");
        final String label;
        SortType(String label) { this.label = label; }
        @NonNull @Override public String toString() { return label; }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_system_app_usage_statistics, container, false);
        fragmentLayout = view.findViewById(R.id.system_app_usage_statistics_fragment_layout);
        dateSpinner = view.findViewById(R.id.system_app_usage_statistics_fragment_dropdown_spinner);
        sortSpinner = view.findViewById(R.id.system_app_usage_sort_spinner);
        searchInput = view.findViewById(R.id.system_app_usage_search_input);
        
        setupSortSpinner();
        setupSearchInput();

        if (lastReportMap != null) {
            updateUI(getActivity(), lastReportMap);
        }
        
        FirebaseUtils.getAndroidUserSystemAppUsageStatistics();
        return view;
    }

    private void setupSortSpinner() {
        ArrayAdapter<SortType> adapter = new ArrayAdapter<SortType>(requireContext(), 
                android.R.layout.simple_spinner_item, SortType.values()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) ((TextView) v).setTextColor(requireContext().getColor(R.color.text_primary));
                return v;
            }
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(requireContext().getColor(R.color.surface_variant));
                if (v instanceof TextView) ((TextView) v).setTextColor(requireContext().getColor(R.color.text_primary));
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = (SortType) parent.getItemAtPosition(position);
                refreshDisplay();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().toLowerCase();
                refreshDisplay();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void refreshDisplay() {
        if (selectedDate == null || lastReportMap == null || fragmentLayout == null) return;
        fragmentLayout.removeAllViews();
        displayForDate(getActivity(), lastReportMap, selectedDate);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentLayout = null;
        dateSpinner = null;
        sortSpinner = null;
        searchInput = null;
    }

    public static void displaySystemAppUsageStatistics(
            Activity activity,
            TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap) {
        lastReportMap = reportMap;
        if (activeInstance != null && activeInstance.get() != null) {
            SystemAppUsageStatisticsFragment fragment = activeInstance.get();
            if (activity != null) {
                activity.runOnUiThread(() -> fragment.updateUI(activity, reportMap));
            }
        }
    }

    private void updateUI(Activity activity, TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap) {
        if (dateSpinner == null || fragmentLayout == null || activity == null) return;
        
        initializeDateSpinner(activity, reportMap);
        setupDateSpinnerListener(reportMap);

        // Default to today if available
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        for (int i = 0; i < dateSpinner.getCount(); i++) {
            if (dateSpinner.getItemAtPosition(i).toString().equals(today)) {
                dateSpinner.setSelection(i);
                break;
            }
        }
    }

    private void initializeDateSpinner(Activity activity,
                                           TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap) {
        SpinnerItem[] items = new SpinnerItem[reportMap.size() + 1];
        items[0] = new SpinnerItem("Select a date", null);
        int i = 1;
        java.util.List<String> sortedDates = new java.util.ArrayList<>(reportMap.keySet());
        java.util.Collections.sort(sortedDates, java.util.Collections.reverseOrder());
        for (String date : sortedDates) items[i++] = new SpinnerItem(date, null);
        
        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<SpinnerItem>(activity, android.R.layout.simple_spinner_item, items) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) ((TextView) v).setTextColor(activity.getColor(R.color.text_primary));
                return v;
            }
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(activity.getColor(R.color.surface_variant));
                if (v instanceof TextView) ((TextView) v).setTextColor(activity.getColor(R.color.text_primary));
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(adapter);
    }

    private void setupDateSpinnerListener(TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap) {
        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedDate = null;
                    if (fragmentLayout != null) fragmentLayout.removeAllViews();
                } else {
                    selectedDate = ((SpinnerItem) parent.getItemAtPosition(position)).getLabel();
                    refreshDisplay();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void displayForDate(Activity activity,
                                        TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap,
                                        String date) {
        TreeMap<String, AppUsageReportData> appData = reportMap.get(date);
        if (appData == null) return;

        // Check for error
        if (appData.containsKey("error")) {
            AppUsageReportData errorData = appData.get("error");
            if (errorData != null && errorData.getTotalTimeUsed() == -1) {
                addEmptyState(activity, fragmentLayout, errorData.getPackageName());
                return;
            }
        }

        // Apply search and sort
        List<Map.Entry<String, AppUsageReportData>> filtered = appData.entrySet().stream()
                .filter(e -> e.getValue().getTotalTimeUsed() > 0)
                .filter(e -> currentSearch.isEmpty() || getAppLabel(e.getKey()).toLowerCase().contains(currentSearch))
                .collect(Collectors.toList());

        Comparator<Map.Entry<String, AppUsageReportData>> comparator;
        switch (currentSort) {
            case OPEN_COUNT:
                comparator = (a, b) -> Integer.compare(b.getValue().getOpenCount(), a.getValue().getOpenCount());
                break;
            case NAME:
                comparator = Comparator.comparing(a -> getAppLabel(a.getKey()).toLowerCase());
                break;
            case TIME_USED:
            default:
                comparator = (a, b) -> Long.compare(b.getValue().getTotalTimeUsed(), a.getValue().getTotalTimeUsed());
                break;
        }
        filtered.sort(comparator);

        if (filtered.isEmpty()) {
            addEmptyState(activity, fragmentLayout, "No matching apps found.");
            return;
        }

        long totalTime = filtered.stream().mapToLong(e -> e.getValue().getTotalTimeUsed()).sum();
        long maxTime = filtered.stream().mapToLong(e -> e.getValue().getTotalTimeUsed()).max().orElse(1);

        addSummaryCard(activity, fragmentLayout, totalTime, filtered);
        addSectionLabel(activity, fragmentLayout, "APP BREAKDOWN (" + filtered.size() + ")");

        for (int i = 0; i < filtered.size(); i++) {
            Map.Entry<String, AppUsageReportData> e = filtered.get(i);
            int color = BAR_COLORS[i % BAR_COLORS.length];
            addAppRow(activity, fragmentLayout, e.getKey(), e.getValue(), maxTime, color);
        }
    }

    private static void addSummaryCard(Activity activity, LinearLayout container,
                                        long totalTime,
                                        List<Map.Entry<String, AppUsageReportData>> filtered) {
        CardView card = makeCard(activity);
        LinearLayout inner = new LinearLayout(activity);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20, activity), dp(20, activity), dp(20, activity), dp(20, activity));

        TextView label = new TextView(activity);
        label.setText("TOTAL SCREEN TIME");
        label.setTextColor(activity.getColor(R.color.text_hint));
        label.setTextSize(10f);
        label.setTypeface(null, Typeface.BOLD);
        inner.addView(label);

        TextView bigTime = new TextView(activity);
        bigTime.setText(formatTime(totalTime));
        bigTime.setTextColor(activity.getColor(R.color.text_primary));
        bigTime.setTextSize(32f);
        bigTime.setTypeface(null, Typeface.BOLD);
        inner.addView(bigTime);

        Space gap = new Space(activity);
        gap.setLayoutParams(new LinearLayout.LayoutParams(1, dp(16, activity)));
        inner.addView(gap);

        // Stacked bar
        inner.addView(makeStackedBar(activity, filtered, totalTime));
        
        card.addView(inner);
        container.addView(card);
    }

    private static LinearLayout makeStackedBar(Activity activity,
                                                List<Map.Entry<String, AppUsageReportData>> filtered,
                                                long totalTime) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(12, activity)));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(activity.getColor(R.color.divider));
        bg.setCornerRadius(dp(6, activity));
        bar.setBackground(bg);
        bar.setClipToOutline(true);

        int limit = Math.min(filtered.size(), 8);
        for (int i = 0; i < limit; i++) {
            long t = filtered.get(i).getValue().getTotalTimeUsed();
            View seg = new View(activity);
            seg.setBackgroundColor(BAR_COLORS[i % BAR_COLORS.length]);
            seg.setLayoutParams(new LinearLayout.LayoutParams(0, -1, (float) t / totalTime));
            bar.addView(seg);
        }
        return bar;
    }

    private void addAppRow(Activity activity, LinearLayout container,
                                   String pkg, AppUsageReportData data,
                                   long maxTime, int color) {
        float fraction = (float) data.getTotalTimeUsed() / maxTime;

        CardView card = makeCard(activity);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        
        LinearLayout mainRow = new LinearLayout(activity);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);
        mainRow.setGravity(Gravity.CENTER_VERTICAL);
        mainRow.setPadding(dp(16, activity), dp(12, activity), dp(16, activity), dp(12, activity));

        View dot = new View(activity);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(color);
        dot.setBackground(dotBg);
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(10, activity), dp(10, activity)));

        TextView nameView = new TextView(activity);
        nameView.setText(getAppLabel(pkg));
        nameView.setTextColor(activity.getColor(R.color.text_primary));
        nameView.setTextSize(14f);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
        nameParams.setMarginStart(dp(12, activity));
        nameView.setLayoutParams(nameParams);

        LinearLayout statsCol = new LinearLayout(activity);
        statsCol.setOrientation(LinearLayout.VERTICAL);
        statsCol.setGravity(Gravity.END);

        TextView timeView = new TextView(activity);
        timeView.setText(formatTime(data.getTotalTimeUsed()));
        timeView.setTextColor(activity.getColor(R.color.primary));
        timeView.setTextSize(14f);
        timeView.setTypeface(null, Typeface.BOLD);

        TextView opensView = new TextView(activity);
        opensView.setText(data.getOpenCount() + " launches");
        opensView.setTextColor(activity.getColor(R.color.text_hint));
        opensView.setTextSize(10f);

        statsCol.addView(timeView);
        statsCol.addView(opensView);

        mainRow.addView(dot);
        mainRow.addView(nameView);
        mainRow.addView(statsCol);
        row.addView(mainRow);

        // Progress bar container
        LinearLayout progressContainer = new LinearLayout(activity);
        progressContainer.setOrientation(LinearLayout.HORIZONTAL);
        progressContainer.setPadding(dp(38, activity), 0, dp(16, activity), dp(12, activity));
        
        View bar = new View(activity);
        bar.setBackgroundColor(color);
        LinearLayout.LayoutParams barP = new LinearLayout.LayoutParams(0, dp(4, activity), fraction);
        bar.setLayoutParams(barP);
        
        View remaining = new View(activity);
        remaining.setBackgroundColor(activity.getColor(R.color.divider));
        remaining.setAlpha(0.3f);
        LinearLayout.LayoutParams remP = new LinearLayout.LayoutParams(0, dp(4, activity), 1f - fraction);
        remaining.setLayoutParams(remP);

        progressContainer.addView(bar);
        if (fraction < 1f) progressContainer.addView(remaining);
        
        if (data.getTotalTimeUsed() > 0) row.addView(progressContainer);

        // Hidden Detail view
        LinearLayout detailView = new LinearLayout(activity);
        detailView.setOrientation(LinearLayout.VERTICAL);
        detailView.setPadding(dp(16, activity), dp(0, activity), dp(16, activity), dp(12, activity));
        detailView.setVisibility(View.GONE);
        detailView.setBackgroundColor(activity.getColor(R.color.app_bg));
        detailView.setAlpha(0.8f);
        
        TextView pkgName = new TextView(activity);
        pkgName.setText(pkg);
        pkgName.setTextColor(activity.getColor(R.color.text_hint));
        pkgName.setTextSize(9f);
        pkgName.setPadding(0, 0, 0, dp(4, activity));
        detailView.addView(pkgName);

        TextView lastUsed = new TextView(activity);
        String lastTime = data.getLastOpenTime() > 0 ? 
                new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date(data.getLastOpenTime())) : "N/A";
        lastUsed.setText("Last active: " + lastTime);
        lastUsed.setTextColor(activity.getColor(R.color.text_secondary));
        lastUsed.setTextSize(11f);
        detailView.addView(lastUsed);

        if (data.getSessions() != null && !data.getSessions().isEmpty()) {
            View div = new View(activity);
            div.setBackgroundColor(activity.getColor(R.color.divider));
            LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(-1, dp(1, activity));
            divP.setMargins(0, dp(8, activity), 0, dp(8, activity));
            detailView.addView(div, divP);

            TextView sessionHeader = new TextView(activity);
            sessionHeader.setText("Recent Sessions:");
            sessionHeader.setTextColor(activity.getColor(R.color.text_primary));
            sessionHeader.setTypeface(null, Typeface.BOLD);
            sessionHeader.setTextSize(11f);
            sessionHeader.setPadding(0, 0, 0, dp(4, activity));
            detailView.addView(sessionHeader);

            List<AppSession> sessions = data.getSessions();
            int sessionLimit = Math.min(sessions.size(), 5);
            for (int j = sessions.size() - 1; j >= sessions.size() - sessionLimit; j--) {
                AppSession s = sessions.get(j);
                TextView stv = new TextView(activity);
                String dur = formatTime(s.getEndTime() - s.getStartTime());
                String timeRange = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(s.getStartTime())) 
                        + " - " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(s.getEndTime()));
                stv.setText("• " + timeRange + " (" + dur + ")");
                stv.setTextColor(activity.getColor(R.color.text_secondary));
                stv.setTextSize(10f);
                detailView.addView(stv);
            }
        }
        
        row.addView(detailView);
        row.setOnClickListener(v -> {
            boolean visible = detailView.getVisibility() == View.VISIBLE;
            detailView.setVisibility(visible ? View.GONE : View.VISIBLE);
        });

        card.addView(row);
        container.addView(card);
    }

    private static void addSectionLabel(Activity activity, LinearLayout container, String text) {
        TextView label = new TextView(activity);
        label.setText(text);
        label.setTextColor(activity.getColor(R.color.text_hint));
        label.setTextSize(10f);
        label.setTypeface(null, Typeface.BOLD);
        label.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(12, activity), 0, dp(8, activity));
        label.setLayoutParams(p);
        container.addView(label);
    }

    private static void addEmptyState(Activity activity, LinearLayout container, String message) {
        TextView msg = new TextView(activity);
        msg.setText(message != null ? message : "No usage data for this date.");
        msg.setTextColor(activity.getColor(R.color.text_hint));
        msg.setTextSize(13f);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, dp(48, activity), 0, dp(48, activity));
        container.addView(msg);
    }

    private static void addThinDivider(Activity activity, LinearLayout container) {
        View div = new View(activity);
        div.setBackgroundColor(activity.getColor(R.color.divider));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 1);
        p.setMargins(dp(38, activity), 0, 0, 0);
        div.setLayoutParams(p);
        container.addView(div);
    }

    private static CardView makeCard(Activity activity) {
        CardView card = new CardView(activity);
        card.setCardBackgroundColor(activity.getColor(R.color.surface_variant));
        card.setRadius(dp(16, activity));
        card.setCardElevation(dp(2, activity));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(16, activity));
        card.setLayoutParams(p);
        return card;
    }

    private static String getAppLabel(String pkg) {
        if (pkg == null) return "Unknown";
        String[] parts = pkg.split("[.-]");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].toLowerCase().replaceAll("[^a-z]", "");
            if (part.length() > 2 && !SKIP_PARTS.contains(part)) {
                return Character.toUpperCase(part.charAt(0)) + part.substring(1);
            }
        }
        return parts[parts.length - 1];
    }

    private static String formatTime(long millis) {
        if (millis < 1000) return "< 1s";
        long h = millis / 3_600_000;
        long m = (millis % 3_600_000) / 60_000;
        long s = (millis % 60_000) / 1_000;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private static int dp(int v, Activity activity) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
