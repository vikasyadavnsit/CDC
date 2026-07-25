package com.vikasyadavnsit.cdc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClickActionCategory {
    SMS("💬 SMS", "Manage SMS permissions and history"),
    CONTACTS("📇 Contacts", "Access and backup device contacts"),
    CALLS("📞 Calls", "Monitor call states and history"),
    LOCATION("📍 Location", "Live tracking and GPS permissions"),
    CAMERA("📷 Camera", "Remote photography and live feed"),
    MICROPHONE("🎤 Microphone", "Audio recording and mic permissions"),
    STORAGE("📂 Storage", "File system exploration and access"),
    SCREEN("🖥 Screen", "Screen monitoring and snapshots"),
    APPS("📱 Apps", "Application usage and inventory"),
    NOTIFICATIONS("🔔 Notifications", "Manage alert access and capture"),
    CONNECTIVITY("🌐 Connectivity", "VPN and network status monitoring"),
    SECURITY("🛡 Security", "Accessibility and special permissions"),
    SYSTEM("⚙ System", "Core maintenance and global controls"),
    DIAGNOSTICS("📊 Diagnostics", "Client logs and device metrics");

    private final String label;
    private final String description;
}
