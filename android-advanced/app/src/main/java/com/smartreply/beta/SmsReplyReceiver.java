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

        Bundle bundle = intent.getExtras();
        int subscriptionId = SimRouter.smsSubscription(bundle);
        if (!BusinessProfiles.exists(context, subscriptionId) || !SimRouter.active(context, subscriptionId)) {
            ReplySender.issue(context, "SMS skipped: receiving SIM could not be identified, is inactive, or has no profile.");
            return;
        }
        SharedPreferences prefs = BusinessProfiles.prefs(context, subscriptionId);
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

        handleMessage(context, subscriptionId, sender, body.toString());
    }

    void handleMessage(Context context, int subscriptionId, String sender, String body) {
        if (!BusinessProfiles.exists(context, subscriptionId) || !SimRouter.active(context, subscriptionId)) return;
        SharedPreferences prefs = BusinessProfiles.prefs(context, subscriptionId);
        String key = Integer.toHexString(sender.hashCode());
        String normalized = body.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("stop") || normalized.equals("unsubscribe")) {
            prefs.edit().putBoolean("chat_opt_out_" + key, true).apply();
            if (ReplyPolicy.shouldReply(prefs, "sms")) ReplySender.send(context, subscriptionId, sender, "You have been unsubscribed from automatic SMS replies. Text START to enable them again.", "SMS");
            return;
        }
        if (normalized.equals("start")) {
            prefs.edit().remove("chat_opt_out_" + key).apply();
            if (ReplyPolicy.shouldReply(prefs, "sms")) ReplySender.send(context, subscriptionId, sender, "Automatic SMS replies are active again. How can we help?", "SMS");
            return;
        }
        if (prefs.getBoolean("chat_opt_out_" + key, false)) return;
        if (!ReplyPolicy.shouldReply(prefs, "sms")) return;

        long now = System.currentTimeMillis();
        long last = prefs.getLong("chat_last_" + key, 0L);
        if (now - last < 30_000L) return;

        String reply;
        if (prefs.getBoolean("sms_chatbot_mode", prefs.getBoolean("chatbot_enabled", false))) {
            reply = findMenuReply(normalized, prefs);
            if (reply == null) reply = findReply(normalized, prefs.getString("chatbot_rules", ""));
            if (reply == null || reply.trim().isEmpty()) {
                reply = prefs.getString("chatbot_fallback", context.getString(R.string.default_chatbot_fallback));
            }
        } else {
            reply = prefs.getString("sms_reply_message", context.getString(R.string.default_plain_sms_reply));
        }
        if (reply == null || reply.trim().isEmpty()) return;

        if (!ReplySender.send(context, subscriptionId, sender, reply.trim(), "SMS")) return;
        prefs.edit()
                .putLong("chat_last_" + key, now)
                .putLong("last_sent_at", now)
                .putString("last_sent_number", sender)
                .apply();
    }

    private String findMenuReply(String incoming, SharedPreferences prefs) {
        if (!prefs.getBoolean("menu_enabled", false)) return null;
        if (!incoming.matches("(10|[1-9])")) return null;
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

}
