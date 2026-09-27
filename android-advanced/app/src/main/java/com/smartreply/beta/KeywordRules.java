package com.smartreply.beta;

import java.util.*;
import org.json.*;

/** Reads legacy rules and editable answer cards without putting configuration in an SMS. */
final class KeywordRules {
    private KeywordRules() {}
    static final class Rule {
        final String keywords, reply;
        final boolean enabled;
        Rule(String keywords,String reply,boolean enabled){this.keywords=keywords.trim();this.reply=reply.trim();this.enabled=enabled;}
        Rule(String keywords,String reply){this(keywords,reply,true);}
    }
    static boolean containsRule(String text) { return text!=null && text.contains("=>"); }
    static String normalize(String rules) {
        if(rules==null)return "";
        return rules.replaceAll("(?i)(POWERED BY:\\s*ReplyDesk\\s*-\\s*Business SMS Bot)\\s+(?=[^\\n=]{1,180}=>)","$1\n");
    }
    static List<Rule> parse(String raw) {
        List<Rule> rules=new ArrayList<>();
        if(raw==null || raw.trim().isEmpty())return rules;
        if(raw.trim().startsWith("[")){
            try{
                JSONArray a=new JSONArray(raw);
                for(int i=0;i<a.length();i++){
                    JSONObject o=a.getJSONObject(i);
                    Rule r=new Rule(o.getString("keywords"),o.getString("reply"),o.optBoolean("enabled",true));
                    validate(r);rules.add(r);
                }
                return rules;
            }catch(JSONException e){throw new IllegalArgumentException("Some saved answers could not be read. Your original text has been kept.");}
        }
        String keys=null;StringBuilder reply=new StringBuilder();
        for(String line:normalize(raw).split("\\r?\\n")){
            int at=line.indexOf("=>");
            if(at>=0){
                if(at==0 || line.indexOf("=>",at+2)>=0)throw new IllegalArgumentException("Some answers are joined together. Review the saved text before importing.");
                if(keys!=null){Rule r=new Rule(keys,reply.toString());validate(r);rules.add(r);}
                keys=line.substring(0,at);reply=new StringBuilder(line.substring(at+2).trim());
            }else if(keys!=null){reply.append('\n').append(line);}
            else if(!line.trim().isEmpty())throw new IllegalArgumentException("Some saved text is not an answer rule. Your original text has been kept.");
        }
        if(keys!=null){Rule r=new Rule(keys,reply.toString());validate(r);rules.add(r);}
        return rules;
    }
    static void validate(Rule r){
        if(r.keywords.replace(",","").trim().isEmpty())throw new IllegalArgumentException("Add a word or phrase customers may use.");
        if(r.reply.isEmpty())throw new IllegalArgumentException("Write the answer customers should receive.");
        if(containsRule(r.keywords)||containsRule(r.reply))throw new IllegalArgumentException("Enter words in the first box and one answer in the second box.");
    }
    static String encode(List<Rule> rules){
        JSONArray a=new JSONArray();
        try{for(Rule r:rules){validate(r);a.put(new JSONObject().put("keywords",r.keywords).put("reply",r.reply).put("enabled",r.enabled));}}
        catch(JSONException e){throw new IllegalStateException(e);}
        return a.toString();
    }
    static String find(String incoming,String rules){
        String q=incoming==null?"":incoming.trim().toLowerCase(Locale.ROOT);
        try{for(Rule r:parse(rules)){
            if(!r.enabled)continue;
            for(String key:r.keywords.split(",")){
                String word=key.trim().toLowerCase(Locale.ROOT);
                if(!word.isEmpty() && q.contains(word))return r.reply;
            }
        }}catch(IllegalArgumentException ignored){}
        return null;
    }
}
