package com.smartreply.beta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;
import java.util.Locale;

public class SmsReplyReceiver extends BroadcastReceiver {
    private static final String PREFS = "smart_reply_settings";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!ReplyPolicy.shouldReply(context, "sms")) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) return;

        String format = bundle.getString("format");
        String sender = null;
        StringBuilder body = new StringBuilder();
        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            if (sms == null) continue;
            if (sender == null) sender = sms.getDisplayOriginatingAddress();
            body.append(sms.getDisplayMessageBody());
        }
        if (sender == null || body.length() == 0) return;

        String key = Integer.toHexString(sender.hashCode());
        String normalized = body.toString().trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("stop") || normalized.equals("unsubscribe")) {
            prefs.edit().putBoolean("chat_opt_out_" + key, true).apply();
            send(context, prefs, sender, "You have been unsubscribed from automatic SMS replies. Text START to enable them again.");
            return;
        }
        if (normalized.equals("start")) {
            prefs.edit().remove("chat_opt_out_" + key).apply();
            send(context, prefs, sender, "Automatic SMS replies are active again. How can we help?");
            return;
        }
        if (prefs.getBoolean("chat_opt_out_" + key, false)) return;

        long now = System.currentTimeMillis();
        long last = prefs.getLong("chat_last_" + key, 0L);
        if (now - last < 30_000L) return;

        String reply = findMenuReply(normalized, prefs);
        if (reply == null) reply = findReply(normalized, prefs.getString("chatbot_rules", ""));
        if (reply == null || reply.trim().isEmpty()) {
            reply = prefs.getString("chatbot_fallback", context.getString(R.string.default_chatbot_fallback));
        }
        if (reply == null || reply.trim().isEmpty()) return;

        send(context, prefs, sender, reply.trim());
        prefs.edit()
                .putLong("chat_last_" + key, now)
                .putLong("last_sent_at", now)
                .putString("last_sent_number", sender)
                .apply();
    }

    private String findMenuReply(String incoming, SharedPreferences prefs) {
        if (!prefs.getBoolean("menu_enabled", false)) return null;
        if (!incoming.matches("[1-4]")) return null;
        String reply = prefs.getString("menu_reply_" + incoming, "").trim();
        return reply.isEmpty() ? null : reply;
    }

    private String findReply(String incoming, String rules) {
        for (String line : rules.split("\\r?\\n")) {
            int separator = line.indexOf("=>");
            if (separator <= 0) continue;
            String keywords = line.substring(0, separator);
            String reply = line.substring(separator + 2).trim();
            for (String keyword : keywords.split(",")) {
                String clean = keyword.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty() && incoming.contains(clean)) return reply;
            }
        }
        return null;
    }

    private void send(Context context, SharedPreferences prefs, String number, String message) {
        int subscriptionId = prefs.getInt(
                "subscription_id", SubscriptionManager.getDefaultSmsSubscriptionId());
        SmsManager manager = subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? SmsManager.getDefault()
                : SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
        ArrayList<String> parts = manager.divideMessage(message);
        if (parts.size() > 1) manager.sendMultipartTextMessage(number, null, parts, null, null);
        else manager.sendTextMessage(number, null, message, null, null);
    }
}
