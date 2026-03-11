package com.vikasyadavnsit.cdc.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.InputFilter;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.ApplicationInputActions;
import com.vikasyadavnsit.cdc.filter.RegexInputFilter;

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

    public static void multilineInputDialog(final Context context, String title, ApplicationInputActions actions) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.input_dialog);

        TextView titleTextView = dialog.findViewById(R.id.inputDialogTitleTextView);
        final EditText editText = dialog.findViewById(R.id.inputDialogEditText);
        editText.setFilters(new InputFilter[]{new RegexInputFilter(), new InputFilter.LengthFilter(200)});
        Button submitButton = dialog.findViewById(R.id.inputDialogSubmitButton);

        // Set the title text
        titleTextView.setText(title);

        // Set up button click listener
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText = editText.getText().toString().trim();

                // Handle the input text
                if (!inputText.isEmpty()) {
                    actions.getBiConsumer().accept(context, inputText);
                    dialog.dismiss();
                } else {
                    // Handle empty input
                    Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

}

