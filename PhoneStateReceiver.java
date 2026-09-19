package com.smartreply.beta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhoneStateReceiver extends BroadcastReceiver {
    private static final String PREFS = "smart_reply_settings";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (!TelephonyManager.EXTRA_STATE_IDLE.equals(state)) return;

        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                Thread.sleep(1800L);
                processLatestMissedCall(appContext);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void processLatestMissedCall(Context context) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) return;

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE
        };

        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC")) {

            if (cursor == null || !cursor.moveToFirst()) return;

            String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
            long callDate = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
            int callType = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
            long lastProcessed = prefs.getLong("last_processed_call", 0L);

            if (callType != CallLog.Calls.MISSED_TYPE || callDate <= lastProcessed) return;
            prefs.edit().putLong("last_processed_call", callDate).apply();

            if (number == null || number.trim().isEmpty() || number.equals("-1")) return;

            String replyKey = "last_reply_" + Integer.toHexString(number.hashCode());
            long lastReply = prefs.getLong(replyKey, 0L);
            long now = System.currentTimeMillis();
            if (now - lastReply < 24L * 60L * 60L * 1000L) return;

            int delaySeconds = prefs.getInt("delay_seconds", 30);
            if (delaySeconds > 0) Thread.sleep(delaySeconds * 1000L);

            String message = prefs.getString("message", context.getString(R.string.default_message));
            int subscriptionId = prefs.getInt(
                    "subscription_id", SubscriptionManager.getDefaultSmsSubscriptionId());

            SmsManager smsManager = subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                    ? SmsManager.getDefault()
                    : SmsManager.getSmsManagerForSubscriptionId(subscriptionId);

            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(number, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(number, null, message, null, null);
            }

            prefs.edit()
                    .putLong(replyKey, now)
                    .putLong("last_sent_at", now)
                    .putString("last_sent_number", number)
                    .apply();
        } catch (Exception ignored) {
            // A later version will expose detailed error logs in the dashboard.
        }
    }
}
