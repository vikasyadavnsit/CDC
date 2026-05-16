package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static TextView shayariTextView;
    private static TextView counterView;
    private RecyclerView collectionRv;
    private ShayariAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        shayariTextView = view.findViewById(R.id.home_fragment_text_view);
        counterView = view.findViewById(R.id.shayari_counter);
        collectionRv = view.findViewById(R.id.shayari_collection_rv);

        adapter = new ShayariAdapter(new ArrayList<>());
        collectionRv.setLayoutManager(new LinearLayoutManager(getContext()));
        collectionRv.setAdapter(adapter);

        // Show default while loading
        shayariTextView.setText(AppConstants.DEFAULT_SHAYARI_TEXT);

        // Fetch collection from Firebase, pick next shayari, populate list
        FirebaseUtils.getShayariCollection(shayaris -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                ActionUtils.performShayariAction(shayaris);
                adapter = new ShayariAdapter(shayaris);
                collectionRv.setAdapter(adapter);
            });
        });

        return view;
    }

    public static void updateShayariText(String text, int index, int total) {
        if (shayariTextView != null) {
            shayariTextView.setText(text.replace("#", "\n"));
        }
        if (counterView != null) {
            counterView.setText("#" + index + " / " + total);
        }
    }

    // Legacy overload kept for any existing callers
    public static void updateShayariText(String text) {
        updateShayariText(text, 1, 1);
    }
}
