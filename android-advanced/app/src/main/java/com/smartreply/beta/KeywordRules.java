package com.smartreply.beta;

import java.util.Locale;

/** Keyword rules are configuration, never an outgoing message. */
final class KeywordRules {
    private KeywordRules() {}

    static boolean containsRule(String text) {
        return text != null && text.contains("=>");
    }

    static String normalize(String rules) {
        if (rules == null) return "";
        // Older imports may have joined every clinic rule into a single line.
        return rules.replaceAll("(?i)(POWERED BY:\\s*ReplyDesk\\s*-\\s*Business SMS Bot)\\s+(?=[^\\n=]{1,180}=>)", "$1\n");
    }

    static String find(String incoming, String rules) {
        String question=incoming == null ? "" : incoming.trim().toLowerCase(Locale.ROOT);
        for(String line:normalize(rules).split("\\r?\\n")) {
            int at=line.indexOf("=>");
            if(at<=0 || line.indexOf("=>",at+2)>=0)continue;
            String answer=line.substring(at+2).trim();
            if(answer.isEmpty())continue;
            for(String keyword:line.substring(0,at).split(",")) {
                String word=keyword.trim().toLowerCase(Locale.ROOT);
                if(!word.isEmpty() && question.contains(word))return answer;
            }
        }
        return null;
    }
}
