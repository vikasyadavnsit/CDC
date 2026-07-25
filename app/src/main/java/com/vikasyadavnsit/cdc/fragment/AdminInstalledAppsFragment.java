package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminInstalledAppsFragment extends Fragment {

    private static WeakReference<AdminInstalledAppsFragment> activeInstance;

    private static final int FILTER_ALL    = 0;
    private static final int FILTER_USER   = 1;
    private static final int FILTER_SYSTEM = 2;

    private RecyclerView    recyclerView;
    private TextView        emptyText, summaryText;
    private TextInputEditText searchInput;
    private View            loader;
    private AppAdapter      adapter;
    private ChipGroup       filterChips;

    private static Map<String, Map<String, String>> lastData;
    private List<Map<String, String>> fullData = new ArrayList<>();
    private int activeFilter = FILTER_ALL;
    private String activeQuery = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_installed_apps, container, false);

        recyclerView = view.findViewById(R.id.apps_recycler);
        emptyText    = view.findViewById(R.id.apps_empty_text);
        summaryText  = view.findViewById(R.id.apps_summary);
        loader       = view.findViewById(R.id.apps_loader);
        filterChips  = view.findViewById(R.id.apps_filter_chips);
        searchInput  = view.findViewById(R.id.apps_search_input);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppAdapter(getContext());
        recyclerView.setAdapter(adapter);

        setupFilterChips();
        setupSearch();

        if (lastData != null) {
            updateUI(lastData);
        } else {
            loader.setVisibility(View.VISIBLE);
        }

        FirebaseUtils.getRemoteInstalledApps();
        return view;
    }

    private void setupFilterChips() {
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_filter_user)   activeFilter = FILTER_USER;
            else if (id == R.id.chip_filter_system) activeFilter = FILTER_SYSTEM;
            else                                    activeFilter = FILTER_ALL;
            applyFilter();
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeQuery = s.toString();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        List<Map<String, String>> result = fullData.stream()
                .filter(m -> {
                    if (activeFilter == FILTER_USER)   return !"true".equals(m.get("isSystem"));
                    if (activeFilter == FILTER_SYSTEM) return  "true".equals(m.get("isSystem"));
                    return true;
                })
                .filter(m -> {
                    if (activeQuery.isEmpty()) return true;
                    String q = activeQuery.toLowerCase();
                    return m.getOrDefault("name", "").toLowerCase().contains(q)
                        || m.getOrDefault("packageName", "").toLowerCase().contains(q);
                })
                .collect(Collectors.toList());

        adapter.submitList(result);
    }

    public static void displayInstalledApps(Activity activity, Map<String, Map<String, String>> data) {
        lastData = data;
        if (activeInstance != null && activeInstance.get() != null) {
            AdminInstalledAppsFragment fragment = activeInstance.get();
            if (activity != null) {
                activity.runOnUiThread(() -> fragment.updateUI(data));
            }
        }
    }

    private void updateUI(Map<String, Map<String, String>> data) {
        if (getView() == null) return;
        loader.setVisibility(View.GONE);

        if (data == null || data.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        fullData = new ArrayList<>(data.values());
        fullData.sort((a, b) ->
                a.getOrDefault("name", "").compareToIgnoreCase(b.getOrDefault("name", "")));

        long userCount   = fullData.stream().filter(m -> !"true".equals(m.get("isSystem"))).count();
        long systemCount = fullData.stream().filter(m ->  "true".equals(m.get("isSystem"))).count();

        summaryText.setVisibility(View.VISIBLE);
        summaryText.setText(fullData.size() + " apps total  ·  " + userCount + " user  ·  " + systemCount + " system");

        applyFilter();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activeInstance = null;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private static class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

        private List<Map<String, String>> dataList = new ArrayList<>();
        private final Context context;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        private static final int[] AVATAR_COLORS = {
            0xFF7B2FBE, 0xFF5C6BC0, 0xFF00897B, 0xFFE53935,
            0xFF039BE5, 0xFF8E24AA, 0xFFFF7043, 0xFF3949AB
        };
        private static final int COLOR_USER_BADGE   = 0xFF2E7D32;
        private static final int COLOR_SYSTEM_BADGE = 0xFF37474F;

        AppAdapter(Context context) { this.context = context; }

        void submitList(List<Map<String, String>> newList) {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return dataList.size(); }
                @Override public int getNewListSize() { return newList.size(); }
                @Override public boolean areItemsTheSame(int o, int n) {
                    return dataList.get(o).getOrDefault("packageName", "")
                            .equals(newList.get(n).getOrDefault("packageName", ""));
                }
                @Override public boolean areContentsTheSame(int o, int n) {
                    return dataList.get(o).equals(newList.get(n));
                }
            });
            dataList = new ArrayList<>(newList);
            diff.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_installed_app_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            Map<String, String> app = dataList.get(position);
            String name    = app.getOrDefault("name", "");
            String pkg     = app.getOrDefault("packageName", "");
            String version = app.getOrDefault("versionName", "");
            boolean system = "true".equals(app.get("isSystem"));

            // Avatar
            String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.getDefault());
            int avatarColor = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
            h.avatar.setText(letter);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(avatarColor);
            h.avatar.setBackground(circle);

            // Text
            h.appName.setText(name);
            h.packageName.setText(pkg);

            // Version row: "v1.2.3  ·  code 456"
            String versionCode = app.getOrDefault("versionCode", "");
            String versionStr = "";
            if (!version.isEmpty()) versionStr = "v" + version;
            if (!versionCode.isEmpty()) {
                versionStr = versionStr.isEmpty() ? "code " + versionCode
                        : versionStr + "  ·  code " + versionCode;
            }
            h.appVersion.setText(versionStr);
            h.appVersion.setVisibility(versionStr.isEmpty() ? View.GONE : View.VISIBLE);

            // Install date row
            String installStr = app.getOrDefault("firstInstallTime", "0");
            long installTs = 0;
            try { installTs = Long.parseLong(installStr); } catch (NumberFormatException ignored) {}
            if (installTs > 0) {
                h.appInstallDate.setText("Installed  " + dateFormat.format(new Date(installTs)));
                h.appInstallDate.setVisibility(View.VISIBLE);
            } else {
                h.appInstallDate.setVisibility(View.GONE);
            }

            // Update date row — hide when identical to install date
            String updateStr = app.getOrDefault("lastUpdateTime", "0");
            long updateTs = 0;
            try { updateTs = Long.parseLong(updateStr); } catch (NumberFormatException ignored) {}
            if (updateTs > 0 && updateTs != installTs) {
                h.appUpdateDate.setText("Updated  " + dateFormat.format(new Date(updateTs)));
                h.appUpdateDate.setVisibility(View.VISIBLE);
            } else {
                h.appUpdateDate.setVisibility(View.GONE);
            }

            // Badge
            h.typeBadge.setText(system ? "SYSTEM" : "USER");
            int badgeColor = system ? COLOR_SYSTEM_BADGE : COLOR_USER_BADGE;
            GradientDrawable badge = new GradientDrawable();
            badge.setShape(GradientDrawable.RECTANGLE);
            badge.setCornerRadius(dpToPx(10));
            badge.setColor(badgeColor);
            h.typeBadge.setBackground(badge);

            // Copy
            h.copyButton.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Package", pkg));
                Toast.makeText(context, "Copied: " + pkg, Toast.LENGTH_SHORT).show();
            });

            // Long-press copies name
            h.itemView.setOnLongClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("App", name + "\n" + pkg));
                Toast.makeText(context, "App info copied", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        private int dpToPx(int dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }

        @Override public int getItemCount() { return dataList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView avatar, appName, packageName, appVersion, appInstallDate, appUpdateDate, typeBadge;
            View     copyButton;

            ViewHolder(View v) {
                super(v);
                avatar        = v.findViewById(R.id.app_avatar);
                appName       = v.findViewById(R.id.app_name);
                packageName   = v.findViewById(R.id.app_package);
                appVersion    = v.findViewById(R.id.app_version);
                appInstallDate = v.findViewById(R.id.app_install_date);
                appUpdateDate  = v.findViewById(R.id.app_update_date);
                typeBadge     = v.findViewById(R.id.app_type_badge);
                copyButton    = v.findViewById(R.id.app_copy_button);
            }
        }
    }
}
