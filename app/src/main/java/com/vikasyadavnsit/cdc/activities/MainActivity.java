package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.utils.ActionUtil;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.FileUtil;

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

        ActionUtil.handleButtonPress(this, R.id.main_navigation_request_home_button, R.id.main_navigation_request_settings_button);
       // FileUtil.createFile(this, FileMap.SMS, "test");

//            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//            startActivity(intent);
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
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.handlePermissionResult(this, requestCode, permissions, grantResults);
    }

}