package com.smartreply.beta;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AppCallLogStore {
    private static final String PREFS = "smart_reply_settings";
    private static final String KEY = "replydesk_call_logs";

    private AppCallLogStore() {}

    public static synchronized void add(Context context, String type, String number, String replyStatus) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray current = readArray(prefs);
        JSONArray next = new JSONArray();

        long now = System.currentTimeMillis();
        int retentionDays = prefs.getInt("call_log_retention_days", 14);
        long cutoff = now - retentionDays * 24L * 60L * 60L * 1000L;

        for (int i = 0; i < current.length(); i++) {
            JSONObject item = current.optJSONObject(i);
            if (item != null && item.optLong("timestamp", 0) >= cutoff) next.put(item);
        }

        JSONObject event = new JSONObject();
        try {
            event.put("timestamp", now);
            event.put("type", type);
            event.put("maskedNumber", mask(number));
            event.put("replyStatus", replyStatus == null ? "" : replyStatus);
            next.put(event);
        } catch (Exception ignored) {}

        prefs.edit().putString(KEY, next.toString()).apply();
    }

    public static synchronized JSONArray get(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        purge(context);
        return readArray(prefs);
    }

    public static synchronized void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }

    public static synchronized void purge(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("auto_delete_logs", true)) return;

        JSONArray current = readArray(prefs);
        JSONArray next = new JSONArray();
        long now = System.currentTimeMillis();
        int retentionDays = prefs.getInt("call_log_retention_days", 14);
        long cutoff = now - retentionDays * 24L * 60L * 60L * 1000L;

        for (int i = 0; i < current.length(); i++) {
            JSONObject item = current.optJSONObject(i);
            if (item != null && item.optLong("timestamp", 0) >= cutoff) next.put(item);
        }
        prefs.edit().putString(KEY, next.toString()).apply();
    }

    private static JSONArray readArray(SharedPreferences prefs) {
        try {
            return new JSONArray(prefs.getString(KEY, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static String mask(String number) {
        if (number == null || number.length() < 4) return "Unknown";
        return "••••" + number.substring(number.length() - 4);
    }
}
