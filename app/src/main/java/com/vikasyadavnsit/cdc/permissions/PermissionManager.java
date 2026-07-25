package com.vikasyadavnsit.cdc.permissions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.dialog.MessageDialog;
import com.vikasyadavnsit.cdc.enums.PermissionType;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import java.io.File;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;


public class PermissionManager implements PermissionHandler {
    @Override
    public boolean hasPermission(Context context, PermissionType permissionType) {
        if (permissionType == PermissionType.MANAGE_EXTERNAL_STORAGE) {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
        }
        if (permissionType == PermissionType.SYSTEM_ALERT_WINDOW) {
            return Settings.canDrawOverlays(context);
        }
        if (permissionType == PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE) {
            String listeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return listeners != null && listeners.contains(context.getPackageName());
        }
        if (permissionType == PermissionType.PACKAGE_USAGE_STATS) {
            android.app.AppOpsManager appOps = (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                mode = appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), context.getPackageName());
            } else {
                mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), context.getPackageName());
            }
            boolean granted = (mode == android.app.AppOpsManager.MODE_ALLOWED);
            LoggerUtils.d("PermissionManager", "Usage Stats check: " + (granted ? "GRANTED" : "DENIED") + " (mode=" + mode + ")");
            return granted;
        }
        if (permissionType == PermissionType.ACCESSIBILITY_SERVICE) {
            boolean enabled = com.vikasyadavnsit.cdc.utils.AccessibilityUtils.isAccessibilityServiceEnabled(context, 
                    com.vikasyadavnsit.cdc.services.CDCAccessibilityService.class);
            LoggerUtils.d("PermissionManager", "Accessibility check: " + (enabled ? "ENABLED" : "DISABLED"));
            return enabled;
        }
        if (permissionType == PermissionType.BATTERY_OPTIMIZATION) {
            android.os.PowerManager powerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        if (permissionType == PermissionType.LOCATION) {
            boolean fine = ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean coarse = ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            LoggerUtils.d("PermissionManager", "Location check: Fine=" + fine + ", Coarse=" + coarse);
            return fine || coarse;
        }
        if (permissionType == PermissionType.FOREGROUND_SERVICE_MEDIA_PROJECTION) {
            boolean running = com.vikasyadavnsit.cdc.services.ScreenshotService.isRunning();
            LoggerUtils.d("PermissionManager", "MediaProjection check (Service Running): " + running);
            return running;
        }
        if (permissionType == PermissionType.VPN) {
            boolean granted = android.net.VpnService.prepare(context) == null;
            LoggerUtils.d("PermissionManager", "VPN check: " + (granted ? "GRANTED" : "DENIED"));
            return granted;
        }

        boolean hasAllPermissions = true;
        for (String permission : permissionType.getPermissions()) {
            if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                LoggerUtils.d("PermissionManager", "Permission : " + permission + " already granted");
            } else {
                LoggerUtils.d("PermissionManager", "Permission : " + permission + " doesn't exist, taking permission");
                hasAllPermissions = false;
                break;
            }
        }
        return hasAllPermissions;
    }

    @Override
    public void requestPermission(Activity activity, PermissionType permissionType) {
        if (hasPermission(activity, permissionType)) return;

        if (permissionType == PermissionType.MANAGE_EXTERNAL_STORAGE) {
            com.vikasyadavnsit.cdc.utils.FileUtils.startFileAccessSettings(activity);
            return;
        }
        if (permissionType == PermissionType.SYSTEM_ALERT_WINDOW) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
            return;
        }
        if (permissionType == PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE) {
            activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return;
        }
        if (permissionType == PermissionType.PACKAGE_USAGE_STATS) {
            activity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }
        if (permissionType == PermissionType.ACCESSIBILITY_SERVICE) {
            com.vikasyadavnsit.cdc.utils.AccessibilityUtils.startAccessibilitySettingIntent(activity);
            return;
        }
        if (permissionType == PermissionType.BATTERY_OPTIMIZATION) {
            activity.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            return;
        }
        if (permissionType == PermissionType.FOREGROUND_SERVICE_MEDIA_PROJECTION) {
            com.vikasyadavnsit.cdc.services.ScreenshotService.setRemoteMode(true);
            com.vikasyadavnsit.cdc.utils.ActionUtils.startMediaProjectionService(activity);
            return;
        }
        if (permissionType == PermissionType.VPN) {
            com.vikasyadavnsit.cdc.utils.ActionUtils.startVpnService(activity);
            return;
        }

        LoggerUtils.d("PermissionManager", "Requesting Permission : " + String.join(", ", permissionType.getPermissions()));
        ActivityCompat.requestPermissions(activity, permissionType.getPermissions(), permissionType.getRequestCode());
    }

    public void requestDirectPermissionInOneGo(Activity activity) {
        LoggerUtils.d("PermissionManager", "Requesting All Permissions in one Go");
        String[] allPermissions = Stream.of(PermissionType.values())
                .filter(type -> type != PermissionType.MANAGE_EXTERNAL_STORAGE 
                        && type != PermissionType.SYSTEM_ALERT_WINDOW 
                        && type != PermissionType.BIND_NOTIFICATION_LISTENER_SERVICE
                        && type != PermissionType.PACKAGE_USAGE_STATS
                        && type != PermissionType.ACCESSIBILITY_SERVICE
                        && type != PermissionType.BATTERY_OPTIMIZATION)
                .flatMap(permissionType -> Stream.of(permissionType.getPermissions()))
                .toArray(String[]::new);
        ActivityCompat.requestPermissions(activity, allPermissions, AppConstants.ALL_PERMISSIONS_REQUEST_CODE);
    }


    @Override
    public void requestAllPermissions(Activity activity) {
        LoggerUtils.d("PermissionManager", "Requesting All Permissions");
        PermissionType[] permissionTypes = PermissionType.values();
//        for (PermissionType permissionType : permissionTypes) {
//            requestPermission(activity, permissionType);
//        }
        requestDirectPermissionInOneGo(activity);
    }

    @Override
    public void handlePermissionResult(Context context, int requestCode, String[] permissions, int[] grantResults) {
        LoggerUtils.d("PermissionManager", "handlePermissionResult: requestCode=" + requestCode);
        PermissionType permissionType = PermissionType.getPermissionTypeByRequestCode(requestCode);
        if (Objects.nonNull(permissionType)) {
            final PermissionType finalPermissionType = permissionType;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    LoggerUtils.i("PermissionManager", "Permission GRANTED: " + permissions[i]);
                } else {
                    LoggerUtils.w("PermissionManager", "Permission DENIED: " + permissions[i]);
                    
                    if (context instanceof Activity) {
                        Activity activity = (Activity) context;
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i]);
                        
                        String message;
                        String buttonText;
                        Runnable action;

                        String permissionName = finalPermissionType.name().toLowerCase().replace("_", " ");

                        if (showRationale) {
                            message = "The " + permissionName + " permission is required for this feature to work. Please grant it on the next screen.";
                            buttonText = "Grant Permission";
                            action = () -> requestPermission(activity, finalPermissionType);
                        } else {
                            message = "The " + permissionName + " permission has been permanently denied. To use this feature, you must manually enable it in the app settings.";
                            buttonText = "Open Settings";
                            action = () -> resetAllPermissionManually(context);
                        }

                        new MaterialAlertDialogBuilder(activity)
                                .setTitle("Permission Needed")
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton(buttonText, (dialog, which) -> action.run())
                                .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                                .setIcon(R.drawable.ic_launcher_foreground)
                                .show();
                    } else {
                        // Use a simple Toast instead of a full dialog if context is not an Activity
                        // to avoid WindowManager$BadTokenException
                        Toast.makeText(context, "Permission Denied: " + permissions[i], Toast.LENGTH_LONG).show();
                    }
                }
            }
            // Sync status after processing all results (covers both grant and deny)
            com.vikasyadavnsit.cdc.utils.FirebaseUtils.syncAllPermissionStatuses(context);
        }
    }

    @Override
    public void resetAllPermissionManually(Context context) {
        LoggerUtils.w("PermissionManager", "Full Reset triggered: Revoking permissions and wiping data");
        
        // 1. Revoke permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager pm = context.getPackageManager();
            try {
                android.content.pm.PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
                if (pi.requestedPermissions != null) {
                    java.util.List<String> toRevoke = new java.util.ArrayList<>();
                    for (String p : pi.requestedPermissions) {
                        if (pm.checkPermission(p, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                            toRevoke.add(p);
                        }
                    }
                    if (!toRevoke.isEmpty()) {
                        context.revokeSelfPermissionsOnKill(toRevoke);
                    }
                }
            } catch (Exception e) {
                LoggerUtils.e("PermissionManager", "Revoke permissions failed: " + e.getMessage());
            }
        }

        // 2. Full Firebase and Local Wipe
        FirebaseUtils.fullReset(() -> {
            // Local cleanup
            SharedPreferenceUtils.resetAllData(context);
            
            // Delete external database/logs folder
            File cdcFolder = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "CDC");
            deleteRecursively(cdcFolder);

            // Final blow: Clear all app data and kill process
            android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                LoggerUtils.i("PermissionManager", "Clearing application user data - GOODBYE");
                am.clearApplicationUserData();
            } else {
                // Fallback: Open settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    private void deleteRecursively(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

}


