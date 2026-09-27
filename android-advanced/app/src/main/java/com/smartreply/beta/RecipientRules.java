package com.smartreply.beta;
import android.content.*;
import android.telephony.PhoneNumberUtils;
import org.json.*;
import java.util.*;
public final class RecipientRules {
    private RecipientRules() {}
    public static String normal(SharedPreferences p,String number) {
        if(number==null) return "";
        String clean=PhoneNumberUtils.normalizeNumber(number.trim());
        String region=p.getString("country_iso",Locale.getDefault().getCountry()).toUpperCase(Locale.ROOT);
        String international=PhoneNumberUtils.formatNumberToE164(clean,region);
        return international==null ? clean : international;
    }
    public static List<String> numbers(SharedPreferences p,String input) {
        LinkedHashSet<String> result=new LinkedHashSet<>();
        for(String raw:input.split("[,;\n]+")) {
            if(raw.trim().isEmpty()) continue;
            if(!raw.trim().matches("[+0-9() .-]+")) throw new IllegalArgumentException("Enter phone numbers separated by commas or new lines");
            String n=normal(p,raw);
            if(!n.matches("\\+?[0-9]{7,15}")) throw new IllegalArgumentException("Check the phone number: "+raw.trim());
            result.add(n);
        }
        return new ArrayList<>(result);
    }
    public static JSONArray contacts(SharedPreferences p) {
        try{return new JSONArray(p.getString("saved_recipients","[]"));}catch(Exception e){return new JSONArray();}
    }
    public static String reason(SharedPreferences p,String number) {
        String target=normal(p,number);
        for(String n:p.getString("ignored_numbers","").split("[,;\n]+"))
            if(!n.trim().isEmpty() && target.equals(normal(p,n))) return "Sender is on the ignored list";
        if(!"selected".equals(p.getString("sender_filter","everyone"))) return "Ready";
        JSONArray list=contacts(p);
        for(int i=0;i<list.length();i++) if(target.equals(normal(p,list.optJSONObject(i).optString("phone")))) return "Ready";
        return "Sender is not in this business's recipient list";
    }
}
