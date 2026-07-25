package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

public class AdminMessageFragment extends Fragment {

    private static final String ARG_USER = "arg_user";
    private User targetUser;

    public static AdminMessageFragment newInstance(User user) {
        AdminMessageFragment fragment = new AdminMessageFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_message, container, false);

        if (getArguments() != null) {
            targetUser = (User) getArguments().getSerializable(ARG_USER);
        }

        TextView userLabel = view.findViewById(R.id.admin_message_target_user);
        TextView idLabel = view.findViewById(R.id.admin_message_target_id);
        EditText input = view.findViewById(R.id.admin_message_input);
        Button sendButton = view.findViewById(R.id.admin_message_send_button);

        if (targetUser != null) {
            userLabel.setText("Target: " + targetUser.getFullName());
            String androidId = targetUser.getDeviceDetails() != null 
                    ? (String) targetUser.getDeviceDetails().get("androidId") 
                    : "Unknown";
            idLabel.setText("ID: " + androidId);
        }

        sendButton.setOnClickListener(v -> {
            String message = input.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (targetUser != null && targetUser.getDeviceDetails() != null) {
                String androidId = (String) targetUser.getDeviceDetails().get("androidId");
                FirebaseUtils.updateRemoteUserMessage(androidId, message);
                Toast.makeText(getContext(), "Message updated successfully", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
    }
}
