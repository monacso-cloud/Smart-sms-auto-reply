package com.smartreply.beta;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
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
        long now = System.currentTimeMillis();
        int position = periodSpinner.getSelectedItemPosition();
        int days = position == 0 ? 7 : position == 2 ? 30 : 14;
        long cutoff = now - days * 24L * 60L * 60L * 1000L;

        StringBuilder text = new StringBuilder();
        text.append("PHONE CALL LOG\n");

        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            try (Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[] {CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE},
                    CallLog.Calls.DATE + " >= ?",
                    new String[] {String.valueOf(cutoff)},
                    CallLog.Calls.DATE + " DESC")) {

                if (cursor != null) {
                    int count = 0;
                    while (cursor.moveToNext() && count < 300) {
                        String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                        int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                        text.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(new Date(timestamp)))
                                .append("  •  ")
                                .append(callType(type))
                                .append("  •  ")
                                .append(mask(number))
                                .append("\n");
                        count++;
                    }
                }
            } catch (Exception error) {
                text.append("Unable to read phone call history.\n");
            }
        } else {
            text.append("Call Log permission is required to show phone call history.\n");
        }

        text.append("\nREPLYDESK AUTO-REPLY HISTORY\n");
        JSONArray logs = AppCallLogStore.get(this);
        boolean hasReplyDeskLogs = false;
        for (int i = logs.length() - 1; i >= 0; i--) {
            JSONObject item = logs.optJSONObject(i);
            if (item == null) continue;
            long timestamp = item.optLong("timestamp", 0);
            if (timestamp < cutoff) continue;
            hasReplyDeskLogs = true;
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
        if (!hasReplyDeskLogs) text.append("No ReplyDesk auto-reply events in this period.\n");

        logText.setText(text.toString());
    }

    private String callType(int type) {
        if (type == CallLog.Calls.MISSED_TYPE) return "Missed";
        if (type == CallLog.Calls.INCOMING_TYPE) return "Incoming";
        if (type == CallLog.Calls.OUTGOING_TYPE) return "Outgoing";
        if (type == CallLog.Calls.REJECTED_TYPE) return "Rejected";
        return "Call";
    }

    private String mask(String number) {
        if (number == null || number.length() < 4) return "Unknown";
        return "••••" + number.substring(number.length() - 4);
    }
}
