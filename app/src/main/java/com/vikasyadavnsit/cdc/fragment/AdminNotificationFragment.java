package com.vikasyadavnsit.cdc.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminNotificationFragment extends Fragment {

    private EditText titleInput, messageInput;
    private LinearLayout historyContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_notification, container, false);

        titleInput = view.findViewById(R.id.notification_title_input);
        messageInput = view.findViewById(R.id.notification_message_input);
        Button sendBtn = view.findViewById(R.id.notification_send_btn);
        historyContainer = view.findViewById(R.id.alert_history_container);

        view.findViewById(R.id.notification_back_button).setVisibility(View.GONE);

        sendBtn.setOnClickListener(v -> sendNotification());

        loadAlertHistory();

        return view;
    }

    private void loadAlertHistory() {
        FirebaseUtils.getDbRef(FirebaseUtils.getSelectedUserPath("/userDeviceData/alertHistory"))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Context ctx = getContext();
                        if (ctx == null || historyContainer == null) return;
                        
                        historyContainer.removeAllViews();
                        
                        List<DataSnapshot> children = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (child.exists()) children.add(child);
                        }
                        Collections.reverse(children);

                        if (children.isEmpty()) {
                            TextView empty = new TextView(ctx);
                            empty.setText("No alerts sent yet.");
                            empty.setTextColor(ctx.getColor(R.color.text_hint));
                            empty.setTextSize(12f);
                            empty.setPadding(32, 32, 32, 32);
                            historyContainer.addView(empty);
                            return;
                        }

                        for (DataSnapshot child : children) {
                            addHistoryItem(ctx, child);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void addHistoryItem(Context ctx, DataSnapshot snap) {
        View item = LayoutInflater.from(ctx).inflate(R.layout.item_remote_trigger_card, historyContainer, false);
        
        // Hide unused buttons in history list
        item.findViewById(R.id.btn_explore).setVisibility(View.GONE);
        item.findViewById(R.id.btn_reset).setVisibility(View.GONE);
        item.findViewById(R.id.btn_configure).setVisibility(View.GONE);
        item.findViewById(R.id.status_dot).setVisibility(View.GONE);
        item.findViewById(R.id.active_status_text).setVisibility(View.GONE);
        item.findViewById(R.id.perm_status_icon).setVisibility(View.GONE);
        
        TextView title = item.findViewById(R.id.trigger_title);
        TextView desc = item.findViewById(R.id.trigger_description);
        TextView icon = item.findViewById(R.id.trigger_icon);
        TextView status = item.findViewById(R.id.permission_status_text);
        TextView time = item.findViewById(R.id.metric_delay);
        item.findViewById(R.id.metric_runs).setVisibility(View.GONE);

        String titleStr = snap.child("title").getValue(String.class);
        String msgStr = snap.child("message").getValue(String.class);
        Object tsObj = snap.child("timestamp").getValue();
        String deliveryStatus = snap.child("status").getValue(String.class);

        icon.setText("📧");
        title.setText(titleStr != null && !titleStr.isEmpty() ? titleStr : "System Alert");
        desc.setText(msgStr != null ? msgStr : "No message body");
        
        if (deliveryStatus != null) {
            status.setText("✓ " + deliveryStatus);
            status.setTextColor(ctx.getColor(R.color.spending_credit));
        }
        
        if (tsObj instanceof Number) {
            long ts = ((Number) tsObj).longValue();
            String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date(ts));
            time.setText("🕒 Sent: " + date);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, (int)(12 * ctx.getResources().getDisplayMetrics().density));
        item.setLayoutParams(params);

        historyContainer.addView(item);
    }

    private void sendNotification() {
        String title = titleInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUtils.sendRemoteNotificationCommand(title, message);
        Toast.makeText(getContext(), "Notification command sent!", Toast.LENGTH_SHORT).show();
        
        // Optional: clear inputs
        titleInput.setText("");
        messageInput.setText("");
    }
}
