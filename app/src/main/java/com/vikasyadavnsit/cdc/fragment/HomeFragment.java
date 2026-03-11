package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;


public class HomeFragment extends Fragment {

    private static TextView textView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Find the TextView in the layout
        textView = view.findViewById(R.id.home_fragment_text_view);
        CommonUtil.setShayari(getContext());
        return view;
    }

    // Public method to update the TextView from another class
    public static void updateShayariText(String newText) {
        if (textView != null) {
            textView.setText(newText.replace("#","\n"));
        }
    }


}