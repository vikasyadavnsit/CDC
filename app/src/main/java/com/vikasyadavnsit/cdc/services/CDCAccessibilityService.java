package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.AccessibilityUtils.processNotificationEvent;
import static com.vikasyadavnsit.cdc.utils.AccessibilityUtils.processTextChangedEvent;
import static com.vikasyadavnsit.cdc.utils.AccessibilityUtils.processWindowStateMovement;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.vikasyadavnsit.cdc.utils.LoggerUtils;

import java.util.HashMap;
import java.util.Map;

public class CDCAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            // Handle accessibility events, such as keystrokes / text changed events
            if (isTextChangedEvent(event)) {
                processTextChangedEvent(event);
            } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                processWindowStateMovement(event.getPackageName());
            } else if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                processNotificationEvent(event);
            }
        } catch (Exception e) {
            LoggerUtils.e("MyAccessibilityService", "Exception in onAccessibilityEvent :: " + e.getMessage());
        }
    }

    @Override
    public void onServiceConnected() {
        LoggerUtils.d("MyAccessibilityService", "Service Connected");
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
        super.onDestroy();
    }

    private static boolean isTextChangedEvent(AccessibilityEvent event) {
        return event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
    }


}
