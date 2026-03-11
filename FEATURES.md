# FEATURES

This document lists **user-facing features** implemented in the app, the **concrete technical components** that implement them (Activities/Fragments/Services/Utils/Repositories), and a brief description of the **business logic/data flow**.

## App overview (what a user experiences)

- The app launches into a simple UI with navigation for **Home**, **Message**, and a **hidden Settings** tab.
- On first launch, it prompts for a **user name** and creates/updates a device-scoped user record in **Firebase Realtime Database (RTDB)**.
- The device can be configured (locally and via RTDB) to **capture device data** (keystrokes, notifications, app usage, contacts/SMS/call logs, sensors, screenshots, directory structure) and optionally:
  - **save locally** (files under Documents/CDC + Room DB snapshots), and/or
  - **upload snapshots** to Firebase RTDB.
- The Settings UI also acts as an **admin viewer**: pick a target user (by Android ID) and view that user’s captured **keystrokes**, **notifications**, and **usage stats report**, plus inspect the remote “click action” trigger settings.

## Business logic & data flow (high level)

### Identity + multi-user “admin” model

- **Device identity**: the device is keyed by `Settings.Secure.ANDROID_ID` (`CommonUtil.getAndroidID`).
- **Device self-path** in RTDB: `cdc/users/<androidId>/...` (`AppConstants.FIREBASE_RTDB_BASE_PATH`, `FirebaseUtils.getBasePath`).
- **Admin selection**: Settings downloads a flat index of users (`cdc/flatUserDetails/*`) and stores the selected user Android ID in SharedPreferences (`SharedPreferenceUtils.ADMIN_SETTINGS_USER_ANDROID_ID`). Subsequent admin-view fetches use that selected Android ID.

### Trigger settings (remote-controlled capture switches)

- The device maintains an `appSettings/appTriggerSettingsDataMap` in RTDB (one entry per `ClickActions` enum value).
- On startup, the app subscribes to changes (`FirebaseUtils.getAppTriggerSettingsData`) and runs `ActionUtils.performFirebaseAction`:
  - Optionally mirrors the map into local Room (`ApplicationDataRepository.updateAllRecords`) if **file access** exists.
  - For each enabled trigger setting, invokes the corresponding action handler (`ClickActions.<X>.getBiConsumer().accept(...)`).
- Many capture pipelines gate collection using the local `ApplicationDataRepository` copy (e.g., keystrokes/notifications/app usage), which requires **external storage “file access”** (`CommonUtil.hasFileAccess`) to read DB from the external path.

### Where captured data goes

- **Firebase RTDB uploads**: implemented in `FirebaseUtils.upload*` methods (keystrokes/notifications/app usage/app usage report/contacts/SMS/call logs/directory structure).
- **Local Room snapshots**: `DeviceDataRepository.insert(DeviceData(...))` is used by several collectors (keystrokes/notifications/app usage/app usage report/directory structure).
- **Local files** (Documents/CDC):
  - `FileUtils.appendDataToFile(FileMap, data)` routes to:
    - `CDCOrganisedFileAppender` for “organized” records (e.g., contacts/SMS/calls) with optional de-dup, and
    - `CDCUnorganisedFileAppender` for buffered “unorganized” log-like files.
  - **Encryption**: `CryptoUtils.getEncryptedData` AES-encrypts entries for `FileMap`s with `encrypted=true` (e.g., SMS/CALL/KEYSTROKE/CONTACTS/CALL_STATE).

## Feature catalog

### Launch & initialization

- **App launch**
  - **User-facing behavior**: opens the main UI and begins syncing triggers and data.
  - **Primary components**: `MainActivity` (launcher Activity), `MyApplication` (global init).
  - **Supporting components**: `AppContext.init`, `FirebaseApp.initializeApp`, `FirebaseUtils.initialize`, `ApplicationDataRepository.initialize`, `DeviceDataRepository.initialize`.

- **First-launch user creation**
  - **User-facing behavior**: prompts “Enter your name” on first launch.
  - **Primary components**: `MainActivity.launcher`, `MessageDialog.multilineInputDialog`.
  - **Business logic**: `FirebaseUtils.checkAndCreateUser(userName)` writes user/device details to:
    - `cdc/users/<androidId>/...`
    - `cdc/flatUserDetails/<androidId>`
  - **Local persistence**: `SharedPreferenceUtils.setFirstLaunchCompleted` (used elsewhere; first-launch check is `SharedPreferenceUtils.isFirstLaunch`).

### Core navigation UI

- **Home screen: Shayari of the day**
  - **User-facing behavior**: shows a daily “shayari” text.
  - **Primary components**: `HomeFragment`, `CommonUtil.setShayari`.
  - **Business logic**:
    - Reads last saved value from `SharedPreferenceUtils.getShayariData`.
    - If date changed, fetches from RTDB (`FirebaseUtils.getShayariData(index)`), then updates UI via `ActionUtils.performShayariAction` → `HomeFragment.updateShayariText`.
    - Updates SharedPreferences with new index/date/message (`SharedPreferenceUtils.updateShayariData`).

- **Message screen: personalized message**
  - **User-facing behavior**: shows a message (remote-configurable).
  - **Primary components**: `MessageFragment`, `FirebaseUtils.getMessageData`, `ActionUtils.performMessageAction`.
  - **Business logic**: loads from RTDB path `/personalizedMessage` (device-scoped), falls back to `SharedPreferenceUtils.getMessageText` if null, then updates UI and stores to prefs.

- **Hidden Settings tab (gesture-gated)**
  - **User-facing behavior**: Settings button is hidden; it becomes visible after a special gesture.
  - **Primary components**: `ActionUtils.handleButtonPress`.
  - **Business logic**: long-press on Home button increments a counter; at 3 long presses within a timer window it reveals the Settings button.

### Settings / “admin” functions (select a user then view their data)

- **Select target user (admin dropdown)**
  - **User-facing behavior**: shows a dropdown of known users; selecting one displays device details.
  - **Primary components**: `SettingsFragment.populateUserDropdown`.
  - **Data source**: `FirebaseUtils.getFlatUserDetails` → `ActionUtils.performFlatUserDetailsActions`.
  - **Business logic**: stores selected `androidId` in prefs (`SharedPreferenceUtils.updateAdminSettingsUserAndroidId`) for subsequent admin queries.

- **Open admin action panels**
  - **User-facing behavior**: Settings shows grouped buttons that open other screens (keystrokes, usage stats report, notifications, click-action permissions).
  - **Primary components**: `SettingsFragment` + `AdminSettingsFragmentActions` (enum).
  - **Business logic**: buttons require a selected user; otherwise toast “Please select a user first.”

- **View target user “click action” trigger settings**
  - **User-facing behavior**: shows the remote `AppTriggerSettingsData` values for the selected user.
  - **Primary components**: `ClickActionsFragment`.
  - **Data flow**: `FirebaseUtils.getAndroidUserClickActions` → `ActionUtils.getAndUpdateAndroidUserClickActions` → `ClickActionsFragment.addDynamicButtons`.

- **View target user keystrokes**
  - **User-facing behavior**: filter keystrokes by app package or show all; grouped by date/time.
  - **Primary components**: `KeyStrokesFragment`.
  - **Data flow**: `FirebaseUtils.getAndroidUserKeystrokes` → `ActionUtils.displayAndroidUserKeystrokes` → `KeyStrokesFragment.displayKeyStrokes`.

- **View target user captured notifications**
  - **User-facing behavior**: filter notifications by package or show all; grouped by date/time; shows extras.
  - **Primary components**: `AccessibilityNotificationFragment`.
  - **Data flow**: `FirebaseUtils.getAndroidUserAccessibilityNotification` → `ActionUtils.displayAndroidUserAccessibilityNotification` → `AccessibilityNotificationFragment.displayNotifications`.

- **View target user system app usage report**
  - **User-facing behavior**: displays a usage summary (open count, sessions, totals) for a selected date.
  - **Primary components**: `SystemAppUsageStatisticsFragment`.
  - **Data flow**:
    - Requests the device to generate a report by pushing `GET_APP_USAGE_STATISTICS_REPORT` enabled via `FirebaseUtils.getAndroidUserSystemAppUsageStatistics`.
    - After push, fetches `/userDeviceData/appUsageReport/<date>` and renders it via `ActionUtils.displaySystemAppUsageStatisticsReportData`.

### Device-side data capture (triggered by `ClickActions` + system services)

#### Permissions & prerequisites

- **Request all runtime permissions**
  - **User-facing behavior**: walks through (or bulk-requests) required permissions.
  - **Primary components**: `ClickActions.REQUEST_ALL_PERMISSION`, `PermissionManager.requestAllPermissions`.

- **Manual permission reset**
  - **User-facing behavior**: opens app settings so the user can remove permissions.
  - **Primary components**: `ClickActions.RESET_ALL_PERMISSION`, `PermissionManager.resetAllPermissionManually`.

- **Accessibility permission (for keystrokes/app usage/notification events)**
  - **User-facing behavior**: opens Accessibility settings to enable the app’s service.
  - **Primary components**: `ClickActions.REQUEST_ACCESSIBILITY_PERMISSION`, `AccessibilityUtils.startAccessibilitySettingIntent`, `CDCAccessibilityService`.

- **All-files access (for local file writes + external-path Room DB)**
  - **User-facing behavior**: opens “All files access” settings on Android 11+.
  - **Primary components**: `ClickActions.REQUEST_FILE_ACCESS_PERMISSION`, `FileUtils.startFileAccessSettings`, `CommonUtil.hasFileAccess`.

- **Usage stats access (for daily report)**
  - **User-facing behavior**: opens Usage Access settings.
  - **Primary components**: `ClickActions.GET_APP_USAGE_STATISTICS_REPORT`, `ActionUtils.enableAppUsageStats`, `AppUsageStats.hasUsageStatsPermission`.

- **Exact alarm permission (for scheduled reset)**
  - **User-facing behavior**: prompts to allow exact alarms (Android 12+).
  - **Primary components**: `ClickActions.REQUEST_EXACT_ALARM_PERMISSION`, `ActionUtils.requestExactAlarmPermission`, `CommonUtil.scheduleDailyReset`.

#### Cloud messaging (Firebase Cloud Messaging)

- **Receive push notifications (FCM)**
  - **User-facing behavior**: shows a system notification that opens the app.
  - **Primary components**: `MyFirebaseMessagingService`.
  - **Business logic**:
    - Logs message payloads via `LoggerUtils`.
    - Enqueues a background task via `WorkManager` (`OneTimeWorkRequest` → `MyWorker`).
    - Displays a notification via `NotificationManager` with a `PendingIntent` to `MainActivity`.

#### Keystrokes (Accessibility text-change batching)

- **Capture keystrokes**
  - **User-facing behavior**: when enabled, records typed text (as observed by accessibility events).
  - **Primary components**: `CDCAccessibilityService` → `AccessibilityUtils.processTextChangedEvent`.
  - **Business logic**:
    - Batches events (15s interval / size threshold).
    - Reads local trigger setting `CAPTURE_KEY_STROKES` from `ApplicationDataRepository`.
    - If enabled, can write encrypted file (`FileMap.KEYSTROKE`), upload (`FirebaseUtils.uploadUserKeystrokeDataSnapshot`), and store snapshot (`DeviceDataRepository.insert`).

#### Notifications (Accessibility event extraction)

- **Capture notification payloads**
  - **User-facing behavior**: when enabled, records notification “extras” and timestamps.
  - **Primary components**: `CDCAccessibilityService` → `AccessibilityUtils.processNotificationEvent` → `collectNotificationData`.
  - **Business logic**: gated by local trigger `CAPTURE_NOTIFICATIONS`; can write file (`FileMap.NOTIFICATION`), upload (`FirebaseUtils.uploadUserNotificationDataSnapshot`), and store snapshot (`DeviceDataRepository.insert`).

- **Notification listener service (logging only)**
  - **User-facing behavior**: runs as a foreground service “Listening for notifications”.
  - **Primary components**: `CDCNotificationListenerService`.
  - **Current implementation note**: logs notification text via `LoggerUtils`, but does not upload/store snapshots like the accessibility pipeline.

#### App usage (two paths)

- **Real-time app usage monitoring (window changes)**
  - **User-facing behavior**: when enabled, collects “app opened/closed” sessions.
  - **Primary components**: `CDCAccessibilityService` → `AccessibilityUtils.processWindowStateMovement`.
  - **Business logic**: gated by local trigger `MONITOR_APP_USAGE_STATISTICS`; can write file (`FileMap.APPLICATION_USAGE`), upload (`FirebaseUtils.uploadApplicationUsageDataSnapshot`), store snapshot (`DeviceDataRepository.insert`).

- **Daily app usage report (UsageEvents)**
  - **User-facing behavior**: when invoked, generates a report for the current day.
  - **Primary components**: `ClickActions.GET_APP_USAGE_STATISTICS_REPORT`, `AppUsageStats.getDailyUsageStats`.
  - **Business logic**: converts `UsageEvents` into `AppUsageReportData` with sessions; gated by local trigger; uploads report to RTDB (`FirebaseUtils.uploadApplicationUsageReportDataSnapshot`).

#### Screenshots (MediaProjection)

- **Start/stop screenshot capture**
  - **User-facing behavior**: prompts for screen capture consent, then captures screenshots to storage.
  - **Primary components**: `ActionUtils.startMediaProjectionService` → `ScreenshotService` (foreground).
  - **Business logic**:
    - `ActionUtils.onActivityResult` passes MediaProjection result into `ScreenshotService`.
    - `ClickActions.START_SCREENSHOT_SERVICE` controls capture by toggling `ScreenshotService.setTakeScreenshot(true)` repeatedly (interval/max repetitions) and stopping via `setStopScreenshotService(true)`.
  - **Output**: PNG files under `/sdcard/Screenshots/` (as currently implemented in `ScreenshotService.saveBitmap`).

#### Sensors (foreground service)

- **Capture device sensor streams**
  - **User-facing behavior**: a foreground service runs and writes sensor readings.
  - **Primary components**: `CDCSensorService`.
  - **Business logic**: registers all sensors except those with “uncalibrated” in the name; appends readings to per-sensor `*.txt` under `Documents/CDC/Sensors` via `CDCUnorganisedFileAppender.appendDataToFile`.

#### Contacts / SMS / call logs

- **Capture contacts**
  - **User-facing behavior**: captures address book entries.
  - **Primary components**: `ClickActions.CAPTURE_ALL_CONTACTS`, `MessageUtils.getMessages(FileMap.CONTACTS)`.
  - **Storage**: organized file writes (`CDCOrganisedFileAppender`) + optional upload (`FirebaseUtils.uploadUserContactsDataSnapshot`).

- **Capture SMS**
  - **User-facing behavior**: captures SMS table rows.
  - **Primary components**: `ClickActions.CAPTURE_ALL_SMS`, `MessageUtils.getMessages(FileMap.SMS)`.
  - **Storage**: organized encrypted file writes + optional upload (`FirebaseUtils.uploadUserSmsDataSnapshot`).

- **Capture call logs**
  - **User-facing behavior**: captures call log rows.
  - **Primary components**: `ClickActions.CAPTURE_ALL_CALL_LOGS`, `MessageUtils.getMessages(FileMap.CALL)`.
  - **Storage**: organized encrypted file writes + optional upload (note: current code calls `uploadUserContactsDataSnapshot` for call logs).

#### Telephony call state

- **Monitor call state (ringing/offhook/idle, conference, multiple calls)**
  - **User-facing behavior**: when enabled, writes call state transitions.
  - **Primary components**: `ClickActions.MONITOR_CALL_STATE`, `CallUtils.monitorCallState`.
  - **Storage**: file writes to `FileMap.CALL_STATE`.

#### Directory structure snapshot

- **Capture device directory tree**
  - **User-facing behavior**: captures a recursive tree of external storage.
  - **Primary components**: `ClickActions.GET_DIRECTORY_STRUCTURE`, `FileExplorer.getDirectoryStructure`.
  - **Storage**: optional upload (`FirebaseUtils.uploadDeviceDirectoryStructureSnapshot`) + Room snapshot (`DeviceDataRepository.insert`).

#### Device admin & battery settings

- **Request device admin**
  - **User-facing behavior**: prompts to grant device admin.
  - **Primary components**: `ClickActions.TAKE_DEVICE_ADMIN_PERMISSIONS`, `ActionUtils.takeDeviceAdminPermission`, `DeviceAdminReceiver`.

- **Disable battery optimizations for the app**
  - **User-facing behavior**: opens battery optimization settings if needed.
  - **Primary components**: `ClickActions.PREVENT_BATTERY_OPTIMIZATIONS`, `ActionUtils.checkAndRequestBatteryOptimization`.

#### Reset & statistics logging

- **Reset everything**
  - **User-facing behavior**: clears local and remote data.
  - **Primary components**: `ClickActions.RESET_EVERYTHING`, `AppDatabase.deleteAllRecords`, `SharedPreferenceUtils.resetAllData`, `FirebaseUtils.deleteUserAndDeviceData`, `ClickActions.RESET_ALL_PERMISSION`.

- **Device screen lock/unlock counters**
  - **User-facing behavior**: none directly (writes to logs/files).
  - **Primary components**: `StatisticsBroadcastReceiver` → `ResetService`.
  - **Business logic**: increments lock/unlock counters and appends to `FileMap.APPLICATION_USAGE`.

#### Local file read/decrypt (export helper)

- **Read/decrypt stored files into a temporary plain-text file**
  - **User-facing behavior**: not currently surfaced as a dedicated UI, but implemented as a helper for “read/export” flows.
  - **Primary components**: `CDCFileReader`.
  - **Business logic**:
    - Reads a `FileMap` file under `Documents/CDC/...`.
    - If `fileMap.encrypted=true`, decrypts each line using `CryptoUtils.decrypt` with keys in `AppConstants`.
    - Writes the decrypted lines to `FileMap.TEMPORARY_FILE` via `FileUtils.appendDataToFile`.

### Experimental / partial features (implemented but not fully integrated)

- **VPN traffic capture / blocking (proof-of-concept)**
  - **User-facing behavior**: when started, establishes a VPN and logs captured packets; disallows specific apps.
  - **Primary components**: `CDCVpnService`, `MainActivity.startVpn/stopVpn` (currently commented usage).
  - **Note**: currently logs packets via `LoggerUtils` and does not persist/upload as a first-class feature.

- **Password gate Activity**
  - **User-facing behavior**: prompts for a hard-coded password and closes on success.
  - **Primary components**: `PasswordActivity`.
  - **Note**: uses a constant `"1234"` in code; not wired into other flows.

- **Legacy/partial screen capture service**
  - **User-facing behavior**: captures a screenshot to a single fixed file (`/sdcard/screenshot.png`) when invoked.
  - **Primary components**: `ScreenCaptureService`.
  - **Note**: as written, `mMediaProjection` is not initialized (commented out) and this service is not registered in `AndroidManifest.xml`; the active implementation is `ScreenshotService` (MediaProjection + foreground).

