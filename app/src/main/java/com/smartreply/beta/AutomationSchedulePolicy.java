package com.smartreply.beta;

import android.content.SharedPreferences;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class AutomationSchedulePolicy {
    private static final DateTimeFormatter TWELVE_HOUR =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter TWELVE_HOUR_SHORT =
            DateTimeFormatter.ofPattern("h a", Locale.US);
    private static final DateTimeFormatter TWENTY_FOUR_HOUR =
            DateTimeFormatter.ofPattern("H:mm", Locale.US);

    private AutomationSchedulePolicy() {}

    public static boolean allowMissedCall(SharedPreferences prefs, long nowMillis) {
        if (!prefs.getBoolean("schedule_missed_calls", true)) return false;
        return allowBySchedule(prefs, nowMillis);
    }

    public static boolean allowIncomingSms(SharedPreferences prefs, long nowMillis) {
        if (!prefs.getBoolean("schedule_incoming_sms", true)) return false;
        return allowBySchedule(prefs, nowMillis);
    }

    public static boolean allowUnmatchedSms(SharedPreferences prefs) {
        return prefs.getBoolean("schedule_unmatched_sms", true);
    }

    public static boolean allowBySchedule(SharedPreferences prefs, long nowMillis) {
        if (!prefs.getBoolean("schedule_enabled", false)) return true;

        int mode = prefs.getInt("schedule_mode", 0);
        if (mode == 0) return true;   // Always on
        if (mode == 3) return false;  // Manual only

        LocalTime start = parseTime(prefs.getString("schedule_start", "8:00 AM"), LocalTime.of(8, 0));
        LocalTime end = parseTime(prefs.getString("schedule_end", "8:00 PM"), LocalTime.of(20, 0));
        LocalTime now = java.time.Instant.ofEpochMilli(nowMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime();

        boolean inside = isInsideWindow(now, start, end);
        return mode == 1 ? inside : !inside; // 1 selected hours, 2 outside selected hours
    }

    static boolean isInsideWindow(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) return true; // treat equal times as 24-hour window
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        // Overnight window, e.g. 6:00 PM -> 6:00 AM
        return !now.isBefore(start) || now.isBefore(end);
    }

    static LocalTime parseTime(String raw, LocalTime fallback) {
        if (raw == null) return fallback;
        String value = raw.trim().toUpperCase(Locale.US).replaceAll("\\s+", " ");
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{TWELVE_HOUR, TWELVE_HOUR_SHORT, TWENTY_FOUR_HOUR}) {
            try {
                return LocalTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return fallback;
    }
}
