package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.vikasyadavnsit.cdc.enums.FileMap;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;

public class CallUtils {

    //Todo: Just as done in POC, read the verification over the call and delete it immediately. To prevent the consumer for DDOS us.
    // Traces can be left if call was rejected or we have captured the logs.

    /**
     * Starts monitoring call state transitions using a default listener implementation.
     *
     * <p>This delegates to {@link #monitorCallState(Context, CallStateListener)} and uses a listener
     * that writes call state events to {@link FileMap#CALL_STATE} via {@link FileUtils}.</p>
     *
     * @param context Android {@link Context} used to obtain {@link TelephonyManager}.
     */
    public static void monitorCallState(Context context) {
        CallUtils.monitorCallState(context, new CallUtils.CallStateListenerImpl(context, FileMap.CALL_STATE));
    }

    /**
     * Starts monitoring call state transitions and routes state events to the provided listener.
     *
     * <p>Internally this registers a {@link PhoneStateListener} with {@link TelephonyManager} and
     * maps {@link TelephonyManager#CALL_STATE_RINGING}, {@link TelephonyManager#CALL_STATE_OFFHOOK},
     * and {@link TelephonyManager#CALL_STATE_IDLE} to higher-level callbacks (incoming/outgoing,
     * answered/ended, conference/multiple incoming detection).</p>
     *
     * @param context  Android {@link Context} used to obtain {@link TelephonyManager}.
     * @param listener Callback interface that receives derived call-state events.
     */
    public static void monitorCallState(Context context, final CallStateListener listener) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            LoggerUtils.d("CallUtils", "Error in monitoring call state");
        }
    }

    private interface CallStateListener {
        /**
         * Called when an incoming call transitions to the answered state.
         *
         * @param phoneNumber Incoming phone number as provided by {@link TelephonyManager}.
         */
        default void onIncomingCallAnswered(String phoneNumber) {

            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Incoming call answered: " + phoneNumber);
        }

        /**
         * Called when an outgoing call is initiated.
         *
         * @param phoneNumber Outgoing phone number as provided by {@link TelephonyManager}.
         */
        default void onOutgoingCallStarted(String phoneNumber) {

            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Outgoing call started: " + phoneNumber);
        }

        /**
         * Called when an outgoing call is answered.
         *
         * @param phoneNumber Outgoing phone number as provided by {@link TelephonyManager}.
         */
        default void onOutgoingCallAnswered(String phoneNumber) {

            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Outgoing call answered: " + phoneNumber);
        }

        /**
         * Called when the device reports a ringing incoming call.
         *
         * @param phoneNumber Incoming phone number as provided by {@link TelephonyManager}.
         */
        default void onIncomingCallRinging(String phoneNumber) {

            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Incoming call ringing: " + phoneNumber);
        }

        /**
         * Called when an incoming call ends (device returns to idle state).
         *
         * @param phoneNumber Incoming phone number as provided by {@link TelephonyManager}.
         */
        default void onIncomingCallEnded(String phoneNumber) {

            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Incoming call ended: " + phoneNumber);
        }

        /**
         * Called when an outgoing call ends (device returns to idle state).
         *
         * @param phoneNumber Outgoing phone number as provided by {@link TelephonyManager}.
         */
        default void onOutgoingCallEnded(String phoneNumber) {

            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Outgoing call ended: " + phoneNumber);
        }

        /**
         * Called when multiple simultaneous calls are detected (conference-like scenario).
         *
         * @param phoneNumber Phone number associated with the latest state transition.
         */
        default void onConferenceCall(String phoneNumber) {

            FileUtils.appendDataToFile(FileMap.CALL_STATE, "Conference call detected with number: " + phoneNumber);
        }

        /**
         * Called when a second incoming call is detected while another call is ongoing.
         *
         * @param phoneNumber Incoming phone number as provided by {@link TelephonyManager}.
         */
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
