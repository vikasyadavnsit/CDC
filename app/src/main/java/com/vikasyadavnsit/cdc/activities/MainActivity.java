package com.vikasyadavnsit.cdc.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.repositories.MyRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_SMS_PERMISSION = 123;
    private static final int REQUEST_ALL_PERMISSIONS = 1000;

    @Inject
    MyRepository myRepository;

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

        myRepository.doSomething();

        // Request camera permission
        Button openCameraButton = findViewById(R.id.request_permission_button);
        openCameraButton.setOnClickListener(view -> {
//            if (checkCameraPermission()) {
//                openCamera();
//            } else {
//                requestCameraPermission();
//            }


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request it

                String[] permissions = {
                        Manifest.permission.READ_SMS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_SMS
                        // Add more permissions as needed
                };
                Toast.makeText(this, "Taking all permissions", Toast.LENGTH_SHORT).show();

                ActivityCompat.requestPermissions(this, permissions,
                        REQUEST_ALL_PERMISSIONS);
            } else {
                // Permission already granted, proceed with reading SMS
                // readSmsMessages();
                Toast.makeText(this, "All Permission already granted", Toast.LENGTH_SHORT).show();
            }


            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);

        });


    }


    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with reading SMS
                readSmsMessages();
            } else {
                // Permission denied, show a message or take appropriate action
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with reading SMS
                Toast.makeText(this, "All Permission   granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, show a message or take appropriate action
                Toast.makeText(this, "ALL permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void readSmsMessages() {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String[] columnNames = cursor.getColumnNames();
                String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                // Process the SMS message
                Log.d("SMS", "Sender: " + sender + ", Message: " + message);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Do something with the image captured by the camera (if needed)
            Bundle extras = data.getExtras();
            // For example, you can display the captured image in an ImageView
            // Bitmap imageBitmap = (Bitmap) extras.get("data");
            // imageView.setImageBitmap(imageBitmap);
        }
    }
}