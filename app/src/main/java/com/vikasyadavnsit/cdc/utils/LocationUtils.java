package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

public class LocationUtils {

    private static final String TAG = "LocationUtils";
    private static LocationManager locationManager;
    private static LocationListener activeListener;

    public static void captureAndUpload(Context context) {
        captureAndUpload(context, false);
    }

    public static void captureAndUpload(Context context, boolean persistHistory) {
        LoggerUtils.d(TAG, "captureAndUpload called, persistHistory=" + persistHistory);
        if (!hasLocationPermission(context)) {
            LoggerUtils.w(TAG, "Location permission not granted");
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Location permission not granted on device");
            errorData.put("timestamp", System.currentTimeMillis());
            FirebaseUtils.getDbRef(FirebaseUtils.getPath(com.vikasyadavnsit.cdc.constants.AppConstants.FIREBASE_RTDB_LIVE_LOCATION_PATH)).setValue(errorData);
            return;
        }
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            LoggerUtils.e(TAG, "LocationManager not available");
            return;
        }

        Location best = getBestLastKnown();
        if (best != null) {
            LoggerUtils.d(TAG, "Uploading last known location: " + best.getLatitude() + "," + best.getLongitude());
            FirebaseUtils.uploadLiveLocation(best);
            if (persistHistory) FirebaseUtils.uploadLocationHistory(best);
        } else {
            LoggerUtils.d(TAG, "No last known location available");
        }

        requestFreshFix(context, persistHistory);
    }

    private static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static Location getBestLastKnown() {
        if (locationManager == null) return null;
        Location best = null;
        for (String provider : new String[]{
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) best = l;
            } catch (SecurityException ignored) {}
        }
        return best;
    }

    private static void requestFreshFix(Context context, boolean persistHistory) {
        if (locationManager == null) return;
        if (activeListener != null) {
            try { locationManager.removeUpdates(activeListener); } catch (Exception ignored) {}
        }

        activeListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LoggerUtils.d(TAG, "Fresh location fix received: " + location.getLatitude() + "," + location.getLongitude());
                FirebaseUtils.uploadLiveLocation(location);
                if (persistHistory) FirebaseUtils.uploadLocationHistory(location);
                try { locationManager.removeUpdates(this); } catch (Exception ignored) {}
                activeListener = null;
            }

            @Override public void onStatusChanged(String p, int s, Bundle e) {}
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
        };

        boolean anyProviderEnabled = false;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, activeListener, Looper.getMainLooper());
                anyProviderEnabled = true;
                LoggerUtils.d(TAG, "Requested GPS location updates");
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, activeListener, Looper.getMainLooper());
                anyProviderEnabled = true;
                LoggerUtils.d(TAG, "Requested Network location updates");
            }
        } catch (SecurityException e) {
            LoggerUtils.w(TAG, "Failed to request location updates :: " + e.getMessage());
        }

        if (!anyProviderEnabled) {
            LoggerUtils.w(TAG, "No location providers (GPS/Network) are enabled on device");
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "GPS and Network location providers are both disabled on target device.");
            errorData.put("timestamp", System.currentTimeMillis());
            FirebaseUtils.getDbRef(FirebaseUtils.getPath(com.vikasyadavnsit.cdc.constants.AppConstants.FIREBASE_RTDB_LIVE_LOCATION_PATH)).setValue(errorData);
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (activeListener != null) {
                LoggerUtils.d(TAG, "Location fix timeout reached (30s)");
                FirebaseUtils.uploadLiveLocation(null);
                try { locationManager.removeUpdates(activeListener); } catch (Exception ignored) {}
                activeListener = null;
            }
        }, 30_000);
    }
}
