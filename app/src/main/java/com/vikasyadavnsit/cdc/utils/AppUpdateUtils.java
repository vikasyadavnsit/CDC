package com.vikasyadavnsit.cdc.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.FileProvider;

import com.google.firebase.storage.FirebaseStorage;
import com.vikasyadavnsit.cdc.BuildConfig;
import com.vikasyadavnsit.cdc.receiver.InstallStatusReceiver;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdateUtils {

    private static final String TAG = "AppUpdateUtils";
    private static final String APK_CACHE_NAME = "cdc_update.apk";

    /**
     * Registers a Firebase listener that triggers a download + install whenever
     * cdc/updates/latestVersionCode is greater than the currently installed version.
     * Safe to call multiple times — Firebase deduplicates listeners per path.
     */
    public static void checkForUpdate(Context context) {
        Context appContext = context.getApplicationContext();
        LoggerUtils.d(TAG, "Registering for remote app update checks");
        FirebaseUtils.listenForAppUpdate((latestVersionCode, apkStoragePath) -> {
            if (latestVersionCode > BuildConfig.VERSION_CODE) {
                LoggerUtils.i(TAG, "Update available! v" + BuildConfig.VERSION_CODE + " -> v" + latestVersionCode);
                if (!apkStoragePath.isEmpty()) {
                    downloadAndInstall(appContext, apkStoragePath);
                } else {
                    LoggerUtils.w(TAG, "Update detected but APK path is empty in config");
                }
            } else {
                LoggerUtils.d(TAG, "Device is already on the latest version (v" + BuildConfig.VERSION_CODE + ")");
            }
        });
    }

    /**
     * Download and install an APK from either source:
     *   - HTTPS URL  (starts with "http://" or "https://") → downloaded via HttpURLConnection
     *   - Firebase Storage path (anything else)            → downloaded via Firebase Storage SDK
     */
    public static void downloadAndInstall(Context context, String source) {
        LoggerUtils.i(TAG, "Initiating APK download from: " + source);
        if (source.startsWith("http://") || source.startsWith("https://")) {
            downloadFromUrl(context, source);
        } else {
            downloadFromFirebaseStorage(context, source);
        }
    }

    private static void downloadFromFirebaseStorage(Context context, String storagePath) {
        File apkFile = new File(context.getCacheDir(), APK_CACHE_NAME);
        FirebaseStorage.getInstance()
                .getReference()
                .child(storagePath)
                .getFile(apkFile)
                .addOnSuccessListener(snapshot -> {
                    LoggerUtils.d(TAG, "Firebase Storage download done (" + apkFile.length() + " bytes)");
                    installApk(context, apkFile);
                })
                .addOnFailureListener(e ->
                        LoggerUtils.e(TAG, "Firebase Storage download failed: " + e.getMessage()));
    }

    private static void downloadFromUrl(Context context, String urlString) {
        File apkFile = new File(context.getCacheDir(), APK_CACHE_NAME);
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(60_000);
                conn.connect();
                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    LoggerUtils.e(TAG, "URL download failed — HTTP " + code);
                    return;
                }
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(apkFile)) {
                    byte[] buf = new byte[65536];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                }
                LoggerUtils.d(TAG, "URL download done (" + apkFile.length() + " bytes)");
                new Handler(Looper.getMainLooper()).post(() -> installApk(context, apkFile));
            } catch (IOException e) {
                LoggerUtils.e(TAG, "URL download error: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static void installApk(Context context, File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installViaPackageInstaller(context, apkFile);
        } else {
            installViaIntent(context, apkFile);
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static void installViaPackageInstaller(Context context, File apkFile) {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);

        try {
            int sessionId = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(sessionId);

            try (OutputStream out = session.openWrite(APK_CACHE_NAME, 0, apkFile.length());
                 InputStream in = new FileInputStream(apkFile)) {
                byte[] buf = new byte[65536];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                session.fsync(out);
            }

            Intent callbackIntent = new Intent(context, InstallStatusReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, sessionId, callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            session.commit(pi.getIntentSender());
            session.close();
            LoggerUtils.d(TAG, "PackageInstaller session committed (id=" + sessionId + ")");
        } catch (IOException e) {
            LoggerUtils.e(TAG, "PackageInstaller error: " + e.getMessage() + " — falling back to intent");
            installViaIntent(context, apkFile);
        }
    }

    /** API 24-30 — shows the system install prompt via a FileProvider URI. */
    private static void installViaIntent(Context context, File apkFile) {
        Uri apkUri = FileProvider.getUriForFile(
                context, context.getPackageName() + ".fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
