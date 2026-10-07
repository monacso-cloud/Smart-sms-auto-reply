package com.smartreply.beta;

import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.*;

/** Persistent, revisioned, one-shot messages. A claimed send is never retried automatically. */
public final class ScheduledSmsStore {
    public static final long MAX_LATENESS=15*60_000L;
    private ScheduledSmsStore() {}
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences("replydesk_scheduled",0);}
    private static JSONArray read(Context c){try{return new JSONArray(prefs(c).getString("jobs","[]"));}catch(Exception e){return new JSONArray();}}
    private static void write(Context c,JSONArray jobs){if(!prefs(c).edit().putString("jobs",jobs.toString()).commit()) throw new IllegalStateException("Unable to save scheduled messages");}
    public static synchronized JSONArray list(Context c,int business) {
        JSONArray result=new JSONArray(), jobs=read(c);
        for(int i=0;i<jobs.length();i++){JSONObject o=jobs.optJSONObject(i);if(o!=null && o.optInt("business")==business)result.put(o);}
        return result;
    }
    public static synchronized JSONObject get(Context c,String id) {
        JSONArray jobs=read(c);for(int i=0;i<jobs.length();i++)if(id.equals(jobs.optJSONObject(i).optString("id")))return jobs.optJSONObject(i);
        return null;
    }
    public static boolean canSchedule(Context c){
        AlarmManager am=c.getSystemService(AlarmManager.class);
        return am!=null && (Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms());
    }
    private static PendingIntent operation(Context c,JSONObject job){
        Intent i=new Intent(c,ScheduledSmsReceiver.class).setAction("replydesk.schedule."+job.optString("id")+"."+job.optInt("version"))
            .putExtra("job",job.optString("id")).putExtra("version",job.optInt("version"));
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void disarm(Context c,JSONObject job){
        AlarmManager am=c.getSystemService(AlarmManager.class);if(am!=null)am.cancel(operation(c,job));
    }
    private static void arm(Context c,JSONObject job){
        if(!canSchedule(c))throw new IllegalStateException("Allow Alarms & reminders to schedule SMS");
        c.getSystemService(AlarmManager.class).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            Math.max(System.currentTimeMillis()+1000,job.optLong("time")),operation(c,job));
    }
    public static synchronized String save(Context c,int business,String id,List<String> numbers,String message,long when){
        if(!BusinessProfiles.exists(c,business))throw new IllegalArgumentException("Select a business SIM");
        if(numbers.isEmpty())throw new IllegalArgumentException("Add at least one recipient");
        if(message.trim().isEmpty())throw new IllegalArgumentException("Write your message");
        if(when<=System.currentTimeMillis()+30_000)throw new IllegalArgumentException("Choose a time at least one minute from now");
        if(!canSchedule(c))throw new IllegalStateException("Allow Alarms & reminders first");
        JSONArray jobs=read(c);JSONObject previous=null;int at=-1;
        for(int i=0;i<jobs.length();i++)if(jobs.optJSONObject(i).optString("id").equals(id)){previous=jobs.optJSONObject(i);at=i;break;}
        if(id!=null && previous==null)throw new IllegalStateException("This scheduled message no longer exists");
        if(previous!=null && (previous.optInt("business")!=business || !editable(previous)))throw new IllegalStateException("Only pending messages can be edited");
        if(id==null)id=UUID.randomUUID().toString();
        try {
            JSONObject job=new JSONObject().put("id",id).put("business",business).put("version",previous==null?1:previous.optInt("version")+1)
                .put("time",when).put("zone",TimeZone.getDefault().getID()).put("message",message.trim())
                .put("numbers",new JSONArray(numbers)).put("state","Pending").put("reason","").put("created",System.currentTimeMillis());
            if(at<0)jobs.put(job);else jobs.put(at,job);
            write(c,jobs);
            if(previous!=null)disarm(c,previous);
            try{arm(c,job);}catch(Exception e){setState(c,id,"Blocked","Alarms permission unavailable; open Settings to allow it");throw e;}
            Diagnostics.record(c,business,"SCHEDULED","Saved "+numbers.size()+" recipient(s)");
            return id;
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    public static boolean editable(JSONObject job){return "Pending".equals(job.optString("state"))||"Blocked".equals(job.optString("state"));}
    public static synchronized boolean cancel(Context c,String id) {
        JSONObject job=get(c,id);if(job==null || !editable(job))return false;
        setState(c,id,"Cancelled","Cancelled before sending");disarm(c,job);return true;
    }
    private static synchronized void setState(Context c,String id,String state,String reason){
        JSONArray jobs=read(c);
        for(int i=0;i<jobs.length();i++){JSONObject o=jobs.optJSONObject(i);if(id.equals(o.optString("id")))try{o.put("state",state).put("reason",reason);}catch(Exception ignored){}}
        write(c,jobs);
    }
    static synchronized JSONObject claim(Context c,String id,int revision,long now) {
        JSONArray jobs=read(c);
        for(int i=0;i<jobs.length();i++) {
            JSONObject job=jobs.optJSONObject(i);
            if(!id.equals(job.optString("id")) || job.optInt("version")!=revision || !"Pending".equals(job.optString("state")))continue;
            if(now<job.optLong("time"))return null;
            try {
                if(now-job.optLong("time")>MAX_LATENESS){job.put("state","Failed").put("reason","Over 15 minutes late; not sent");write(c,jobs);return null;}
                JSONArray statuses=new JSONArray();for(int n=0;n<job.getJSONArray("numbers").length();n++)statuses.put("Pending");
                job.put("state","Dispatching").put("started",now).put("recipients",statuses);
                write(c,jobs);return job;
            }catch(JSONException e){throw new IllegalStateException(e);}
        }
        return null;
    }
    public static void dispatch(Context c,String id,int revision) {
        dispatch(c,id,revision,System.currentTimeMillis());
    }
    static void dispatch(Context c,String id,int revision,long now) {
        JSONObject job=claim(c,id,revision,now);if(job==null)return;
        int business=job.optInt("business");JSONArray numbers=job.optJSONArray("numbers");
        for(int n=0;n<numbers.length();n++) {
            String number=numbers.optString(n);
            SharedPreferences p=BusinessProfiles.prefs(c,business);
            if(p.getBoolean("chat_opt_out_"+Integer.toHexString(number.hashCode()),false)){
                recipient(c,id,n,"Failed: recipient opted out");continue;
            }
            recipient(c,id,n,"Submitted");
            if(!ReplySender.sendScheduled(c,business,number,job.optString("message"),id,n))
                recipient(c,id,n,"Failed: permission, SIM or sending error; see history");
        }
    }
    public static synchronized void recipient(Context c,String id,int number,String state) {
        JSONArray jobs=read(c);
        for(int i=0;i<jobs.length();i++){
            JSONObject job=jobs.optJSONObject(i);if(!id.equals(job.optString("id")))continue;
            JSONArray states=job.optJSONArray("recipients");if(states==null || number<0 || number>=states.length())return;
            try{
                String old=states.optString(number);
                if(old.startsWith("Failed") || "Sent".equals(old))return;
                states.put(number,state);
                boolean pending=false,submitted=false,failed=false;
                for(int n=0;n<states.length();n++){
                    String s=states.optString(n);pending|="Pending".equals(s);submitted|="Submitted".equals(s);failed|=s.startsWith("Failed");
                }
                job.put("state",pending?"Dispatching":submitted?"Submitted":failed?"Failed":"Sent");
                job.put("reason",failed?"One or more recipients were not sent; open details":"");
                write(c,jobs);return;
            }catch(JSONException e){throw new IllegalStateException(e);}
        }
    }
    public static synchronized void restore(Context c,boolean reboot) {
        JSONArray jobs=read(c);
        for(int i=0;i<jobs.length();i++){
            JSONObject job=jobs.optJSONObject(i);String state=job.optString("state");
            try{
                if(editable(job)) {
                    if(System.currentTimeMillis()-job.optLong("time")>MAX_LATENESS)job.put("state","Failed").put("reason","Over 15 minutes late; not sent");
                    else if(!canSchedule(c))job.put("state","Blocked").put("reason","Allow Alarms & reminders");
                    else {job.put("state","Pending").put("reason","");arm(c,job);}
                }else if(("Dispatching".equals(state)||"Submitted".equals(state)) && (reboot || System.currentTimeMillis()-job.optLong("started")>MAX_LATENESS))
                    job.put("state","Unconfirmed").put("reason","Sending was interrupted or no result arrived. Check with recipients before scheduling again. Not retried.");
            }catch(Exception e){try{job.put("state","Blocked").put("reason","Unable to restore alarm");}catch(Exception ignored){}}
        }
        write(c,jobs);
    }
}
