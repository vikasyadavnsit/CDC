package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository;
import com.vikasyadavnsit.cdc.fragment.DashboardFragment;
import com.vikasyadavnsit.cdc.fragment.ShayariFragment;
import com.vikasyadavnsit.cdc.fragment.MessageFragment;
import com.vikasyadavnsit.cdc.fragment.MonitorFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.services.CDCVpnService;
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

        applyWindowInsets();
        initialiser();
        setupBottomNavigation();

        FirebaseUtils.checkUserExistsAndInit(this);
    }

    private void applyWindowInsets() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                fragment = new DashboardFragment();
            } else if (id == R.id.nav_shayari) {
                fragment = new ShayariFragment();
            } else if (id == R.id.nav_message) {
                fragment = new MessageFragment();
            } else if (id == R.id.nav_settings) {
                fragment = new SettingsFragment();
            } else if (id == R.id.nav_monitor) {
                fragment = new MonitorFragment();
            }
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });

        bottomNav.setSelectedItemId(R.id.nav_settings);

        ActionUtils.setContext(this);
    }

    private void loadFragment(Fragment fragment) {
        CommonUtil.loadFragment(getSupportFragmentManager(), fragment);
    }

    private void initialiser() {
        ApplicationDataRepository.initialize(this);
        FirebaseUtils.initialize(this);
        DeviceDataRepository.initialize(this);
    }

    private void startVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 22);
        } else {
            onActivityResult(22, RESULT_OK, null);
        }
    }

    private void stopVpn() {
        Intent intent = new Intent(this, CDCVpnService.class);
        intent.putExtra("stop", true);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActionUtils.onActivityResult(this, requestCode, resultCode, data);
        if (requestCode == 22 && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, CDCVpnService.class);
            startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.handlePermissionResult(this, requestCode, permissions, grantResults);
    }
}
