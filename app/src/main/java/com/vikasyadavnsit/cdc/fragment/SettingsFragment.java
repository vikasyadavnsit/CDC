package com.vikasyadavnsit.cdc.fragment;

import static com.vikasyadavnsit.cdc.utils.FirebaseUtils.getFlatUserDetails;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.util.Arrays;
import java.util.Map;

public class SettingsFragment extends Fragment {


    private static Spinner dropdownSpinner;
    private static ArrayAdapter<SpinnerItem> spinnerArrayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        dropdownSpinner = view.findViewById(R.id.settings_fragment_dropdown_spinner);

        GridLayout fragmentLayout = view.findViewById(R.id.fragment_layout);
        fragmentLayout.setColumnCount(calculateNoOfColumns());
        addDynamicButtons(fragmentLayout);

        getFlatUserDetails();

        return view;
    }

    public static void populateUserDropdown(Activity activity, Map<String, User> userMap) {
        SpinnerItem[] items = new SpinnerItem[userMap.size() + 1];
        items[0] = new SpinnerItem("Select a user", null);
        int index = 1;
        for (Map.Entry<String, User> entry : userMap.entrySet()) {
            String label = entry.getValue() != null && entry.getValue().getFullName() != null
                    ? entry.getValue().getFullName() : entry.getKey();
            items[index++] = new SpinnerItem(label, entry.getValue());
        }
        spinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownSpinner.setAdapter(spinnerArrayAdapter);
    }

    private int calculateNoOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 300); // Assuming each button's min width is 180dp
        return Math.max(1, Math.min(noOfColumns, 3)); // Minimum 1 column, maximum 3 columns
    }

    private void addDynamicButtons(GridLayout fragmentLayout) {
        ClickActions[] clickActions = ClickActions.values();
        Arrays.sort(clickActions, (a, b) -> a.getOrder() - b.getOrder());
        Arrays.stream(clickActions).forEach(action -> fragmentLayout.addView(buildTile(action)));
    }

    private LinearLayout buildTile(ClickActions action) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setLayoutParams(createGroupLayoutParams());
        tile.setPadding(dp(16), dp(16), dp(16), dp(16));
        tile.setBackgroundResource(R.drawable.group_border);

        // ── Icon + title row ──────────────────────────────
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dp(10));
        header.setLayoutParams(headerParams);

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(requireContext().getColor(R.color.surface_variant));

        TextView icon = new TextView(getContext());
        icon.setText(iconFor(action));
        icon.setTextSize(14f);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(iconBg);
        int iconSize = dp(30);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd(dp(10));
        icon.setLayoutParams(iconParams);

        TextView title = new TextView(getContext());
        title.setText(action.getActionLabel());
        title.setTextColor(requireContext().getColor(R.color.text_secondary));
        title.setTextSize(14f);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        header.addView(icon);
        header.addView(title);
        tile.addView(header);

        // ── Divider ───────────────────────────────────────
        View divider = new View(getContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.divider));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, 0, 0, dp(10));
        divider.setLayoutParams(divParams);
        tile.addView(divider);

        // ── Short description ─────────────────────────────
        TextView desc = new TextView(getContext());
        desc.setText(action.getDescription());
        desc.setTextColor(requireContext().getColor(R.color.text_hint));
        desc.setTextSize(13f);
        desc.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, 0, 0, dp(14));
        desc.setLayoutParams(descParams);
        tile.addView(desc);

        // ── Action button ─────────────────────────────────
        Button button = new Button(getContext());
        button.setText(action.getActionLabel());
        button.setBackgroundResource(R.drawable.button_action);
        button.setTextColor(requireContext().getColor(R.color.on_primary));
        button.setTextSize(13f);
        button.setAllCaps(false);
        button.setLetterSpacing(0.03f);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v -> {
            Toast.makeText(getContext(), action.getActionLabel(), Toast.LENGTH_SHORT).show();
            action.getBiConsumer().accept(getActivity(),
                    User.AppTriggerSettingsData.builder()
                            .enabled(true)
                            .saveOnLocalFile(true)
                            .uploadDataSnapshot(true)
                            .actionStatus(ActionStatus.IDLE)
                            .build());
        });
        tile.addView(button);

        return tile;
    }

    private String iconFor(ClickActions action) {
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
    }

    private GridLayout.LayoutParams createGroupLayoutParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(8), dp(8), dp(8), dp(8));
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
