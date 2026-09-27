package com.smartreply.beta;
import android.content.SharedPreferences;
import java.util.Calendar;
public final class ReplyPolicy {
    private ReplyPolicy() {}
    public static boolean shouldReply(SharedPreferences p,String channel) { return "Ready".equals(reason(p,channel)); }
    public static String reason(SharedPreferences p,String channel) { return reason(p,channel,Calendar.getInstance()); }
    static String reason(SharedPreferences p,String channel,Calendar now) {
        if(!p.getBoolean("master_enabled",p.getBoolean("enabled",false))) return "Business replies paused";
        if("sms".equals(channel) && !p.getBoolean("reply_to_incoming_sms",p.getBoolean("chatbot_enabled",false))) return "SMS replies switched off";
        if("missed_call".equals(channel) && !p.getBoolean("reply_to_missed_calls",p.getBoolean("enabled",false))) return "Missed-call replies switched off";
        String mode=p.getString("schedule_mode","always");
        if("off".equals(mode)) return "Reply schedule is off";
        if(!"custom_hours".equals(mode)) return "Ready";
        int day=now.get(Calendar.DAY_OF_WEEK)-1, minutes=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);
        int start=p.getInt("schedule_start_minutes",0), end=p.getInt("schedule_end_minutes",1440);
        if(start>end && minutes<end) day=(day+6)%7;
        if(!p.getBoolean("schedule_day_"+day,true)) return "Outside selected reply days";
        boolean inside=start==end || (start<end ? minutes>=start && minutes<end : minutes>=start || minutes<end);
        return inside ? "Ready" : "Outside reply hours";
    }
}
