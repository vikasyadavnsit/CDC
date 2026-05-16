package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
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
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.Map;

public class ClickActionsFragment extends Fragment {

    private static GridLayout fragmentLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_click_actions, container, false);
        fragmentLayout = view.findViewById(R.id.click_actions_fragment_layout);
        fragmentLayout.setColumnCount(calculateNoOfColumns());
        FirebaseUtils.getAndroidUserClickActions();
        return view;
    }

    private int calculateNoOfColumns() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3));
    }

    public static void addDynamicButtons(Activity activity,
                                         Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap) {
        float density = activity.getResources().getDisplayMetrics().density;
        appTriggerSettingsDataMap.entrySet().forEach(entry -> {
            LinearLayout card = buildCard(activity, density);

            TextView actionName = new TextView(activity);
            actionName.setText(formatActionKey(entry.getKey()));
            actionName.setTextColor(activity.getColor(R.color.text_primary));
            actionName.setTextSize(13f);
            actionName.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            nameParams.setMargins(0, 0, 0, dp(6, density));
            actionName.setLayoutParams(nameParams);
            card.addView(actionName);

            User.AppTriggerSettingsData data = entry.getValue();
            boolean enabled = data.isEnabled();

            TextView statusChip = new TextView(activity);
            statusChip.setText(enabled ? "● Enabled" : "○ Disabled");
            statusChip.setTextColor(enabled
                    ? activity.getColor(R.color.primary)
                    : activity.getColor(R.color.text_hint));
            statusChip.setTextSize(11f);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            chipParams.setMargins(0, 0, 0, dp(8, density));
            statusChip.setLayoutParams(chipParams);
            card.addView(statusChip);

            View divider = new View(activity);
            divider.setBackgroundColor(activity.getColor(R.color.divider));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divParams.setMargins(0, 0, 0, dp(8, density));
            divider.setLayoutParams(divParams);
            card.addView(divider);

            TextView detail = new TextView(activity);
            detail.setText(buildDetailText(data));
            detail.setTextColor(activity.getColor(R.color.text_hint));
            detail.setTextSize(11f);
            detail.setLineSpacing(0, 1.4f);
            detail.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            card.addView(detail);

            fragmentLayout.addView(card);
        });
    }

    private static LinearLayout buildCard(Activity activity, float density) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.group_border);
        int pad = dp(16, density);
        card.setPadding(pad, pad, pad, pad);
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(8, density), dp(8, density), dp(8, density), dp(8, density));
        card.setLayoutParams(p);
        return card;
    }

    private static String formatActionKey(String key) {
        return key.replace('_', ' ').toLowerCase()
                .substring(0, 1).toUpperCase()
                + key.replace('_', ' ').toLowerCase().substring(1);
    }

    private static String buildDetailText(User.AppTriggerSettingsData d) {
        return "Interval: " + d.getInterval() + "ms"
                + "  |  Max: " + d.getMaxRepetitions()
                + "  |  Status: " + d.getActionStatus();
    }

    private static int dp(int v, float density) {
        return (int) (v * density);
    }
}
