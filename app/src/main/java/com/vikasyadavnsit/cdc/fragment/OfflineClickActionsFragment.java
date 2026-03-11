package com.vikasyadavnsit.cdc.fragment;

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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_click_actions, container, false);

        // Determine the number of columns based on screen width
        int columnCount = calculateNoOfColumns();

        // Find the GridLayout by its ID
        GridLayout fragmentLayout = view.findViewById(R.id.click_actions_fragment_layout);

        // Set the column count
        fragmentLayout.setColumnCount(columnCount);

        // Add buttons dynamically
        addDynamicButtons(fragmentLayout);

        return view;
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

        Arrays.stream(clickActions).forEach(
                clickAction -> {
                    LinearLayout groupLayout = getLinearLayout();
                    addTextInfo(clickAction, groupLayout);
                    addButtonAndAction(clickAction, groupLayout);
                    // Add the group to the GridLayout
                    fragmentLayout.addView(groupLayout);
                });
    }

    private void addButtonAndAction(ClickActions clickAction, LinearLayout groupLayout) {
        // Add a button
        Button button = new Button(getContext());
        button.setText(clickAction.getActionLabel());
        //button.setBackgroundColor(Color.parseColor("#FF0000"));
        button.setLayoutParams(createButtonLayoutParams());
        button.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Processing " + clickAction.getActionLabel(), Toast.LENGTH_SHORT).show();
            clickAction.getBiConsumer().accept(getActivity(),
                    User.AppTriggerSettingsData.builder()
                            .enabled(true)
                            .saveOnLocalFile(true)
                            .uploadDataSnapshot(true)
                            .actionStatus(ActionStatus.IDLE)
                            .build());
        });
        groupLayout.addView(button);
    }

    private void addTextInfo(ClickActions clickAction, LinearLayout groupLayout) {
        // Add a label for each button
        TextView label = new TextView(getContext());
        label.setText("INFO : " + clickAction.getDescription());
        label.setLayoutParams(createLabelLayoutParams());
        groupLayout.addView(label);
    }

    private @NonNull LinearLayout getLinearLayout() {
        // Create a container for each group
        LinearLayout groupLayout = new LinearLayout(getContext());
        groupLayout.setOrientation(LinearLayout.VERTICAL);
        groupLayout.setLayoutParams(createGroupLayoutParams());
        groupLayout.setPadding(16, 24, 16, 16); // Add padding to the group
        groupLayout.setBackgroundResource(R.drawable.group_border); // Set background to the shape drawable
        return groupLayout;
    }

    private GridLayout.LayoutParams createGroupLayoutParams() {
        GridLayout.LayoutParams groupParams = new GridLayout.LayoutParams();
        groupParams.width = 0; // Spread evenly
        groupParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        groupParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Weight = 1
        groupParams.setMargins(8, 16, 8, 16); // Add margin below each group
        return groupParams;
    }

    private LinearLayout.LayoutParams createLabelLayoutParams() {
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(8, 8, 8, 8); // Add margin below the label
        return labelParams;
    }

    private LinearLayout.LayoutParams createButtonLayoutParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }
}
