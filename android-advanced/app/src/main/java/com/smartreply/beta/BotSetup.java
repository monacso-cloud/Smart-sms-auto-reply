package com.smartreply.beta;

import android.content.*;
import java.util.*;

/** Per-business setup operations, with original settings retained before conversion. */
final class BotSetup {
    private BotSetup(){}
    static final String[] MISPLACED={"chatbot_fallback","sms_reply_message"};
    static boolean needsRecovery(SharedPreferences p){
        for(String key:MISPLACED)if(KeywordRules.containsRule(p.getString(key,"")))return true;
        return false;
    }
    static List<KeywordRules.Rule> read(SharedPreferences p){return KeywordRules.parse(p.getString("chatbot_rules",""));}
    static void backup(SharedPreferences p,SharedPreferences.Editor e,String key){
        if(!p.contains("setup_original_"+key) && p.contains(key))e.putString("setup_original_"+key,p.getString(key,""));
    }
    static void save(SharedPreferences p,List<KeywordRules.Rule> rules){
        String encoded=KeywordRules.encode(rules);SharedPreferences.Editor e=p.edit();backup(p,e,"chatbot_rules");
        if(!e.putString("chatbot_rules",encoded).commit())throw new IllegalStateException("Could not save. Please try again.");
    }
    static List<KeywordRules.Rule> recoveryPreview(SharedPreferences p){
        List<KeywordRules.Rule> all=new ArrayList<>();
        for(String key:MISPLACED){String text=p.getString(key,"");if(KeywordRules.containsRule(text))appendUnique(all,KeywordRules.parse(text));}
        appendUnique(all,read(p));return all;
    }
    private static void appendUnique(List<KeywordRules.Rule> target,List<KeywordRules.Rule> source){
        for(KeywordRules.Rule candidate:source){
            boolean found=false;
            for(KeywordRules.Rule existing:target)if(existing.keywords.equalsIgnoreCase(candidate.keywords) && existing.reply.equals(candidate.reply)){found=true;break;}
            if(!found)target.add(candidate);
        }
    }
    static void recover(Context c,SharedPreferences p){
        // Parse every source before modifying anything; an invalid source cannot be silently lost.
        List<KeywordRules.Rule> all=recoveryPreview(p);String encoded=KeywordRules.encode(all);
        SharedPreferences.Editor e=p.edit();backup(p,e,"chatbot_rules");e.putString("chatbot_rules",encoded);
        for(String key:MISPLACED)if(KeywordRules.containsRule(p.getString(key,""))){
            backup(p,e,key);e.putString(key,c.getString(key.equals("chatbot_fallback")?R.string.default_chatbot_fallback:R.string.default_plain_sms_reply));
        }
        if(!e.commit())throw new IllegalStateException("Could not organise your answers. Please try again.");
    }
}
