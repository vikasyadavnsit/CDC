package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    PermissionHandler permissionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Todo Automatically start service post restart or shutdown
        //Todo: set value once read from firebase when it is consumed with disabled flag only if it is allowed

        initaliser();

        CommonUtil.loadFragment(getSupportFragmentManager(), new HomeFragment());
        ActionUtils.handleButtonPress(this);

        FirebaseUtils.checkAndCreateUser();
        FirebaseUtils.getAppTriggerSettingsData();
    }

    private void initaliser() {
        FirebaseApp.initializeApp(this);
        ApplicationDataRepository.initialize(this);
        FirebaseUtils.initialize(this);
        DeviceDataRepository.initialize(this);
    }

    // Handle Activity Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActionUtils.onActivityResult(this, requestCode, resultCode, data);
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.handlePermissionResult(this, requestCode, permissions, grantResults);
    }

}