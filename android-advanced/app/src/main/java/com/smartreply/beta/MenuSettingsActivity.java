package com.smartreply.beta;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class MenuSettingsActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Switch enabled = findViewById(R.id.menuEnabledSwitch);
        EditText intro = findViewById(R.id.menuIntroInput);

        int[] labelIds = {
                R.id.menuItem1Input, R.id.menuItem2Input, R.id.menuItem3Input, R.id.menuItem4Input, R.id.menuItem5Input,
                R.id.menuItem6Input, R.id.menuItem7Input, R.id.menuItem8Input, R.id.menuItem9Input, R.id.menuItem10Input
        };
        int[] replyIds = {
                R.id.menuReply1Input, R.id.menuReply2Input, R.id.menuReply3Input, R.id.menuReply4Input, R.id.menuReply5Input,
                R.id.menuReply6Input, R.id.menuReply7Input, R.id.menuReply8Input, R.id.menuReply9Input, R.id.menuReply10Input
        };

        String[] defaultLabels = {
                "Service prices",
                "Availability",
                "Booking or service information",
                "Human assistance",
                "",
                "",
                "",
                "",
                "",
                ""
        };
        String[] defaultReplies = {
                "Our service prices start from [PRICE]. More information: [WEBSITE].",
                "Please check current availability here: [WEBSITE].",
                "You can find booking or service information here: [WEBSITE].",
                "Thanks. A team member will help you as soon as possible.",
                "",
                "",
                "",
                "",
                "",
                ""
        };

        enabled.setChecked(prefs.getBoolean("menu_enabled", false));
        intro.setText(prefs.getString("menu_intro", "Automated assistant: How can we help? Reply with a number:"));

        EditText[] labels = new EditText[10];
        EditText[] replies = new EditText[10];
        for (int i = 0; i < 10; i++) {
            labels[i] = findViewById(labelIds[i]);
            replies[i] = findViewById(replyIds[i]);
            labels[i].setText(prefs.getString("menu_item_" + (i + 1), defaultLabels[i]));
            replies[i].setText(prefs.getString("menu_reply_" + (i + 1), defaultReplies[i]));
        }

        ((Button) findViewById(R.id.saveMenuButton)).setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit()
                    .putBoolean("menu_enabled", enabled.isChecked())
                    .putString("menu_intro", intro.getText().toString().trim());

            for (int i = 0; i < 10; i++) {
                editor.putString("menu_item_" + (i + 1), labels[i].getText().toString().trim());
                editor.putString("menu_reply_" + (i + 1), replies[i].getText().toString().trim());
            }

            editor.apply();
            Toast.makeText(this, "Menu settings saved", Toast.LENGTH_SHORT).show();
        });
    }
}
