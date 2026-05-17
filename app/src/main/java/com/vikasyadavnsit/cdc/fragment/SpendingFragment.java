package com.vikasyadavnsit.cdc.fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.SpendingEntry;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.SpendingCategory;
import com.vikasyadavnsit.cdc.utils.MessageUtils;
import com.vikasyadavnsit.cdc.utils.SpendingCategoryStore;
import com.vikasyadavnsit.cdc.utils.SpendingParser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SpendingFragment extends Fragment {

    private static final String[] PERIODS = {"This Month", "Last Month", "Last 3 Months", "This Year"};
    private static final NumberFormat NUM_FMT = NumberFormat.getInstance(new Locale("en", "IN"));
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMM", Locale.getDefault());

    private TextView totalAmountView, txnCountView, loadingView, summaryLabelView;
    private RadioButton filterAllBtn, filterDebitBtn, filterCreditBtn;
    private CategoryAdapter categoryAdapter;
    private TransactionAdapter transactionAdapter;

    private List<SpendingEntry> allEntries = new ArrayList<>();
    private Map<String, SpendingCategory> categoryMap = new HashMap<>();
    private Map<String, String> typeMap = new HashMap<>();
    private Set<String> deletedIds = new HashSet<>();

    private SpendingCategory selectedCategory = SpendingCategory.ALL;
    private String selectedType = null;
    private int selectedPeriod = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spending, container, false);

        view.findViewById(R.id.spending_back_button).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        summaryLabelView = view.findViewById(R.id.spending_summary_label);
        totalAmountView = view.findViewById(R.id.spending_total_amount);
        txnCountView = view.findViewById(R.id.spending_txn_count);
        loadingView = view.findViewById(R.id.spending_loading_text);

        filterAllBtn = view.findViewById(R.id.filter_type_all);
        filterDebitBtn = view.findViewById(R.id.filter_type_debit);
        filterCreditBtn = view.findViewById(R.id.filter_type_credit);
        updateFilterTabs();

        RadioGroup typeFilter = view.findViewById(R.id.spending_type_filter);
        typeFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_type_debit) selectedType = "DEBIT";
            else if (checkedId == R.id.filter_type_credit) selectedType = "CREDIT";
            else selectedType = null;
            updateFilterTabs();
            applyFilters();
        });

        categoryAdapter = new CategoryAdapter(cat -> {
            selectedCategory = cat;
            applyFilters();
        });
        RecyclerView categoryRv = view.findViewById(R.id.spending_category_rv);
        categoryRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryRv.setAdapter(categoryAdapter);

        transactionAdapter = new TransactionAdapter(this::showTagDialog);
        RecyclerView transactionRv = view.findViewById(R.id.spending_transaction_rv);
        transactionRv.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionRv.setAdapter(transactionAdapter);

        Spinner spinner = view.findViewById(R.id.spending_period_spinner);
        ArrayAdapter<String> spinnerAdapter = styledAdapter(PERIODS);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                selectedPeriod = pos;
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadSmsData();
        return view;
    }

    // ── Filter tab visuals ────────────────────────────────────────────────────

    private void updateFilterTabs() {
        Context ctx = requireContext();
        int primary = ctx.getColor(R.color.primary);
        int debitColor = ctx.getColor(R.color.spending_debit);
        int creditColor = ctx.getColor(R.color.spending_credit);

        setTabState(filterAllBtn, selectedType == null, primary);
        setTabState(filterDebitBtn, "DEBIT".equals(selectedType), debitColor);
        setTabState(filterCreditBtn, "CREDIT".equals(selectedType), creditColor);
    }

    private void setTabState(RadioButton btn, boolean selected, int color) {
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(10 * dp);
        if (selected) {
            bg.setColor(color);
        } else {
            bg.setColor(requireContext().getColor(R.color.app_bg));
            bg.setStroke(Math.round(dp), requireContext().getColor(R.color.shayari_card_border));
        }
        btn.setBackground(bg);
        btn.setTextColor(selected ? Color.WHITE : color);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadSmsData() {
        loadingView.setVisibility(View.VISIBLE);
        categoryMap = SpendingCategoryStore.loadAll(requireContext());
        typeMap = SpendingCategoryStore.loadTypes(requireContext());
        deletedIds = SpendingCategoryStore.loadDeleted(requireContext());

        new Thread(() -> {
            List<Map<String, String>> smsList = MessageUtils.getMessages(requireContext(), FileMap.SMS);
            List<SpendingEntry> parsed = new ArrayList<>();
            if (smsList != null) {
                for (Map<String, String> sms : smsList) {
                    if (!"1".equals(sms.get("type"))) continue;
                    String id = sms.get("_id");
                    String sender = sms.get("address");
                    String body = sms.get("body");
                    long date = 0;
                    try { date = Long.parseLong(sms.get("date")); } catch (Exception ignored) {}
                    SpendingEntry entry = SpendingParser.parse(id, sender, body, date);
                    if (entry != null) parsed.add(entry);
                }
            }
            allEntries = parsed;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                loadingView.setVisibility(View.GONE);
                applyFilters();
            });
        }).start();
    }

    private void applyFilters() {
        for (SpendingEntry e : allEntries) {
            SpendingCategory stored = categoryMap.get(e.id);
            if (stored != null) e.category = stored;
            String storedType = typeMap.get(e.id);
            if (storedType != null) e.type = storedType;
        }

        long[] range = getDateRange(selectedPeriod);

        List<SpendingEntry> periodEntries = allEntries.stream()
                .filter(e -> !deletedIds.contains(e.id))
                .filter(e -> e.date >= range[0] && e.date <= range[1])
                .collect(Collectors.toList());

        List<SpendingEntry> typeFiltered = periodEntries.stream()
                .filter(e -> selectedType == null || selectedType.equals(e.type))
                .collect(Collectors.toList());

        List<SpendingEntry> displayed = typeFiltered.stream()
                .filter(e -> selectedCategory == SpendingCategory.ALL || e.category == selectedCategory)
                .sorted((a, b) -> Long.compare(b.date, a.date))
                .collect(Collectors.toList());

        double total = displayed.stream().mapToDouble(e -> e.amount).sum();
        summaryLabelView.setText("DEBIT".equals(selectedType) ? "Total Debit"
                : "CREDIT".equals(selectedType) ? "Total Credit" : "Total");
        totalAmountView.setText("₹ " + NUM_FMT.format((long) total));
        txnCountView.setText(displayed.size() + " transactions");

        Map<SpendingCategory, Double> catTotals = new EnumMap<>(SpendingCategory.class);
        for (SpendingEntry e : typeFiltered) catTotals.merge(e.category, e.amount, Double::sum);
        categoryAdapter.update(catTotals, selectedCategory);
        transactionAdapter.setEntries(displayed);
    }

    // ── Tag dialog ────────────────────────────────────────────────────────────

    private void showTagDialog(SpendingEntry entry) {
        Context ctx = requireContext();
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_tag_spending, null);

        ((TextView) dialogView.findViewById(R.id.dialog_sms_sender))
                .setText(entry.sender != null ? entry.sender : "Unknown");
        ((TextView) dialogView.findViewById(R.id.dialog_sms_body))
                .setText(entry.body);

        // Type spinner
        String[] typeItems = {"DEBIT", "CREDIT", "UNKNOWN"};
        Spinner typeSpinner = dialogView.findViewById(R.id.dialog_type_spinner);
        ArrayAdapter<String> typeAdapter = styledAdapter(typeItems);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        String currentType = typeMap.getOrDefault(entry.id, entry.type != null ? entry.type : "UNKNOWN");
        for (int i = 0; i < typeItems.length; i++) {
            if (typeItems[i].equals(currentType)) { typeSpinner.setSelection(i); break; }
        }

        // Category spinner
        SpendingCategory[] catOptions = Arrays.stream(SpendingCategory.values())
                .filter(c -> c != SpendingCategory.ALL)
                .toArray(SpendingCategory[]::new);
        String[] catLabels = Arrays.stream(catOptions)
                .map(c -> c.emoji + "  " + c.label)
                .toArray(String[]::new);
        Spinner catSpinner = dialogView.findViewById(R.id.dialog_category_spinner);
        ArrayAdapter<String> catAdapter = styledAdapter(catLabels);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        catSpinner.setAdapter(catAdapter);
        for (int i = 0; i < catOptions.length; i++) {
            if (catOptions[i] == entry.category) { catSpinner.setSelection(i); break; }
        }

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String type = typeItems[typeSpinner.getSelectedItemPosition()];
                    SpendingCategory cat = catOptions[catSpinner.getSelectedItemPosition()];
                    categoryMap.put(entry.id, cat);
                    typeMap.put(entry.id, type);
                    SpendingCategoryStore.assign(ctx, entry, cat, type);
                    applyFilters();
                })
                .setNeutralButton("Delete", (d, w) -> {
                    deletedIds.add(entry.id);
                    SpendingCategoryStore.deleteEntry(ctx, entry.id);
                    applyFilters();
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        // Transparent window so dialog_bg rounded corners are visible
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ctx.getColor(R.color.primary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ctx.getColor(R.color.text_hint));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ctx.getColor(R.color.spending_debit));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ArrayAdapter<String> styledAdapter(String[] items) {
        return new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
            @NonNull
            @Override
            public View getView(int pos, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                if (v instanceof TextView)
                    ((TextView) v).setTextColor(getContext().getColor(R.color.text_secondary));
                return v;
            }
            @NonNull
            @Override
            public View getDropDownView(int pos, @Nullable View cv, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(pos, cv, parent);
                if (v instanceof TextView)
                    ((TextView) v).setTextColor(getContext().getColor(R.color.text_secondary));
                return v;
            }
        };
    }

    private long[] getDateRange(int period) {
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        switch (period) {
            case 0: cal.set(Calendar.DAY_OF_MONTH, 1); return new long[]{cal.getTimeInMillis(), end};
            case 1: {
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.add(Calendar.MONTH, -1);
                long s = cal.getTimeInMillis(); cal.add(Calendar.MONTH, 1); cal.add(Calendar.MILLISECOND, -1);
                return new long[]{s, cal.getTimeInMillis()};
            }
            case 2: cal.set(Calendar.DAY_OF_MONTH, 1); cal.add(Calendar.MONTH, -2); return new long[]{cal.getTimeInMillis(), end};
            case 3: cal.set(Calendar.DAY_OF_YEAR, 1); return new long[]{cal.getTimeInMillis(), end};
            default: return new long[]{0, end};
        }
    }

    // ── Category adapter ──────────────────────────────────────────────────────

    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
        private final SpendingCategory[] cats = SpendingCategory.values();
        private Map<SpendingCategory, Double> totals = new EnumMap<>(SpendingCategory.class);
        private SpendingCategory selected = SpendingCategory.ALL;
        private final OnClick listener;
        interface OnClick { void onClick(SpendingCategory cat); }
        CategoryAdapter(OnClick listener) { this.listener = listener; }

        void update(Map<SpendingCategory, Double> totals, SpendingCategory selected) {
            this.totals = totals; this.selected = selected; notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_spending_category, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            SpendingCategory cat = cats[pos];
            h.emoji.setText(cat.emoji);
            h.label.setText(cat.label);
            double amt = cat == SpendingCategory.ALL
                    ? totals.values().stream().mapToDouble(Double::doubleValue).sum()
                    : totals.getOrDefault(cat, 0.0);
            h.amount.setText(amt > 0 ? "₹" + NUM_FMT.format((long) amt) : "—");
            h.card.setCardBackgroundColor(h.card.getContext().getColor(
                    cat == selected ? R.color.primary_container : R.color.surface_variant));
            h.card.setOnClickListener(v -> listener.onClick(cat));
        }

        @Override public int getItemCount() { return cats.length; }

        static class VH extends RecyclerView.ViewHolder {
            CardView card; TextView emoji, label, amount;
            VH(View v) {
                super(v); card = (CardView) v;
                emoji = v.findViewById(R.id.spending_cat_emoji);
                label = v.findViewById(R.id.spending_cat_label);
                amount = v.findViewById(R.id.spending_cat_amount);
            }
        }
    }

    // ── Transaction adapter ───────────────────────────────────────────────────

    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
        private List<SpendingEntry> entries = new ArrayList<>();
        private final OnTap onTap;
        interface OnTap { void onTap(SpendingEntry entry); }
        TransactionAdapter(OnTap onTap) { this.onTap = onTap; }

        void setEntries(List<SpendingEntry> entries) { this.entries = entries; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_spending_transaction, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            SpendingEntry e = entries.get(pos);
            h.emoji.setText(e.category.emoji);
            h.sender.setText(e.sender != null ? e.sender : "Unknown");
            h.body.setText(e.body);
            h.amount.setText("₹ " + NUM_FMT.format((long) e.amount));
            h.date.setText(DATE_FMT.format(new Date(e.date)));
            if ("DEBIT".equals(e.type)) {
                h.typeBadge.setText("DR");
                h.typeBadge.setTextColor(h.typeBadge.getContext().getColor(R.color.spending_debit));
                h.amount.setTextColor(h.amount.getContext().getColor(R.color.spending_debit));
            } else if ("CREDIT".equals(e.type)) {
                h.typeBadge.setText("CR");
                h.typeBadge.setTextColor(h.typeBadge.getContext().getColor(R.color.spending_credit));
                h.amount.setTextColor(h.amount.getContext().getColor(R.color.spending_credit));
            } else {
                h.typeBadge.setText("?");
                h.typeBadge.setTextColor(h.typeBadge.getContext().getColor(R.color.text_hint));
                h.amount.setTextColor(h.amount.getContext().getColor(R.color.text_hint));
            }
            h.itemView.setOnClickListener(v -> onTap.onTap(e));
        }

        @Override public int getItemCount() { return entries.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView emoji, sender, body, amount, date, typeBadge;
            VH(View v) {
                super(v);
                emoji = v.findViewById(R.id.txn_category_emoji);
                sender = v.findViewById(R.id.txn_sender);
                body = v.findViewById(R.id.txn_body);
                amount = v.findViewById(R.id.txn_amount);
                date = v.findViewById(R.id.txn_date);
                typeBadge = v.findViewById(R.id.txn_type_badge);
            }
        }
    }
}
