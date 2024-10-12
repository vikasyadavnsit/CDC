package com.vikasyadavnsit.cdc.fragment;

import static com.vikasyadavnsit.cdc.utils.FirebaseUtils.getFlatUserDetails;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.vikasyadavnsit.cdc.enums.AdminSettingsFragmentActions;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class SettingsFragment extends Fragment {


    private static TextView userDetailsTextView;
    private static Spinner dropdownSpinner;
    private static ArrayAdapter<SpinnerItem> spinnerArrayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Determine the number of columns based on screen width
        int columnCount = calculateNoOfColumns();

        // Find the GridLayout by its ID
        GridLayout fragmentLayout = view.findViewById(R.id.fragment_layout);

        // Set the column count
        fragmentLayout.setColumnCount(columnCount);

        // Add buttons dynamically
        addDynamicButtons(fragmentLayout);

        userDetailsTextView = view.findViewById(R.id.setting_fragment_selected_user_details);
        dropdownSpinner = view.findViewById(R.id.setting_fragment_dropdown_spinner);
        getFlatUserDetails();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public static void populateUserDropdown(Activity activity, Map<String, User> userMap) {

        SpinnerItem[] items = new SpinnerItem[userMap.size() + 1];
        int index = 0;

        // Add a placeholder item
        items[index++] = new SpinnerItem("select a user", null); // Placeholder item

        for (Map.Entry<String, User> entry : userMap.entrySet()) {
            User user = entry.getValue();
            items[index++] = new SpinnerItem(user.getFullName(), user);
        }

        // Clear existing items and add new ones
        spinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownSpinner.setAdapter(spinnerArrayAdapter);

        // Set the spinner item selection listener
        dropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    // Get the selected item
                    SpinnerItem selectedItem = (SpinnerItem) parent.getItemAtPosition(position);
                    User selectedUser = selectedItem.getValue();
                    // Perform action based on the selected item
                    userDetailsTextView.setText(
                            new StringBuilder()
                                    .append("Id : " + selectedUser.getId() + "\n")
                                    .append("Name : " + selectedUser.getFullName() + "\n")
                                    .append("Brand : " + selectedUser.getDeviceDetails().get("brand") + "\n")
                                    .append("Model : " + selectedUser.getDeviceDetails().get("model") + "\n")
                                    .append("Android Version : " + selectedUser.getDeviceDetails().get("androidVersion"))
                    );
                    userDetailsTextView.findViewById(R.id.setting_fragment_selected_user_details).setVisibility(View.VISIBLE);
                    SharedPreferenceUtils.updateAdminSettingsUserAndroidId(activity, selectedUser.getDeviceDetails().get("androidId").toString());
                } else {
                    userDetailsTextView.findViewById(R.id.setting_fragment_selected_user_details).setVisibility(View.GONE);
                    SharedPreferenceUtils.updateAdminSettingsUserAndroidId(activity, null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Optional: Handle case when no selection is made
            }
        });
    }

    private int calculateNoOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 300); // Assuming each button's min width is 180dp
        return Math.max(2, Math.min(noOfColumns, 3)); // Minimum 1 column, maximum 3 columns
    }

    private void addDynamicButtons(GridLayout fragmentLayout) {
        AdminSettingsFragmentActions[] adminSettingsFragmentActions = AdminSettingsFragmentActions.values();
        Arrays.sort(adminSettingsFragmentActions, (a, b) -> a.getOrder() - b.getOrder());

        Arrays.stream(adminSettingsFragmentActions).forEach(
                fragmentAction -> {
                    LinearLayout groupLayout = getLinearLayout();
                    addTextInfo(fragmentAction, groupLayout);
                    addButtonAndAction(fragmentAction, groupLayout);
                    // Add the group to the GridLayout
                    fragmentLayout.addView(groupLayout);
                });

    }

    private void addButtonAndAction(AdminSettingsFragmentActions fragmentAction, LinearLayout groupLayout) {
        // Add a button
        Button button = new Button(getContext());
        button.setText(fragmentAction.getActionLabel());
        //button.setBackgroundColor(Color.parseColor("#FF0000"));
        button.setLayoutParams(createButtonLayoutParams());
        button.setOnClickListener(v -> {
            if (Objects.nonNull(SharedPreferenceUtils.getAdminSettingsUserAndroidId(getActivity()))) {
                fragmentAction.getConsumer().accept(getActivity().getSupportFragmentManager());
            } else {
                Toast.makeText(getContext(), "Please select a user first.", Toast.LENGTH_SHORT).show();
            }
        });

        groupLayout.addView(button);
    }

    private void addTextInfo(AdminSettingsFragmentActions fragmentAction, LinearLayout groupLayout) {
        // Add a label for each button
        TextView label = new TextView(getContext());
        label.setText("INFO : " + fragmentAction.getDescription());
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
