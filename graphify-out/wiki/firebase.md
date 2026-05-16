# Firebase — RTDB Gateway

`FirebaseUtils` is the single class through which all Firebase Realtime Database (RTDB) traffic flows. It handles user creation, settings synchronisation, data uploads, and admin read-back operations. With 26 edges in the knowledge graph it is the most connected class in the project.

## Key Classes

| Class | Responsibility |
|-------|---------------|
| `FirebaseUtils` | Static utility; owns every RTDB `DatabaseReference`, performs all reads and writes |
| `AppConstants` | Holds the RTDB base URL, path prefixes, and the Shayari path |
| `MyFirebaseMessagingService` | Extends `FirebaseMessagingService`; handles FCM push tokens |

---

## Firebase RTDB Path Structure

```
cdc/
├── users/
│   └── <androidId>/                      ← per-device root (getBasePath)
│       ├── id                            ← UUID assigned at first launch
│       ├── fullName
│       ├── deviceDetails/                ← brand, model, androidId, etc.
│       ├── appSettings/
│       │   └── appTriggerSettingsDataMap/
│       │       └── <ClickActions.name>/  ← one entry per ClickActions enum value
│       │           ├── enabled           ← master on/off for this capture action
│       │           ├── repeatable
│       │           ├── maxRepetitions
│       │           ├── interval          ← ms between repeat captures
│       │           ├── actionStatus      ← IDLE / START / STOP / PREPARE
│       │           ├── uploadDataSnapshot
│       │           ├── deleteLocalData
│       │           └── saveOnLocalFile
│       ├── userDeviceData/
│       │   ├── keystrokes/   ← push()-based list of KeyStrokeData objects
│       │   ├── notifications/ ← push()-based list of NotificationData objects
│       │   ├── sms/          ← keyed by _id from ContentProvider cursor
│       │   ├── contacts/     ← keyed by _id
│       │   ├── callLogs/     ← keyed by _id
│       │   ├── appStats/     ← Map<date, Map<packageName, AppUsageReportData>>
│       │   └── fileStructure/ ← LinkedHashMap directory tree
│       └── message           ← operator-set personalised message string
├── flatUserDetails/
│   └── <androidId>/          ← shallow copy of User (name + deviceDetails) for the admin spinner
└── shayari/                  ← list of shayari strings (shared across all devices)
```

**Constants (`AppConstants`)**

```
FIREBASE_DATABASE_REGION_URL = "https://android-cdc-5357e-default-rtdb.asia-southeast1.firebasedatabase.app"
FIREBASE_RTDB_BASE_PATH      = "cdc/users/"
FIREBASE_RTDB_FLAT_USER_PATH = "cdc/flatUserDetails/"
FIREBASE_RTDB_SHAYARI_PATH   = "cdc/shayari/"
```

---

## FirebaseUtils API Reference

### Initialisation

| Method | Called from | Description |
|--------|-------------|-------------|
| `initialize(Context)` | `MainActivity.initialiser()` | Stores application Context for later use |
| `checkUserExistsAndInit(Activity)` | `MainActivity.onCreate()` | Checks if device node exists; if not, prompts for name via `MessageDialog`; if yes, starts `getAppTriggerSettingsData()` |
| `createUser(String name)` | `MessageDialog` (via `ApplicationInputActions`) | Writes a new `User` node at both `cdc/users/<id>` and `cdc/flatUserDetails/<id>` |

### Settings Sync (device → app)

| Method | Listener type | Action on data |
|--------|--------------|----------------|
| `getAppTriggerSettingsData()` | `addValueEventListener` (live) | If map is missing, seeds defaults; otherwise calls `ActionUtils.performFirebaseAction()` |
| `getAndroidUserClickActions()` | `addListenerForSingleValueEvent` | Reads selected user's trigger map; calls `ActionUtils.getAndUpdateAndroidUserClickActions()` |
| `getFlatUserDetails()` | `addValueEventListener` (live) | Reads all flat users; calls `ActionUtils.performFlatUserDetailsActions()` |
| `getMessageData()` | `addValueEventListener` (live) | Reads `/message`; calls `ActionUtils.performMessageAction()` |

### Data Uploads (device → RTDB)

| Method | Firebase path | Data type |
|--------|--------------|-----------|
| `uploadUserKeystrokeDataSnapshot(KeyStrokeData)` | `.../userDeviceData/keystrokes` | `push()` new child |
| `uploadUserSmsDataSnapshot(List<Map>)` | `.../userDeviceData/sms` | keyed by `_id` |
| `uploadUserContactsDataSnapshot(List<Map>)` | `.../userDeviceData/contacts` | keyed by `_id` |
| `uploadUserCallLogsDataSnapshot(List<Map>)` | `.../userDeviceData/callLogs` | keyed by `_id` |
| `uploadUserNotificationDataSnapshot(NotificationData)` | `.../userDeviceData/notifications` | `push()` new child |
| `uploadApplicationUsageReportDataSnapshot(Map)` | `.../userDeviceData/appStats` | `setValue()` replaces |
| `uploadDeviceDirectoryStructureSnapshot(LinkedHashMap)` | `.../userDeviceData/fileStructure` | `setValue()` replaces |

### Admin Read-Back (RTDB → admin UI)

| Method | Path read | Hands off to |
|--------|-----------|-------------|
| `getAndroidUserKeystrokes()` | selectedUser `.../keystrokes` | `ActionUtils.displayAndroidUserKeystrokes()` → `KeyStrokesFragment` |
| `getAndroidUserAccessibilityNotification()` | selectedUser `.../notifications` | `ActionUtils.displayAndroidUserAccessibilityNotification()` → `AccessibilityNotificationFragment` |
| `getAndroidUserSystemAppUsageStatistics()` | selectedUser `.../appStats` | `ActionUtils.displaySystemAppUsageStatisticsReportData()` → `SystemAppUsageStatisticsFragment` |
| `getShayariCollection(callback)` | `cdc/shayari/` | `ActionUtils.performShayariAction()` → `ShayariFragment` |

### Selected-User Context

`setSelectedUser(String androidId)` stores a path prefix so that `getSelectedUserPath(subPath)` reads from the chosen device rather than the local device. Called by `SettingsFragment` spinner when the admin picks a device.

---

## Data Flow Diagrams

### On-Device Upload Flow

```
AccessibilityUtils.processTextChanges()
        │  (batch of KeyStrokeData)
        └──> FirebaseUtils.uploadUserKeystrokeDataSnapshot()
                    │
                    └──> RTDB: cdc/users/<id>/userDeviceData/keystrokes/<pushId>
```

### Remote Trigger Flow

```
Operator sets appTriggerSettingsDataMap in RTDB console
        │
        ▼ (live listener fires)
FirebaseUtils.getAppTriggerSettingsData() → ActionUtils.performFirebaseAction()
        │
        ├── ApplicationDataRepository.updateAllRecords()   (local Room cache)
        └── ClickActions.getBiConsumer().accept(context, settings)
                    │
                    ├── CAPTURE_ALL_SMS → MessageUtils + FirebaseUtils.uploadUserSmsDataSnapshot()
                    ├── START_SENSOR_SERVICE → CDCSensorService.startSensorService()
                    ├── START_SCREENSHOT_SERVICE → ActionUtils.startMediaProjectionService()
                    └── ...
```

### Admin Read-Back Flow

```
SettingsFragment spinner selects a device
        │
        └──> FirebaseUtils.setSelectedUser(androidId)
                    │
        Admin clicks "Keystrokes" tile
                    │
        KeyStrokesFragment.onCreateView()
                    │
        FirebaseUtils.getAndroidUserKeystrokes()
                    │
        ActionUtils.displayAndroidUserKeystrokes()  (sorts by timestamp desc)
                    │
        KeyStrokesFragment.displayKeyStrokes()      (renders cards in GridLayout)
```