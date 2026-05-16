package com.vikasyadavnsit.cdc.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vikasyadavnsit.cdc.R;

import java.util.List;

public class ShayariAdapter extends RecyclerView.Adapter<ShayariAdapter.ViewHolder> {

    private final List<String> shayaris;

    public ShayariAdapter(List<String> shayaris) {
        this.shayaris = shayaris;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shayari, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.index.setText(String.valueOf(position + 1));
        holder.text.setText(shayaris.get(position).replace("#", "\n"));
    }

    @Override
    public int getItemCount() {
        return shayaris.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView index;
        TextView text;

        ViewHolder(View view) {
            super(view);
            index = view.findViewById(R.id.item_shayari_index);
            text = view.findViewById(R.id.item_shayari_text);
        }
    }
}
