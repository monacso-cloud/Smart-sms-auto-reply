package com.smartreply.beta;

import android.content.SharedPreferences;

/** Durable per-business handoff and one-time fallback. Never sends SMS. */
final class ConversationPolicy {
    private ConversationPolicy(){}
    private static String key(SharedPreferences p,String sender){return RecipientRules.normal(p,sender);}
    static boolean paused(SharedPreferences p,String sender){
        return p.getBoolean("human_takeover",false) || p.getBoolean("human_sender_"+key(p,sender),false);
    }
    static synchronized void setPaused(SharedPreferences p,String sender,boolean paused){
        SharedPreferences.Editor e=p.edit().putBoolean("human_sender_"+key(p,sender),paused);
        if(!paused)e.remove("fallback_used_"+key(p,sender));
        if(!e.commit())throw new IllegalStateException("Could not save customer automation state");
    }
    static boolean optedOut(SharedPreferences p,String sender){
        String canonical=key(p,sender), state="chat_opt_out_v2_"+IncomingSmsGuard.digest(canonical);
        // An explicit START also overrides old raw-format hashes which cannot be enumerated.
        if(p.contains(state))return p.getBoolean(state,false);
        return p.getBoolean("chat_opt_out_"+Integer.toHexString(sender.hashCode()),false)
            || p.getBoolean("chat_opt_out_"+Integer.toHexString(canonical.hashCode()),false);
    }
    static void command(SharedPreferences.Editor e,SharedPreferences p,String sender,String command){
        String canonical=key(p,sender), raw="chat_opt_out_"+Integer.toHexString(sender.hashCode());
        String legacy="chat_opt_out_"+Integer.toHexString(canonical.hashCode());
        String state="chat_opt_out_v2_"+IncomingSmsGuard.digest(canonical);
        if("stop".equals(command)||"unsubscribe".equals(command)){
            e.putBoolean(state,true).putBoolean(raw,true).putBoolean(legacy,true);
        }else if("start".equals(command)){
            e.putBoolean(state,false).remove(raw).remove(legacy)
                .putBoolean("human_sender_"+canonical,false).remove("fallback_used_"+canonical);
        }else if("human".equals(command))e.putBoolean("human_sender_"+canonical,true);
    }
    static synchronized boolean claimFallback(SharedPreferences p,String sender){
        String key="fallback_used_"+key(p,sender);
        if(p.getBoolean(key,false))return false;
        // Persist before dispatch: a failure or process restart must not produce another fallback.
        return p.edit().putBoolean(key,true).commit();
    }
}

