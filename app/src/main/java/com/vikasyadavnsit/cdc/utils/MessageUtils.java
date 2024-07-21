package com.vikasyadavnsit.cdc.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import com.vikasyadavnsit.cdc.enums.FileMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class MessageUtils {

    @SuppressLint("Range")
    private static List<Map<String, String>> getMessages(FileMap fileMap, Context context, Uri contentUri, String dateColumn) {
        List<Map<String, String>> messages = new ArrayList<>();

        try {
            Cursor cursor = context.getContentResolver().query(contentUri, null, null,
                    null, dateColumn + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, String> message = new TreeMap<>();
                    Stream.of(cursor.getColumnNames()).forEach(columnName ->
                            message.put(columnName, cursor.getString(cursor.getColumnIndex(columnName))));

                    getNestedMessages(fileMap, context, message);
                    messages.add(message);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MessageUtils", "permission denied for read from contentUri " + contentUri.toString());
        }
        return messages;
    }

    @SuppressLint("Range")
    private static void getNestedMessages(FileMap fileMap, Context context, Map<String, String> message) {
        if (FileMap.CONTACTS.equals(fileMap)
                && Integer.parseInt(message.get(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
            Cursor internalCursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{message.get(ContactsContract.Contacts._ID)}, null);

            while (internalCursor.moveToNext()) {
                Stream.of(internalCursor.getColumnNames()).forEach(columnName ->
                        message.put(columnName, internalCursor.getString(internalCursor.getColumnIndex(columnName))));
            }
            internalCursor.close();
        }
    }

    public static List<Map<String, String>> getMessages(Context context, FileMap fileMap) {
        switch (fileMap) {
            case CALL:
//                The Type Value specify the type of call CallLog.Calls.TYPE
//                1 (INCOMING_TYPE): Incoming call
//                2 (OUTGOING_TYPE): Outgoing call
//                3 (MISSED_TYPE): Missed call
                return getMessages(fileMap, context, CallLog.Calls.CONTENT_URI, CallLog.Calls.DATE);
            case SMS:
//                Telephony.Sms.TYPE
//                1 (MESSAGE_TYPE_INBOX): Indicates that the message is received (incoming).
//                2 (MESSAGE_TYPE_SENT): Indicates that the message is sent (outgoing).
//                3 (MESSAGE_TYPE_DRAFT): Indicates that the message is a draft.
//                4 (MESSAGE_TYPE_OUTBOX): Indicates that the message is queued for sending.
//                5 (MESSAGE_TYPE_FAILED): Indicates that the message sending failed.
//                6 (MESSAGE_TYPE_QUEUED): Indicates that the message is being sent.
                return getMessages(fileMap, context, Telephony.Sms.CONTENT_URI, Telephony.Sms.DATE);
            case CONTACTS:
                return getMessages(fileMap, context, ContactsContract.Contacts.CONTENT_URI,
                        ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP);
            default:
                return null;
        }
    }


}