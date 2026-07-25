package com.vikasyadavnsit.cdc.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class AdminScreenshotFragment extends Fragment {

    private static WeakReference<AdminScreenshotFragment> activeInstance;

    private ImageView imageView;
    private TextView emptyText;
    private TextView timestampText;
    private TextView sizeText;
    private View metaCard;
    private View loader;
    private Button captureBtn;
    private Button deleteBtn;

    private final ValueEventListener screenshotListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists()) {
                String base64 = snapshot.child("imageBase64").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                if (base64 != null) {
                    renderScreenshot(base64, timestamp != null ? timestamp : 0L);
                } else {
                    showEmpty();
                }
            } else {
                showEmpty();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            LoggerUtils.e("AdminScreenshot", "Listener cancelled: " + error.getMessage());
            showEmpty();
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
        View view = inflater.inflate(R.layout.fragment_admin_screenshot, container, false);

        imageView = view.findViewById(R.id.screenshot_image_view);
        emptyText = view.findViewById(R.id.screenshot_empty_text);
        timestampText = view.findViewById(R.id.screenshot_timestamp_text);
        sizeText = view.findViewById(R.id.screenshot_size_text);
        metaCard = view.findViewById(R.id.screenshot_meta_card);
        loader = view.findViewById(R.id.screenshot_loader);
        captureBtn = view.findViewById(R.id.screenshot_capture_btn);
        deleteBtn = view.findViewById(R.id.screenshot_delete_btn);

        view.findViewById(R.id.screenshot_back_button).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        captureBtn.setOnClickListener(v -> requestCapture());
        deleteBtn.setOnClickListener(v -> deleteScreenshot());

        FirebaseUtils.monitorRemoteScreenshot(screenshotListener);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FirebaseUtils.removeRemoteScreenshotListener(screenshotListener);
    }

    private void requestCapture() {
        loader.setVisibility(View.VISIBLE);
        captureBtn.setEnabled(false);
        FirebaseUtils.requestRemoteScreenshot();
        Toast.makeText(getContext(), "Capture command sent — waiting for device...", Toast.LENGTH_SHORT).show();
        captureBtn.postDelayed(() -> {
            captureBtn.setEnabled(true);
            loader.setVisibility(View.GONE);
        }, 8000);
    }

    private void deleteScreenshot() {
        FirebaseUtils.deleteRemoteScreenshot(); // Only deletes the live 'latest' node
        showEmpty();
        Toast.makeText(getContext(), "Latest preview cleared. History preserved.", Toast.LENGTH_SHORT).show();
    }

    private void renderScreenshot(String base64, long timestamp) {
        if (getView() == null) return;

        loader.setVisibility(View.GONE);
        captureBtn.setEnabled(true);

        try {
            byte[] imageBytes = Base64.decode(base64, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
                emptyText.setVisibility(View.GONE);
                metaCard.setVisibility(View.VISIBLE);
                deleteBtn.setVisibility(View.VISIBLE);

                String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date(timestamp));
                timestampText.setText("Captured: " + formattedTime);
                sizeText.setText("Resolution: " + bitmap.getWidth() + " x " + bitmap.getHeight()
                        + "  |  Data: " + (base64.length() / 1024) + " KB");
            } else {
                showEmpty();
            }
        } catch (Exception e) {
            LoggerUtils.e("AdminScreenshot", "Failed to decode screenshot: " + e.getMessage());
            showEmpty();
        }
    }

    private void showEmpty() {
        if (getView() == null) return;
        loader.setVisibility(View.GONE);
        captureBtn.setEnabled(true);
        imageView.setVisibility(View.GONE);
        metaCard.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
    }

    /** Called from ActionUtils for one-shot reads (not currently wired, but available). */
    public static void displayScreenshot(Context context, Object obj) {
        AdminScreenshotFragment fragment = activeInstance != null ? activeInstance.get() : null;
        if (fragment == null || fragment.getView() == null) return;

        if (obj == null) {
            fragment.showEmpty();
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) obj;
            String base64 = (String) data.get("imageBase64");
            Object tsObj = data.get("timestamp");
            long ts = tsObj instanceof Number ? ((Number) tsObj).longValue() : 0L;
            if (base64 != null) {
                fragment.renderScreenshot(base64, ts);
            } else {
                fragment.showEmpty();
            }
        } catch (Exception e) {
            LoggerUtils.e("AdminScreenshot", "displayScreenshot error: " + e.getMessage());
            fragment.showEmpty();
        }
    }
}
