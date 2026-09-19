package com.smartreply.beta;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MissedCallReplyWorker extends Worker {
    private static final String PREFS = "smart_reply_settings";

    public MissedCallReplyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) return Result.success();

        int delaySeconds = Math.max(0, Math.min(60, prefs.getInt("delay_seconds", 30)));
        if (delaySeconds > 0) {
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return Result.retry();
            }
        }

        String[] projection = {CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE};
        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " DESC")) {
            if (cursor == null || !cursor.moveToFirst()) return Result.success();

            String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
            long callDate = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
            int callType = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
            long lastProcessed = prefs.getLong("last_processed_call", 0L);
            if (callType != CallLog.Calls.MISSED_TYPE || callDate <= lastProcessed) return Result.success();
            prefs.edit().putLong("last_processed_call", callDate).apply();
            if (number == null || number.trim().isEmpty() || number.equals("-1")) return Result.success();

            String replyKey = "last_reply_" + Integer.toHexString(number.hashCode());
            long now = System.currentTimeMillis();
            int repeatMinutes = prefs.getInt("repeat_minutes", 0);
            if (repeatMinutes > 0
                    && now - prefs.getLong(replyKey, 0L) < repeatMinutes * 60L * 1000L) {
                return Result.success();
            }

            String message = prefs.getString("message", context.getString(R.string.default_message));
            SmsSender.send(context, number, message, "missed_call");

            prefs.edit()
                    .putLong(replyKey, now)
                    .putLong("last_sent_at", now)
                    .putString("last_sent_number", number)
                    .putString("last_error", "Waiting for carrier confirmation")
                    .apply();
            return Result.success();
        } catch (Exception error) {
            prefs.edit().putString("last_error", error.getClass().getSimpleName()
                    + ": " + String.valueOf(error.getMessage())).apply();
            return Result.failure();
        }
    }
}
