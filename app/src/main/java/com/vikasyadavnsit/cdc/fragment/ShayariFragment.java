package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;

public class ShayariFragment extends Fragment {

    private TextView shayariTextView;
    private TextView counterTextView;
    private ImageButton prevButton;
    private ImageButton nextButton;

    private List<String> shayariList = new ArrayList<>();
    private int currentIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shayari, container, false);

        shayariTextView = view.findViewById(R.id.shayari_fragment_text_view);
        counterTextView = view.findViewById(R.id.shayari_counter_text);
        prevButton = view.findViewById(R.id.shayari_previous_button);
        nextButton = view.findViewById(R.id.shayari_next_button);

        shayariTextView.setText(AppConstants.DEFAULT_SHAYARI_TEXT);

        prevButton.setOnClickListener(v -> showPrevious());
        nextButton.setOnClickListener(v -> showNext());

        loadShayaris();

        return view;
    }

    private void loadShayaris() {
        FirebaseUtils.getShayariCollection(shayaris -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (shayaris != null && !shayaris.isEmpty()) {
                    shayariList = shayaris;

                    // Restore and auto-increment for a fresh look on app open
                    String currentData = com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.getShayariData(getContext());
                    try {
                        String[] parts = currentData.split(":");
                        int lastIndex = Integer.parseInt(parts[0]);
                        // Increment by 1 and wrap around if at the end
                        currentIndex = (lastIndex + 1) % shayariList.size();
                    } catch (Exception e) {
                        currentIndex = 0;
                    }
                    // Save the new position immediately
                    saveCurrentIndex();
                    updateUI();
                }
            });
        });
    }

    private void showNext() {
        if (shayariList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % shayariList.size();
        updateUI();
        saveCurrentIndex();
    }

    private void showPrevious() {
        if (shayariList.isEmpty()) return;
        currentIndex = (currentIndex - 1 + shayariList.size()) % shayariList.size();
        updateUI();
        saveCurrentIndex();
    }

    private void saveCurrentIndex() {
        if (getContext() != null && !shayariList.isEmpty()) {
            com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils.updateShayariData(
                    getContext(), currentIndex + ":" + shayariList.size());
        }
    }

    private void updateUI() {
        if (shayariList.isEmpty()) return;
        
        String text = shayariList.get(currentIndex);
        shayariTextView.setText(text.replace("#", "\n"));
        counterTextView.setText((currentIndex + 1) + " / " + shayariList.size());
    }

    // Kept for backward compatibility if called from elsewhere
    public static void updateShayariText(String text, int index, int total) {
        // This is no longer the primary way to update, but we keep it to avoid breaks
    }

    public static void updateShayariText(String text) {
        // This is no longer the primary way to update, but we keep it to avoid breaks
    }
}
