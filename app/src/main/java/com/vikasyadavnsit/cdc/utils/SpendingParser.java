package com.vikasyadavnsit.cdc.utils;

import com.vikasyadavnsit.cdc.data.SpendingEntry;
import com.vikasyadavnsit.cdc.enums.SpendingCategory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpendingParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    public static SpendingEntry parse(String id, String sender, String body, long date) {
        if (body == null || sender == null) return null;

        // Skip personal phone numbers — bank/financial senders use alphanumeric IDs
        if (sender.matches("^\\+?[0-9\\s\\-]{7,15}$")) return null;

        double amount = extractAmount(body);
        if (amount <= 0) return null;

        return new SpendingEntry(id, sender, body, amount, SpendingCategory.UNCATEGORIZED, "UNKNOWN", date);
    }

    private static double extractAmount(String body) {
        Matcher m = AMOUNT_PATTERN.matcher(body);
        if (m.find()) {
            try {
                String g = m.group(1);
                if (g == null) return 0;
                return Double.parseDouble(g.replace(",", ""));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}
