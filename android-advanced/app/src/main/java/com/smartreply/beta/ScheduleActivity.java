package com.smartreply.beta;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ScheduleActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";
    private int startMinutes;
    private int endMinutes;
    private TextView startText;
    private TextView endText;
    private final int[] dayIds = {
            R.id.daySunday, R.id.dayMonday, R.id.dayTuesday, R.id.dayWednesday,
            R.id.dayThursday, R.id.dayFriday, R.id.daySaturday
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String mode = prefs.getString("schedule_mode", "always");
        ((RadioButton) findViewById(R.id.scheduleAlways)).setChecked("always".equals(mode));
        ((RadioButton) findViewById(R.id.scheduleCustom)).setChecked("custom_hours".equals(mode));
        ((RadioButton) findViewById(R.id.scheduleOff)).setChecked("off".equals(mode));

        startMinutes = prefs.getInt("schedule_start_minutes", 18 * 60);
        endMinutes = prefs.getInt("schedule_end_minutes", 6 * 60);
        startText = findViewById(R.id.startTimeText);
        endText = findViewById(R.id.endTimeText);
        renderTimes();

        for (int i = 0; i < dayIds.length; i++) {
            ((CheckBox) findViewById(dayIds[i])).setChecked(prefs.getBoolean("schedule_day_" + i, true));
        }

        ((Button) findViewById(R.id.presetMorningButton)).setOnClickListener(v -> setPreset(8 * 60, 12 * 60));
        ((Button) findViewById(R.id.presetAfternoonButton)).setOnClickListener(v -> setPreset(12 * 60, 17 * 60));
        ((Button) findViewById(R.id.presetEveningButton)).setOnClickListener(v -> setPreset(17 * 60, 22 * 60));
        ((Button) findViewById(R.id.presetAfterHoursButton)).setOnClickListener(v -> setPreset(18 * 60, 6 * 60));

        startText.setOnClickListener(v -> pickTime(true));
        endText.setOnClickListener(v -> pickTime(false));

        ((Button) findViewById(R.id.saveScheduleButton)).setOnClickListener(v -> {
            String selectedMode = ((RadioButton) findViewById(R.id.scheduleOff)).isChecked()
                    ? "off"
                    : ((RadioButton) findViewById(R.id.scheduleCustom)).isChecked()
                    ? "custom_hours"
                    : "always";

            SharedPreferences.Editor edit = prefs.edit()
                    .putString("schedule_mode", selectedMode)
                    .putInt("schedule_start_minutes", startMinutes)
                    .putInt("schedule_end_minutes", endMinutes);
            for (int i = 0; i < dayIds.length; i++) {
                edit.putBoolean("schedule_day_" + i, ((CheckBox) findViewById(dayIds[i])).isChecked());
            }
            edit.apply();
            Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show();
        });
    }

    private void setPreset(int start, int end) {
        startMinutes = start;
        endMinutes = end;
        ((RadioButton) findViewById(R.id.scheduleCustom)).setChecked(true);
        renderTimes();
    }

    private void pickTime(boolean start) {
        int current = start ? startMinutes : endMinutes;
        new TimePickerDialog(this, (view, hour, minute) -> {
            if (start) startMinutes = hour * 60 + minute;
            else endMinutes = hour * 60 + minute;
            renderTimes();
        }, current / 60, current % 60, true).show();
    }

    private void renderTimes() {
        startText.setText(format(startMinutes));
        endText.setText(format(endMinutes));
    }

    private String format(int minutes) {
        return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60);
    }
}
