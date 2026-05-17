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

public class LocationUtils {

    private static final String TAG = "LocationUtils";
    private static LocationManager locationManager;
    private static LocationListener activeListener;

    public static void captureAndUpload(Context context) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted");
            return;
        }
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;

        Location best = getBestLastKnown();
        if (best != null) FirebaseUtils.uploadLiveLocation(best);

        requestFreshFix(context);
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

    private static void requestFreshFix(Context context) {
        if (locationManager == null) return;
        if (activeListener != null) {
            try { locationManager.removeUpdates(activeListener); } catch (Exception ignored) {}
        }
        String provider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        if (!locationManager.isProviderEnabled(provider)) return;

        activeListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                FirebaseUtils.uploadLiveLocation(location);
                try { locationManager.removeUpdates(this); } catch (Exception ignored) {}
                activeListener = null;
            }

            @Override public void onStatusChanged(String p, int s, Bundle e) {}
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
        };

        try {
            locationManager.requestLocationUpdates(provider, 0, 0, activeListener, Looper.getMainLooper());
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (activeListener != null) {
                    try { locationManager.removeUpdates(activeListener); } catch (Exception ignored) {}
                    activeListener = null;
                }
            }, 30_000);
        } catch (SecurityException e) {
            Log.w(TAG, "Failed to request location updates", e);
        }
    }
}
