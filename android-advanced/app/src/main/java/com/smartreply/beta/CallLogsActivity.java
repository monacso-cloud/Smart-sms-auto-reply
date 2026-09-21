package com.smartreply.beta;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class CallLogsActivity extends Activity {
    private TextView logText;
    private Spinner periodSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_logs);

        logText = findViewById(R.id.callLogText);
        periodSpinner = findViewById(R.id.callLogPeriodSpinner);

        ((Button) findViewById(R.id.refreshCallLogsButton)).setOnClickListener(v -> render());
        ((Button) findViewById(R.id.clearCallLogsButton)).setOnClickListener(v -> {
            AppCallLogStore.clear(this);
            render();
        });
        render();
    }

    private void render() {
        JSONArray logs = AppCallLogStore.get(this);
        long now = System.currentTimeMillis();
        int position = periodSpinner.getSelectedItemPosition();
        int days = position == 0 ? 7 : position == 2 ? 30 : 14;
        long cutoff = now - days * 24L * 60L * 60L * 1000L;

        StringBuilder text = new StringBuilder();
        for (int i = logs.length() - 1; i >= 0; i--) {
            JSONObject item = logs.optJSONObject(i);
            if (item == null) continue;
            long timestamp = item.optLong("timestamp", 0);
            if (timestamp < cutoff) continue;
            text.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(timestamp)))
                    .append("  •  ")
                    .append(item.optString("type"))
                    .append("  •  ")
                    .append(item.optString("maskedNumber"))
                    .append("  •  ")
                    .append(item.optString("replyStatus"))
                    .append("\n");
        }
        logText.setText(text.length() == 0 ? "No ReplyDesk call logs in this period." : text.toString());
    }
}
