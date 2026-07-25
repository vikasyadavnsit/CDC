package com.vikasyadavnsit.cdc.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminRemoteLogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private View loader;
    private ImageView backButton;
    private EditText searchInput;
    private Spinner dateSpinner;
    private ImageView deleteButton;
    private ChipGroup levelChipGroup;
    
    private LogAdapter adapter;
    private ValueEventListener logsListener;
    private List<Map<String, Object>> fullDataForSelectedDate = new ArrayList<>();
    private Map<String, List<Map<String, Object>>> dateGroupedData = new java.util.TreeMap<>(Collections.reverseOrder());
    private String selectedLevel = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_remote_logs, container, false);
        recyclerView = view.findViewById(R.id.logs_rv);
        emptyText = view.findViewById(R.id.logs_empty_text);
        loader = view.findViewById(R.id.logs_loader);
        backButton = view.findViewById(R.id.logs_back_button);
        searchInput = view.findViewById(R.id.logs_search_input);
        dateSpinner = view.findViewById(R.id.logs_date_spinner);
        deleteButton = view.findViewById(R.id.logs_delete_button);
        levelChipGroup = view.findViewById(R.id.logs_level_chips);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDate = (String) parent.getItemAtPosition(position);
                fullDataForSelectedDate = dateGroupedData.getOrDefault(selectedDate, new ArrayList<>());
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        deleteButton.setOnClickListener(v -> {
            String selectedDate = (String) dateSpinner.getSelectedItem();
            if (selectedDate != null) {
                showDeleteConfirmationDialog(selectedDate);
            }
        });

        setupSearch();
        setupLevelChips();
        fetchLogs();

        return view;
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupLevelChips() {
        levelChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                com.google.android.material.chip.Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedLevel = chip.getText().toString();
                    applyFilters();
                }
            }
        });
    }

    private void showDeleteConfirmationDialog(String date) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Logs")
                .setMessage("Are you sure you want to delete all logs for " + date + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteLogsForDate(date))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLogsForDate(String date) {
        if (loader != null) loader.setVisibility(View.VISIBLE);
        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/userDeviceData/remoteLogs/" + date))
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        android.widget.Toast.makeText(getContext(), "Logs deleted for " + date, android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(getContext(), "Failed to delete logs", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchLogs() {
        if (loader != null) loader.setVisibility(View.VISIBLE);
        logsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loader != null) loader.setVisibility(View.GONE);
                
                dateGroupedData.clear();
                List<String> dateList = new ArrayList<>();

                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String dateKey = dateSnapshot.getKey();
                    List<Map<String, Object>> logsForDate = new ArrayList<>();
                    
                    for (DataSnapshot logSnapshot : dateSnapshot.getChildren()) {
                        Map<String, Object> log = (Map<String, Object>) logSnapshot.getValue();
                        if (log != null) {
                            log.put("date_header", dateKey);
                            logsForDate.add(log);
                        }
                    }
                    
                    logsForDate.sort((a, b) -> {
                        long t1 = a.get("timestamp") instanceof Number ? ((Number) a.get("timestamp")).longValue() : 0;
                        long t2 = b.get("timestamp") instanceof Number ? ((Number) b.get("timestamp")).longValue() : 0;
                        return Long.compare(t2, t1);
                    });
                    
                    if (!logsForDate.isEmpty()) {
                        dateGroupedData.put(dateKey, logsForDate);
                        dateList.add(dateKey);
                    }
                }

                if (dateList.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    dateSpinner.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.GONE);
                    return;
                }

                dateList.sort(Collections.reverseOrder());

                if (getContext() != null) {
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), 
                            android.R.layout.simple_spinner_item, dateList);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    dateSpinner.setAdapter(spinnerAdapter);
                    dateSpinner.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loader != null) loader.setVisibility(View.GONE);
            }
        };
        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/userDeviceData/remoteLogs"))
                .addValueEventListener(logsListener);
    }

    private void applyFilters() {
        String query = searchInput.getText().toString().toLowerCase();
        
        List<Map<String, Object>> filtered = fullDataForSelectedDate.stream()
                .filter(log -> {
                    String level = log.get("level") != null ? log.get("level").toString() : "";
                    String tag = log.get("tag") != null ? log.get("tag").toString() : "";
                    String message = log.get("message") != null ? log.get("message").toString() : "";
                    
                    boolean matchesLevel = "ALL".equals(selectedLevel) || level.equalsIgnoreCase(selectedLevel);
                    boolean matchesQuery = query.isEmpty() || 
                            tag.toLowerCase().contains(query) ||
                            message.toLowerCase().contains(query);
                    
                    return matchesLevel && matchesQuery;
                })
                .collect(Collectors.toList());
        
        adapter.setData(filtered);
        
        if (adapter.getItemCount() == 0) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (logsListener != null) {
            FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/userDeviceData/remoteLogs"))
                    .removeEventListener(logsListener);
        }
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private List<Map<String, Object>> logs = new ArrayList<>();

        public void setData(List<Map<String, Object>> data) {
            this.logs = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_remote_log, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> log = logs.get(position);
            Object levelObj = log.get("level");
            String level = levelObj != null ? levelObj.toString() : "INFO";
            
            Object tagObj = log.get("tag");
            holder.tag.setText(tagObj != null ? tagObj.toString() : "System");
            
            Object msgObj = log.get("message");
            holder.message.setText(msgObj != null ? msgObj.toString() : "");
            
            Object timeObj = log.get("time");
            Object dateObj = log.get("date_header");
            String timeValue = timeObj != null ? timeObj.toString() : "";
            String dateHeader = dateObj != null ? dateObj.toString() : "";
            holder.time.setText(dateHeader + " " + timeValue);

            int color;
            String levelUpper = level.toUpperCase();
            switch (levelUpper) {
                case "ERROR": color = holder.itemView.getContext().getColor(R.color.spending_debit); break;
                case "WARN":  color = holder.itemView.getContext().getColor(R.color.text_secondary); break;
                case "INFO":  color = holder.itemView.getContext().getColor(R.color.primary); break;
                default:      color = holder.itemView.getContext().getColor(R.color.text_hint); break;
            }
            holder.indicator.setBackgroundColor(color);
            holder.level.setText(levelUpper);
            holder.level.setBackgroundTintList(ColorStateList.valueOf(color));
        }

        @Override
        public int getItemCount() { return logs.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView level, tag, message, time;
            View indicator;
            ViewHolder(View v) {
                super(v);
                level = v.findViewById(R.id.log_level);
                tag = v.findViewById(R.id.log_tag);
                message = v.findViewById(R.id.log_message);
                time = v.findViewById(R.id.log_time);
                indicator = v.findViewById(R.id.log_indicator);
            }
        }
    }
}
