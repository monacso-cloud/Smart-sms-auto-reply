package com.smartreply.beta;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class AutoReplySettingsActivity extends ProfileActivity {
    private static final String PREFS = "smart_reply_settings";
    private Switch masterSwitch;
    private Switch missedSwitch;
    private Switch smsSwitch;
    private Switch autoDeleteSwitch;
    private Spinner retentionSpinner;
    private EditText missedCallMessageInput;
    private EditText smsReplyMessageInput;
    private EditText plainSmsMessageInput;
    private Switch smsChatbotModeSwitch;
    private Switch missedCallMenuSwitch;
    private boolean refreshBotMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_reply_settings);

        findViewById(R.id.backToDashboardButton).setOnClickListener(v -> finish());
        plainSmsMessageInput = findViewById(R.id.plainSmsMessageInput);
        smsChatbotModeSwitch = findViewById(R.id.smsChatbotModeSwitch);
        missedCallMenuSwitch = findViewById(R.id.missedCallMenuSwitch);
        missedCallMessageInput = findViewById(R.id.missedCallMessageInput);
        smsReplyMessageInput = findViewById(R.id.smsReplyMessageInput);
        masterSwitch = findViewById(R.id.masterReplySwitch);
        missedSwitch = findViewById(R.id.missedCallReplySwitch);
        smsSwitch = findViewById(R.id.incomingSmsReplySwitch);
        autoDeleteSwitch = findViewById(R.id.autoDeleteLogsSwitch);
        findViewById(R.id.openChatbotSetupButton).setOnClickListener(v -> openBotSetup());
        retentionSpinner = findViewById(R.id.retentionSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.retention_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        retentionSpinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        plainSmsMessageInput.setText(prefs.getString("sms_reply_message", getString(R.string.default_plain_sms_reply)));
        smsChatbotModeSwitch.setChecked(prefs.getBoolean("sms_chatbot_mode", prefs.getBoolean("chatbot_enabled", false)));
        missedCallMenuSwitch.setChecked(prefs.getBoolean("missed_call_include_menu", prefs.getBoolean("menu_enabled", false)));
        updateSmsMode();
        smsChatbotModeSwitch.setOnCheckedChangeListener((button, checked) -> updateSmsMode());
        missedCallMessageInput.setText(prefs.getString("message", getString(R.string.default_message)));
        smsReplyMessageInput.setText(prefs.getString("chatbot_fallback", getString(R.string.default_chatbot_fallback)));
        masterSwitch.setChecked(prefs.contains("master_enabled")
                ? prefs.getBoolean("master_enabled", false)
                : prefs.getBoolean("enabled", false));
        missedSwitch.setChecked(prefs.getBoolean("reply_to_missed_calls", true));
        smsSwitch.setChecked(prefs.contains("reply_to_incoming_sms")
                ? prefs.getBoolean("reply_to_incoming_sms", false)
                : prefs.getBoolean("chatbot_enabled", false));
        autoDeleteSwitch.setChecked(prefs.getBoolean("auto_delete_logs", true));

        int days = prefs.getInt("call_log_retention_days", 14);
        retentionSpinner.setSelection(days == 7 ? 0 : days == 30 ? 2 : 1);

        ((Button) findViewById(R.id.saveAutoReplyButton)).setOnClickListener(v -> {
            String missedMessage = missedCallMessageInput.getText().toString().trim();
            String plainSmsMessage = plainSmsMessageInput.getText().toString().trim();
            String smsMessage = smsReplyMessageInput.getText().toString().trim();
            if (missedSwitch.isChecked() && missedMessage.isEmpty()) {
                missedCallMessageInput.setError("Enter the message to send after a missed call");
                missedCallMessageInput.requestFocus();
                return;
            }
            if (smsSwitch.isChecked() && !smsChatbotModeSwitch.isChecked() && plainSmsMessage.isEmpty()) {
                plainSmsMessageInput.setError("Enter the message to send after an incoming SMS");
                plainSmsMessageInput.requestFocus();
                return;
            }
            if (smsSwitch.isChecked() && smsChatbotModeSwitch.isChecked() && smsMessage.isEmpty()) {
                smsReplyMessageInput.setError("Enter the default SMS reply");
                smsReplyMessageInput.requestFocus();
                return;
            }
            if (KeywordRules.containsRule(plainSmsMessage) || KeywordRules.containsRule(smsMessage)) {
                new android.app.AlertDialog.Builder(this).setTitle("Organise your saved answers")
                    .setMessage("Your answers are safe. Chatbot setup can move them into separate cards for you.")
                    .setNegativeButton("Keep editing",null)
                    .setPositiveButton("Open chatbot setup",(d,w)->openBotSetup()).show();
                return;
            }
            if (KeywordRules.containsRule(missedMessage)) {
                missedCallMessageInput.setError("This field sends one message. Remove keyword rules.");
                missedCallMessageInput.requestFocus();return;
            }
            int[] values = {7, 14, 30};
            int retentionDays = values[Math.max(0, Math.min(retentionSpinner.getSelectedItemPosition(), 2))];
            prefs.edit()
                    .putString("sms_reply_message", plainSmsMessage)
                    .putBoolean("sms_chatbot_mode", smsChatbotModeSwitch.isChecked())
                    .putBoolean("missed_call_include_menu", missedCallMenuSwitch.isChecked())
                    .putString("message", missedMessage)
                    .putString("chatbot_fallback", smsMessage)
                    .putBoolean("enabled", masterSwitch.isChecked())
                    .putBoolean("chatbot_enabled", smsSwitch.isChecked())
                    .putBoolean("master_enabled", masterSwitch.isChecked())
                    .putBoolean("reply_to_missed_calls", missedSwitch.isChecked())
                    .putBoolean("reply_to_incoming_sms", smsSwitch.isChecked())
                    .putBoolean("auto_delete_logs", autoDeleteSwitch.isChecked())
                    .putInt("call_log_retention_days", retentionDays)
                    .apply();
            AppCallLogStore.purge(this);
            Toast.makeText(this, "Auto reply settings saved", Toast.LENGTH_SHORT).show();
        });
    }
    private void openBotSetup(){refreshBotMode=true;openProfile(KeywordsActivity.class);}
    @Override protected void onResume(){
        super.onResume();
        if(smsReplyMessageInput==null)return;
        SharedPreferences p=BusinessProfiles.prefs(this,businessId);
        if(refreshBotMode){
            masterSwitch.setChecked(p.getBoolean("master_enabled",false));
            smsSwitch.setChecked(p.getBoolean("reply_to_incoming_sms",false));
            smsChatbotModeSwitch.setChecked(p.getBoolean("sms_chatbot_mode",false));
            refreshBotMode=false;
        }
        // Refresh only fields that were recovered while this screen was behind the setup screen.
        if(KeywordRules.containsRule(smsReplyMessageInput.getText().toString()) && !KeywordRules.containsRule(p.getString("chatbot_fallback","")))
            smsReplyMessageInput.setText(p.getString("chatbot_fallback",getString(R.string.default_chatbot_fallback)));
        if(KeywordRules.containsRule(plainSmsMessageInput.getText().toString()) && !KeywordRules.containsRule(p.getString("sms_reply_message","")))
            plainSmsMessageInput.setText(p.getString("sms_reply_message",getString(R.string.default_plain_sms_reply)));
        refreshSetupHint();
    }
    private void refreshSetupHint(){
        boolean wrong=KeywordRules.containsRule(smsReplyMessageInput.getText().toString()) || KeywordRules.containsRule(plainSmsMessageInput.getText().toString());
        ((android.widget.TextView)findViewById(R.id.chatbotSetupHint)).setText(wrong
            ? "Your saved answers need organising. Tap below to move them into answer cards without copying text."
            : "Set up your chatbot with simple question-and-answer cards and try a customer question.");
        smsReplyMessageInput.setVisibility(KeywordRules.containsRule(smsReplyMessageInput.getText().toString())?android.view.View.GONE:android.view.View.VISIBLE);
        plainSmsMessageInput.setVisibility(KeywordRules.containsRule(plainSmsMessageInput.getText().toString())?android.view.View.GONE:android.view.View.VISIBLE);
    }
    private void updateSmsMode() {
        boolean chatbot = smsChatbotModeSwitch.isChecked();
        findViewById(R.id.plainSmsSection).setVisibility(chatbot ? android.view.View.GONE : android.view.View.VISIBLE);
        findViewById(R.id.chatbotFallbackSection).setVisibility(chatbot ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
