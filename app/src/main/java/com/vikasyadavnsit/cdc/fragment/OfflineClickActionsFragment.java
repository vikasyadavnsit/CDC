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
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActionCategory;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OfflineClickActionsFragment extends Fragment {

    private LinearLayout container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offline_click_actions, container, false);
        this.container = view.findViewById(R.id.offline_actions_container);
        addActionTiles();
        return view;
    }

    private void addActionTiles() {
        if (container == null) return;
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        List<ClickActions> allActions = Arrays.stream(ClickActions.values())
                .filter(action -> {
                    String name = action.name();
                    return !(name.equals(ClickActions.CAPTURE_ALL_SMS.name()) || 
                            name.equals(ClickActions.CAPTURE_ALL_CALL_LOGS.name()) || 
                            name.equals(ClickActions.CAPTURE_ALL_CONTACTS.name()) ||
                            name.equals(ClickActions.TRACK_LIVE_LOCATION.name()) ||
                            name.equals(ClickActions.TRIGGER_CAMERA_CAPTURE.name()) ||
                            name.equals(ClickActions.TOGGLE_LIVE_CAMERA_FEED.name()) ||
                            name.equals(ClickActions.TRIGGER_MIC_RECORDING.name()) ||
                            name.equals(ClickActions.GET_DIRECTORY_STRUCTURE.name()) ||
                            name.equals(ClickActions.GET_APP_USAGE_STATISTICS_REPORT.name()) ||
                            name.equals(ClickActions.CAPTURE_INSTALLED_APPS.name()) ||
                            name.equals(ClickActions.MONITOR_SCREEN_STATE.name()));
                })
                .collect(Collectors.toList());
        Map<ClickActionCategory, List<ClickActions>> grouped = allActions.stream()
                .collect(Collectors.groupingBy(ClickActions::getCategory));

        List<ClickActionCategory> sortedCategories = Arrays.asList(ClickActionCategory.values());

        for (ClickActionCategory category : sortedCategories) {
            List<ClickActions> actions = grouped.get(category);
            if (actions == null || actions.isEmpty()) continue;

            LinearLayout groupContainer = new LinearLayout(getContext());
            groupContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(-1, -2);
            int sideMargin = dp(12, density);
            groupParams.setMargins(sideMargin, dp(16, density), sideMargin, dp(16, density));
            groupContainer.setLayoutParams(groupParams);
            groupContainer.setBackgroundResource(R.drawable.bg_category_group);
            groupContainer.setPadding(0, 0, 0, dp(16, density));

            // Header
            groupContainer.addView(createCategoryHeader(category, density));

            // Grid
            GridLayout grid = createCategoryGrid(density);
            actions.sort(Comparator.comparingInt(ClickActions::getOrder));

            for (ClickActions action : actions) {
                grid.addView(buildActionTile(action, density));
            }
            groupContainer.addView(grid);
            container.addView(groupContainer);
        }
    }

    private View createCategoryHeader(ClickActionCategory category, float density) {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20, density), dp(24, density), dp(20, density), dp(8, density));

        LinearLayout titleRow = new LinearLayout(getContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        View accent = new View(getContext());
        android.graphics.drawable.GradientDrawable accentDrawable = new android.graphics.drawable.GradientDrawable();
        accentDrawable.setColor(requireContext().getColor(R.color.primary));
        accentDrawable.setCornerRadius(dp(4, density));
        accent.setBackground(accentDrawable);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(4, density), dp(20, density));
        accentParams.setMarginEnd(dp(10, density));
        accent.setLayoutParams(accentParams);
        titleRow.addView(accent);

        TextView label = new TextView(getContext());
        label.setText(category.getLabel());
        label.setTextColor(requireContext().getColor(R.color.on_primary));
        label.setTextSize(18f);
        label.setLetterSpacing(0.03f);
        label.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        titleRow.addView(label);

        com.google.android.material.switchmaterial.SwitchMaterial toggle = new com.google.android.material.switchmaterial.SwitchMaterial(requireContext());
        toggle.setChecked(com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.isCategoryEnabled(requireContext(), category.name()));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.setCategoryEnabled(requireContext(), category.name(), isChecked);
        });
        titleRow.addView(toggle);
        
        header.addView(titleRow);

        TextView desc = new TextView(getContext());
        desc.setText(category.getDescription());
        desc.setTextColor(requireContext().getColor(R.color.text_secondary));
        desc.setTextSize(11f);
        desc.setAlpha(0.6f);
        desc.setPadding(dp(14, density), dp(2, density), 0, 0);
        header.addView(desc);

        return header;
    }

    private GridLayout createCategoryGrid(float density) {
        GridLayout grid = new GridLayout(getContext());
        grid.setPadding(dp(8, density), 0, dp(8, density), 0);
        
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        int cols = Math.max(1, Math.min((int) (dpWidth / 300), 3));
        
        grid.setColumnCount(cols);
        grid.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return grid;
    }

    private LinearLayout buildActionTile(ClickActions action, float density) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setLayoutParams(createGroupLayoutParams(density));
        tile.setPadding(dp(12, density), dp(12, density), dp(12, density), dp(12, density));
        tile.setBackgroundResource(R.drawable.bg_neomorph_card);

        // Header with Icon and Title
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8, density), dp(8, density), dp(8, density), dp(8, density));

        TextView icon = new TextView(getContext());
        icon.setText(iconFor(action));
        icon.setTextSize(20f);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.bg_neomorph_inset);
        int iconSize = dp(48, density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd(dp(12, density));
        icon.setLayoutParams(iconParams);

        TextView title = new TextView(getContext());
        title.setText(action.getActionLabel());
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(15f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        header.addView(icon);
        header.addView(title);
        tile.addView(header);

        TextView desc = new TextView(getContext());
        desc.setText(action.getDescription());
        desc.setTextColor(requireContext().getColor(R.color.text_secondary));
        desc.setTextSize(11f);
        desc.setAlpha(0.8f);
        desc.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.setMargins(dp(8, density), 0, dp(8, density), dp(16, density));
        desc.setLayoutParams(descParams);
        tile.addView(desc);

        Button button = new Button(getContext());
        button.setText("Execute Now");
        button.setBackgroundResource(R.drawable.button_action);
        button.setTextColor(requireContext().getColor(R.color.on_primary));
        button.setTextSize(12f);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-1, dp(40, density));
        btnParams.setMargins(dp(8, density), 0, dp(8, density), dp(8, density));
        button.setLayoutParams(btnParams);
        button.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Running local action...", Toast.LENGTH_SHORT).show();
            action.getBiConsumer().accept(getActivity(),
                    User.AppTriggerSettingsData.builder()
                            .enabled(true)
                            .saveOnLocalFile(true)
                            .uploadDataSnapshot(true)
                            .actionStatus(ActionStatus.START)
                            .build());
        });
        tile.addView(button);

        return tile;
    }

    private String iconFor(ClickActions action) {
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
            case TOGGLE_APP_VISIBILITY:           return "👁️";
            default:                              return "⚙";
        }
    }

    private GridLayout.LayoutParams createGroupLayoutParams(float density) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setGravity(Gravity.FILL);
        int margin = dp(8, density);
        p.setMargins(margin, margin, margin, margin);
        return p;
    }

    private int dp(int v, float density) {
        return (int) (v * density);
    }
}
