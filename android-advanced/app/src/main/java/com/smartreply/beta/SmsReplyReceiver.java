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
        Diagnostics.record(context,-1,"SMS_RECEIVED","Android SMS broadcast received");
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Diagnostics.record(context,-1,"SKIPPED","SEND_SMS permission missing"); return;
        }

        Bundle bundle = intent.getExtras();
        int subscriptionId = SimRouter.smsSubscription(context, bundle);
        if (!BusinessProfiles.exists(context, subscriptionId) || !SimRouter.active(context, subscriptionId)) {
            ReplySender.issue(context, "SMS skipped: receiving SIM could not be identified, is inactive, or has no profile.");
            return;
        }
        SharedPreferences prefs = BusinessProfiles.prefs(context, subscriptionId);
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) { Diagnostics.record(context,subscriptionId,"SKIPPED","SMS has no PDU data"); return; }

        String sender=null;
        StringBuilder body=new StringBuilder();
        try {
            SmsMessage[] messages=android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if(messages!=null) for(SmsMessage sms:messages) {
                if(sms==null)continue;
                if(sender==null)sender=sms.getDisplayOriginatingAddress();
                if(sms.getDisplayMessageBody()!=null)body.append(sms.getDisplayMessageBody());
            }
        } catch(RuntimeException e) {
            Diagnostics.record(context,subscriptionId,"SKIPPED","Unable to decode incoming SMS: "+e.getClass().getSimpleName());return;
        }
        if(sender==null || body.length()==0){Diagnostics.record(context,subscriptionId,"SKIPPED","Empty or undecodable incoming SMS");return;}

        handleMessage(context, subscriptionId, sender, body.toString());
    }

    void handleMessage(Context context, int subscriptionId, String sender, String body) {
        if (!BusinessProfiles.exists(context, subscriptionId) || !SimRouter.active(context, subscriptionId)) return;
        SharedPreferences prefs = BusinessProfiles.prefs(context, subscriptionId);
        String key = Integer.toHexString(sender.hashCode());
        String normalizedKey=Integer.toHexString(RecipientRules.normal(prefs,sender).hashCode());
        String normalized = body.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("stop") || normalized.equals("unsubscribe")) {
            prefs.edit().putBoolean("chat_opt_out_" + key, true).putBoolean("chat_opt_out_"+normalizedKey,true).apply();
            if (ReplyPolicy.shouldReply(prefs, "sms")) ReplySender.send(context, subscriptionId, sender, "You have been unsubscribed from automatic SMS replies. Text START to enable them again.", "SMS");
            return;
        }
        if (normalized.equals("start")) {
            prefs.edit().remove("chat_opt_out_" + key).remove("chat_opt_out_"+normalizedKey).apply();
            if (ReplyPolicy.shouldReply(prefs, "sms")) ReplySender.send(context, subscriptionId, sender, "Automatic SMS replies are active again. How can we help?", "SMS");
            return;
        }
        if (prefs.getBoolean("chat_opt_out_" + key, false) || prefs.getBoolean("chat_opt_out_"+normalizedKey,false)) { skip(context,subscriptionId,sender,"Sender opted out with STOP"); return; }
        String policy=ReplyPolicy.reason(prefs,"sms");
        if (!"Ready".equals(policy)) { skip(context,subscriptionId,sender,policy); return; }
        String filter=RecipientRules.reason(prefs,sender);
        if(!"Ready".equals(filter)) { skip(context,subscriptionId,sender,filter); return; }

        long now = System.currentTimeMillis();
        long last = prefs.getLong("chat_last_" + key, 0L);
        if (now - last < 30_000L) { skip(context,subscriptionId,sender,"30-second repeat protection"); return; }

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
        if (reply == null || reply.trim().isEmpty()) { skip(context,subscriptionId,sender,"Reply text is empty"); return; }

        if (!ReplySender.send(context, subscriptionId, sender, reply.trim(), "SMS")) return;
        prefs.edit()
                .putLong("chat_last_" + key, now)
                .putLong("last_sent_at", now)
                .putString("last_sent_number", sender)
                .apply();
    }

    private void skip(Context c,int id,String number,String reason) {
        Diagnostics.record(c,id,"SKIPPED",reason);
        AppCallLogStore.add(c,id,"SMS",number,"Skipped: "+reason);
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
