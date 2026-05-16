# CDC Android Project — Wiki

CDC (Covert Data Capture) is an Android surveillance and data-collection app. It runs background services to capture keystrokes, notifications, SMS, contacts, call logs, screenshots, and sensor data, then uploads the collected data to Firebase Realtime Database. A built-in admin UI lets an operator view all captured data for any registered device remotely.

## Quick Navigation

| Page | What it covers |
|------|----------------|
| [firebase.md](firebase.md) | FirebaseUtils, RTDB paths, data-flow to/from Firebase |
| [ui.md](ui.md) | Activities, Fragments, bottom-nav structure |
| [capture.md](capture.md) | CDCSensorService, ScreenshotService, AccessibilityUtils, capture pipeline |
| [data-models.md](data-models.md) | User, AppSettings, AppTriggerSettingsData, UserDeviceData, data enums |
| [services.md](services.md) | Background services, BroadcastReceivers, VPN service |
| [file-io.md](file-io.md) | CDCFileReader, CDCOrganisedFileAppender, CDCUnorganisedFileAppender, CryptoUtils |

---

## Module Map

```
com.vikasyadavnsit.cdc
├── activities/
│   ├── MainActivity           — single-activity host, bottom navigation, Firebase init
│   └── PasswordActivity       — gate screen before MainActivity
├── fragment/
│   ├── DashboardFragment      — home tab (placeholder)
│   ├── ShayariFragment        — romantic shayari display (Firebase-backed)
│   ├── MessageFragment        — personalized message display (Firebase-backed)
│   ├── MonitorFragment        — live-monitoring view (placeholder)
│   ├── SettingsFragment       — admin hub: device selector + action tiles + viewer tiles
│   ├── ClickActionsFragment   — shows/edits remote trigger settings for a selected device
│   ├── KeyStrokesFragment     — displays captured keystrokes from Firebase
│   ├── AccessibilityNotificationFragment — displays captured notifications from Firebase
│   ├── SystemAppUsageStatisticsFragment  — displays app-usage report from Firebase
│   └── OfflineClickActionsFragment       — runs trigger actions locally (no remote user needed)
├── services/
│   ├── CDCAccessibilityService — AccessibilityService; keystroke + notification + app-usage tracking
│   ├── CDCSensorService        — foreground service; streams all device sensors to local files
│   ├── ScreenshotService       — MediaProjection-based screen capture service
│   ├── CDCVpnService           — VPN stub; captures raw IP packets (logs to logcat)
│   ├── AppUsageStats           — UsageStatsManager query; uploads daily app-usage report
│   ├── ResetService            — handles screen-on/off logging and daily usage reset
│   ├── CDCNotificationListenerService — NotificationListenerService (supplementary)
│   ├── CDCFileReader           — reads / decrypts local capture files
│   ├── CDCOrganisedFileAppender — deduplication-aware file writer for structured data
│   ├── CDCUnorganisedFileAppender — buffered file writer for streaming/sensor data
│   ├── AppContext              — Application-level context holder
│   ├── MyFirebaseMessagingService — FCM push token handler
│   ├── MyWorker / ScheduledWorker — WorkManager workers for periodic tasks
├── utils/
│   ├── FirebaseUtils           — all Firebase RTDB reads and writes (god node, 26 edges)
│   ├── ActionUtils             — dispatches Firebase-driven and UI-driven actions (18 edges)
│   ├── AccessibilityUtils      — batches keystroke events; processes notification events
│   ├── MessageUtils            — reads SMS, call logs, contacts via ContentResolver
│   ├── CryptoUtils             — AES-256/CBC encrypt/decrypt for file storage
│   ├── FileUtils               — high-level file append dispatcher (routes to Organised/Unorganised)
│   ├── CallUtils               — phone-state listener (IDLE/RINGING/OFFHOOK)
│   ├── CommonUtil              — shared helpers (fragment loading, Android ID, device details)
│   ├── SharedPreferenceUtils   — persists shayari index, message text, etc.
│   ├── LoggerUtils             — centralised logging wrapper
│   ├── SchedulerUtils          — AlarmManager scheduling helpers
│   ├── DirectoryMonitor        — file-system tree walker for directory-structure capture
│   ├── FileExplorer            — file browser utility
│   ├── DatabaseUtil            — Room DB helpers
│   └── ExecutorUtils           — shared ExecutorService pool
├── data/                       — POJOs / Lombok-generated data classes
├── enums/
│   ├── ClickActions            — 16 trigger actions, each carrying a BiConsumer handler
│   └── FileMap                 — maps logical data types to file paths and encryption flags
├── database/                   — Room database, DAOs, repositories
├── receiver/
│   ├── ResetBroadcastReceiver  — routes reset alarms to ResetService
│   ├── StatisticsBroadcastReceiver — listens for system events (screen, boot, power, BT…)
│   └── DeviceAdminReceiver     — device-admin policy receiver
├── permissions/
│   ├── PermissionHandler       — interface; implemented by PermissionManager
│   └── PermissionManager       — requests and resets all runtime permissions
├── module/
│   └── MyModule                — Hilt DI module; provides PermissionHandler + ActionUtils
└── constants/
    ├── AppConstants            — RTDB URLs, crypto keys, file buffer size, request codes
    └── DBConstants             — Room DB column/table name constants
```

## God Nodes (most connected classes)

| Class | Edges | Role |
|-------|-------|------|
| `FirebaseUtils` | 26 | Central RTDB gateway — every upload and every read goes through here |
| `ActionUtils` | 18 | Action dispatcher — bridges Firebase callbacks to UI and service calls |
| `CDCSensorService` | 15 | Foreground sensor capture — registers every non-uncalibrated sensor |
| `AccessibilityNotificationFragment` | 13 | Admin viewer for captured notifications |

## Key Data-Flow Summary

```
Firebase RTDB  ──trigger settings──>  FirebaseUtils.getAppTriggerSettingsData()
                                             │
                                      ActionUtils.performFirebaseAction()
                                             │
                              ClickActions BiConsumer (per enabled action)
                                    ┌────────┴────────┐
                            capture data         start service
                                    │
                         MessageUtils / AccessibilityUtils / AppUsageStats
                                    │
                          CDCOrganisedFileAppender   (structured: SMS, contacts, calls)
                          CDCUnorganisedFileAppender  (streaming: sensors, keystrokes)
                          FirebaseUtils.upload*()     (RTDB push)
```