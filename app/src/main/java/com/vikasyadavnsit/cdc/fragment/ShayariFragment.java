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
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

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

        prevButton.setOnClickListener(v -> showPrevious());
        nextButton.setOnClickListener(v -> showNext());

        loadFromCache();

        return view;
    }

    private void loadFromCache() {
        shayariList = SharedPreferenceUtils.getShayariList(getContext());
        if (shayariList.isEmpty()) {
            shayariTextView.setText(AppConstants.DEFAULT_SHAYARI_TEXT);
            return;
        }
        String currentData = SharedPreferenceUtils.getShayariData(getContext());
        try {
            int lastIndex = Integer.parseInt(currentData.split(":")[0]);
            currentIndex = (lastIndex + 1) % shayariList.size();
        } catch (Exception e) {
            currentIndex = 0;
        }
        saveCurrentIndex();
        updateUI();
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

}
