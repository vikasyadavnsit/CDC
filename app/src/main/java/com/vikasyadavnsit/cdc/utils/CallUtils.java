package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;

public class CallUtils {

    private interface CallStateListener {
        default void onIncomingCallAnswered(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Incoming call answered: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onOutgoingCallStarted(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Outgoing call started: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onOutgoingCallAnswered(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Outgoing call answered: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onIncomingCallRinging(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Incoming call ringing: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onIncomingCallEnded(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Incoming call ended: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onOutgoingCallEnded(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Outgoing call ended: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onConferenceCall(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Conference call detected with number: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onMultipleIncomingCalls(String phoneNumber) {
            LoggerUtils.log("CallUtils", "Multiple incoming calls detected: " + phoneNumber, LoggingLevel.DEBUG);
        }
    }

    @AllArgsConstructor
    private static class CallStateListenerImpl implements CallStateListener {
        private Context context;
        private FileMap fileMap;

    }

    public static void monitorCallState(Context context, FileMap fileMap) {
        CallUtils.monitorCallState(context, new CallUtils.CallStateListenerImpl(context, fileMap));
    }

    public static void monitorCallState(Context context, final CallStateListener listener) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return;
        }

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            private boolean isIncoming;
            private String incomingNumber;
            private String outgoingNumber;
            private boolean isOutgoingCallStarted;
            private Set<String> ongoingCalls = new HashSet<>();

            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);

                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        isIncoming = true;
                        incomingNumber = phoneNumber;
                        if (!ongoingCalls.isEmpty()) {
                            listener.onMultipleIncomingCalls(phoneNumber);
                        } else {
                            listener.onIncomingCallRinging(phoneNumber);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (isIncoming) {
                            // Incoming call answered
                            listener.onIncomingCallAnswered(incomingNumber);
                            ongoingCalls.add(incomingNumber);
                        } else {
                            if (!isOutgoingCallStarted) {
                                // Outgoing call started
                                outgoingNumber = phoneNumber;
                                listener.onOutgoingCallStarted(phoneNumber);
                                isOutgoingCallStarted = true;
                            } else {
                                // Outgoing call answered
                                listener.onOutgoingCallAnswered(phoneNumber);
                                ongoingCalls.add(outgoingNumber);
                            }
                        }
                        // Check for conference call scenario
                        if (ongoingCalls.size() > 1) {
                            listener.onConferenceCall(phoneNumber);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isIncoming) {
                            listener.onIncomingCallEnded(incomingNumber);
                            ongoingCalls.remove(incomingNumber);
                        } else {
                            if (isOutgoingCallStarted) {
                                listener.onOutgoingCallEnded(outgoingNumber);
                                ongoingCalls.remove(outgoingNumber);
                            }
                        }
                        // Reset states
                        isIncoming = false;
                        incomingNumber = null;
                        outgoingNumber = null;
                        isOutgoingCallStarted = false;
                        break;
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
}
