package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

public class ShayariFragment extends Fragment {

    private static TextView shayariTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shayari, container, false);

        shayariTextView = view.findViewById(R.id.shayari_fragment_text_view);
        shayariTextView.setText(AppConstants.DEFAULT_SHAYARI_TEXT);

        FirebaseUtils.getShayariCollection(shayaris -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> ActionUtils.performShayariAction(shayaris));
        });

        return view;
    }

    public static void updateShayariText(String text, int index, int total) {
        if (shayariTextView != null) {
            shayariTextView.setText(text.replace("#", "\n"));
        }
    }

    public static void updateShayariText(String text) {
        updateShayariText(text, 1, 1);
    }

}
