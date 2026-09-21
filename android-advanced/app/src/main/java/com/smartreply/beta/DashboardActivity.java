package com.smartreply.beta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class DashboardActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bind(R.id.openExistingButton, MainActivity.class);
        bind(R.id.openAutoReplyButton, AutoReplySettingsActivity.class);
        bind(R.id.openKeywordsButton, KeywordsActivity.class);
        bind(R.id.openScheduleButton, ScheduleActivity.class);
        bind(R.id.openCallLogsButton, CallLogsActivity.class);
        bind(R.id.openTestBotButton, TestBotActivity.class);
    }

    private void bind(int id, Class<?> target) {
        Button button = findViewById(id);
        button.setOnClickListener(v -> startActivity(new Intent(this, target)));
    }
}
