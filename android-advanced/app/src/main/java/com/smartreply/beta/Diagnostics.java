package com.smartreply.beta;
import android.content.*;
import org.json.*;
import java.text.DateFormat;
import java.util.Date;

public final class Diagnostics {
    private Diagnostics() {}
    public static synchronized void record(Context c,int id,String stage,String detail) {
        SharedPreferences p=BusinessProfiles.index(c);
        JSONArray events;
        try { events=new JSONArray(p.getString("diagnostic_events","[]")); } catch(Exception e){events=new JSONArray();}
        JSONArray next=new JSONArray();
        for(int i=Math.max(0,events.length()-79);i<events.length();i++) next.put(events.optJSONObject(i));
        try {
            JSONObject event=new JSONObject().put("time",System.currentTimeMillis()).put("business",id)
                .put("stage",stage).put("detail",detail);
            next.put(event);
        } catch(JSONException ignored) {}
        p.edit().putString("diagnostic_events",next.toString()).putLong("event_"+stage,System.currentTimeMillis()).apply();
    }
    public static String report(Context c) {
        StringBuilder s=new StringBuilder("ReplyDesk 0.6.0\nAndroid "+android.os.Build.VERSION.RELEASE+" · "+android.os.Build.MODEL+"\n");
        for(String permission:new String[]{android.Manifest.permission.READ_PHONE_STATE,android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.RECEIVE_SMS,android.Manifest.permission.SEND_SMS})
            s.append(permission.substring(permission.lastIndexOf('.')+1)).append(": ")
                .append(c.checkSelfPermission(permission)==0 ? "allowed" : "missing").append("\n");
        s.append("Alarms: ").append(ScheduledSmsStore.canSchedule(c) ? "allowed" : "not allowed").append("\n");
        if(android.os.Build.VERSION.SDK_INT>=28)
            s.append("Background restricted: ").append(c.getSystemService(android.app.ActivityManager.class).isBackgroundRestricted()).append("\n");
        s.append("Active SIMs: ").append(SimRouter.active(c).size()).append("\n");
        for(String v:BusinessProfiles.ids(c)) {
            int id=Integer.parseInt(v); SharedPreferences p=BusinessProfiles.prefs(c,id);
            s.append("\n").append(BusinessProfiles.summary(c,id)).append("\n")
                .append("SMS: ").append(ReplyPolicy.reason(p,"sms")).append("\n")
                .append("Missed calls: ").append(ReplyPolicy.reason(p,"missed_call")).append("\n");
        }
        s.append("\nRECENT EVENTS (no message contents)\n");
        try {
            JSONArray events=new JSONArray(BusinessProfiles.index(c).getString("diagnostic_events","[]"));
            if(events.length()==0) s.append("No incoming events recorded yet.\n");
            for(int i=events.length()-1;i>=0;i--) {
                JSONObject e=events.getJSONObject(i);
                s.append(DateFormat.getDateTimeInstance().format(new Date(e.optLong("time")))).append(" · ")
                    .append(e.optString("stage")).append(" · business ").append(e.optInt("business",-1))
                    .append(" · ").append(e.optString("detail")).append("\n");
            }
        } catch(Exception ignored) {}
        return s.toString();
    }
}
