package com.smartreply.beta;

import android.content.SharedPreferences;

/** Durable per-business handoff and one-time fallback. Never sends SMS. */
final class ConversationPolicy {
    private ConversationPolicy(){}
    private static String key(SharedPreferences p,String sender){return RecipientRules.normal(p,sender);}
    static boolean paused(SharedPreferences p,String sender){
        return p.getBoolean("human_takeover",false) || p.getBoolean("human_sender_"+key(p,sender),false);
    }
    static void setPaused(SharedPreferences p,String sender,boolean paused){
        SharedPreferences.Editor e=p.edit().putBoolean("human_sender_"+key(p,sender),paused);
        if(!paused)e.remove("fallback_used_"+key(p,sender));
        if(!e.commit())throw new IllegalStateException("Could not save customer automation state");
    }
    static synchronized boolean claimFallback(SharedPreferences p,String sender){
        String key="fallback_used_"+key(p,sender);
        if(p.getBoolean(key,false))return false;
        // Persist before dispatch: a failure or process restart must not produce another fallback.
        return p.edit().putBoolean(key,true).commit();
    }
}
