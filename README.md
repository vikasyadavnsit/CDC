# CDC

CDC is an **Android app** that can capture selected device data (based on permissions and remote trigger settings) and optionally **store it locally** and/or **upload snapshots to Firebase Realtime Database (RTDB)**. It also contains an **admin-style viewer** to select a target device and view some captured feeds (keystrokes, notifications, usage report).

For a comprehensive, code-derived list of user-facing features and the exact implementation components, see [`FEATURES.md`](FEATURES.md).

## Table of contents

- [What the app does](#what-the-app-does)
- [Build & run](#build--run)
- [Permissions & prerequisites](#permissions--prerequisites)
- [High-level architecture](#high-level-architecture)
- [Project status](#project-status)
- [Contributing](#contributing)
- [License](#license)
- [Credits](#credits)

## What the app does

Implemented, user-facing highlights (based on the current code):

- **Home**: shows a daily “shayari” text (cached in SharedPreferences, optionally refreshed from RTDB).
- **Message**: shows a personalized message from RTDB (fallback to SharedPreferences default).
- **Hidden Settings tab**: settings button is gesture-gated (long-press pattern).
- **Admin viewer** (in Settings): select a target device (by Android ID) and view:
  - the target device’s remote “click action” trigger settings,
  - keystroke feed,
  - notification feed,
  - system app usage report (daily report requested via RTDB trigger + then fetched/displayed).
- **Device-side capture actions** (triggered by `ClickActions` settings): contacts/SMS/call logs snapshots, call state events, directory structure snapshot, sensor stream logging, MediaProjection screenshots, accessibility-driven keystrokes/notifications/app-usage events, plus reset utilities.

## Potential future capabilities (not implemented yet)

Ideas that fit the current architecture (permissions + trigger settings + local/file/RTDB sinks). These are **not implemented** in the current codebase:

- **Screen recording** (MediaProjection video) and on-demand clip upload
- **Live screen streaming / mirroring** (WebRTC/RTMP) with admin-controlled start/stop
- **GPS / location history** (foreground/background), geofencing, and “location on trigger”
- **Photo/video gallery snapshotting** (MediaStore indexing + thumbnails)
- **Microphone/audio capture** (with explicit user consent flows) and event-based recording
- **Network activity insights** (per-app traffic stats, DNS logging, simple firewall rules)
- **Remote command execution** beyond current triggers (queueing, retries, idempotency, audit logs)
- **Offline-first sync** (local queue + backoff + “upload when on Wi‑Fi/charging” policies)
- **Admin dashboard improvements**: search/filter/export, per-user timelines, and bulk actions
- **Security hardening**: rotate encryption keys, device-bound key storage (Keystore), signed commands
- **Scheduling**: cron-like schedules for triggers and periodic snapshots (vs. manual invocation)
- **Data minimization**: redaction rules, sampling, and per-app allow/deny lists

## Build & run

- **Requirements**: Android Studio (or Gradle CLI), an Android device/emulator (note: many features require real device + permissions).
- **Open**: open the repository as an Android project.
- **Run**: build and run the `app` module.

## Permissions & prerequisites

Many features are gated by Android permissions and settings panels. The app includes helper actions to open the relevant system settings:

- **Accessibility Service**: required for accessibility-driven keystrokes, notification extraction, and window-change app usage events.
- **Usage Access**: required for the daily usage report path (`UsageStatsManager`).
- **All files access / external storage**: required for writing to `Documents/CDC` and for using the external-path Room DB location when configured that way.
- **MediaProjection consent**: required for screenshot capture.
- **Runtime permissions**: contacts/SMS/call logs/phone state, notifications, etc., depending on which capture actions you enable.

## High-level architecture

- **Firebase RTDB** is used for:
  - device-scoped trigger settings (`appSettings/appTriggerSettingsDataMap`),
  - uploading captured snapshots (`userDeviceData/...`),
  - a flat user index used for admin selection (`cdc/flatUserDetails/...`).
- **Local persistence**:
  - SharedPreferences for lightweight state (first launch, selected admin target device, cached texts),
  - Room database for storing trigger settings (when mirrored) and some captured snapshots,
  - Files under `Documents/CDC` via organized/unorganized appenders with optional AES encryption per file type.

Details and exact class mappings are documented in [`FEATURES.md`](FEATURES.md).

## Project status

This README intentionally describes **only what is implemented** in the current codebase. If you change or add features, please update [`FEATURES.md`](FEATURES.md) first and keep this README aligned.

## Contributing

Not accepting external contributions at this time.

## License

No license file is currently provided. Treat this repository as **All Rights Reserved** unless a license is added.

## Credits

- Android Studio tooling and Android platform documentation