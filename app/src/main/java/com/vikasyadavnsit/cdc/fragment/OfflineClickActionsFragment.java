package com.vikasyadavnsit.cdc.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
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
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.util.Arrays;

public class OfflineClickActionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offline_click_actions, container, false);
        GridLayout grid = view.findViewById(R.id.click_actions_fragment_layout);
        grid.setColumnCount(calculateNoOfColumns());
        addActionTiles(grid);
        return view;
    }

    private int calculateNoOfColumns() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        return Math.max(1, Math.min((int) (dpWidth / 300), 3));
    }

    private void addActionTiles(GridLayout grid) {
        ClickActions[] actions = ClickActions.values();
        Arrays.sort(actions, (a, b) -> a.getOrder() - b.getOrder());
        for (ClickActions action : actions) {
            grid.addView(buildTile(action));
        }
    }

    private LinearLayout buildTile(ClickActions action) {
        float density = getResources().getDisplayMetrics().density;

        LinearLayout tile = new LinearLayout(getContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setBackgroundResource(R.drawable.group_border);
        int pad = dp(16, density);
        tile.setPadding(pad, pad, pad, pad);

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(8, density), dp(8, density), dp(8, density), dp(8, density));
        tile.setLayoutParams(p);

        TextView title = new TextView(getContext());
        title.setText(action.getActionLabel());
        title.setTextColor(requireContext().getColor(R.color.text_secondary));
        title.setTextSize(13f);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(0, 0, 0, dp(6, density));
        title.setLayoutParams(tp);
        tile.addView(title);

        View divider = new View(getContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.divider));
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dp2.setMargins(0, 0, 0, dp(10, density));
        divider.setLayoutParams(dp2);
        tile.addView(divider);

        TextView desc = new TextView(getContext());
        desc.setText(action.getDescription());
        desc.setTextColor(requireContext().getColor(R.color.text_hint));
        desc.setTextSize(12f);
        desc.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams dp3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dp3.setMargins(0, 0, 0, dp(14, density));
        desc.setLayoutParams(dp3);
        tile.addView(desc);

        Button button = new Button(getContext());
        button.setText(action.getActionLabel());
        button.setBackgroundResource(R.drawable.button_action);
        button.setTextColor(requireContext().getColor(R.color.on_primary));
        button.setTextSize(13f);
        button.setAllCaps(false);
        button.setLetterSpacing(0.03f);
        button.setPadding(dp(12, density), dp(10, density), dp(12, density), dp(10, density));
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

    private int dp(int v, float density) {
        return (int) (v * density);
    }
}
