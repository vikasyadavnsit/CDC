package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminScreenStateFragment extends Fragment {

    private static WeakReference<AdminScreenStateFragment> activeInstance;

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault());

    // UI
    private View loader;
    private RecyclerView recyclerView;
    private TextView emptyText, countBadge;
    private TextInputEditText searchInput;
    private Spinner dateSpinner;

    // Status card
    private TextView statusIcon, statusLabel, statusTime, statusBadge;

    // Today stats
    private TextView ssOnCount, ssOffCount, ssUnlockCount, ssTodayDate;

    private static Map<String, List<Map<String, Object>>> lastDataMap;
    private EventAdapter adapter;
    private List<Map<String, Object>> fullData = new ArrayList<>();
    private String selectedDate = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen_state, container, false);

        loader        = view.findViewById(R.id.ss_loader);
        recyclerView  = view.findViewById(R.id.ss_recycler);
        emptyText     = view.findViewById(R.id.ss_empty_text);
        countBadge    = view.findViewById(R.id.ss_count_badge);
        searchInput   = view.findViewById(R.id.ss_search_input);
        dateSpinner   = view.findViewById(R.id.ss_date_spinner);

        statusIcon    = view.findViewById(R.id.ss_status_icon);
        statusLabel   = view.findViewById(R.id.ss_status_label);
        statusTime    = view.findViewById(R.id.ss_status_time);
        statusBadge   = view.findViewById(R.id.ss_status_badge);

        ssOnCount     = view.findViewById(R.id.ss_on_count);
        ssOffCount    = view.findViewById(R.id.ss_off_count);
        ssUnlockCount = view.findViewById(R.id.ss_unlock_count);
        ssTodayDate   = view.findViewById(R.id.ss_today_date);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventAdapter(getContext());
        recyclerView.setAdapter(adapter);

        setupSearch();

        if (lastDataMap != null) {
            updateUI(lastDataMap);
        } else {
            loader.setVisibility(View.VISIBLE);
        }

        FirebaseUtils.getRemoteScreenState();
        return view;
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter(String query) {
        List<Map<String, Object>> filtered;
        if (query.isEmpty()) {
            filtered = fullData;
        } else {
            String lower = query.toLowerCase();
            filtered = fullData.stream()
                    .filter(m -> String.valueOf(m.getOrDefault("event", "")).toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }
        adapter.setData(filtered);
        countBadge.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        countBadge.setText(query.isEmpty()
                ? String.valueOf(fullData.size())
                : filtered.size() + " / " + fullData.size());
    }

    public static void displayScreenState(Activity activity, Object obj) {
        Map<String, List<Map<String, Object>>> dateWiseEvents = parseDateWiseEvents(obj);
        lastDataMap = dateWiseEvents;

        if (activeInstance != null && activeInstance.get() != null) {
            AdminScreenStateFragment fragment = activeInstance.get();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (fragment.getView() != null) {
                        fragment.updateUI(dateWiseEvents);
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<Map<String, Object>>> parseDateWiseEvents(Object obj) {
        Map<String, List<Map<String, Object>>> result = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        if (obj == null) return result;
        try {
            Gson gson = new Gson();
            String json = gson.toJson(obj);
            // The structure is /screenState/YYYY-MM-DD/pushKey/{eventData}
            Type type = new TypeToken<Map<String, Map<String, Map<String, Object>>>>() {}.getType();
            Map<String, Map<String, Map<String, Object>>> raw = gson.fromJson(json, type);
            
            if (raw != null) {
                for (Map.Entry<String, Map<String, Map<String, Object>>> dateEntry : raw.entrySet()) {
                    List<Map<String, Object>> events = new ArrayList<>(dateEntry.getValue().values());
                    events.sort((a, b) -> Double.compare(getDouble(b.get("timestamp")), getDouble(a.get("timestamp"))));
                    result.put(dateEntry.getKey(), events);
                }
            }
        } catch (Exception e) {
            // Fallback for legacy flat structure if any
            try {
                Type flatType = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
                Map<String, Map<String, Object>> rawFlat = new Gson().fromJson(new Gson().toJson(obj), flatType);
                if (rawFlat != null) {
                    for (Map<String, Object> ev : rawFlat.values()) {
                        String date = (String) ev.get("date");
                        if (date == null) date = "Unknown";
                        result.computeIfAbsent(date, k -> new ArrayList<>()).add(ev);
                    }
                    for (List<Map<String, Object>> list : result.values()) {
                        list.sort((a, b) -> Double.compare(getDouble(b.get("timestamp")), getDouble(a.get("timestamp"))));
                    }
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    private void updateUI(Map<String, List<Map<String, Object>>> dateWiseEvents) {
        loader.setVisibility(View.GONE);
        if (dateWiseEvents.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        emptyText.setVisibility(View.GONE);
        List<String> dates = new ArrayList<>(dateWiseEvents.keySet());
        
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, dates) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) ((TextView) v).setTextColor(requireContext().getColor(R.color.text_primary));
                return v;
            }
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(requireContext().getColor(R.color.surface_variant));
                if (v instanceof TextView) ((TextView) v).setTextColor(requireContext().getColor(R.color.text_primary));
                return v;
            }
        };
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(dateAdapter);

        // Default to today or most recent
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int defaultPos = dates.indexOf(today);
        dateSpinner.setSelection(defaultPos >= 0 ? defaultPos : 0);

        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDate = dates.get(position);
                fullData = dateWiseEvents.get(selectedDate);
                if (fullData == null) fullData = new ArrayList<>();
                
                refreshDisplay();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshDisplay() {
        if (fullData == null) return;
        
        updateStatusCard(fullData.isEmpty() ? null : fullData.get(0));
        updateStats(fullData);

        recyclerView.setVisibility(fullData.isEmpty() ? View.GONE : View.VISIBLE);
        String currentQuery = searchInput.getText() != null ? searchInput.getText().toString() : "";
        applyFilter(currentQuery);
    }

    private void updateStatusCard(@Nullable Map<String, Object> latest) {
        if (latest == null) {
            statusIcon.setText("❓");
            statusLabel.setText("No data");
            statusTime.setText("Waiting for first event…");
            statusBadge.setText("UNKNOWN");
            statusBadge.setBackground(null);
            return;
        }
        String event = String.valueOf(latest.getOrDefault("event", "UNKNOWN"));
        long ts = (long) getDouble(latest.get("timestamp"));

        String icon, label;
        int color;
        switch (event) {
            case "SCREEN_ON":  icon = "☀";  label = "Screen On";  color = 0xFF4CAF50; break;
            case "SCREEN_OFF": icon = "🌙"; label = "Screen Off"; color = 0xFFE53935; break;
            case "UNLOCKED":   icon = "🔓"; label = "Unlocked";   color = 0xFF1E88E5; break;
            default:           icon = "❓"; label = event;         color = 0xFF9E9E9E; break;
        }

        statusIcon.setText(icon);
        statusLabel.setText(label);
        statusTime.setText("Last seen  " + (ts > 0 ? dateTimeFormat.format(new Date(ts)) : "—"));
        statusBadge.setText(event);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(20f);
        bg.setColor(color);
        statusBadge.setBackground(bg);
    }

    private void updateStats(List<Map<String, Object>> events) {
        int onCount = 0, offCount = 0, unlockCount = 0;
        for (Map<String, Object> ev : events) {
            switch (String.valueOf(ev.getOrDefault("event", ""))) {
                case "SCREEN_ON":  onCount++;     break;
                case "SCREEN_OFF": offCount++;    break;
                case "UNLOCKED":   unlockCount++; break;
            }
        }
        ssTodayDate.setText(selectedDate != null ? "DATE  ·  " + selectedDate : "");
        ssOnCount.setText(String.valueOf(onCount));
        ssOffCount.setText(String.valueOf(offCount));
        ssUnlockCount.setText(String.valueOf(unlockCount));
    }

    private static long getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static double getDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activeInstance = null;
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    private static class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

        private List<Map<String, Object>> dataList = new ArrayList<>();
        private final Context context;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault());

        private static final int COLOR_ON      = 0xFF4CAF50;
        private static final int COLOR_OFF     = 0xFFE53935;
        private static final int COLOR_UNLOCK  = 0xFF1E88E5;
        private static final int COLOR_UNKNOWN = 0xFF9E9E9E;

        EventAdapter(Context context) { this.context = context; }

        void setData(List<Map<String, Object>> data) {
            dataList = new ArrayList<>(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_screen_state_event, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            Map<String, Object> ev = dataList.get(position);
            String event = String.valueOf(ev.getOrDefault("event", "UNKNOWN"));
            long ts = (long) getDouble(ev.get("timestamp"));
            int onCnt  = (int) getDouble(ev.get("screenOnCount"));
            int offCnt = (int) getDouble(ev.get("screenOffCount"));
            int ulCnt  = (int) getDouble(ev.get("unlockCount"));

            int color;
            switch (event) {
                case "SCREEN_ON":  color = COLOR_ON;      break;
                case "SCREEN_OFF": color = COLOR_OFF;     break;
                case "UNLOCKED":   color = COLOR_UNLOCK;  break;
                default:           color = COLOR_UNKNOWN; break;
            }

            h.badge.setText(event);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(10));
            bg.setColor(color);
            h.badge.setBackground(bg);

            h.time.setText(ts > 0 ? dateFormat.format(new Date(ts)) : "—");
            h.counts.setText("ON: " + onCnt + "  •  OFF: " + offCnt + "  •  UNLOCKS: " + ulCnt);
        }

        private static double getDouble(Object val) {
            if (val instanceof Number) return ((Number) val).doubleValue();
            try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
        }

        private int dpToPx(int dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }

        @Override public int getItemCount() { return dataList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView badge, time, counts;
            ViewHolder(View v) {
                super(v);
                badge  = v.findViewById(R.id.sse_badge);
                time   = v.findViewById(R.id.sse_time);
                counts = v.findViewById(R.id.sse_counts);
            }
        }
    }
}
