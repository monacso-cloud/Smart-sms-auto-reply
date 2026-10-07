package com.smartreply.beta;

import android.content.*;
import java.util.Locale;

/** Shared selection for preview and actual incoming SMS. This class never sends an SMS. */
final class BotReplies {
    private BotReplies(){}
    static final class Answer {
        final String text,source;
        Answer(String text,String source){this.text=text;this.source=source;}
    }
    static Answer safe(String text,String source){return new Answer(KeywordRules.containsRule(text)?null:text,source);}
    static Answer chatbot(Context c,SharedPreferences p,String message){
        String q=message.trim().toLowerCase(Locale.ROOT);
        if(p.getBoolean("menu_enabled",false) && q.matches("(10|[1-9])")){
            String menu=p.getString("menu_reply_"+q,"").trim();if(!menu.isEmpty())return safe(menu,"Numbered menu "+q);
        }
        String reply=KeywordRules.find(q,p.getString("chatbot_rules",""));
        if(reply!=null)return safe(reply,"Matched a saved answer");
        String fallback=p.getString("chatbot_fallback",c.getString(R.string.default_chatbot_fallback));
        if(KeywordRules.containsRule(fallback)){
            reply=KeywordRules.find(q,fallback);
            if(reply!=null)return safe(reply,"Matched an answer waiting to be organised");
            fallback=c.getString(R.string.default_chatbot_fallback);
        }
        return safe(fallback,"No match — general reply");
    }
    static Answer choose(Context c,SharedPreferences p,String message){
        if(p.getBoolean("sms_chatbot_mode",p.getBoolean("chatbot_enabled",false)))return chatbot(c,p,message);
        String plain=p.getString("sms_reply_message",c.getString(R.string.default_plain_sms_reply));
        return safe(KeywordRules.containsRule(plain)?KeywordRules.find(message,plain):plain,"Plain SMS reply");
    }
    static String preview(Context c,SharedPreferences p,String message,boolean bot){
        String q=message.trim().toLowerCase(Locale.ROOT);
        if(q.equals("stop")||q.equals("unsubscribe"))return "STOP would pause automatic replies for this sender. This preview changes nothing.";
        if(q.equals("start"))return "START would allow automatic replies for this sender again. This preview changes nothing.";
        Answer a=bot?chatbot(c,p,message):choose(c,p,message);
        if(a.text==null||a.text.trim().isEmpty())return "No safe reply is configured. Organise your saved answers first.";
        return a.source+"\n\n"+ReplySender.disclosure(a.text)+"\n\nPreview only — no SMS sent.";
    }
}
