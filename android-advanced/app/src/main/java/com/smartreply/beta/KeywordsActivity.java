package com.smartreply.beta;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class KeywordsActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keywords);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        EditText editor = findViewById(R.id.keywordRulesEditor);
        EditText keywordsInput = findViewById(R.id.newKeywordsInput);
        EditText replyInput = findViewById(R.id.newReplyInput);
        editor.setText(prefs.getString("chatbot_rules", getString(R.string.default_chatbot_rules)));

        ((Button) findViewById(R.id.addKeywordRuleButton)).setOnClickListener(v -> {
            String keywords = keywordsInput.getText().toString().trim();
            String reply = replyInput.getText().toString().trim();
            if (keywords.isEmpty()) {
                keywordsInput.setError("Add at least one keyword");
                return;
            }
            if (reply.isEmpty()) {
                replyInput.setError("Add the reply");
                return;
            }
            String existing = editor.getText().toString().trim();
            String rule = keywords + "=>" + reply;
            editor.setText(existing.isEmpty() ? rule : existing + "\\n" + rule);
            keywordsInput.setText("");
            replyInput.setText("");
        });

        ((Button) findViewById(R.id.saveKeywordsButton)).setOnClickListener(v -> {
            prefs.edit().putString("chatbot_rules", editor.getText().toString().trim()).apply();
            Toast.makeText(this, "Keywords saved", Toast.LENGTH_SHORT).show();
        });
    }
}
