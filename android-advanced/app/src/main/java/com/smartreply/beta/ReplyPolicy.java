package com.smartreply.beta;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public final class ReplyPolicy {
    private static final String PREFS = "smart_reply_settings";

    private ReplyPolicy() {}

    public static boolean shouldReply(Context context, String channel) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        boolean masterEnabled = prefs.contains("master_enabled")
                ? prefs.getBoolean("master_enabled", false)
                : prefs.getBoolean("enabled", false);
        if (!masterEnabled) return false;

        if ("missed_call".equals(channel)) {
            boolean enabled = prefs.contains("reply_to_missed_calls")
                    ? prefs.getBoolean("reply_to_missed_calls", true)
                    : prefs.getBoolean("enabled", false);
            if (!enabled) return false;
        }

        if ("sms".equals(channel)) {
            boolean enabled = prefs.contains("reply_to_incoming_sms")
                    ? prefs.getBoolean("reply_to_incoming_sms", false)
                    : prefs.getBoolean("chatbot_enabled", false);
            if (!enabled) return false;
        }

        String scheduleMode = prefs.getString("schedule_mode", "always");
        if ("off".equals(scheduleMode)) return false;
        if (!"custom_hours".equals(scheduleMode)) return true;

        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        int mappedDay = day == Calendar.SUNDAY ? 0 : day - 1;

        if (!prefs.getBoolean("schedule_day_" + mappedDay, true)) return false;

        int minutesNow = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int start = prefs.getInt("schedule_start_minutes", 0);
        int end = prefs.getInt("schedule_end_minutes", 24 * 60);

        if (start == end) return true;
        if (start < end) return minutesNow >= start && minutesNow < end;
        return minutesNow >= start || minutesNow < end;
    }
}
