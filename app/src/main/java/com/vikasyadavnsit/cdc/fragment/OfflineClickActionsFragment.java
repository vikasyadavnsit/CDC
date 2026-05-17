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

        List<ClickActions> allActions = Arrays.asList(ClickActions.values());
        Map<ClickActionCategory, List<ClickActions>> grouped = allActions.stream()
                .collect(Collectors.groupingBy(ClickActions::getCategory));

        List<ClickActionCategory> sortedCategories = Arrays.asList(ClickActionCategory.values());

        for (ClickActionCategory category : sortedCategories) {
            List<ClickActions> actions = grouped.get(category);
            if (actions == null || actions.isEmpty()) continue;

            // Header
            container.addView(createCategoryHeader(category, density));

            // Grid
            GridLayout grid = createCategoryGrid(density);
            actions.sort(Comparator.comparingInt(ClickActions::getOrder));

            for (ClickActions action : actions) {
                grid.addView(buildActionTile(action, density));
            }
            container.addView(grid);
        }
    }

    private View createCategoryHeader(ClickActionCategory category, float density) {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20, density), dp(24, density), dp(20, density), dp(8, density));

        TextView label = new TextView(getContext());
        label.setText(category.getLabel());
        label.setTextColor(requireContext().getColor(R.color.text_secondary));
        label.setTextSize(14f);
        label.setTypeface(null, Typeface.BOLD);
        header.addView(label);

        TextView desc = new TextView(getContext());
        desc.setText(category.getDescription());
        desc.setTextColor(requireContext().getColor(R.color.text_hint));
        desc.setTextSize(11f);
        header.addView(desc);

        return header;
    }

    private GridLayout createCategoryGrid(float density) {
        GridLayout grid = new GridLayout(getContext());
        grid.setPadding(dp(10, density), 0, dp(10, density), dp(16, density));
        
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
        tile.setPadding(dp(16, density), dp(16, density), dp(16, density), dp(16, density));
        tile.setBackgroundResource(R.drawable.group_border);

        // Header with Icon and Title
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.setMargins(0, 0, 0, dp(12, density));
        header.setLayoutParams(headerParams);

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(requireContext().getColor(R.color.surface_variant));

        TextView icon = new TextView(getContext());
        icon.setText(iconFor(action));
        icon.setTextSize(14f);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(iconBg);
        int iconSize = dp(32, density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd(dp(10, density));
        icon.setLayoutParams(iconParams);

        TextView title = new TextView(getContext());
        title.setText(action.getActionLabel());
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(13f);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        header.addView(icon);
        header.addView(title);
        tile.addView(header);

        View divider = new View(getContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.divider));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(-1, 1);
        divParams.setMargins(0, 0, 0, dp(12, density));
        divider.setLayoutParams(divParams);
        tile.addView(divider);

        TextView desc = new TextView(getContext());
        desc.setText(action.getDescription());
        desc.setTextColor(requireContext().getColor(R.color.text_hint));
        desc.setTextSize(11f);
        desc.setLineSpacing(0, 1.3f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.setMargins(0, 0, 0, dp(16, density));
        desc.setLayoutParams(descParams);
        tile.addView(desc);

        Button button = new Button(getContext());
        button.setText("Execute");
        button.setBackgroundResource(R.drawable.button_action);
        button.setTextColor(requireContext().getColor(R.color.on_primary));
        button.setTextSize(11f);
        button.setAllCaps(false);
        button.setPadding(dp(12, density), 0, dp(12, density), 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(32, density)));
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
            case TRACK_LIVE_LOCATION:             return "📍";
            default:                              return "⚙";
        }
    }

    private GridLayout.LayoutParams createGroupLayoutParams(float density) {
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

    private int dp(int v, float density) {
        return (int) (v * density);
    }
}
