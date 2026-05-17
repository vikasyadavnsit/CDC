package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.AppUsageReportData;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SystemAppUsageStatisticsFragment extends Fragment {

    private static final int[] BAR_COLORS = {
            0xFF9C6DFE, 0xFF5EAEFF, 0xFF5EE7D0, 0xFFFFB347,
            0xFFFF6B9C, 0xFF72EFB9, 0xFFFFD166, 0xFFC77DFF,
    };

    private static final Set<String> SKIP_PARTS = new HashSet<>(Arrays.asList(
            "android", "mobile", "app", "lite", "client", "service", "com", "org", "net"));

    private static LinearLayout fragmentLayout;
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
        FirebaseUtils.getAndroidUserSystemAppUsageStatistics();
        return view;
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
        int i = 1;
        for (String date : reportMap.keySet()) items[i++] = new SpinnerItem(date, null);
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
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void displayForDate(Activity activity,
                                        TreeMap<String, TreeMap<String, AppUsageReportData>> reportMap,
                                        SpinnerItem selectedItem) {
        String date = selectedItem.getLabel();
        TreeMap<String, AppUsageReportData> appData = reportMap.get(date);
        if (appData == null) return;

        // Sort by usage time descending, exclude zero-time entries
        List<Map.Entry<String, AppUsageReportData>> sorted = appData.entrySet().stream()
                .filter(e -> e.getValue().getTotalTimeUsed() > 0)
                .sorted((a, b) -> Long.compare(b.getValue().getTotalTimeUsed(), a.getValue().getTotalTimeUsed()))
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            addEmptyState(activity, fragmentLayout);
            return;
        }

        long totalTime = sorted.stream().mapToLong(e -> e.getValue().getTotalTimeUsed()).sum();
        long maxTime = sorted.get(0).getValue().getTotalTimeUsed();

        addSummaryCard(activity, fragmentLayout, totalTime, sorted);
        addSectionLabel(activity, fragmentLayout, "TOP APPS");

        CardView listCard = makeCard(activity);
        LinearLayout listInner = new LinearLayout(activity);
        listInner.setOrientation(LinearLayout.VERTICAL);
        listInner.setPadding(0, dp(4, activity), 0, dp(4, activity));
        listCard.addView(listInner);
        fragmentLayout.addView(listCard);

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, AppUsageReportData> e = sorted.get(i);
            int color = BAR_COLORS[i % BAR_COLORS.length];
            addAppRow(activity, listInner, e.getKey(), e.getValue(), maxTime, color);
            if (i < sorted.size() - 1) addThinDivider(activity, listInner);
        }
    }

    // ── Summary card ──────────────────────────────────────────────────────────

    private static void addSummaryCard(Activity activity, LinearLayout container,
                                        long totalTime,
                                        List<Map.Entry<String, AppUsageReportData>> sorted) {
        float d = density(activity);
        CardView card = makeCard(activity);
        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
        cardParams.setMargins(0, 0, 0, dp(20, activity));
        card.setLayoutParams(cardParams);

        LinearLayout inner = new LinearLayout(activity);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20, activity), dp(20, activity), dp(20, activity), dp(20, activity));

        // Label
        TextView label = new TextView(activity);
        label.setText("SCREEN TIME");
        label.setTextColor(activity.getColor(R.color.text_hint));
        label.setTextSize(10f);
        label.setTypeface(null, Typeface.BOLD);
        label.setLetterSpacing(0.1f);
        inner.addView(label);

        // Big time
        TextView bigTime = new TextView(activity);
        bigTime.setText(formatTime(totalTime));
        bigTime.setTextColor(activity.getColor(R.color.text_primary));
        bigTime.setTextSize(36f);
        bigTime.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams bigParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bigParams.setMargins(0, dp(4, activity), 0, dp(2, activity));
        bigTime.setLayoutParams(bigParams);
        inner.addView(bigTime);

        // App count
        TextView subtitle = new TextView(activity);
        subtitle.setText(sorted.size() + " apps used");
        subtitle.setTextColor(activity.getColor(R.color.text_hint));
        subtitle.setTextSize(12f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subParams.setMargins(0, 0, 0, dp(16, activity));
        subtitle.setLayoutParams(subParams);
        inner.addView(subtitle);

        // Stacked usage bar (top 5 + others)
        inner.addView(makeStackedBar(activity, sorted, totalTime));

        // Legend (top 4 apps)
        inner.addView(makeLegend(activity, sorted));

        card.addView(inner);
        container.addView(card);
    }

    private static LinearLayout makeStackedBar(Activity activity,
                                                List<Map.Entry<String, AppUsageReportData>> sorted,
                                                long totalTime) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(10, activity));
        barParams.setMargins(0, 0, 0, dp(10, activity));
        bar.setLayoutParams(barParams);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(activity.getColor(R.color.divider));
        bg.setCornerRadius(dp(5, activity));
        bar.setBackground(bg);
        bar.setClipToOutline(true);

        int limit = Math.min(sorted.size(), 5);
        long usedWeight = 0;
        for (int i = 0; i < limit; i++) {
            long t = sorted.get(i).getValue().getTotalTimeUsed();
            usedWeight += t;
            View seg = new View(activity);
            seg.setBackgroundColor(BAR_COLORS[i % BAR_COLORS.length]);
            seg.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, (float) t / totalTime));
            bar.addView(seg);
        }
        // Others
        float remaining = (float) (totalTime - usedWeight) / totalTime;
        if (remaining > 0.01f) {
            View others = new View(activity);
            others.setBackgroundColor(activity.getColor(R.color.text_hint));
            others.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, remaining));
            bar.addView(others);
        }
        return bar;
    }

    private static LinearLayout makeLegend(Activity activity,
                                            List<Map.Entry<String, AppUsageReportData>> sorted) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        int limit = Math.min(sorted.size(), 4);
        for (int i = 0; i < limit; i++) {
            int color = BAR_COLORS[i % BAR_COLORS.length];
            String name = getAppLabel(sorted.get(i).getKey());

            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            item.setLayoutParams(itemParams);

            View dot = new View(activity);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(color);
            dot.setBackground(dotBg);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dp(8, activity), dp(8, activity)));

            TextView lbl = new TextView(activity);
            lbl.setText(name);
            lbl.setTextColor(activity.getColor(R.color.text_hint));
            lbl.setTextSize(10f);
            lbl.setSingleLine(true);
            lbl.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lblParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lblParams.setMarginStart(dp(4, activity));
            lbl.setLayoutParams(lblParams);

            item.addView(dot);
            item.addView(lbl);
            row.addView(item);
        }
        return row;
    }

    // ── App row ───────────────────────────────────────────────────────────────

    private static void addAppRow(Activity activity, LinearLayout container,
                                   String pkg, AppUsageReportData data,
                                   long maxTime, int color) {
        float fraction = (float) data.getTotalTimeUsed() / maxTime;

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16, activity), dp(14, activity), dp(16, activity), dp(14, activity));

        // Colored oval
        View dot = new View(activity);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(color);
        dot.setBackground(dotBg);
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(10, activity), dp(10, activity)));

        Space gap1 = new Space(activity);
        gap1.setLayoutParams(new LinearLayout.LayoutParams(dp(12, activity), 1));

        // App name
        TextView nameView = new TextView(activity);
        nameView.setText(getAppLabel(pkg));
        nameView.setTextColor(activity.getColor(R.color.text_secondary));
        nameView.setTextSize(13f);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Space gap2 = new Space(activity);
        gap2.setLayoutParams(new LinearLayout.LayoutParams(dp(10, activity), 1));

        // Proportional bar
        LinearLayout barOuter = new LinearLayout(activity);
        barOuter.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0, dp(5, activity), 1.2f);
        barOuter.setLayoutParams(barParams);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(activity.getColor(R.color.divider));
        barBg.setCornerRadius(dp(3, activity));
        barOuter.setBackground(barBg);
        barOuter.setClipToOutline(true);

        View filled = new View(activity);
        filled.setBackgroundColor(color);
        filled.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, fraction));

        View empty = new View(activity);
        empty.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(0f, 1f - fraction)));

        barOuter.addView(filled);
        barOuter.addView(empty);

        Space gap3 = new Space(activity);
        gap3.setLayoutParams(new LinearLayout.LayoutParams(dp(10, activity), 1));

        // Right column: time + opens
        LinearLayout rightCol = new LinearLayout(activity);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END);

        TextView timeView = new TextView(activity);
        timeView.setText(formatTime(data.getTotalTimeUsed()));
        timeView.setTextColor(activity.getColor(R.color.text_primary));
        timeView.setTextSize(13f);
        timeView.setTypeface(null, Typeface.BOLD);
        timeView.setGravity(Gravity.END);
        timeView.setMinimumWidth(dp(52, activity));

        TextView opensView = new TextView(activity);
        opensView.setText(data.getOpenCount() + "×");
        opensView.setTextColor(activity.getColor(R.color.text_hint));
        opensView.setTextSize(10f);
        opensView.setGravity(Gravity.END);

        rightCol.addView(timeView);
        rightCol.addView(opensView);

        row.addView(dot);
        row.addView(gap1);
        row.addView(nameView);
        row.addView(gap2);
        row.addView(barOuter);
        row.addView(gap3);
        row.addView(rightCol);
        container.addView(row);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void addSectionLabel(Activity activity, LinearLayout container, String text) {
        TextView label = new TextView(activity);
        label.setText(text);
        label.setTextColor(activity.getColor(R.color.text_hint));
        label.setTextSize(10f);
        label.setTypeface(null, Typeface.BOLD);
        label.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(8, activity));
        label.setLayoutParams(p);
        container.addView(label);
    }

    private static void addEmptyState(Activity activity, LinearLayout container) {
        TextView msg = new TextView(activity);
        msg.setText("No usage data for this date.");
        msg.setTextColor(activity.getColor(R.color.text_hint));
        msg.setTextSize(13f);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, dp(32, activity), 0, dp(32, activity));
        container.addView(msg);
    }

    private static void addThinDivider(Activity activity, LinearLayout container) {
        View div = new View(activity);
        div.setBackgroundColor(activity.getColor(R.color.divider));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        p.setMargins(dp(16, activity), 0, dp(16, activity), 0);
        div.setLayoutParams(p);
        container.addView(div);
    }

    private static CardView makeCard(Activity activity) {
        CardView card = new CardView(activity);
        card.setCardBackgroundColor(activity.getColor(R.color.surface_variant));
        card.setRadius(dp(16, activity));
        card.setCardElevation(dp(4, activity));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(12, activity));
        card.setLayoutParams(p);
        return card;
    }

    private static String getAppLabel(String pkg) {
        String[] parts = pkg.split("\\.");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].toLowerCase().replaceAll("[^a-z]", "");
            if (part.length() > 2 && !SKIP_PARTS.contains(part)) {
                return Character.toUpperCase(part.charAt(0)) + part.substring(1);
            }
        }
        return parts[parts.length - 1];
    }

    private static String formatTime(long millis) {
        long h = millis / 3_600_000;
        long m = (millis % 3_600_000) / 60_000;
        long s = (millis % 60_000) / 1_000;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m";
        return s + "s";
    }

    private static int dp(int v, Activity activity) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }

    private static float density(Activity activity) {
        return activity.getResources().getDisplayMetrics().density;
    }
}
