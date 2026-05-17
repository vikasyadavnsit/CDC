package com.vikasyadavnsit.cdc.data;

import com.vikasyadavnsit.cdc.enums.SpendingCategory;

public class SpendingEntry {
    public String id;
    public String sender;
    public String body;
    public double amount;
    public SpendingCategory category;
    public String type; // "DEBIT" or "CREDIT"
    public long date;

    public SpendingEntry() {}

    public SpendingEntry(String id, String sender, String body,
                         double amount, SpendingCategory category, String type, long date) {
        this.id = id;
        this.sender = sender;
        this.body = body;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.date = date;
    }
}
