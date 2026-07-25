package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.UserRole;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        findViewById(R.id.card_admin).setOnClickListener(v -> startAdminGuide());
        findViewById(R.id.btn_select_admin).setOnClickListener(v -> startAdminGuide());
        
        findViewById(R.id.card_client).setOnClickListener(v -> startClientConfig());
        findViewById(R.id.btn_select_client).setOnClickListener(v -> startClientConfig());
    }

    private void startAdminGuide() {
        Intent intent = new Intent(this, AdminSetupGuideActivity.class);
        startActivity(intent);
    }

    private void startClientConfig() {
        Intent intent = new Intent(this, FirebaseConfigActivity.class);
        intent.putExtra(FirebaseConfigActivity.EXTRA_USER_ROLE, UserRole.CLIENT);
        startActivity(intent);
    }
}
