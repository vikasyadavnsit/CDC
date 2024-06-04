package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.enums.LoggingLevel;

public class CallUtils {


    private interface CallStateListener {
        default void onIncomingCallAnswered(String phoneNumber) {
            LoggerUtil.log("CallUtils", "Incoming call answered: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onOutgoingCallStarted(String phoneNumber) {
            LoggerUtil.log("CallUtils", "Outgoing call answered: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onIncomingCallRinging(String phoneNumber) {
            LoggerUtil.log("CallUtils", "Incoming call ringing: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onIncomingCallEnded(String phoneNumber) {
            LoggerUtil.log("CallUtils", "Incoming call ended: " + phoneNumber, LoggingLevel.DEBUG);
        }

        default void onOutgoingCallEnded(String phoneNumber) {
            LoggerUtil.log("CallUtils", "Outgoing call ended: " + phoneNumber, LoggingLevel.DEBUG);
        }
    }

    public static void monitorCallState(Context context, FileMap fileMap) {
        CallUtils.monitorCallState(context, new CallUtils.CallStateListener() {
        });
    }


    public static void monitorCallState(Context context, final CallStateListener listener) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return;
        }

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            private boolean isIncoming;
            private String incomingNumber;

            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);

                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        isIncoming = true;
                        incomingNumber = phoneNumber;
                        listener.onIncomingCallRinging(phoneNumber);
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (isIncoming) {
                            // Incoming call answered
                            listener.onIncomingCallAnswered(incomingNumber);
                        } else {
                            // Outgoing call started
                            listener.onOutgoingCallStarted(phoneNumber);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isIncoming) {
                            listener.onIncomingCallEnded(phoneNumber);
                        } else {
                            listener.onOutgoingCallEnded(phoneNumber);
                        }
                        isIncoming = false;
                        incomingNumber = null;
                        break;
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
}
