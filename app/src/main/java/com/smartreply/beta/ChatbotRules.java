package com.smartreply.beta;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure routing logic, shared by the receiver and regression tests. */
public final class ChatbotRules {
    private ChatbotRules() {}

    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .replace('\u00a0', ' ').trim().toLowerCase(Locale.ROOT);
    }

    public static String findReply(String incoming, String rules) {
        String message = normalize(incoming);
        if (message.isEmpty() || rules == null) return null;
        boolean numberOnly = message.matches("[0-9]+");
        for (String line : rules.split("\\r?\\n")) {
            int separator = line.indexOf("=>");
            if (separator <= 0) continue;
            String reply = line.substring(separator + 2).trim();
            if (reply.isEmpty()) continue;
            for (String keyword : line.substring(0, separator).split(",")) {
                String clean = normalize(keyword);
                if (clean.isEmpty()) continue;
                // Numbers are exact choices, never substrings of a phone number or time.
                if (clean.matches("[0-9]+")) {
                    if (message.equals(clean)) return reply;
                } else if (!numberOnly && Pattern.compile("(?<![\\p{L}\\p{N}])"
                        + Pattern.quote(clean) + "(?![\\p{L}\\p{N}])")
                        .matcher(message).find()) {
                    return reply;
                }
            }
        }
        return null;
    }
}
