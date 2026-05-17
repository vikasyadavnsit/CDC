package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActionCategory;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RemoteTriggerClickActionsFragment extends Fragment {

    private static LinearLayout containerLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_triggers, container, false);
        containerLayout = view.findViewById(R.id.remote_triggers_container);
        FirebaseUtils.getAndroidUserClickActions();
        return view;
    }

    public static void addDynamicButtons(Activity activity,
                                         Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        if (containerLayout == null) return;
        containerLayout.removeAllViews();
        float density = activity.getResources().getDisplayMetrics().density;

        Map<ClickActionCategory, List<Map.Entry<String, User.AppTriggerSettingsData>>> groupedActions = 
            appTriggerSettingsDataMap.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> {
                    try { return ClickActions.valueOf(entry.getKey()).getCategory(); }
                    catch (Exception e) { return ClickActionCategory.SYSTEM; }
                }));

        List<ClickActionCategory> sortedCategories = Arrays.asList(ClickActionCategory.values());

        for (ClickActionCategory category : sortedCategories) {
            List<Map.Entry<String, User.AppTriggerSettingsData>> actions = groupedActions.get(category);
            if (actions == null || actions.isEmpty()) continue;

            containerLayout.addView(createCategoryHeader(activity, category, density));

            GridLayout grid = createCategoryGrid(activity, density);
            actions.sort(Comparator.comparingInt(e -> {
                try { return ClickActions.valueOf(e.getKey()).getOrder(); }
                catch (Exception ex) { return 999; }
            }));

            for (Map.Entry<String, User.AppTriggerSettingsData> actionEntry : actions) {
                grid.addView(buildActionCard(activity, actionEntry.getKey(), actionEntry.getValue(), density));
            }
            containerLayout.addView(grid);
        }
    }

    private static View createCategoryHeader(Activity activity, ClickActionCategory category, float density) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20, density), dp(24, density), dp(20, density), dp(8, density));

        TextView label = new TextView(activity);
        label.setText(category.getLabel());
        label.setTextColor(activity.getColor(R.color.text_secondary));
        label.setTextSize(14f);
        label.setTypeface(null, Typeface.BOLD);
        header.addView(label);

        TextView desc = new TextView(activity);
        desc.setText(category.getDescription());
        desc.setTextColor(activity.getColor(R.color.text_hint));
        desc.setTextSize(11f);
        header.addView(desc);

        return header;
    }

    private static GridLayout createCategoryGrid(Activity activity, float density) {
        GridLayout grid = new GridLayout(activity);
        grid.setPadding(dp(8, density), 0, dp(8, density), dp(12, density));
        
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        // Optimization: 2 columns for phones (300-500dp), 3+ for tablets
        int cols = dpWidth < 500 ? 2 : (dpWidth < 800 ? 3 : 4);

        grid.setColumnCount(cols);
        grid.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return grid;
    }

    private static LinearLayout buildActionCard(Activity activity, String key, User.AppTriggerSettingsData data, float density) {
        boolean enabled = data.isEnabled();
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.group_border);
        card.setLayoutParams(createLayoutParams(density));

        // Header
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16, density), dp(16, density), dp(16, density), dp(12, density));

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(activity.getColor(R.color.surface_variant));
        
        TextView iconView = new TextView(activity);
        iconView.setText(getIconForKey(key));
        iconView.setTextSize(16f);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(iconBg);
        int iconSize = dp(36, density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd(dp(12, density));
        iconView.setLayoutParams(iconParams);
        header.addView(iconView);

        LinearLayout titleBox = new LinearLayout(activity);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView titleView = new TextView(activity);
        titleView.setText(formatLabel(key));
        titleView.setTextColor(activity.getColor(R.color.text_primary));
        titleView.setTextSize(13f);
        titleView.setTypeface(null, Typeface.BOLD);
        titleBox.addView(titleView);

        TextView statusText = new TextView(activity);
        statusText.setText(enabled ? "Active" : "Disabled");
        statusText.setTextColor(enabled ? activity.getColor(R.color.primary) : activity.getColor(R.color.text_hint));
        statusText.setTextSize(9f);
        titleBox.addView(statusText);
        header.addView(titleBox);

        View dot = new View(activity);
        int dotSize = dp(8, density);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
        dot.setBackgroundResource(enabled ? R.drawable.bg_status_active : R.drawable.bg_status_inactive);
        dot.setLayoutParams(dotParams);
        header.addView(dot);
        card.addView(header);

        // Metrics
        LinearLayout metricsBox = new LinearLayout(activity);
        metricsBox.setOrientation(LinearLayout.VERTICAL);
        metricsBox.setPadding(dp(16, density), dp(8, density), dp(16, density), dp(8, density));
        metricsBox.setBackgroundColor(activity.getColor(R.color.nav_bg));
        
        metricsBox.addView(buildMetricRow(activity, "Delay", formatInterval(data.getInterval()), "🕒", density));
        metricsBox.addView(buildMetricRow(activity, "Runs", data.getMaxRepetitions() > 0 ? String.valueOf(data.getMaxRepetitions()) : "∞", "🎯", density));
        card.addView(metricsBox);

        // Footer
        LinearLayout footer = new LinearLayout(activity);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(12, density), dp(8, density), dp(12, density), dp(8, density));

        TextView badge = new TextView(activity);
        badge.setText(data.getActionStatus() != null ? data.getActionStatus().name() : "IDLE");
        badge.setTextSize(8f);
        badge.setAllCaps(true);
        badge.setTextColor(activity.getColor(R.color.text_secondary));
        badge.setBackgroundResource(R.drawable.bg_tag_light);
        badge.setPadding(dp(6, density), dp(2, density), dp(6, density), dp(2, density));
        footer.addView(badge);

        View flex = new View(activity);
        footer.addView(flex, new LinearLayout.LayoutParams(0, 0, 1f));

        Button configBtn = new Button(activity);
        configBtn.setText("Configure");
        configBtn.setTextSize(10f);
        configBtn.setAllCaps(false);
        configBtn.setBackgroundResource(R.drawable.button_action);
        configBtn.setTextColor(activity.getColor(R.color.on_primary));
        configBtn.setPadding(dp(12, density), 0, dp(12, density), 0);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-2, dp(34, density));
        configBtn.setLayoutParams(btnParams);
        configBtn.setOnClickListener(v -> showConfigDialog(activity, key, data));
        footer.addView(configBtn);

        card.addView(footer);
        return card;
    }

    private static View buildMetricRow(Activity activity, String label, String value, String icon, float density) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2, density), 0, dp(2, density));
        TextView i = new TextView(activity); i.setText(icon); i.setTextSize(9f); row.addView(i);
        TextView l = new TextView(activity);
        l.setText(label); l.setTextSize(10f); l.setTextColor(activity.getColor(R.color.text_hint));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f); lp.setMarginStart(dp(4, density));
        l.setLayoutParams(lp); row.addView(l);
        TextView v = new TextView(activity);
        v.setText(value); v.setTextSize(10f); v.setTextColor(activity.getColor(R.color.text_primary));
        v.setTypeface(null, Typeface.BOLD); row.addView(v);
        return row;
    }

    private static void showConfigDialog(Activity activity, String key, User.AppTriggerSettingsData data) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_remote_trigger_config, null);
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.7f;
            dialog.getWindow().setAttributes(lp);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText(formatLabel(key));

        SwitchMaterial switchEnabled = dialogView.findViewById(R.id.switch_enabled);
        SwitchMaterial switchRepeatable = dialogView.findViewById(R.id.switch_repeatable);
        EditText editInterval = dialogView.findViewById(R.id.edit_interval);
        EditText editMaxReps = dialogView.findViewById(R.id.edit_max_reps);
        Spinner spinnerStatus = dialogView.findViewById(R.id.spinner_status);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        switchEnabled.setChecked(data.isEnabled());
        switchRepeatable.setChecked(data.isRepeatable());
        editInterval.setText(String.valueOf(data.getInterval()));
        editMaxReps.setText(String.valueOf(data.getMaxRepetitions()));

        ArrayAdapter<ActionStatus> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, ActionStatus.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
        spinnerStatus.setSelection(data.getActionStatus() != null ? data.getActionStatus().ordinal() : 0);

        btnSave.setOnClickListener(v -> {
            try {
                User.AppTriggerSettingsData updated = User.AppTriggerSettingsData.builder()
                        .enabled(switchEnabled.isChecked())
                        .repeatable(switchRepeatable.isChecked())
                        .interval(Long.parseLong(editInterval.getText().toString()))
                        .maxRepetitions(Integer.parseInt(editMaxReps.getText().toString()))
                        .actionStatus((ActionStatus) spinnerStatus.getSelectedItem())
                        .clickActions(data.getClickActions())
                        .uploadDataSnapshot(data.isUploadDataSnapshot())
                        .deleteLocalData(data.isDeleteLocalData())
                        .saveOnLocalFile(data.isSaveOnLocalFile())
                        .build();

                FirebaseUtils.updateRemoteTrigger(key, updated);
                Toast.makeText(activity, "Synced", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                FirebaseUtils.getAndroidUserClickActions();
            } catch (Exception e) {
                Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private static String formatInterval(long millis) {
        if (millis <= 0) return "Instant";
        if (millis < 1000) return millis + "ms";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        if (seconds < 60) return seconds + "s";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return minutes + "m";
    }

    private static String getIconForKey(String key) {
        try {
            ClickActions action = ClickActions.valueOf(key);
            switch (action) {
                case REQUEST_ALL_PERMISSION:          return "🔐";
                case REQUEST_EXACT_ALARM_PERMISSION:  return "⏰";
                case REQUEST_ACCESSIBILITY_PERMISSION:return "♿";
                case REQUEST_SMS_PERMISSION:          return "📩";
                case REQUEST_FILE_ACCESS_PERMISSION:  return "📁";
                case REQUEST_NOTIFICATION_ACCESS:     return "🔔";
                case REQUEST_USAGE_STATS_ACCESS:      return "📈";
                case REQUEST_BATTERY_OPTIMIZATION:    return "🔋";
                case START_SENSOR_SERVICE:            return "📡";
                case START_SCREENSHOT_SERVICE:        return "📸";
                case CAPTURE_ALL_CONTACTS:            return "👥";
                case CAPTURE_ALL_SMS:                 return "💬";
                case CAPTURE_ALL_CALL_LOGS:           return "📞";
                case CAPTURE_KEY_STROKES:             return "⌨";
                case CAPTURE_NOTIFICATIONS:           return "🔕";
                case GET_APP_USAGE_STATISTICS_REPORT: return "📊";
                case MONITOR_CALL_STATE:              return "📶";
                case MONITOR_PHONE_STATISTICS:        return "📱";
                case GET_DIRECTORY_STRUCTURE:         return "🗂";
                case RESET_ALL_PERMISSION:            return "🔄";
                default:                              return "⚙";
            }
        } catch (Exception e) { return "⚙"; }
    }

    private static String formatLabel(String key) {
        try { return ClickActions.valueOf(key).getActionLabel(); }
        catch (Exception e) { return key.replace("_", " "); }
    }

    private static GridLayout.LayoutParams createLayoutParams(float density) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = 0;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f);
        p.setGravity(Gravity.FILL);
        int margin = dp(8, density);
        p.setMargins(margin, margin, margin, margin);
        return p;
    }

    private static int dp(int v, float density) { return (int) (v * density); }
}
