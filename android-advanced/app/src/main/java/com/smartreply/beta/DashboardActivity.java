package com.smartreply.beta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class DashboardActivity extends ProfileActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bind(R.id.openExistingButton, MainActivity.class);
        bind(R.id.openAutoReplyButton, AutoReplySettingsActivity.class);
        bind(R.id.openKeywordsButton, KeywordsActivity.class);
        bind(R.id.openMenuButton, MenuSettingsActivity.class);
        bind(R.id.openScheduleButton, ScheduleActivity.class);
        bind(R.id.openCallLogsButton, CallLogsActivity.class);
        bind(R.id.openTestBotButton, TestBotActivity.class);
        bind(R.id.openAccountButton, AccountActivity.class);
        bind(R.id.openLegalButton, LegalSupportActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.content.SharedPreferences prefs = getSharedPreferences("smart_reply_settings", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("master_enabled", prefs.getBoolean("enabled", false));
        android.widget.TextView status = findViewById(R.id.dashboardReplyStatus);
        android.widget.TextView detail = findViewById(R.id.dashboardReplyDetail);
        status.setText(!SimRouter.active(this, businessId) ? "SIM inactive — replies paused" : enabled ? "Automatic replies enabled" : "Your replies are paused");
        boolean missed = prefs.getBoolean("reply_to_missed_calls", true);
        boolean sms = prefs.getBoolean("reply_to_incoming_sms", prefs.getBoolean("chatbot_enabled", false));
        boolean chatbot = prefs.getBoolean("sms_chatbot_mode", prefs.getBoolean("chatbot_enabled", false));
        detail.setText("Missed calls: " + (missed ? "on" : "off")
                + "\nIncoming SMS: " + (sms ? (chatbot ? "chatbot" : "plain message") : "off")
                + "\nYour schedule and phone permissions also apply.");
    }

    private void bind(int id, Class<?> target) {
        Button button = findViewById(id);
        button.setOnClickListener(v -> openProfile(target));
    }
}
