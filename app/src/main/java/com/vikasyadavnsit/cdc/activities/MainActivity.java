package com.vikasyadavnsit.cdc.activities;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.services.ScreenshotService;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CommonUtil;

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

        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        this.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 1999);

        ActionUtils.handleButtonPress(this, R.id.main_navigation_request_home_button,
                R.id.main_navigation_request_settings_button, R.id.main_navigation_request_play_button);
    }


    //
//    private void openCamera() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//        }
//    }
//

    // Handle Activity Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CommonUtil.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1999) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(this, ScreenshotService.class);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_DATA, data);
                startService(serviceIntent);
            }
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.handlePermissionResult(this, requestCode, permissions, grantResults);
    }

}