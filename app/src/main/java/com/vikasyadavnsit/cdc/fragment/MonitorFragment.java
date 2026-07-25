package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MonitorFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private TextView emptyText;
    private EditText searchEditText;
    private ImageView clearButton;
    
    private List<String> allLogs = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private boolean isSearching = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monitor, container, false);

        recyclerView = view.findViewById(R.id.monitor_log_rv);
        emptyText = view.findViewById(R.id.monitor_empty_text);
        searchEditText = view.findViewById(R.id.monitor_search_edit_text);
        clearButton = view.findViewById(R.id.monitor_clear_button);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        // layoutManager.setStackFromEnd(true); // Always show latest at bottom
        recyclerView.setLayoutManager(layoutManager);
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        clearButton.setOnClickListener(v -> {
            LoggerUtils.clearLogs();
            refreshLogs();
            Toast.makeText(getContext(), "Logs cleared", Toast.LENGTH_SHORT).show();
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                isSearching = !query.isEmpty();
                filterLogs(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        startAutoRefresh();

        return view;
    }

    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSearching) {
                    refreshLogs();
                }
                handler.postDelayed(this, 3000); // Refresh every 3 seconds
            }
        };
        handler.post(refreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
    }

    private void refreshLogs() {
        List<String> logs = LoggerUtils.getLogs();
        
        if (logs.size() != allLogs.size()) {
            allLogs = logs;
            if (allLogs.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setLogs(allLogs);
                recyclerView.scrollToPosition(allLogs.size() - 1);
            }
        }
    }

    private void filterLogs(String query) {
        if (query.isEmpty()) {
            adapter.setLogs(allLogs);
            if (!allLogs.isEmpty()) {
                recyclerView.scrollToPosition(allLogs.size() - 1);
            }
        } else {
            List<String> filtered = allLogs.stream()
                    .filter(log -> log.toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            adapter.setLogs(filtered);
        }
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private List<String> logs = new ArrayList<>();

        public void setLogs(List<String> logs) {
            this.logs = logs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_monitor_log, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            holder.logTextView.setText(logs.get(position));
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        static class LogViewHolder extends RecyclerView.ViewHolder {
            TextView logTextView;

            public LogViewHolder(@NonNull View itemView) {
                super(itemView);
                logTextView = itemView.findViewById(R.id.log_text);
            }
        }
    }
}
