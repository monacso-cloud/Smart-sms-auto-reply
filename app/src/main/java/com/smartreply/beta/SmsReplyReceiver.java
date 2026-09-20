package com.smartreply.beta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SmsReplyReceiver extends BroadcastReceiver {
    private static final String PREFS = "smart_reply_settings";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("chatbot_enabled", false)) return;
        try {
            receive(context, intent, prefs);
        } catch (RuntimeException error) {
            prefs.edit().putString("last_chat_status", "Chatbot could not process or send this SMS")
                    .putString("last_error", error.getClass().getSimpleName())
                    .putString("last_sms_status", "Chatbot send failed; check permissions and SIM").apply();
        }
    }

    private void receive(Context context, Intent intent, SharedPreferences prefs) {
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;
        String sender = null;
        StringBuilder body = new StringBuilder();
        MessageDigest digest = newDigest();
        for (SmsMessage sms : messages) {
            if (sms == null) continue;
            String address = sms.getOriginatingAddress();
            if (address == null || address.isEmpty()) return;
            if (sender == null) sender = address;
            if (!sender.equals(address)) return;
            if (sms.getMessageBody() != null) body.append(sms.getMessageBody());
            // A new SMS has a new PDU/timestamp; repeated delivery of the same one does not.
            digest.update(sms.getPdu());
        }
        if (sender == null || body.length() == 0) return;
        String receipt = hex(digest.digest());
        String key = Integer.toHexString(sender.hashCode()); // Retain existing STOP preferences.
        String stateKey = "chat_gate_v2_" + hex(newDigest().digest(sender.getBytes(StandardCharsets.UTF_8)));
        String normalized = ChatbotRules.normalize(body.toString());
        String reply;
        boolean matched;
        if (normalized.equals("stop") || normalized.equals("unsubscribe")) {
            prefs.edit().putBoolean("chat_opt_out_" + key, true).apply();
            reply = "Automatic SMS replies are paused. Text START to enable them again.";
            matched = true;
        } else if (normalized.equals("start")) {
            prefs.edit().remove("chat_opt_out_" + key).apply();
            reply = "Automatic SMS replies are active again. Text MENU to see your options.";
            matched = true;
        } else {
            if (prefs.getBoolean("chat_opt_out_" + key, false)) return;
            reply = ChatbotRules.findReply(normalized,
                    prefs.getString("chatbot_rules", context.getString(R.string.default_chatbot_rules)));
            matched = reply != null;
            if (!matched) reply = prefs.getString("chatbot_fallback", context.getString(R.string.default_chatbot_fallback));
        }
        if (reply == null || reply.trim().isEmpty()) return;
        if (reply.toLowerCase(java.util.Locale.ROOT).contains("[enter ")) {
            prefs.edit().putString("last_chat_status", "Complete the business details in your reply template, then save").apply();
            return;
        }
        long now = System.currentTimeMillis();
        ChatbotReplyGate gate = new ChatbotReplyGate(prefs.getString(stateKey, ""));
        String blocked = gate.blockedReason(receipt, matched, now);
        if (blocked != null) {
            prefs.edit().putString("last_chat_status", blocked).apply();
            return;
        }
        // Record only after the send request succeeds; synchronous failures can be retried.
        SmsSender.send(context, sender, reply.trim(), "chatbot");
        gate.recordAccepted(receipt, matched, now);
        prefs.edit().putString(stateKey, gate.save())
                .putString("last_chat_status", matched ? "Matching reply requested" : "Welcome menu requested")
                .putLong("last_sent_at", now).putString("last_sent_number", sender).apply();
    }

    private static MessageDigest newDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }
}
