package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.services.ScreenCaptureService;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CommonUtil;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    PermissionHandler permissionHandler;
    private MediaProjectionManager mMediaProjectionManager;

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

        // new PermissionManager().requestAllPermissions(this);

//        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
//        findViewById(R.id.main_navigation_request_home_button).setOnClickListener(view -> {
//            onCaptureButtonClick(view);
//        });
        ActionUtils.handleButtonPress(this, R.id.main_navigation_request_home_button, R.id.main_navigation_request_settings_button);
    }


    //
//    private void openCamera() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//        }
//    }
//

    // Method to start screen capture service
    private void startScreenCaptureService(int resultCode, Intent data) {
        Intent intent = new Intent(this, ScreenCaptureService.class);
        intent.putExtra("resultCode", resultCode);
        intent.putExtra("data", data);
        startService(intent);
    }

    // Method to request screen capture permission
    private void requestScreenCapturePermission() {
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), 1122);
    }


    // Method to capture screen on button click
    public void onCaptureButtonClick(View view) {
        requestScreenCapturePermission();
    }

    // Handle Activity Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CommonUtil.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 1122 && resultCode == RESULT_OK) {
//            startScreenCaptureService(resultCode, data);
//        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.handlePermissionResult(this, requestCode, permissions, grantResults);
    }

}