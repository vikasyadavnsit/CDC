package com.vikasyadavnsit.cdc.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.TextView;

import com.vikasyadavnsit.cdc.R;

public class MessageDialog {

    public static void showCustomDialog(Context context, String title, String message) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.message_dialog);

        TextView titleTextView = dialog.findViewById(R.id.titleTextView);
        TextView messageTextView = dialog.findViewById(R.id.messageTextView);

        titleTextView.setText(title);
        messageTextView.setText(message);

        dialog.show();
    }
}

