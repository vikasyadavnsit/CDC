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

public class AdminFileStructureFragment extends Fragment {

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
        
        titleView.setText("🗂 Remote Directory Structure");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GenericAdapter();
        recyclerView.setAdapter(adapter);

        FirebaseUtils.getRemoteFileStructure();
        return view;
    }

    public static void displayFileStructure(Activity activity, Map<String, Object> data) {
        if (recyclerView == null) return;
        if (data == null || data.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        
        List<String> flatList = new ArrayList<>();
        flatten(data, "", flatList);
        adapter.setData(flatList);
    }

    private static void flatten(Map<String, Object> map, String prefix, List<String> result) {
        map.forEach((k, v) -> {
            if (v instanceof Map) {
                result.add(prefix + "📁 " + k);
                flatten((Map<String, Object>) v, prefix + "  ", result);
            } else {
                result.add(prefix + "📄 " + k);
            }
        });
    }

    private static class GenericAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {
        private List<String> dataList = new ArrayList<>();

        public void setData(List<String> data) {
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
            String line = dataList.get(position);
            
            TextView tv = new TextView(holder.container.getContext());
            tv.setText(line);
            tv.setTextColor(holder.container.getContext().getColor(R.color.text_primary));
            tv.setTextSize(13f);
            tv.setTypeface(Typeface.MONOSPACE);
            holder.container.addView(tv);
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
