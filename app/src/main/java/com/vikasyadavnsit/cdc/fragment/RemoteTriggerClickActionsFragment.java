package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RemoteTriggerClickActionsFragment extends Fragment {

    private static GridLayout fragmentLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_triggers, container, false);
        fragmentLayout = view.findViewById(R.id.click_actions_fragment_layout);
        fragmentLayout.setColumnCount(calculateNoOfColumns());
        FirebaseUtils.getAndroidUserClickActions();
        return view;
    }

    private int calculateNoOfColumns() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        int noOfColumns = (int) (dpWidth / 300);
        return Math.max(1, Math.min(noOfColumns, 3));
    }

    public static void addDynamicButtons(Activity activity,
                                         Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        if (fragmentLayout == null) return;
        fragmentLayout.removeAllViews();
        float density = activity.getResources().getDisplayMetrics().density;

        appTriggerSettingsDataMap.entrySet().forEach(entry -> {
            String key = entry.getKey();
            User.AppTriggerSettingsData data = entry.getValue();
            boolean enabled = data.isEnabled();

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.group_border);
            card.setPadding(dp(16, density), dp(16, density), dp(16, density), dp(16, density));
            card.setLayoutParams(createLayoutParams(density));

            // Header Section
            LinearLayout header = new LinearLayout(activity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            header.setPadding(0, 0, 0, dp(12, density));

            // Circular Icon
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(activity.getColor(R.color.surface_variant));

            TextView iconView = new TextView(activity);
            iconView.setText(getIconForKey(key));
            iconView.setTextSize(14f);
            iconView.setGravity(Gravity.CENTER);
            iconView.setBackground(iconBg);
            int iconSize = dp(32, density);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.setMarginEnd(dp(10, density));
            iconView.setLayoutParams(iconParams);
            header.addView(iconView);

            // Title Container (Title + Status Indicator)
            LinearLayout titleContainer = new LinearLayout(activity);
            titleContainer.setOrientation(LinearLayout.HORIZONTAL);
            titleContainer.setGravity(Gravity.CENTER_VERTICAL);
            titleContainer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

            TextView titleView = new TextView(activity);
            titleView.setText(formatLabel(key));
            titleView.setTextColor(activity.getColor(R.color.text_primary));
            titleView.setTextSize(13f);
            titleView.setTypeface(null, Typeface.BOLD);
            titleContainer.addView(titleView);

            // Enabled/Disabled Dot
            View dot = new View(activity);
            int dotSize = dp(8, density);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.setMarginStart(dp(8, density));
            dot.setBackgroundResource(enabled ? R.drawable.bg_status_active : R.drawable.bg_status_inactive);
            dot.setLayoutParams(dotParams);
            titleContainer.addView(dot);

            header.addView(titleContainer);
            card.addView(header);

            // Divider
            View divider = new View(activity);
            divider.setBackgroundColor(activity.getColor(R.color.divider));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(-1, 1);
            divParams.setMargins(0, 0, 0, dp(12, density));
            divider.setLayoutParams(divParams);
            card.addView(divider);

            // LAYMAN Metrics
            card.addView(buildMetricRow(activity, "Run Frequency", formatInterval(data.getInterval()), density));
            card.addView(buildMetricRow(activity, "Repeat Mode", data.isRepeatable() ? "Continuous" : "One-time", density));
            card.addView(buildMetricRow(activity, "Max Runs", data.getMaxRepetitions() > 0 ? String.valueOf(data.getMaxRepetitions()) : "No Limit", density));

            // Status Badge Footer
            LinearLayout footer = new LinearLayout(activity);
            footer.setOrientation(LinearLayout.HORIZONTAL);
            footer.setGravity(Gravity.END);
            footer.setPadding(0, dp(12, density), 0, 0);

            TextView statusBadge = new TextView(activity);
            String status = data.getActionStatus() != null ? data.getActionStatus().name() : "IDLE";
            statusBadge.setText(status);
            statusBadge.setTextSize(9f);
            statusBadge.setAllCaps(true);
            statusBadge.setLetterSpacing(0.05f);
            statusBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            statusBadge.setTextColor(activity.getColor(R.color.text_secondary));
            statusBadge.setBackgroundResource(R.drawable.bg_tag_light);
            statusBadge.setPadding(dp(8, density), dp(4, density), dp(8, density), dp(4, density));
            footer.addView(statusBadge);

            card.addView(footer);
            fragmentLayout.addView(card);
        });
    }

    private static String formatInterval(long millis) {
        if (millis <= 0) return "Instant";
        if (millis < 1000) return millis + "ms";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        if (seconds < 60) return seconds + " seconds";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (minutes < 60) return minutes + " " + (minutes == 1 ? "minute" : "minutes");
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        return hours + " " + (hours == 1 ? "hour" : "hours");
    }

    private static String getIconForKey(String key) {
        try {
            ClickActions action = ClickActions.valueOf(key);
            switch (action) {
                case REQUEST_ALL_PERMISSION:          return "🔐";
                case RESET_ALL_PERMISSION:            return "🔄";
                case REQUEST_EXACT_ALARM_PERMISSION:  return "⏰";
                case REQUEST_ACCESSIBILITY_PERMISSION:return "♿";
                case REQUEST_FILE_ACCESS_PERMISSION:  return "📁";
                case START_SENSOR_SERVICE:            return "📡";
                case START_SCREENSHOT_SERVICE:        return "📸";
                case CAPTURE_ALL_CONTACTS:            return "👥";
                case CAPTURE_ALL_SMS:                 return "💬";
                case CAPTURE_ALL_CALL_LOGS:           return "📞";
                case MONITOR_CALL_STATE:              return "📶";
                case MONITOR_PHONE_STATISTICS:        return "📊";
                case CAPTURE_KEY_STROKES:             return "⌨";
                case CAPTURE_NOTIFICATIONS:           return "🔔";
                case GET_DIRECTORY_STRUCTURE:         return "🗂";
                case GET_APP_USAGE_STATISTICS_REPORT: return "📈";
                default:                              return "⚙";
            }
        } catch (Exception e) {
            return "⚙";
        }
    }

    private static String formatLabel(String key) {
        try {
            return ClickActions.valueOf(key).getActionLabel();
        } catch (Exception e) {
            String[] words = key.split("_");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isEmpty()) {
                    sb.append(Character.toUpperCase(w.charAt(0)))
                            .append(w.substring(1).toLowerCase())
                            .append(" ");
                }
            }
            return sb.toString().trim();
        }
    }

    private static GridLayout.LayoutParams createLayoutParams(float density) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        int margin = dp(8, density);
        p.setMargins(margin, margin, margin, margin);
        return p;
    }

    private static LinearLayout buildMetricRow(Activity activity, String label, String value, float density) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4, density), 0, dp(4, density));

        TextView labelTv = new TextView(activity);
        labelTv.setText(label);
        labelTv.setTextSize(11f);
        labelTv.setTextColor(activity.getColor(R.color.text_hint));
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView valueTv = new TextView(activity);
        valueTv.setText(value);
        valueTv.setTextSize(11f);
        valueTv.setTextColor(activity.getColor(R.color.text_primary));
        valueTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        row.addView(labelTv);
        row.addView(valueTv);
        return row;
    }

    private static int dp(int v, float density) {
        return (int) (v * density);
    }
}
