# Setup & Deployment Guide

This guide will walk you through the process of setting up the CDC environment, configuring Firebase, and deploying the application to target devices.

## 🛠️ Environment Prerequisites

*   **JDK**: Version 17
*   **Android Studio**: Hedgehog (2023.1.1) or newer
*   **Gradle**: Version 8.1+
*   **Physical Device**: Android 7.0 (API 24) or higher. 
    > [!WARNING]
    > Emulators are **not recommended** as most capture features (Accessibility, MediaProjection, Sensors) require physical hardware interaction.

---

## 🔥 Firebase Configuration

The app relies heavily on Firebase RTDB and Storage. Follow these steps to link your project:

1.  **Create a Firebase Project**: Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project named `Android-CDC`.
2.  **Add Android App**:
    *   Register the app package: `com.vikasyadavnsit.cdc`
    *   Download the `google-services.json` file.
    *   Place the file into the `app/` directory of your local project.
3.  **Realtime Database Setup**:
    *   Create a database in the **Asia-Southeast1 (Singapore)** region (recommended based on hardcoded constants, or update `AppConstants.java`).
    *   Set the rules to `true` for testing (Note: In production, secure these rules per-user).
4.  **Firebase Storage**:
    *   Enable Storage to allow for remote APK updates and large file snapshots.

---

## 🏗️ Build & Install

1.  **Open Project**: Import the repository into Android Studio.
2.  **Sync Gradle**: Allow Android Studio to download dependencies (Room, Hilt, Firebase, etc.).
3.  **Build Signed APK**:
    *   Navigate to `Build > Generate Signed Bundle / APK...`
    *   Select `APK` > `release`.
    *   Use your keystore to sign the application.
4.  **Install**: Use `adb install` or transfer the APK to your device.

---

## 🚀 First-Launch Configuration

Upon first opening the app:

1.  **Identity**: Enter a "User Name" when prompted. This creates your record in the RTDB.
2.  **Access Hidden Settings**:
    *   Go to the **Home** tab.
    *   Perform **3 long-presses** on the Home button within 20 seconds.
    *   A "Settings" tab will appear in the navigation bar.
3.  **Grant Permissions**:
    *   Open the new Settings tab.
    *   Navigate through the permission buttons to grant:
        *   **Accessibility Service** (CDC Accessibility)
        *   **All Files Access**
        *   **Usage Access**
        *   **Battery Optimization Exemption**

---

## 🔄 Remote Updates

CDC includes a PowerShell utility to manage remote APK releases.

### Initial Setup
Install Firebase CLI:
```bash
npm install -g firebase-tools
firebase login
```

### Using the Update Script
The script is located at `scripts/firebase_update.ps1`.

*   **Initialize RTDB Config**:
    ```powershell
    .\scripts\firebase_update.ps1 init
    ```
*   **Publish New Release (Firebase Storage)**:
    ```powershell
    .\scripts\firebase_update.ps1 release -ApkPath .\app\release\app-release.apk
    ```
*   **Publish New Release (External URL)**:
    ```powershell
    .\scripts\firebase_update.ps1 release -ApkPath .\app\release\app-release.apk -ApkUrl https://your-site.com/cdc.apk
    ```

---
[Return to README](../README.md)
