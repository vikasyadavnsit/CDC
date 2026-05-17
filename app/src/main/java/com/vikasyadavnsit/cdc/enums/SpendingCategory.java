package com.vikasyadavnsit.cdc.enums;

public enum SpendingCategory {
    ALL("All", "☰"),
    UNCATEGORIZED("Untagged", "❓"),
    FOOD("Food", "🍔"),
    GROCERY("Grocery", "🛒"),
    SHOPPING("Shopping", "🛍"),
    FUEL("Fuel", "⛽"),
    TRANSPORT("Transport", "🚗"),
    ENTERTAINMENT("Entertain", "🎬"),
    UTILITIES("Utilities", "💡"),
    HEALTH("Health", "💊"),
    OTHERS("Others", "📦");

    public final String label;
    public final String emoji;

    SpendingCategory(String label, String emoji) {
        this.label = label;
        this.emoji = emoji;
    }
}
