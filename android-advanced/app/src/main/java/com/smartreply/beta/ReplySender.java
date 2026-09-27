package com.smartreply.beta;

import android.Manifest;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.os.SystemClock;
import java.util.*;

public final class ReplySender {
    private ReplySender() {}
    public static void issue(Context c, String reason) {
        BusinessProfiles.index(c).edit().putString("last_routing_issue",
            java.text.DateFormat.getDateTimeInstance().format(new Date()) + "\n" + reason).apply();
    }
    public static String disclosure(String message) {
        String clean = message == null ? "" : message.trim();
        if (!clean.toLowerCase(Locale.ROOT).startsWith("automated reply:")) clean = "Automated reply:\n" + clean;
        if (!clean.toLowerCase(Locale.ROOT).contains("replydesk")) clean += "\nPOWERED BY: ReplyDesk - Business SMS Bot";
        return clean;
    }
    /** Returns whether submitted to Android, not whether delivered. Never uses a default SIM. */
    public static boolean send(Context c, int id, String number, String text, String type) {
        if (!BusinessProfiles.exists(c,id) || !SimRouter.active(c,id)) {
            issue(c,"Reply skipped: receiving SIM is inactive or has no business profile.");
            if (BusinessProfiles.exists(c,id)) AppCallLogStore.add(c,id,type,number,"Skipped: receiving SIM inactive");
            return false;
        }
        if (c.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            AppCallLogStore.add(c,id,type,number,"Skipped: SMS permission missing"); return false;
        }
        try {
            SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(id);
            ArrayList<String> parts = manager.divideMessage(disclosure(text));
            ArrayList<PendingIntent> callbacks = new ArrayList<>();
            String token = UUID.randomUUID().toString();
            for (int i=0;i<parts.size();i++) {
                Intent sent = new Intent(c,SmsSentReceiver.class).setAction("replydesk.sent."+token+"."+i)
                    .putExtra(BusinessProfiles.EXTRA,id).putExtra("number",number).putExtra("type",type)
                    .putExtra("part",i+1).putExtra("parts",parts.size());
                callbacks.add(PendingIntent.getBroadcast(c,0,sent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_ONE_SHOT));
            }
            if (parts.size()>1) manager.sendMultipartTextMessage(number,null,parts,callbacks,null);
            else manager.sendTextMessage(number,null,parts.get(0),callbacks.get(0),null);
            AppCallLogStore.add(c,id,type,number,"Submitted using " + BusinessProfiles.prefs(c,id).getString("sim_label","SIM " + id));
            BusinessProfiles.prefs(c,id).edit().putLong("last_sent_at",System.currentTimeMillis()).putString("last_sent_number",number).apply();
            return true;
        } catch (Exception e) {
            AppCallLogStore.add(c,id,type,number,"Send failed: " + e.getClass().getSimpleName()); return false;
        }
    }
}
