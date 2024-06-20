package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.vikasyadavnsit.cdc.enums.FileMap;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;

public class CallUtils {

    public static void monitorCallState(Context context) {
        CallUtils.monitorCallState(context, new CallUtils.CallStateListenerImpl(context, FileMap.CALL_STATE));
    }

    public static void monitorCallState(Context context, final CallStateListener listener) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return;
        }

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            private final Set<String> ongoingCalls = new HashSet<>();
            private boolean isIncoming;
            private String incomingNumber;
            private String outgoingNumber;
            private boolean isOutgoingCallStarted;

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

    private interface CallStateListener {
        default void onIncomingCallAnswered(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Incoming call answered: " + phoneNumber);
        }

        default void onOutgoingCallStarted(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Outgoing call started: " + phoneNumber);
        }

        default void onOutgoingCallAnswered(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Outgoing call answered: " + phoneNumber);
        }

        default void onIncomingCallRinging(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Incoming call ringing: " + phoneNumber);
        }

        default void onIncomingCallEnded(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Incoming call ended: " + phoneNumber);
        }

        default void onOutgoingCallEnded(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Outgoing call ended: " + phoneNumber);
        }

        default void onConferenceCall(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Conference call detected with number: " + phoneNumber);
        }

        default void onMultipleIncomingCalls(String phoneNumber) {
            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Multiple incoming calls detected: " + phoneNumber);
        }
    }

    @AllArgsConstructor
    private static class CallStateListenerImpl implements CallStateListener {
        private Context context;
        private FileMap fileMap;
    }
}
