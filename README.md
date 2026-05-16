# CDC — Comprehensive Device Capture

CDC is a **Java-based Android surveillance/monitoring app** that captures device data from multiple sources (keystrokes, notifications, app usage, SMS, calls, contacts, sensors, screenshots) and stores it locally (encrypted files, Room SQLite) and/or uploads it to **Firebase Realtime Database**. A gesture-gated admin viewer lets an operator select any enrolled device and browse its captured feeds.

For a comprehensive, code-derived list of user-facing features, see [`FEATURES.md`](FEATURES.md).

---

## Table of Contents

- [What the app does](#what-the-app-does)
- [Architecture overview](#architecture-overview)
- [Data flow](#data-flow)
- [Firebase RTDB structure](#firebase-rtdb-structure)
- [Local storage layout](#local-storage-layout)
- [Trigger system](#trigger-system)
- [Admin viewer](#admin-viewer)
- [Permissions & prerequisites](#permissions--prerequisites)
- [Dependencies](#dependencies)
- [Build & run](#build--run)
- [Project status](#project-status)
- [Potential future capabilities](#potential-future-capabilities)
- [Contributing](#contributing)
- [License](#license)

---

## What the app does

### Capture (device side)

| Source | Mechanism | Output |
|--------|-----------|--------|
| Keystrokes | `CDCAccessibilityService` text-change events | Batched every 15 s or 30 items |
| Notifications | `CDCAccessibilityService` notification events | Extras map + package + timestamp |
| App usage | `CDCAccessibilityService` window-state changes | Open/close/duration per app |
| SMS / call logs / contacts | `MessageUtils` via `ContentResolver` | On-demand snapshots |
| Call state | `CallUtils` `PhoneStateListener` | Incoming / outgoing / conference |
| Screenshots | `ScreenshotService` (MediaProjection) | PNG files |
| Sensor readings | `CDCSensorService` foreground service | Per-sensor `.txt` files |
| Push triggers | `MyFirebaseMessagingService` (FCM) | Enqueues WorkManager tasks |

### Viewer (admin side)

- **Home** — daily "shayari" text from RTDB (cached in SharedPreferences).
- **Message** — personalized per-device message from RTDB.
- **Settings** (gesture-gated) — admin panel; select target device; browse keystroke feed, notification feed, app usage report; fire remote triggers.

---

## Architecture overview

```
┌──────────────────────────────────────────────────────┐
│  Capture Sources                                     │
│  CDCAccessibilityService  ScreenshotService          │
│  CDCSensorService         CallUtils / MessageUtils   │
│  MyFirebaseMessagingService (FCM)                    │
└───────────────────┬──────────────────────────────────┘
                    │
         Processing & routing
         (AccessibilityUtils, FileUtils, CryptoUtils)
         • Check trigger enabled flag
         • Gate by file access permission
         • Optionally encrypt (AES-256-CBC)
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
  Local files    Room DB    Firebase RTDB
  (Documents/    (SQLite,   (userDeviceData/*)
   CDC/*.txt)     cdc.db)
```

**Key packages:**

| Package | Contents |
|---------|----------|
| `activities` | `MainActivity` (launcher, init, gesture unlock), `PasswordActivity` (unused) |
| `services` | Accessibility, screenshot, sensor, notification listener, VPN (stub), FCM, WorkManager |
| `utils` | FirebaseUtils, ActionUtils, AccessibilityUtils, FileUtils, CryptoUtils, MessageUtils, CallUtils, CommonUtil |
| `enums` | `ClickActions` (14 trigger types with BiConsumer handlers), `FileMap` (9 file definitions), `PermissionType` (23 permissions) |
| `database` | Room DB, DAOs, `ApplicationDataRepository` (trigger mirror), `DeviceDataRepository` (snapshots) |
| `fragment` | 9 fragments — home/message/settings + 4 admin viewers |
| `data` | POJOs + Room entities (`User`, `ApplicationData`, `DeviceData`, `KeyStrokeData`, etc.) |

**DI:** Hilt (`@AndroidEntryPoint`, `@Inject`, `MyModule`)

---

## Data flow

1. A capture source produces data (e.g., `CDCAccessibilityService` batches 30 keystrokes).
2. `AccessibilityUtils` / `FileUtils` checks the relevant `ClickActions` trigger setting from `ApplicationDataRepository`.
3. If `saveOnLocalFile` is set, `CDCOrganisedFileAppender` or `CDCUnorganisedFileAppender` writes to `Documents/CDC/`. Encrypted files use `CryptoUtils` (AES-256-CBC).
4. If `uploadDataSnapshot` is set, `FirebaseUtils.upload*Snapshot()` pushes to the device's RTDB path.
5. `ApplicationDataRepository` mirrors remote trigger settings locally (Room) so the app works partially offline.

---

## Firebase RTDB structure

**Region URL:** `https://android-cdc-5357e-default-rtdb.asia-southeast1.firebasedatabase.app`

```
cdc/
├── users/
│   └── <androidId>/
│       ├── appSettings/
│       │   └── appTriggerSettingsDataMap/    ← per-trigger remote switches
│       └── userDeviceData/
│           ├── keystrokes/
│           ├── notifications/
│           ├── contacts/
│           ├── sms/
│           ├── callLogs/
│           ├── appUsageReport/
│           └── directory/
└── flatUserDetails/
    └── <androidId>/                          ← admin device selector dropdown
```

**Operations:**
- `getAppTriggerSettingsData()` — `ValueEventListener` (streams updates live)
- `checkAndCreateUser()` — `ListenerForSingleValueEvent` (first launch)
- `upload*Snapshot()` — `push()` or `child().setValue()`
- `getFlatUserDetails()` — populates admin device dropdown

---

## Local storage layout

All files under `Documents/CDC/` on external storage:

| File | Organized | Dedup | Encrypted |
|------|-----------|-------|-----------|
| `sms.txt` | Yes | by `_id` | Yes |
| `call.txt` | Yes | by `_id` | Yes |
| `contacts.txt` | Yes | by `_id` | Yes |
| `keystroke.txt` | No (buffered) | No | Yes |
| `call_state.txt` | No | No | Yes |
| `notification.txt` | No | No | No |
| `application_usage.text` | No | No | No |
| `log.txt` | No | No | No |
| `temp.txt` | — | — | — |
| `Sensors/<SensorName>.txt` | No | No | No |
| `db/cdc.db` | — | — | Room SQLite |

**Encryption:** AES-256-CBC. Key and IV are hardcoded in `AppConstants`. Decryption is handled by `CDCFileReader` for admin export.

**Appenders:**
- `CDCOrganisedFileAppender` — static; writes contacts/SMS/calls with optional deduplication.
- `CDCUnorganisedFileAppender` — singleton; buffers writes and flushes in batches.

---

## Trigger system

Remote triggers are defined by the `ClickActions` enum (14 values). Each value carries a `BiConsumer<Context, AppTriggerSettingsData>` handler.

`AppTriggerSettingsData` fields per trigger:

| Field | Purpose |
|-------|---------|
| `enabled` | Master on/off switch |
| `repeatable` | Run in a loop |
| `maxRepetitions` | Loop cap |
| `interval` | Delay between repetitions (ms) |
| `saveOnLocalFile` | Write to `Documents/CDC/` |
| `uploadDataSnapshot` | Push to Firebase RTDB |
| `deleteLocalData` | Clear local file after upload |
| `actionStatus` | IDLE / PREPARE / START / STOP / RUNNING |

**Trigger examples:**

| ClickAction | Handler |
|-------------|---------|
| `REQUEST_ALL_PERMISSION` | `PermissionManager.requestAllPermissions()` |
| `START_SCREENSHOT_SERVICE` | Loop `ScreenshotService` with interval |
| `CAPTURE_KEY_STROKES` | Gated inside `AccessibilityUtils` |
| `CAPTURE_NOTIFICATIONS` | Gated inside `AccessibilityUtils` |
| `MONITOR_CALL_STATE` | `CallUtils.monitorCallState()` |

**Flow:** `FirebaseUtils` streams `appTriggerSettingsDataMap` → on change, `ActionUtils.performFirebaseAction()` iterates enabled triggers → dispatches to handler.

---

## Admin viewer

1. **Unlock:** 3 long-presses within 20 s on the Home button.
2. **Device select:** `SettingsFragment` loads `flatUserDetails` dropdown; selection stored in SharedPreferences (`ADMIN_SETTINGS_USER_ANDROID_ID`).
3. **Viewer fragments:**
   - `KeyStrokesFragment` — keystrokes filtered by app package.
   - `AccessibilityNotificationFragment` — notifications grouped by date.
   - `SystemAppUsageStatisticsFragment` — app usage report (open count, sessions, duration) by date.
   - `ClickActionsFragment` — view/set trigger settings for the target device.

---

## Permissions & prerequisites

| Permission / Setting | Required for |
|----------------------|-------------|
| Accessibility Service | Keystroke capture, notification extraction, app usage tracking |
| Usage Access | `UsageStatsManager` daily usage report |
| All files access (`MANAGE_EXTERNAL_STORAGE`) | Writing to `Documents/CDC/`, external Room DB |
| MediaProjection consent | Screenshot capture |
| `READ_CONTACTS`, `READ_SMS`, `READ_CALL_LOG`, `READ_PHONE_STATE` | Contact/SMS/call log snapshots, call state |
| `POST_NOTIFICATIONS` | Foreground service notifications |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | `CDCNotificationListenerService` |
| `BODY_SENSORS`, `HIGH_SAMPLING_RATE_SENSORS` | Sensor logging |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keep services alive |
| `RECEIVE_BOOT_COMPLETED` | Re-register receivers on reboot |

The app includes helper actions (`ActionUtils`) to open the relevant system settings panels for each special permission.

---

## Dependencies

| Library | Purpose |
|---------|---------|
| Firebase Database (BOM 33.1.2) | Realtime Database |
| Firebase Messaging (BOM 33.1.2) | FCM push notifications |
| Room 2.5.0 | Local SQLite database |
| Hilt 2.39.1 | Dependency injection |
| Gson | JSON serialization |
| WorkManager | Background task scheduling |
| Lombok | Boilerplate reduction |
| AndroidX (appcompat, material, constraintlayout) | UI components |

**Build config:** AGP 8.13.2 · Java 8 · minSdk 24 · targetSdk 34 · compileSdk 34

---

## Build & run

**Requirements:** Android Studio (Hedgehog or newer) or Gradle CLI. Real device recommended — most capture features do not work on emulators.

1. Clone the repo and open it as an Android project in Android Studio.
2. Add your `google-services.json` to `app/` (Firebase project: `android-cdc-5357e`).
3. Build and run the `app` module on a device running Android 7.0+ (API 24+).
4. On first launch, grant permissions via the Settings panel (unlock with 3 long-presses on the home button).

---

## Project status

Core features are working:
- User creation and trigger subscription (Firebase RTDB)
- Accessibility-based keystroke/notification/app-usage capture
- Local encrypted file writes (organised + unorganised)
- Room DB snapshot storage
- Screenshot service (MediaProjection)
- Sensor logging
- Call state monitoring
- Admin viewer (keystrokes, notifications, usage)

Incomplete / experimental:
- `CDCVpnService` — proof of concept, not wired up
- `PasswordActivity` — unused
- `PlayFragment`, `OfflineClickActionsFragment` — stubs
- Notification listener upload — logs locally only, not uploaded to RTDB
- Directory snapshot upload — partially implemented

This README describes **only what is implemented** in the current codebase. Update [`FEATURES.md`](FEATURES.md) when adding features, then keep this README aligned.

---

## Potential future capabilities

Ideas that fit the current architecture but are **not yet implemented**:

- Screen recording (MediaProjection video) and on-demand clip upload
- Live screen streaming / mirroring (WebRTC/RTMP)
- GPS / location history, geofencing, location-on-trigger
- Photo/video gallery snapshotting (MediaStore indexing + thumbnails)
- Microphone / audio capture and event-based recording
- Network activity insights (per-app traffic stats, DNS logging)
- Remote command execution with queuing, retries, idempotency, audit logs
- Offline-first sync (local queue + backoff + upload-on-Wi-Fi/charging policies)
- Admin dashboard improvements: search/filter/export, per-user timelines, bulk actions
- Security hardening: rotate encryption keys, device-bound key storage (Android Keystore), signed commands
- Cron-like scheduling for triggers and periodic snapshots
- Data minimization: redaction rules, sampling, per-app allow/deny lists

---

## Contributing

Not accepting external contributions at this time.

---

## License

No license file is currently provided. Treat this repository as **All Rights Reserved** unless a license is added.
