package com.vikasyadavnsit.cdc.fragment;

import static com.vikasyadavnsit.cdc.utils.FirebaseUtils.getFlatUserDetails;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.BuildConfig;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.SpinnerItem;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import java.util.Map;

public class SettingsFragment extends Fragment {

    private static Spinner dropdownSpinner;
    private static ArrayAdapter<SpinnerItem> spinnerArrayAdapter;

    private Button continueButton;
    private Button resetModeButton;
    private ImageView refreshButton;
    private TextView versionInstalled;
    private TextView versionLatest;
    private TextView versionStatus;
    private User selectedUser = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        dropdownSpinner   = view.findViewById(R.id.settings_fragment_dropdown_spinner);
        continueButton    = view.findViewById(R.id.settings_continue_button);
        resetModeButton   = view.findViewById(R.id.settings_reset_mode_button);
        refreshButton     = view.findViewById(R.id.settings_refresh_button);
        versionInstalled  = view.findViewById(R.id.settings_version_installed);
        versionLatest     = view.findViewById(R.id.settings_version_latest);
        versionStatus     = view.findViewById(R.id.settings_version_status);

        continueButton.setOnClickListener(v -> {
            AdminViewersFragment fragment = AdminViewersFragment.newInstance(selectedUser);
            CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), fragment);
        });

        resetModeButton.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_CDC_MaterialAlertDialog)
                    .setTitle(R.string.settings_reset_title)
                    .setMessage(R.string.settings_reset_message)
                    .setIcon(R.drawable.ic_nav_settings)
                    .setPositiveButton("Reset", (dialog, which) -> {
                        SharedPreferenceUtils.setUserRole(requireContext(), com.vikasyadavnsit.cdc.enums.UserRole.NONE);
                        android.content.Intent intent = new android.content.Intent(requireContext(), com.vikasyadavnsit.cdc.activities.MainActivity.class);
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Fetching latest users...", Toast.LENGTH_SHORT).show();
            FirebaseUtils.getFlatUserDetails(true);
        });

        setupSpinnerListener();
        getFlatUserDetails();
        setupVersionCard();

        return view;
    }

    private void setupSpinnerListener() {
        dropdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    FirebaseUtils.setSelectedUser(null);
                    SharedPreferenceUtils.updateAdminSettingsUserAndroidId(requireContext(), null);
                    setContinueEnabled(false);
                    return;
                }
                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                User user = item.getValue();
                if (user != null && user.getDeviceDetails() != null) {
                    String androidId = (String) user.getDeviceDetails().get("androidId");
                    FirebaseUtils.setSelectedUser(androidId);
                    SharedPreferenceUtils.updateAdminSettingsUserAndroidId(requireContext(), androidId);
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

    private void setupVersionCard() {
        String installed = BuildConfig.VERSION_NAME + "  (" + BuildConfig.VERSION_CODE + ")";
        versionInstalled.setText(installed);

        FirebaseUtils.listenForAppUpdate((latestVersionCode, apkStoragePath) -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                versionLatest.setText(String.valueOf(latestVersionCode));
                versionStatus.setVisibility(View.VISIBLE);
                if (latestVersionCode > BuildConfig.VERSION_CODE) {
                    versionStatus.setText("Update available  v"
                            + BuildConfig.VERSION_CODE + " → v" + latestVersionCode);
                    versionStatus.setTextColor(Color.parseColor("#FF9800")); // orange
                } else {
                    versionStatus.setText("Up to date");
                    versionStatus.setTextColor(Color.parseColor("#66BB6A")); // green
                }
            });
        });
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

        // Auto-restore previously selected device
        String savedId = SharedPreferenceUtils.getAdminSettingsUserAndroidId(activity);
        if (savedId != null) {
            for (int i = 1; i < items.length; i++) {
                User u = items[i].getValue();
                if (u != null && u.getDeviceDetails() != null
                        && savedId.equals(u.getDeviceDetails().get("androidId"))) {
                    dropdownSpinner.setSelection(i);
                    break;
                }
            }
        }
    }
}