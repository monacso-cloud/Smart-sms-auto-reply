package com.smartreply.beta;

import android.Manifest;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.PersistableBundle;
import android.provider.CallLog;
import java.util.concurrent.*;

/** Delays are managed by Android, not by sleeping inside a BroadcastReceiver. */
public class MissedCallJobService extends JobService {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    public static void scheduleScan(Context c, boolean retry) {
        PersistableBundle extras = new PersistableBundle(); extras.putBoolean("scan",true); extras.putBoolean("retry",retry);
        JobInfo job = new JobInfo.Builder(retry ? 401 : 400,new ComponentName(c,MissedCallJobService.class))
            .setMinimumLatency(retry ? 5000 : 1800).setOverrideDeadline(retry ? 10000 : 5000).setExtras(extras).build();
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        if (scheduler != null && scheduler.schedule(job)!=JobScheduler.RESULT_SUCCESS)
            Diagnostics.record(c,-1,"SKIPPED","Android rejected missed-call background job");
    }
    @Override public boolean onStartJob(JobParameters params) {
        WORKER.execute(() -> {
            try {
                if (params.getExtras().getBoolean("scan")) {
                    scan();
                    if (!params.getExtras().getBoolean("retry")) scheduleScan(this,true);
                } else dispatch(params.getExtras());
            } catch (Exception e) { ReplySender.issue(this,"Missed-call processing failed: " + e.getClass().getSimpleName()); }
            finally { jobFinished(params,false); }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters p) { return false; }
    private void scan() {
        Diagnostics.record(this,-1,"CALL_SCAN","Checking recent missed calls");
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED) {
            Diagnostics.record(this,-1,"SKIPPED","READ_CALL_LOG permission missing"); return;
        }
        long cutoff=System.currentTimeMillis()-5*60_000L;
        try (Cursor rows=getContentResolver().query(CallLog.Calls.CONTENT_URI,
            new String[]{CallLog.Calls._ID,CallLog.Calls.NUMBER,CallLog.Calls.DATE,
                CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME,CallLog.Calls.PHONE_ACCOUNT_ID},
            CallLog.Calls.TYPE+" = ? AND "+CallLog.Calls.DATE+" >= ?",
            new String[]{String.valueOf(CallLog.Calls.MISSED_TYPE),String.valueOf(cutoff)},CallLog.Calls.DATE+" ASC")) {
            if (rows==null) return;
            while (rows.moveToNext()) {
                long rowId=rows.getLong(0), date=rows.getLong(2);
                String event="call_"+rowId+"_"+date;
                SharedPreferences index=BusinessProfiles.index(this);
                if (index.contains(event)) continue;
                int id=SimRouter.callSubscription(this,rows.getString(3),rows.getString(4));
                if (!BusinessProfiles.exists(this,id)) {
                    ReplySender.issue(this,"Missed call skipped: Android did not identify a configured receiving SIM. No other SIM was used.");
                    index.edit().putLong(event,date).apply(); continue;
                }
                SharedPreferences p=BusinessProfiles.prefs(this,id);
                if (date<p.getLong("created_at",0) || !ReplyPolicy.shouldReply(p,"missed_call")) {
                    Diagnostics.record(this,id,"SKIPPED",date<p.getLong("created_at",0) ? "Call predates business profile" : ReplyPolicy.reason(p,"missed_call"));
                    index.edit().putLong(event,date).apply(); continue;
                }
                PersistableBundle extras=new PersistableBundle();
                extras.putInt(BusinessProfiles.EXTRA,id); extras.putString("number",rows.getString(1));
                extras.putString("event",event); extras.putLong("date",date);
                int jobId=1000+(int)(rowId % 1_000_000_000L);
                JobScheduler scheduler=getSystemService(JobScheduler.class);
                if (scheduler.getPendingJob(jobId)!=null) continue;
                long delay=Math.max(0,Math.min(60,p.getInt("delay_seconds",0)))*1000L;
                JobInfo job=new JobInfo.Builder(jobId,new ComponentName(this,MissedCallJobService.class))
                    .setMinimumLatency(delay).setOverrideDeadline(delay+5000).setExtras(extras).build();
                if (scheduler.schedule(job)!=JobScheduler.RESULT_SUCCESS)
                    AppCallLogStore.add(this,id,"MISSED",rows.getString(1),"Unable to schedule reply");
            }
        }
        SharedPreferences.Editor cleanup=BusinessProfiles.index(this).edit();
        for (java.util.Map.Entry<String,?> entry:BusinessProfiles.index(this).getAll().entrySet())
            if(entry.getKey().startsWith("call_") && entry.getValue() instanceof Long && (Long)entry.getValue()<cutoff)
                cleanup.remove(entry.getKey());
        cleanup.apply();
    }
    private void dispatch(PersistableBundle data) {
        String event=data.getString("event"), number=data.getString("number");
        int id=data.getInt(BusinessProfiles.EXTRA,-1);
        SharedPreferences index=BusinessProfiles.index(this);
        if (event==null || index.contains(event)) return;
        // At most one submission, including after duplicate phone-state broadcasts.
        index.edit().putLong(event,data.getLong("date")).commit();
        if (!BusinessProfiles.exists(this,id)) return;
        SharedPreferences p=BusinessProfiles.prefs(this,id);
        if (System.currentTimeMillis()-data.getLong("date")>5*60_000L) {
            AppCallLogStore.add(this,id,"MISSED",number,"Skipped: delayed event expired"); return;
        }
        if (!ReplyPolicy.shouldReply(p,"missed_call")) {
            AppCallLogStore.add(this,id,"MISSED",number,"Skipped: business paused or outside schedule"); return;
        }
        if (number==null || number.trim().isEmpty() || number.startsWith("-")) return;
        String key=Integer.toHexString(number.hashCode());
        if (p.getBoolean("chat_opt_out_"+key,false) || p.getBoolean("chat_opt_out_"+Integer.toHexString(RecipientRules.normal(p,number).hashCode()),false)) { Diagnostics.record(this,id,"SKIPPED","Caller opted out"); return; }
        String filter=RecipientRules.reason(p,number);
        if(!"Ready".equals(filter)) { Diagnostics.record(this,id,"SKIPPED",filter); AppCallLogStore.add(this,id,"MISSED",number,"Skipped: "+filter); return; }
        long now=System.currentTimeMillis();
        long last=p.getLong("last_reply_"+key,0);
        if (now-last<Math.max(0,p.getInt("repeat_minutes",0))*60_000L) return;
        String message=appendNumberedMenu(p.getString("message",getString(R.string.default_message)),p);
        if (ReplySender.send(this,id,number,message,"MISSED")) p.edit().putLong("last_reply_"+key,now).apply();
    }
    static String appendNumberedMenu(String message, SharedPreferences prefs) {
        return appendNumberedMenu(message,prefs,prefs.getBoolean("missed_call_include_menu",prefs.getBoolean("menu_enabled",false)));
    }
    static String appendNumberedMenu(String message,SharedPreferences prefs,boolean includeMenu) {
        if (!prefs.getBoolean("menu_enabled", false) || !includeMenu) return message;

        String intro = prefs.getString("menu_intro", "How can we help? Reply with a number:").trim();
        StringBuilder menu = new StringBuilder();
        if (!intro.isEmpty()) menu.append(intro);

        for (int i = 1; i <= 10; i++) {
            String label = prefs.getString("menu_item_" + i, "").trim();
            if (!label.isEmpty()) {
                if (menu.length() > 0) menu.append("\n");
                menu.append(i).append(" — ").append(label);
            }
        }

        if (menu.length() == 0) return message;
        return message.trim() + "\n\n" + menu;
    }
}
