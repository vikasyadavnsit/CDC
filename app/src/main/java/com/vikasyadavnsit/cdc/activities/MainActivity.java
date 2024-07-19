package com.vikasyadavnsit.cdc.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CommonUtil;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    PermissionHandler permissionHandler;

    DatabaseReference myRef;

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
// set value once read from firebase when it is consumed with disabled flag only if it is allowed

        initaliser();

        CommonUtil.loadFragment(getSupportFragmentManager(), new HomeFragment());
        ActionUtils.handleButtonPress(this);

        //Read data with ValueEventListener
//        myRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // Get the data
//                Object obj = dataSnapshot.getValue(Object.class);
//                Log.d("sd", "Value is: " + obj);
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                Log.w("TAG", "Failed to read value.", databaseError.toException());
//            }
//        });

        // Write a message to the database
//        if (Objects.isNull(myRef)) {
//            myRef.setValue(User.builder().fullName("Vikas Simulator")
//                    .userDetails(Map.of("hi", "sd"))
//                    .build()).addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    Log.d("Firebase", "Data written successfully");
//                } else {
//                    Log.w("Firebase", "Data write failed", task.getException());
//                }
//            });
//        }

        //myRef.push();

//        // Reading data
//        myRef.get().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                Object value = task.getResult().getValue(Object.class);
//                Log.d("Firebase", "Read value: " + value);
//            } else {
//                Log.w("Firebase", "Failed to read value.", task.getException());
//            }
//        });
        FirebaseUtils.checkAndCreateUser(this);
        FirebaseUtils.getAppTriggerSettingsData(this);
    }

    private void initaliser() {
        // Initialize Firebase RealTimeDatabase
        FirebaseApp.initializeApp(this);
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
        ActionUtils.onActivityResult(this, requestCode, resultCode, data);
    }


    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.handlePermissionResult(this, requestCode, permissions, grantResults);
    }

}