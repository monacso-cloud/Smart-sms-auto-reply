package com.smartreply.beta;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telephony.*;
import java.util.*;

public final class SimRouter {
    private SimRouter() {}
    public static List<SubscriptionInfo> active(Context c) {
        if (c.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            return Collections.emptyList();
        try {
            SubscriptionManager m = c.getSystemService(SubscriptionManager.class);
            List<SubscriptionInfo> list = m == null ? null : m.getActiveSubscriptionInfoList();
            return list == null ? Collections.emptyList() : list;
        } catch (SecurityException | UnsupportedOperationException e) { return Collections.emptyList(); }
    }
    public static boolean active(Context c, int id) {
        for (SubscriptionInfo s : active(c)) if (s.getSubscriptionId() == id) return true;
        return false;
    }
    public static String label(SubscriptionInfo s) {
        String kind = Build.VERSION.SDK_INT >= 28 && s.isEmbedded() ? "eSIM" : "SIM";
        return kind + " " + (s.getSimSlotIndex() + 1) + " — " + s.getDisplayName();
    }
    public static int smsSubscription(Bundle extras) {
        if (extras == null) return -1;
        // Android releases use both names, and subscription can be Integer or Long.
        int found = -1;
        for (String key : new String[]{"android.telephony.extra.SUBSCRIPTION_INDEX", "subscription"}) {
            if (!extras.containsKey(key)) continue;
            Object value = extras.get(key);
            if (!(value instanceof Integer) && !(value instanceof Long)) return -1;
            long n = ((Number)value).longValue();
            if (n < 0 || n > Integer.MAX_VALUE) return -1;
            if (found >= 0 && found != (int)n) return -1;
            found = (int)n;
        }
        return found;
    }
    public static int smsSubscription(Context c,Bundle extras) {
        if(extras==null)return -1;
        boolean hasId=extras.containsKey("subscription") || extras.containsKey("android.telephony.extra.SUBSCRIPTION_INDEX");
        int id=smsSubscription(extras);
        if(hasId && id<0)return -1;
        int slot=-1;
        for(String key:new String[]{"android.telephony.extra.SLOT_INDEX","slot"}){
            if(!extras.containsKey(key))continue;
            Object raw=extras.get(key);
            if(!(raw instanceof Integer) && !(raw instanceof Long))return -1;
            long n=((Number)raw).longValue();
            if(n<0 || n>Integer.MAX_VALUE || (slot>=0 && slot!=(int)n))return -1;
            slot=(int)n;
        }
        if(slot<0)return id;
        int match=-1;
        for(SubscriptionInfo info:active(c))if(info.getSimSlotIndex()==slot){
            if(match>=0)return -1;
            match=info.getSubscriptionId();
        }
        return hasId ? (match==id ? id : -1) : match;
    }
    public static int callSubscription(Context c, String component, String account) {
        if (Build.VERSION.SDK_INT < 30 || component == null || account == null || account.isEmpty()) return -1;
        ComponentName name = ComponentName.unflattenFromString(component);
        if (name == null) return -1;
        try {
            TelephonyManager tm = c.getSystemService(TelephonyManager.class);
            int id = tm == null ? -1 : tm.getSubscriptionId(new PhoneAccountHandle(name, account));
            return active(c, id) ? id : -1;
        } catch (SecurityException | UnsupportedOperationException e) { return -1; }
    }
}

