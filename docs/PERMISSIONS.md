# Permissions Reference

CDC requires several high-level Android permissions to function effectively. This document explains why each permission is requested and which components rely on them.

## 🔑 Critical Permissions

| Permission | Feature | Why it's needed |
| :--- | :--- | :--- |
| **Accessibility Service** | Keystrokes, Notifications, App Usage | Allows the app to "observe" UI changes, text entry, and system events. |
| **All Files Access** (`MANAGE_EXTERNAL_STORAGE`) | Local Logging, SQLite | Required to write encrypted logs and the Room database to public directories for persistence across installs. |
| **Usage Stats** | Daily Reports | Accesses the `UsageStatsManager` to generate detailed summaries of time spent in apps. |
| **Media Projection** | Screenshots | Standard Android API for capturing screen content. Requires a persistent notification. |

---

## 📱 Runtime Permissions

These are requested via the standard Android dialogs or the bulk `REQUEST_ALL_PERMISSION` trigger.

*   **`READ_CONTACTS`**: To snapshot the address book.
*   **`READ_SMS` / `RECEIVE_SMS`**: To monitor and log incoming/existing text messages.
*   **`READ_CALL_LOG`**: To capture call history.
*   **`READ_PHONE_STATE`**: To monitor call transitions (Ringing, Off-hook).
*   **`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`**: Required for real-time location tracking and historical logging.
*   **`CAMERA`**: For snapshot and live frame capture.
*   **`RECORD_AUDIO`**: For remote audio monitoring.
*   **`BODY_SENSORS`**: To access heart rate or step counters (if available).
*   **`POST_NOTIFICATIONS`**: Required on Android 13+ to show foreground service indicators.

---

## ⚙️ Special Access & Battery

To ensure the app isn't killed by the system's battery management:

1.  **Ignore Battery Optimizations**: CDC requests to be added to the whitelist so the system doesn't put the capture services to sleep.
2.  **Device Admin**: Granting Device Admin prevents simple uninstallation and allows for advanced device management features.
3.  **Exact Alarms**: Required for the `CommonUtil.scheduleDailyReset` function to run tasks at precise times.

---

## 🛠️ Management via Admin Panel

The **ClickActions** panel in the Admin Viewer allows an operator to remotely trigger permission requests on the target device:

*   `REQUEST_ALL_PERMISSION`: Triggers a sequential request for all runtime permissions.
*   `RESET_ALL_PERMISSION`: Opens the system app settings to allow for a manual permission reset.
*   `REQUEST_ACCESSIBILITY_PERMISSION`: Directly opens the Accessibility settings page.

---
[Return to README](../README.md)
