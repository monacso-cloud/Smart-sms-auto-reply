package com.smartreply.beta;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class AutoReplySettingsActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";
    private Switch masterSwitch;
    private Switch missedSwitch;
    private Switch smsSwitch;
    private Switch autoDeleteSwitch;
    private Spinner retentionSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_reply_settings);

        masterSwitch = findViewById(R.id.masterReplySwitch);
        missedSwitch = findViewById(R.id.missedCallReplySwitch);
        smsSwitch = findViewById(R.id.incomingSmsReplySwitch);
        autoDeleteSwitch = findViewById(R.id.autoDeleteLogsSwitch);
        retentionSpinner = findViewById(R.id.retentionSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.retention_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        retentionSpinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
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
            int[] values = {7, 14, 30};
            int retentionDays = values[Math.max(0, Math.min(retentionSpinner.getSelectedItemPosition(), 2))];
            prefs.edit()
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
}
