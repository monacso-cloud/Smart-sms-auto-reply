package com.smartreply.beta;

import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/** Durable at-most-once receipt claims. Contains no SMS dispatch or retry logic. */
final class IncomingSmsGuard {
    private IncomingSmsGuard() {}
    static final long RETENTION_MS = 30L * 24 * 60 * 60 * 1000;
    static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static synchronized boolean claim(SharedPreferences p, String sender, String body,
                                      long sourceTime, long now, String command) {
        String canonical = RecipientRules.normal(p, sender);
        // Old events cannot become new again after ledger cleanup. Unknown dates fail closed.
        if (canonical.isEmpty() || sourceTime <= 0 || sourceTime < now - RETENTION_MS
                || sourceTime > now + 24 * 60 * 60 * 1000L) return false;
        String key = "sms_event_" + digest(canonical + "\n" + sourceTime + "\n" + body);
        if (p.contains(key)) return false;
        SharedPreferences.Editor edit = p.edit().putLong(key, sourceTime);
        for (Map.Entry<String, ?> entry : p.getAll().entrySet()) {
            if (entry.getKey().startsWith("sms_event_") && entry.getValue() instanceof Long
                    && (Long) entry.getValue() < now - RETENTION_MS) edit.remove(entry.getKey());
        }
        // Command state and its receipt must reach disk together, before any acknowledgement.
        ConversationPolicy.command(edit, p, sender, command);
        return edit.commit();
    }
    static synchronized boolean reserveReply(SharedPreferences p, String sender, long now,
                                              boolean fallback, String type, long cooldown) {
        String canonical = RecipientRules.normal(p, sender);
        String prefix = "MISSED".equals(type) ? "last_reply_" : "chat_last_";
        String key = prefix + "v2_" + digest(canonical);
        long last = Math.max(p.getLong(key, 0L), Math.max(
            p.getLong(prefix + Integer.toHexString(sender.hashCode()), 0L),
            p.getLong(prefix + Integer.toHexString(canonical.hashCode()), 0L)));
        if (last > 0 && (now < last || now - last < cooldown)) return false;
        String fallbackKey = "fallback_used_" + canonical;
        if (fallback && (!p.getBoolean("fallback_enabled", false) || p.getBoolean(fallbackKey, false))) return false;
        SharedPreferences.Editor edit = p.edit().putLong(key, now);
        if (fallback) edit.putBoolean(fallbackKey, true);
        // Do not release a reservation on failure: Android may already have accepted the SMS.
        return edit.commit();
    }
    static synchronized boolean claimCall(SharedPreferences index, String event, long date) {
        if (event == null || index.contains(event)) return false;
        return index.edit().putLong(event, date).commit();
    }
}
