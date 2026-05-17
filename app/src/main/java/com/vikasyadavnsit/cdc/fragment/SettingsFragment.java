package com.vikasyadavnsit.cdc.fragment;

import static com.vikasyadavnsit.cdc.utils.FirebaseUtils.getFlatUserDetails;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.Map;

public class SettingsFragment extends Fragment {

    private static Spinner dropdownSpinner;
    private static ArrayAdapter<SpinnerItem> spinnerArrayAdapter;

    private Button continueButton;
    private ImageView refreshButton;
    private User selectedUser = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        dropdownSpinner = view.findViewById(R.id.settings_fragment_dropdown_spinner);
        continueButton = view.findViewById(R.id.settings_continue_button);
        refreshButton = view.findViewById(R.id.settings_refresh_button);

        continueButton.setOnClickListener(v -> {
            AdminViewersFragment fragment = AdminViewersFragment.newInstance(selectedUser);
            CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), fragment);
        });

        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Fetching latest users...", Toast.LENGTH_SHORT).show();
            FirebaseUtils.getFlatUserDetails(true);
        });

        setupSpinnerListener();
        getFlatUserDetails();

        return view;
    }

    private void setupSpinnerListener() {
        dropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    FirebaseUtils.setSelectedUser(null);
                    setContinueEnabled(false);
                    return;
                }
                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                User user = item.getValue();
                if (user != null && user.getDeviceDetails() != null) {
                    String androidId = (String) user.getDeviceDetails().get("androidId");
                    FirebaseUtils.setSelectedUser(androidId);
                    selectedUser = user;
                    setContinueEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                FirebaseUtils.setSelectedUser(null);
                setContinueEnabled(false);
            }
        });
    }

    private void setContinueEnabled(boolean enabled) {
        if (continueButton == null) return;
        continueButton.setEnabled(enabled);
        continueButton.setAlpha(enabled ? 1f : 0.4f);
    }

    public static void populateUserDropdown(Activity activity, Map<String, User> userMap) {
        SpinnerItem[] items = new SpinnerItem[userMap.size() + 1];
        items[0] = new SpinnerItem("Select a device", null);
        int index = 1;
        for (Map.Entry<String, User> entry : userMap.entrySet()) {
            String label = entry.getValue() != null && entry.getValue().getFullName() != null
                    ? entry.getValue().getFullName() : entry.getKey();
            items[index++] = new SpinnerItem(label, entry.getValue());
        }
        // Override getView and getDropDownView to set text color
        spinnerArrayAdapter = new ArrayAdapter<SpinnerItem>(activity, android.R.layout.simple_spinner_item, items) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView view = (android.widget.TextView) super.getView(position, convertView, parent);
                view.setTextColor(activity.getResources().getColor(R.color.text_primary));
                return view;
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView view = (android.widget.TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(activity.getResources().getColor(R.color.text_primary));
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownSpinner.setAdapter(spinnerArrayAdapter);
    }
}