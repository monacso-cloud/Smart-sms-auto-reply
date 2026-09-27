package com.smartreply.beta;
import android.content.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** One completion per logical SMS, even when Android calls back once for each part. */
public final class DeliveryTracker {
    private DeliveryTracker(){}
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences("replydesk_deliveries",0);}
    public static synchronized String begin(Context c,int business,String number,String type,int parts,String job,int recipient){
        String id=UUID.randomUUID().toString();
        try{
            JSONArray results=new JSONArray();for(int n=0;n<parts;n++)results.put(0);
            JSONObject o=new JSONObject().put("business",business).put("number",number).put("type",type)
                .put("parts",results).put("finished",false).put("created",System.currentTimeMillis()).put("job",job).put("recipient",recipient);
            if(!prefs(c).edit().putString(id,o.toString()).commit())throw new IllegalStateException("Unable to track SMS result");
            return id;
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    public static synchronized void result(Context c,String id,int part,boolean sent,String reason){
        if(id==null)return;
        try{
            JSONObject o=new JSONObject(prefs(c).getString(id,"{}"));if(o.optBoolean("finished") || !o.has("parts"))return;
            JSONArray parts=o.getJSONArray("parts");if(part<0 || part>=parts.length() || parts.getInt(part)!=0)return;
            parts.put(part,sent?1:-1);
            boolean all=true,failed=false;for(int n=0;n<parts.length();n++){all&=parts.getInt(n)!=0;failed|=parts.getInt(n)<0;}
            if(failed || all){
                o.put("finished",true);
                int business=o.getInt("business");
                String status=failed?"Failed: "+reason:"Sent (delivery not confirmed)";
                AppCallLogStore.add(c,business,o.getString("type"),o.getString("number"),status);
                Diagnostics.record(c,business,failed?"SEND_FAILED":"SENT",status);
                if(!failed){
                    SharedPreferences p=BusinessProfiles.prefs(c,business);
                    String day=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
                    p.edit().putInt("sent_total",p.getInt("sent_total",0)+1).putInt("sent_"+day,p.getInt("sent_"+day,0)+1).apply();
                }
                String job=o.optString("job","");if(!job.isEmpty())ScheduledSmsStore.recipient(c,job,o.optInt("recipient"),failed?"Failed: "+reason:"Sent");
            }
            prefs(c).edit().putString(id,o.toString()).apply();
        }catch(JSONException ignored){}
    }
    public static synchronized void cleanup(Context c){
        SharedPreferences p=prefs(c);SharedPreferences.Editor edit=p.edit();long cutoff=System.currentTimeMillis()-2*86400_000L;
        for(Map.Entry<String,?> e:p.getAll().entrySet())try{
            JSONObject o=new JSONObject((String)e.getValue());if(o.optLong("created")<cutoff)edit.remove(e.getKey());
        }catch(Exception ignored){}
        edit.apply();
    }
}
