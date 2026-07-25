package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.ApplicationInputActions;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

public class WelcomeActivity extends AppCompatActivity {

    private EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.vikasyadavnsit.cdc.utils.LoggerUtils.d("WelcomeActivity", "onCreate: User setup started");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        etName = findViewById(R.id.et_user_name);

        etName.setOnEditorActionListener((v, actionId, event) -> {
            findViewById(R.id.btn_get_started).performClick();
            return true;
        });

        findViewById(R.id.btn_get_started).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String error = ApplicationInputActions.FIREBASE_CREATE_USER.getValidator().apply(name);
            
            if (error != null) {
                etName.setError(error);
                etName.requestFocus();
            } else {
                ApplicationInputActions.FIREBASE_CREATE_USER.getBiConsumer().accept(this, name);
                FirebaseUtils.initializeServices(this);
                Toast.makeText(this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();
                
                // Restart MainActivity to enter the normal app flow
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Make it non-closable via back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing to prevent closing
                Toast.makeText(WelcomeActivity.this, "Please enter your name to continue", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
