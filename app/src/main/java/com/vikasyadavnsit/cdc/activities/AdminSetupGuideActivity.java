package com.vikasyadavnsit.cdc.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.UserRole;

public class AdminSetupGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_setup_guide);

        // Force dark status bar appearance for dark background
        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        applyWindowInsets();
        setupStepsContent();
        setupCopyButtons();

        findViewById(R.id.btn_proceed_to_config).setOnClickListener(v -> {
            Intent intent = new Intent(this, FirebaseConfigActivity.class);
            intent.putExtra(FirebaseConfigActivity.EXTRA_USER_ROLE, UserRole.ADMIN);
            startActivity(intent);
            finish();
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.main_content_root);
        AppBarLayout appBar = findViewById(R.id.app_bar);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(0, systemBars.top, 0, 0);
            v.setPadding(0, 0, 0, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupStepsContent() {
        // Step 1
        setSubpoint(R.id.sub1_1, "Go to the <a href=\"https://console.firebase.google.com/\">Firebase Console</a>.");
        setSubpoint(R.id.sub1_2, "Click <b>'Add Project'</b> and follow the setup wizard.");
        setSubpoint(R.id.sub1_3, "<b>Critical:</b> You <b>MUST</b> use the package name provided below. If you use any other name, the configuration will fail.");
        setSubpoint(R.id.sub1_4, "<b>Warning:</b> During setup, <b>UNCHECK</b> 'Enable Google Analytics' for privacy.");

        setupCopyableField(R.id.field_project_name, "Suggested Project Name", "CDC-Infrastructure");
        setupCopyableField(R.id.field_package_name, "Required Package Name", "com.vikasyadavnsit.cdc");

        // Step 2
        setSubpoint(R.id.sub2_1, "Navigate to <b>Build > Realtime Database</b> and click <b>'Create Database'</b>.");
        setSubpoint(R.id.sub2_2, "<b>Warning:</b> If asked, do <b>NOT</b> opt for Gemini or other AI assistance.");
        setSubpoint(R.id.sub2_3, "Choose <b>'Start in Test Mode'</b> and click Enable.");

        setupCopyableField(R.id.field_db_name, "Suggested DB Name", "CDC-Data-Store");

        // Step 3
        setSubpoint(R.id.sub3_1, "Navigate to the <b>'Rules'</b> tab and replace everything with the code below:");
        setSubpoint(R.id.sub3_2, "Click <b>'Publish'</b> to save the changes.");

        // Step 4
        setSubpoint(R.id.sub4_1, "Go to <b>Project Settings</b> (gear icon next to Project Overview).");
        setSubpoint(R.id.sub4_2, "Download the <code>google-services.json</code> file from the 'Your Apps' section.");
        setSubpoint(R.id.sub4_3, "Keep this file ready; you'll paste its contents on the next screen.");
    }

    private void setSubpoint(int includeId, String htmlText) {
        View includeView = findViewById(includeId);
        if (includeView != null) {
            TextView tv = includeView.findViewById(R.id.tv_subpoint_text);
            if (tv != null) {
                tv.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT));
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private void setupCopyableField(int includeId, String label, String value) {
        View includeView = findViewById(includeId);
        if (includeView != null) {
            TextView tvLabel = includeView.findViewById(R.id.tv_field_label);
            TextView tvValue = includeView.findViewById(R.id.tv_field_value);
            View btnCopy = includeView.findViewById(R.id.btn_copy_field);

            if (tvLabel != null) tvLabel.setText(label);
            if (tvValue != null) tvValue.setText(value);
            if (btnCopy != null) {
                btnCopy.setOnClickListener(v -> copyToClipboard(label, value));
            }
        }
    }

    private void setupCopyButtons() {
        TextView tvRulesCode = findViewById(R.id.tv_rules_code);
        findViewById(R.id.btn_copy_rules).setOnClickListener(v -> {
            copyToClipboard("Firebase Rules", tvRulesCode.getText().toString());
        });
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copied", Toast.LENGTH_SHORT).show();
    }
}
