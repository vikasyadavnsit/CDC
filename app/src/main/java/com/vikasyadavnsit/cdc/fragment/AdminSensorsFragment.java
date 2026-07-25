package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminSensorsFragment extends Fragment {

    private static WeakReference<AdminSensorsFragment> activeInstance;

    private RecyclerView recyclerView;
    private TextView titleView;
    private TextView emptyText;
    private EditText searchInput;
    private View loader;
    private SensorDashboardAdapter adapter;
    private final Map<String, SensorItem> sensorHistoryMap = new HashMap<>();
    private List<SensorItem> currentDisplayList = new ArrayList<>();

    private final ValueEventListener sensorListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            Object value = snapshot.getValue();
            if (value instanceof List) {
                updateHistoryUI((List<Map<String, Object>>) value);
            } else if (value instanceof Map) {
                updateUI((Map<String, Object>) value);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            LoggerUtils.e("AdminSensors", "Sensor listener cancelled: " + error.getMessage());
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generic_list, container, false);
        recyclerView = view.findViewById(R.id.generic_list_rv);
        titleView = view.findViewById(R.id.generic_list_title);
        emptyText = view.findViewById(R.id.generic_list_empty_text);
        searchInput = view.findViewById(R.id.generic_list_search_input);
        loader = view.findViewById(R.id.generic_list_loader);

        view.findViewById(R.id.generic_list_back_button).setOnClickListener(v -> 
                getParentFragmentManager().popBackStack());
        view.findViewById(R.id.generic_list_back_button).setVisibility(View.VISIBLE);

        titleView.setText("📡 Live Sensor Intelligence");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SensorDashboardAdapter();
        recyclerView.setAdapter(adapter);

        View refreshBtn = view.findViewById(R.id.generic_list_filter_button);
        refreshBtn.setVisibility(View.VISIBLE);
        if (refreshBtn instanceof android.widget.ImageView) {
            ((android.widget.ImageView) refreshBtn).setImageResource(android.R.drawable.stat_notify_sync);
        }
        refreshBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Requesting fresh snapshot...", Toast.LENGTH_SHORT).show();
            loader.setVisibility(View.VISIBLE);
            FirebaseUtils.requestSensorUpdate();
        });

        setupSearch();
        loader.setVisibility(View.VISIBLE);
        FirebaseUtils.monitorRemoteSensorHistory(sensorListener);
        return view;
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        List<SensorItem> allItems = new ArrayList<>(sensorHistoryMap.values());
        allItems.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        
        if (query.isEmpty()) {
            currentDisplayList = allItems;
        } else {
            String lowerQuery = query.toLowerCase();
            currentDisplayList = allItems.stream()
                    .filter(item -> item.displayName.toLowerCase().contains(lowerQuery) || 
                                   item.category.toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
        }
        adapter.setData(currentDisplayList);
    }

    public static void displaySensors(Activity activity, Map<String, Object> data) {
        if (activeInstance != null && activeInstance.get() != null) {
            activeInstance.get().updateUI(data);
        }
    }

    private void updateUI(Map<String, Object> data) {
        if (getView() == null || data == null) return;
        loader.setVisibility(View.GONE);
        
        data.forEach((name, value) -> {
            if (name.toLowerCase().contains("uncalibrated")) return;
            
            String valStr = String.valueOf(value);
            SensorItem item = sensorHistoryMap.get(name);
            if (item != null) {
                item.update(valStr);
            } else {
                sensorHistoryMap.put(name, new SensorItem(name, valStr));
            }
        });

        if (sensorHistoryMap.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            filter(searchInput.getText().toString());
        }
    }

    private void updateHistoryUI(List<Map<String, Object>> history) {
        if (getView() == null || history == null) return;
        loader.setVisibility(View.GONE);
        
        sensorHistoryMap.clear();
        for (Map<String, Object> snapshot : history) {
            snapshot.forEach((name, value) -> {
                if (name.toLowerCase().contains("uncalibrated")) return;
                
                String valStr = String.valueOf(value);
                SensorItem item = sensorHistoryMap.get(name);
                if (item != null) {
                    item.update(valStr);
                } else {
                    sensorHistoryMap.put(name, new SensorItem(name, valStr));
                }
            });
        }

        if (sensorHistoryMap.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            filter(searchInput.getText().toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FirebaseUtils.removeSensorHistoryListener(sensorListener);
    }

    private static class SensorItem {
        String name, displayName, value, category, icon, description;
        float magnitude = 0f;
        LinkedList<Float> history = new LinkedList<>();
        private static final int MAX_HISTORY = 10;

        SensorItem(String name, String value) {
            this.name = name;
            determineStaticMetadata();
            update(value);
        }

        void update(String newValue) {
            this.value = newValue;
            parseValue();
            updateDescription();
            history.addLast(magnitude);
            if (history.size() > MAX_HISTORY) {
                history.removeFirst();
            }
        }

        private void determineStaticMetadata() {
            String n = name.toLowerCase();
            if (n.contains("accel")) { category = "Device Movement"; icon = "🏃"; displayName = "Accelerometer"; }
            else if (n.contains("gyro")) { category = "Rotation & Tilt"; icon = "🔄"; displayName = "Gyroscope"; }
            else if (n.contains("magne")) { category = "Compass & Magnetic"; icon = "🧭"; displayName = "Magnetometer"; }
            else if (n.contains("light")) { category = "Light Environment"; icon = "💡"; displayName = "Light Sensor"; }
            else if (n.contains("proxi")) { category = "Proximity Detector"; icon = "📏"; displayName = "Proximity Sensor"; }
            else if (n.contains("gravity")) { category = "Motion Physics"; icon = "🌍"; displayName = "Gravity Sensor"; }
            else if (n.contains("step")) { category = "Activity Tracker"; icon = "👟"; displayName = "Step Counter"; }
            else if (n.contains("battery")) { category = "Power System"; icon = "🔋"; displayName = "Battery Level"; }
            else if (n.contains("last update")) { category = "Last Sync"; icon = "🕒"; displayName = "Refresh Time"; }
            else { 
                category = "System Component"; 
                icon = "📡";
                // Prettify name
                displayName = name.replace("Sensor", "").replace("_", " ").trim();
                if (displayName.isEmpty()) displayName = name;
            }
        }

        private void parseValue() {
            try {
                if (value.startsWith("[") && value.endsWith("]")) {
                    String clean = value.substring(1, value.length() - 1);
                    String[] parts = clean.split(",");
                    float sumSq = 0;
                    for (String p : parts) {
                        float f = Float.parseFloat(p.trim());
                        sumSq += f * f;
                    }
                    magnitude = (float) Math.sqrt(sumSq);
                } else if (value.contains("%")) {
                    magnitude = Float.parseFloat(value.replace("%", "").trim());
                } else {
                    magnitude = Float.parseFloat(value);
                }
            } catch (Exception e) { magnitude = 0f; }
        }

        private void updateDescription() {
            String n = name.toLowerCase();
            if (n.contains("accel")) {
                if (magnitude < 1.0) description = "Device is laying perfectly still.";
                else if (magnitude < 11.0) description = "Resting on a surface.";
                else if (magnitude < 15.0) description = "Moderate movement detected.";
                else description = "Device is shaking or in fast motion!";
            } else if (n.contains("gyro")) {
                description = magnitude > 0.5 ? "Device is currently rotating." : "Orientation is stable.";
            } else if (n.contains("light")) {
                if (magnitude < 10) description = "Environment is very dark.";
                else if (magnitude < 100) description = "Indoor lighting environment.";
                else description = "Bright light or direct sunlight detected.";
            } else if (n.contains("proxi")) {
                description = magnitude == 0 ? "Something is covering the sensor." : "Front of device is clear.";
            } else if (n.contains("battery")) {
                description = "Current charge: " + value;
            } else if (n.contains("last update")) {
                description = "Data captured at " + value;
            } else {
                description = "Status: " + value;
            }
        }
    }

    private static class SensorDashboardAdapter extends RecyclerView.Adapter<SensorDashboardAdapter.ViewHolder> {
        private List<SensorItem> items = new ArrayList<>();

        public void setData(List<SensorItem> data) {
            this.items = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_dashboard_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SensorItem item = items.get(position);
            holder.icon.setText(item.icon);
            holder.name.setText(item.displayName);
            holder.category.setText(item.category);
            holder.description.setText(item.description);
            
            // Interaction: Expand on click
            holder.itemView.setOnClickListener(v -> {
                boolean isRawVisible = holder.rawValues.getVisibility() == View.VISIBLE;
                holder.rawValues.setVisibility(isRawVisible ? View.GONE : View.VISIBLE);
                holder.graphContainer.setVisibility(isRawVisible ? View.GONE : View.VISIBLE);
            });

            if (item.value.startsWith("[") && item.value.endsWith("]")) {
                holder.lastVal.setText(String.format(Locale.getDefault(), "%.2f", item.magnitude));
                holder.rawValues.setText("Raw Data: " + item.value);
                
                // Keep history graphs visible but compact by default or toggle
                holder.graphContainer.setVisibility(View.VISIBLE);
                renderSparkline(holder.graphContainer, item.history);
            } else {
                holder.lastVal.setText(item.value);
                holder.rawValues.setVisibility(View.GONE);
                holder.graphContainer.setVisibility(View.GONE);
            }
            
            // Variance bar visualization
            float progress = Math.min(1.0f, item.magnitude / 30.0f); // Calibrated range
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.varianceBar.getLayoutParams();
            lp.weight = progress;
            holder.varianceBar.setLayoutParams(lp);
        }

        private void renderSparkline(LinearLayout container, List<Float> history) {
            container.removeAllViews();
            if (history.isEmpty()) return;
            
            float max = 0.1f;
            for (float f : history) if (f > max) max = f;
            
            float density = container.getContext().getResources().getDisplayMetrics().density;
            int barWidth = (int) (8 * density); // Bigger bars
            int margin = (int) (1 * density);

            for (float val : history) {
                View bar = new View(container.getContext());
                float heightPercent = val / max;
                int height = (int) (34 * density * heightPercent);
                height = Math.max(height, (int)(3 * density));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(barWidth, height);
                lp.setMargins(margin, 0, margin, 0);
                bar.setLayoutParams(lp);
                
                // Color mapping for intensity
                if (heightPercent > 0.8) bar.setBackgroundColor(Color.parseColor("#FF5252")); // Red
                else if (heightPercent > 0.4) bar.setBackgroundColor(container.getContext().getColor(R.color.primary));
                else bar.setBackgroundColor(Color.parseColor("#66BB6A")); // Green

                container.addView(bar);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView icon, name, category, lastVal, rawValues, description;
            View varianceBar;
            LinearLayout graphContainer;
            ViewHolder(View v) {
                super(v);
                icon = v.findViewById(R.id.sensor_icon);
                name = v.findViewById(R.id.sensor_name);
                category = v.findViewById(R.id.sensor_category);
                lastVal = v.findViewById(R.id.sensor_last_val);
                rawValues = v.findViewById(R.id.sensor_raw_values);
                description = v.findViewById(R.id.sensor_description);
                varianceBar = v.findViewById(R.id.sensor_variance_bar);
                graphContainer = v.findViewById(R.id.sensor_graph_container);
            }
        }
    }
}
