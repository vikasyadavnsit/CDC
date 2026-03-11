package com.vikasyadavnsit.cdc.activities;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.vikasyadavnsit.cdc.R;

public class PasswordActivity extends Activity {

    private static final String CORRECT_PASSWORD = "1234"; // Replace with your password logic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText passwordInput = findViewById(R.id.et_password);
                String inputPassword = passwordInput.getText().toString();

                if (inputPassword.equals(CORRECT_PASSWORD)) {
                    Toast.makeText(PasswordActivity.this, "Access granted!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                } else {
                    Toast.makeText(PasswordActivity.this, "Incorrect password!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

