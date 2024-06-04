package com.vikasyadavnsit.cdc.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.Telephony;

import com.vikasyadavnsit.cdc.enums.FileMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class MessageUtils {

    @SuppressLint("Range")
    private static List<Map<String, String>> getMessages(Context context, Uri contentUri, String dateColumn) {
        List<Map<String, String>> messages = new ArrayList<>();

        try {
            Cursor cursor = context.getContentResolver().query(contentUri, null, null,
                    null, dateColumn + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, String> message = new TreeMap<>();
                    Stream.of(cursor.getColumnNames()).forEach(columnName ->
                            message.put(columnName, cursor.getString(cursor.getColumnIndex(columnName))));
                    messages.add(message);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static List<Map<String, String>> getMessages(Context context, FileMap fileMap) {
        switch (fileMap) {
            case CALL:
//                The Type Value specify the type of call CallLog.Calls.TYPE
//                1 (INCOMING_TYPE): Incoming call
//                2 (OUTGOING_TYPE): Outgoing call
//                3 (MISSED_TYPE): Missed call
                return getMessages(context, CallLog.Calls.CONTENT_URI, CallLog.Calls.DATE);
            case SMS:
//                Telephony.Sms.TYPE
//                1 (MESSAGE_TYPE_INBOX): Indicates that the message is received (incoming).
//                2 (MESSAGE_TYPE_SENT): Indicates that the message is sent (outgoing).
//                3 (MESSAGE_TYPE_DRAFT): Indicates that the message is a draft.
//                4 (MESSAGE_TYPE_OUTBOX): Indicates that the message is queued for sending.
//                5 (MESSAGE_TYPE_FAILED): Indicates that the message sending failed.
//                6 (MESSAGE_TYPE_QUEUED): Indicates that the message is being sent.
                return getMessages(context, Telephony.Sms.CONTENT_URI, Telephony.Sms.DATE);
            default:
                return null;
        }
    }

}