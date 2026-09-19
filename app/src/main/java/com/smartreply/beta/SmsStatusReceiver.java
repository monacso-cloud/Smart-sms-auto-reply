package com.smartreply.beta;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

public class SmsStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_SMS_SENT = "com.smartreply.beta.SMS_SENT";
    private static final String PREFS = "smart_reply_settings";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_SMS_SENT.equals(intent.getAction())) return;

        String number = intent.getStringExtra("number");
        String source = intent.getStringExtra("source");
        int part = intent.getIntExtra("part", 1);
        int parts = intent.getIntExtra("parts", 1);
        String result = describeResult(getResultCode());
        long now = System.currentTimeMillis();

        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong("last_attempt_at", now)
                .putString("last_attempt_number", number == null ? "" : number)
                .putString("last_attempt_source", source == null ? "unknown" : source);

        if (getResultCode() == Activity.RESULT_OK) {
            editor.putString("last_error", "")
                    .putString("last_sms_status", parts > 1
                            ? "Sent part " + part + " of " + parts
                            : "Sent successfully");
        } else {
            editor.putString("last_error", result)
                    .putString("last_sms_status", "Failed: " + result);
        }
        editor.apply();
    }

    private String describeResult(int code) {
        switch (code) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                return "Carrier rejected the SMS or an unknown modem error occurred";
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                return "No mobile network service";
            case SmsManager.RESULT_ERROR_NULL_PDU:
                return "Invalid SMS data";
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                return "Mobile radio is off";
            default:
                return "SMS error code " + code;
        }
    }
}
