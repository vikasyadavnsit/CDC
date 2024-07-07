package com.vikasyadavnsit.cdc.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.utils.ActionUtils;
import com.vikasyadavnsit.cdc.utils.CommonUtil;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    PermissionHandler permissionHandler;

    private Handler handler = new Handler();
    private boolean isLongPress = false;
    private boolean isCounting = false;
    private int pressCount = 0;
    private boolean flag = false;
    private CountDownTimer countDownTimer;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            isLongPress = true;
            pressCount++;
            // Check if the pressCount reaches 3 within 30 seconds
            if (pressCount == 3) {
                flag = true;
                Toast.makeText(MainActivity.this, "Flag set to true", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(MainActivity.this,"Press count: " + pressCount, Toast.LENGTH_SHORT).show();
            // If the timer is not running, start the 30-second timer
            if (!isCounting) {
                startCountDown();
            }
        }
    };

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

        // ActionUtils.startMediaProjectionService(this);
        //Todo: GLobal Exception Handler to prevent app from crashing
        // Automatically start service post restart or shutdown

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//        }


        findViewById(R.id.main_navigation_request_home_button).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isLongPress = false;
                        handler.postDelayed(runnable, 5000); // 5 seconds
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isLongPress) {
                            handler.removeCallbacks(runnable);
                        }
                        return true;
                }
                return false;
            }
        });

        CommonUtil.loadFragment(getSupportFragmentManager(), new HomeFragment());


        ActionUtils.handleButtonPress(this, R.id.main_navigation_request_home_button,
                R.id.main_navigation_request_settings_button, R.id.main_navigation_request_play_button);
    }


    private void startCountDown() {
        isCounting = true;
        countDownTimer = new CountDownTimer(30000, 1000) { // 30 seconds countdown
            @Override
            public void onTick(long millisUntilFinished) {
                // No action needed on each tick
            }

            @Override
            public void onFinish() {
                isCounting = false;
                if (pressCount < 3) {
                    flag = false;
                    Toast.makeText(MainActivity.this, "Flag set to false", Toast.LENGTH_SHORT).show();
                }
                pressCount = 0; // Reset press count after 30 seconds
            }
        };
        countDownTimer.start();
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