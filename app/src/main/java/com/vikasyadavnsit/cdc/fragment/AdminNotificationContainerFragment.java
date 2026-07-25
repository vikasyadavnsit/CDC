package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

public class AdminNotificationContainerFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_notification_container, container, false);

        tabLayout = view.findViewById(R.id.notification_tabs);
        viewPager = view.findViewById(R.id.notification_viewpager);

        view.findViewById(R.id.notification_back_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        view.findViewById(R.id.notification_refresh_button).setOnClickListener(v -> {
            FirebaseUtils.getAndroidUserAccessibilityNotification();
            android.widget.Toast.makeText(getContext(), "Syncing notifications...", android.widget.Toast.LENGTH_SHORT).show();
        });

        viewPager.setAdapter(new NotificationPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Sent Alerts");
                    tab.setIcon(android.R.drawable.ic_dialog_email);
                    break;
                case 1:
                    tab.setText("Captured");
                    tab.setIcon(android.R.drawable.ic_menu_recent_history);
                    break;
            }
        }).attach();

        // Initial load
        FirebaseUtils.getAndroidUserAccessibilityNotification();

        return view;
    }

    private static class NotificationPagerAdapter extends FragmentStateAdapter {
        public NotificationPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new AdminNotificationFragment();
                case 1: return new AccessibilityNotificationFragment();
                default: return new AdminNotificationFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
