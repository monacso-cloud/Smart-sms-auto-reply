package com.smartreply.beta;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244,247,251));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(24));

        TextView title = text("ReplyDesk: Business SMS Bot", 28, true);
        content.addView(title);

        TextView subtitle = text("Business SMS automation dashboard", 15, false);
        subtitle.setTextColor(Color.rgb(82,98,120));
        subtitle.setPadding(0, dp(4), 0, dp(14));
        content.addView(subtitle);

        Switch master = new Switch(this);
        master.setText("ReplyDesk Bot");
        master.setTextSize(18);
        master.setChecked(prefs.getBoolean("chatbot_enabled", false) || prefs.getBoolean("enabled", false));
        master.setPadding(dp(14), dp(12), dp(14), dp(12));
        master.setBackgroundColor(Color.WHITE);
        master.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("chatbot_enabled", checked).putBoolean("enabled", checked).apply();
        });
        content.addView(master, matchWrap());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setPadding(0, dp(14), 0, dp(8));
        content.addView(stats);
        addStat(stats, "Missed calls today", prefs.getInt("stat_missed_calls_today", 0));
        addStat(stats, "SMS received today", prefs.getInt("stat_sms_received_today", 0));
        addStat(stats, "Bot replies today", prefs.getInt("stat_bot_replies_today", 0));
        addStat(stats, "Unmatched questions", prefs.getInt("stat_unmatched_today", 0));
        addStat(stats, "Staff assistance requests", prefs.getInt("stat_staff_required_today", 0));
        addStat(stats, "Failed SMS", prefs.getInt("stat_failed_today", 0));

        TextView permissions = text("Permissions: " + (hasCorePermissions() ? "Ready" : "Action required"), 15, true);
        permissions.setPadding(dp(14), dp(12), dp(14), dp(12));
        permissions.setBackgroundColor(hasCorePermissions() ? Color.rgb(231,246,239) : Color.rgb(255,244,229));
        permissions.setOnClickListener(v -> open("permissions"));
        content.addView(permissions, matchWrap());

        String[][] cards = {
                {"Missed Call Auto Reply","missed_call"},
                {"SMS Auto Bot","sms_bot"},
                {"Auto Reply Library","library"},
                {"Staff Assistance","staff"},
                {"Department Routing / Forwarding","department_routing"},
                {"Business Hours","business_hours"},
                {"After-Hours Reply","after_hours"},
                {"Conversations","conversations"},
                {"Activity & Call Logs","activity"},
                {"Test Bot","test_bot"},
                {"Message Templates","templates"},
                {"Contacts / Customers","contacts"},
                {"Scheduled Messages","scheduled"},
                {"Business Profile / Logo","business_profile"},
                {"Automation Schedule","automation_schedule"},
                {"Bot Settings","bot_settings"},
                {"Permissions & Diagnostics","permissions"},
                {"Subscription / Account","account"},
                {"Help & Support","help"},
                {"Suggest a Feature / Contact Us","feature_request"}
        };

        for (String[] card : cards) {
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(card[0] + "   ›");
            b.setTextSize(16);
            b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            b.setPadding(dp(16),0,dp(16),0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58));
            lp.setMargins(0, dp(8), 0, 0);
            b.setOnClickListener(v -> open(card[1]));
            content.addView(b, lp);
        }

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(bottomNav("home"));
        setContentView(root);
    }

    private void addStat(LinearLayout parent, String label, int value) {
        TextView row = text(label + "    " + value, 15, true);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0,0,0,dp(6));
        parent.addView(row, lp);
    }

    private boolean hasCorePermissions() {
        return checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private LinearLayout bottomNav(String selected) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.WHITE);
        String[][] items = {{"HOME","home"},{"CONVERSATIONS","conversations"},{"AUTO BOT","sms_bot"},{"ACTIVITY","activity"},{"SETTINGS","bot_settings"}};
        for (String[] item : items) {
            Button b = new Button(this);
            b.setText(item[0]);
            b.setAllCaps(false);
            b.setTextSize(11);
            b.setEnabled(!selected.equals(item[1]));
            b.setOnClickListener(v -> {
                if ("home".equals(item[1])) {
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                } else {
                    open(item[1]);
                }
            });
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(58), 1f));
        }
        return nav;
    }

    private void open(String page) {
        Intent i = new Intent(this, SectionActivity.class);
        i.putExtra("page", page);
        startActivity(i);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(11,31,58));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
