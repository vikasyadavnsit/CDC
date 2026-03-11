package com.vikasyadavnsit.cdc.enums;

import androidx.fragment.app.FragmentManager;

import com.vikasyadavnsit.cdc.fragment.AccessibilityNotificationFragment;
import com.vikasyadavnsit.cdc.fragment.ClickActionsFragment;
import com.vikasyadavnsit.cdc.fragment.KeyStrokesFragment;
import com.vikasyadavnsit.cdc.fragment.SystemAppUsageStatisticsFragment;
import com.vikasyadavnsit.cdc.utils.CommonUtil;

import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdminSettingsFragmentActions {

    CLICK_ACTIONS_PERMISSIONS(
            1,
            (fragmentManager) -> {
                CommonUtil.loadFragmentWithBackStack(fragmentManager, new ClickActionsFragment());
            },
            "This will open a permissions window to take system permissions.",
            "Click Action Permissions"
    ),

    VIEW_KEYSTROKES(
            2,
            (fragmentManager) -> {
                CommonUtil.loadFragmentWithBackStack(fragmentManager, new KeyStrokesFragment());
            },
            "This will open a keystrokes window, showing real-time keystrokes feeds of the users. You can read/delete and export the keystrokes.",
            "View Keystrokes"
    ),

    VIEW_APP_USAGE_STATISTICS(
            3,
            (fragmentManager) -> {
                CommonUtil.loadFragmentWithBackStack(fragmentManager, new SystemAppUsageStatisticsFragment());
            },
            "This will open a system app usage statistics window that will show the system app usage statistics of the users.",
            "View System App Usage Statistics"
    ),
    VIEW_ACCESSIBILITY_NOTIFICATIONS(
            4,
            (fragmentManager) -> {
                CommonUtil.loadFragmentWithBackStack(fragmentManager, new AccessibilityNotificationFragment());
            },
            "This will open an accessibility notification window that will show the accessibility notifications of the users.",
            "View Accessibility Notifications"
    );

    private final int order;
    private final Consumer<FragmentManager> consumer;
    private final String description;
    private final String actionLabel;

}
