package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
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

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCallLogsFragment extends Fragment {

    private static RecyclerView recyclerView;
    private static TextView titleView;
    private static TextView emptyText;
    private static GenericAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generic_list, container, false);
        recyclerView = view.findViewById(R.id.generic_list_rv);
        titleView = view.findViewById(R.id.generic_list_title);
        emptyText = view.findViewById(R.id.generic_list_empty_text);
        
        titleView.setText("📞 Remote Call Logs");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GenericAdapter();
        recyclerView.setAdapter(adapter);

        FirebaseUtils.getRemoteCallLogs();
        return view;
    }

    public static void displayCallLogs(Activity activity, Map<String, Map<String, String>> callData) {
        if (recyclerView == null) return;
        if (callData == null || callData.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setData(new ArrayList<>(callData.values()));
    }

    private static class GenericAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {
        private List<Map<String, String>> dataList = new ArrayList<>();

        public void setData(List<Map<String, String>> data) {
            this.dataList = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_generic_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.container.removeAllViews();
            Map<String, String> data = dataList.get(position);
            
            addDetail(holder.container, "Number", data.get("number"), true);
            addDetail(holder.container, "Name", data.get("name"), false);
            addDetail(holder.container, "Type", data.get("type"), false);
            addDetail(holder.container, "Duration", data.get("duration") + "s", false);
            addDetail(holder.container, "Date", data.get("date"), false);
        }

        private void addDetail(LinearLayout container, String label, String value, boolean bold) {
            TextView tv = new TextView(container.getContext());
            tv.setText(label + ": " + (value != null ? value : "N/A"));
            tv.setTextColor(container.getContext().getColor(R.color.text_primary));
            tv.setTextSize(13f);
            if (bold) tv.setTypeface(null, Typeface.BOLD);
            container.addView(tv);
        }

        @Override
        public int getItemCount() { return dataList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout container;
            ViewHolder(View itemView) {
                super(itemView);
                container = itemView.findViewById(R.id.card_content_container);
            }
        }
    }
}
