package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.util.Objects;


public class MessageFragment extends Fragment {

    private static TextView textView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        textView = view.findViewById(R.id.message_fragment_text_view);
        FirebaseUtils.getMessageData();
        return view;
    }

    public static void updateMessage(String newText) {
        if (Objects.nonNull(newText)) {
            textView.setText(newText.replace("#", "\n"));
        }
    }
}