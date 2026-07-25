package com.vikasyadavnsit.cdc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TriggerType {
    PERMISSION("🔐 Permission"),
    DATA_CAPTURE("📊 Data Capture"),
    SERVICE("📡 Service"),
    SYSTEM("⚙ System"),
    DIAGNOSTICS("📈 Diagnostics");

    private final String label;
}
