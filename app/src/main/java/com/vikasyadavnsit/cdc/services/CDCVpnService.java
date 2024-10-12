package com.vikasyadavnsit.cdc.services;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CDCVpnService extends VpnService {

    private static final String TAG = "CDCVpnService";
    private static final int MAX_PACKET_SIZE = 32767;
    private ParcelFileDescriptor vpnInterface = null;
    private Thread vpnThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("stop", false)) {
            stopVpn();
            return START_NOT_STICKY;
        } else {
            startVpn();
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    private void startVpn() {
        if (vpnInterface != null) {
            LoggerUtils.d(TAG, "VPN is already running.");
            return;
        }

        Builder builder = new Builder();
        builder.setSession("MyVPNService")
                .setMtu(1500)
                .addAddress("10.0.0.2", 24)  // Local IP address for the VPN
                .addRoute("0.0.0.0", 0);  // Route all traffic through the VPN

        // Add allowed and disallowed apps
        try {
            builder.addDisallowedApplication("com.google.android.youtube");
            builder.addDisallowedApplication("com.android.chrome");
            // builder.addAllowedApplication("com.vikasyadavnsit.cdc");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        vpnInterface = builder.establish();
        if (vpnInterface != null) {
            vpnThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    capturePackets();
                }
            });
            vpnThread.start();
        }
    }

    private void stopVpn() {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }
    }

    private void capturePackets() {
        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
        ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_SIZE);

        try {
            while (true) {
                int length = in.read(packet.array());
                if (length > 0) {
                    LoggerUtils.d(TAG, "Captured packet: " + bytesToHex(packet.array(), length));
                    packet.clear();
                }
            }
        } catch (IOException e) {
            LoggerUtils.e(TAG, "Error capturing packets" + e.getMessage());
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                LoggerUtils.e(TAG, "Error closing streams" + e.getMessage());
            }
        }
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

}