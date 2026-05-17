package com.vikasyadavnsit.cdc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClickActionCategory {
    PERMISSIONS("🔐 Permissions", "Request various system and special access permissions"),
    SERVICES("📡 Services", "Manage background monitoring and capture services"),
    DATA_CAPTURE("📊 Data Capture", "Initiate snapshots of device data (SMS, Contacts, etc.)"),
    SYSTEM("⚙ System", "General system utilities and maintenance tasks");

    private final String label;
    private final String description;
}
