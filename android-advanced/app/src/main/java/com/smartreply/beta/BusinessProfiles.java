package com.smartreply.beta;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SubscriptionInfo;
import java.util.*;

/** UI selection never controls background routing. Each subscription owns a separate store. */
public final class BusinessProfiles {
    public static final String EXTRA = "business_subscription_id";
    public static final String LEGACY = "smart_reply_settings";
    private static final String INDEX = "replydesk_profiles";
    private BusinessProfiles() {}
    public static SharedPreferences index(Context c) {
        return c.getApplicationContext().getSharedPreferences(INDEX, Context.MODE_PRIVATE);
    }
    public static SharedPreferences prefs(Context c, int id) {
        if (id < 0) throw new IllegalArgumentException("A business SIM is required");
        return c.getApplicationContext().getSharedPreferences("replydesk_business_" + id, Context.MODE_PRIVATE);
    }
    public static Set<String> ids(Context c) {
        return new HashSet<>(index(c).getStringSet("ids", Collections.emptySet()));
    }
    public static synchronized void register(Context c, SubscriptionInfo info) {
        int id = info.getSubscriptionId();
        SharedPreferences p = prefs(c, id);
        if (!p.contains("business_name")) {
            p.edit().putString("business_name", "My business · " + info.getDisplayName())
                .putLong("created_at", System.currentTimeMillis())
                .putBoolean("master_enabled", false).putInt("delay_seconds", 0).apply();
        }
        p.edit().putString("sim_label", SimRouter.label(info)).apply();
        Set<String> ids = ids(c); ids.add(String.valueOf(id));
        index(c).edit().putStringSet("ids", ids).apply();
    }
    public static String name(Context c, int id) {
        return prefs(c, id).getString("business_name", "Business " + id);
    }
    public static String summary(Context c, int id) {
        return name(c, id) + " · " + prefs(c, id).getString("sim_label", "SIM " + id);
    }
    public static boolean exists(Context c, int id) {
        return id >= 0 && ids(c).contains(String.valueOf(id));
    }
    public static boolean canImport(Context c) {
        return !index(c).contains("legacy_imported_into")
            && !c.getApplicationContext().getSharedPreferences(LEGACY, 0).getAll().isEmpty();
    }
    /** Copy settings once, preserving the original file. Never enable a new line by migration. */
    public static synchronized boolean importLegacy(Context c, int id) {
        if (!canImport(c)) return false;
        SharedPreferences.Editor e = prefs(c, id).edit();
        for (Map.Entry<String, ?> item : c.getApplicationContext().getSharedPreferences(LEGACY, 0).getAll().entrySet()) {
            String k = item.getKey();
            if (k.equals("subscription_id") || k.startsWith("last_") || k.startsWith("chat_last_")
                || k.equals("replydesk_call_logs")) continue;
            Object v = item.getValue();
            if (v instanceof String) e.putString(k, (String)v);
            else if (v instanceof Boolean) e.putBoolean(k, (Boolean)v);
            else if (v instanceof Integer) e.putInt(k, (Integer)v);
            else if (v instanceof Long) e.putLong(k, (Long)v);
        }
        e.putBoolean("master_enabled", false).putBoolean("enabled", false).commit();
        index(c).edit().putInt("legacy_imported_into", id).commit();
        return true;
    }
}
