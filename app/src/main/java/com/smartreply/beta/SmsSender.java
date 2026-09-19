package com.smartreply.beta;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;

public final class SmsSender {
    private static final String PREFS = "smart_reply_settings";

    private SmsSender() {}

    public static void send(Context context, String number, String message, String source) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int subscriptionId = prefs.getInt(
                "subscription_id", SubscriptionManager.getDefaultSmsSubscriptionId());
        SmsManager manager = subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? SmsManager.getDefault()
                : SmsManager.getSmsManagerForSubscriptionId(subscriptionId);

        ArrayList<String> parts = manager.divideMessage(message);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        int requestBase = (int) (System.currentTimeMillis() & 0x7fffffff);
        for (int i = 0; i < parts.size(); i++) {
            Intent statusIntent = new Intent(context, SmsStatusReceiver.class)
                    .setAction(SmsStatusReceiver.ACTION_SMS_SENT)
                    .putExtra("number", number)
                    .putExtra("source", source)
                    .putExtra("part", i + 1)
                    .putExtra("parts", parts.size());
            sentIntents.add(PendingIntent.getBroadcast(
                    context,
                    requestBase + i,
                    statusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }

        if (parts.size() > 1) {
            manager.sendMultipartTextMessage(number, null, parts, sentIntents, null);
        } else {
            manager.sendTextMessage(number, null, message, sentIntents.get(0), null);
        }
    }
}
