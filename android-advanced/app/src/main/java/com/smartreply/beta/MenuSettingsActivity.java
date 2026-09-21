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
        EditText item1 = findViewById(R.id.menuItem1Input);
        EditText reply1 = findViewById(R.id.menuReply1Input);
        EditText item2 = findViewById(R.id.menuItem2Input);
        EditText reply2 = findViewById(R.id.menuReply2Input);
        EditText item3 = findViewById(R.id.menuItem3Input);
        EditText reply3 = findViewById(R.id.menuReply3Input);
        EditText item4 = findViewById(R.id.menuItem4Input);
        EditText reply4 = findViewById(R.id.menuReply4Input);

        enabled.setChecked(prefs.getBoolean("menu_enabled", false));
        intro.setText(prefs.getString("menu_intro", "How can we help? Reply with a number:"));
        item1.setText(prefs.getString("menu_item_1", "Service prices"));
        reply1.setText(prefs.getString("menu_reply_1", "Our service prices start from [PRICE]. More information: [WEBSITE]."));
        item2.setText(prefs.getString("menu_item_2", "Availability"));
        reply2.setText(prefs.getString("menu_reply_2", "Please check current availability here: [WEBSITE]."));
        item3.setText(prefs.getString("menu_item_3", "Booking or service information"));
        reply3.setText(prefs.getString("menu_reply_3", "You can find booking or service information here: [WEBSITE]."));
        item4.setText(prefs.getString("menu_item_4", "Human assistance"));
        reply4.setText(prefs.getString("menu_reply_4", "Thanks. A team member will help you as soon as possible."));

        ((Button) findViewById(R.id.saveMenuButton)).setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean("menu_enabled", enabled.isChecked())
                    .putString("menu_intro", intro.getText().toString().trim())
                    .putString("menu_item_1", item1.getText().toString().trim())
                    .putString("menu_reply_1", reply1.getText().toString().trim())
                    .putString("menu_item_2", item2.getText().toString().trim())
                    .putString("menu_reply_2", reply2.getText().toString().trim())
                    .putString("menu_item_3", item3.getText().toString().trim())
                    .putString("menu_reply_3", reply3.getText().toString().trim())
                    .putString("menu_item_4", item4.getText().toString().trim())
                    .putString("menu_reply_4", reply4.getText().toString().trim())
                    .apply();
            Toast.makeText(this, "Menu settings saved", Toast.LENGTH_SHORT).show();
        });
    }
}
