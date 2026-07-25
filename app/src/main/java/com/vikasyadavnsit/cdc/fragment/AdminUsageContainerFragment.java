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

public class AdminUsageContainerFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_usage_container, container, false);

        tabLayout = view.findViewById(R.id.usage_tabs);
        viewPager = view.findViewById(R.id.usage_viewpager);

        view.findViewById(R.id.usage_back_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        view.findViewById(R.id.usage_refresh_button).setOnClickListener(v -> {
            FirebaseUtils.requestAppUsageUpdate();
            FirebaseUtils.getRemoteInstalledApps();
            FirebaseUtils.getRemoteScreenState();
            android.widget.Toast.makeText(getContext(), "Refresh command sent to device", android.widget.Toast.LENGTH_SHORT).show();
        });

        viewPager.setAdapter(new UsagePagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Usage Report");
                    tab.setIcon(android.R.drawable.ic_menu_recent_history);
                    break;
                case 1:
                    tab.setText("Installed Apps");
                    tab.setIcon(android.R.drawable.ic_menu_manage);
                    break;
                case 2:
                    tab.setText("Screen Events");
                    tab.setIcon(android.R.drawable.ic_menu_view);
                    break;
            }
        }).attach();

        // Initial load
        FirebaseUtils.getAndroidUserSystemAppUsageStatistics();
        FirebaseUtils.getRemoteInstalledApps();
        FirebaseUtils.getRemoteScreenState();

        return view;
    }

    private static class UsagePagerAdapter extends FragmentStateAdapter {
        public UsagePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new SystemAppUsageStatisticsFragment();
                case 1: return new AdminInstalledAppsFragment();
                case 2: return new AdminScreenStateFragment();
                default: return new SystemAppUsageStatisticsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
