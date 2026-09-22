package com.smartreply.beta;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

public class TestBotActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_bot);

        EditText input = findViewById(R.id.testMessageInput);
        TextView result = findViewById(R.id.testBotResult);

        ((Button) findViewById(R.id.runTestButton)).setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String incoming = input.getText().toString().trim().toLowerCase(Locale.ROOT);
            String menuReply = null;
            if (prefs.getBoolean("menu_enabled", false) && incoming.matches("(10|[1-9])")) {
                String candidate = prefs.getString("menu_reply_" + incoming, "").trim();
                if (!candidate.isEmpty()) menuReply = candidate;
            }

            if (menuReply != null) {
                result.setText("Matched menu option: " + incoming + "\n\nReply:\n" + menuReply);
                return;
            }

            Match match = findReply(incoming, prefs.getString("chatbot_rules", ""));
            if (match == null) {
                result.setText("No keyword matched.\n\nFallback reply:\n" +
                        prefs.getString("chatbot_fallback", getString(R.string.default_chatbot_fallback)));
            } else {
                result.setText("Matched keyword: " + match.keyword + "\n\nReply:\n" + match.reply);
            }
        });
    }

    private Match findReply(String incoming, String rules) {
        Match best = null;
        for (String line : rules.split("\\r?\\n")) {
            int separator = line.indexOf("=>");
            if (separator <= 0) continue;
            String keywords = line.substring(0, separator);
            String reply = line.substring(separator + 2).trim();
            for (String keyword : keywords.split(",")) {
                String clean = keyword.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty() && incoming.contains(clean)) {
                    if (best == null || clean.length() > best.keyword.length()) {
                        best = new Match(clean, reply);
                    }
                }
            }
        }
        return best;
    }

    private static final class Match {
        final String keyword;
        final String reply;
        Match(String keyword, String reply) {
            this.keyword = keyword;
            this.reply = reply;
        }
    }
}
