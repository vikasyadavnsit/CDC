package com.vikasyadavnsit.cdc.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.ApplicationInputActions;

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

    public static void multilineInputDialog(final Context context, ApplicationInputActions actions) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.input_dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.6f;
            dialog.getWindow().setAttributes(lp);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dialog.getWindow().setBackgroundBlurRadius(20);
            }
        }

        TextView titleTextView = dialog.findViewById(R.id.inputDialogTitleTextView);
        final EditText editText = dialog.findViewById(R.id.inputDialogEditText);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        Button submitButton = dialog.findViewById(R.id.inputDialogSubmitButton);

        titleTextView.setText(actions.getDescription());

        submitButton.setOnClickListener(v -> {
            String input = editText.getText().toString().trim();
            String error = actions.getValidator().apply(input);
            if (error != null) {
                editText.setError(error);
                editText.requestFocus();
            } else {
                actions.getBiConsumer().accept(context, input);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

}

