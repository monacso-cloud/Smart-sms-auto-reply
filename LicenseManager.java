package com.smartreply.beta;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class LicenseManager {
    private static final String PREFS = "smart_reply_settings";
    private static final String EXPECTED_KEY_HASH =
            "74dc55490f80d7e322e83b7e9181061da348e382390fb07369a1e9f66cae3a33";
    private static final long LICENSE_DURATION_MS = 14L * 24L * 60L * 60L * 1000L;

    private LicenseManager() {}

    public static ActivationResult activate(Context context, String rawKey) {
        String normalized = normalize(rawKey);
        if (!EXPECTED_KEY_HASH.equals(sha256(normalized))) {
            return new ActivationResult(false, "Invalid license key");
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String deviceHash = getDeviceHash(context);
        String savedDeviceHash = prefs.getString("license_device_hash", "");

        if (!savedDeviceHash.isEmpty() && !savedDeviceHash.equals(deviceHash)) {
            return new ActivationResult(false, "This license is already linked to another device");
        }

        long activatedAt = prefs.getLong("license_activated_at", 0L);
        if (activatedAt == 0L) {
            activatedAt = System.currentTimeMillis();
        }

        prefs.edit()
                .putString("license_key_hash", EXPECTED_KEY_HASH)
                .putString("license_device_hash", deviceHash)
                .putLong("license_activated_at", activatedAt)
                .apply();

        return new ActivationResult(true, "Free test license activated");
    }

    public static boolean isActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!EXPECTED_KEY_HASH.equals(prefs.getString("license_key_hash", ""))) return false;
        if (!getDeviceHash(context).equals(prefs.getString("license_device_hash", ""))) return false;

        long activatedAt = prefs.getLong("license_activated_at", 0L);
        return activatedAt > 0L && System.currentTimeMillis() < activatedAt + LICENSE_DURATION_MS;
    }

    public static long daysRemaining(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long activatedAt = prefs.getLong("license_activated_at", 0L);
        if (activatedAt == 0L) return 0L;

        long remaining = activatedAt + LICENSE_DURATION_MS - System.currentTimeMillis();
        if (remaining <= 0L) return 0L;
        return Math.max(1L, (remaining + 86_399_999L) / 86_400_000L);
    }

    private static String getDeviceHash(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return sha256(context.getPackageName() + ":" + String.valueOf(androidId));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b));
            return out.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static final class ActivationResult {
        public final boolean success;
        public final String message;

        ActivationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
