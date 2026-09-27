package com.smartreply.beta;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.widget.*;
import java.util.*;

public class BusinessListActivity extends Activity {
    private LinearLayout body;
    @Override public void onCreate(Bundle state) { super.onCreate(state); }
    @Override public void onResume() { super.onResume(); render(); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private TextView text(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size);
        t.setTextColor(0xff123347); t.setPadding(0, dp(8), 0, dp(8)); return t;
    }
    private Button button(String s, LinearLayout parent, Runnable click) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false);
        b.setTextColor(0xffffffff); b.setBackgroundResource(R.drawable.rd_button);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10); b.setLayoutParams(lp); b.setMinHeight(dp(50));
        parent.addView(b); b.setOnClickListener(v -> click.run()); return b;
    }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(0xfff3f6fb);
        body = new LinearLayout(this); body.setOrientation(1); body.setPadding(dp(20),dp(20),dp(20),dp(28));
        scroll.addView(body); setContentView(scroll);
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.replydesk_logo);
        body.addView(logo, new LinearLayout.LayoutParams(dp(64),dp(64)));
        body.addView(text("Your businesses", 30));
        body.addView(text("One profile for each SIM or eSIM. Replies use the receiving SIM.", 16));
        List<SubscriptionInfo> active = SimRouter.active(this);
        for (SubscriptionInfo info : active) BusinessProfiles.register(this, info);
        body.addView(text(active.size() + " active SIMs · " + BusinessProfiles.ids(this).size() + " saved profiles", 16));
        List<String> ids = new ArrayList<>(BusinessProfiles.ids(this));
        Collections.sort(ids);
        for (String value : ids) {
            int id = Integer.parseInt(value);
            SharedPreferences p = BusinessProfiles.prefs(this, id);
            boolean available = SimRouter.active(this, id);
            LinearLayout card = new LinearLayout(this); card.setOrientation(1);
            card.setPadding(dp(18),dp(14),dp(18),dp(18)); card.setBackgroundResource(R.drawable.rd_card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(16);
            body.addView(card,lp);
            card.addView(text(BusinessProfiles.name(this,id),22));
            card.addView(text(p.getString("sim_label", "") + "\n" + (!available ? "Inactive — replies unavailable" :
                p.getBoolean("master_enabled", false) ? "Replies enabled" : "Replies paused"),15));
            button("Open business",card,() -> startActivity(new Intent(this,DashboardActivity.class).putExtra(BusinessProfiles.EXTRA,id)));
            Button rename = new Button(this); rename.setText("Rename business"); rename.setAllCaps(false); card.addView(rename);
            rename.setOnClickListener(v -> {
                EditText input = new EditText(this); input.setSingleLine(true); input.setText(BusinessProfiles.name(this,id));
                new AlertDialog.Builder(this).setTitle("Business name").setView(input)
                    .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
                        String name=input.getText().toString().trim();
                        if (!name.isEmpty()) p.edit().putString("business_name",name).apply(); render();
                    }).show();
            });
            if (BusinessProfiles.canImport(this)) button("Import my previous settings",card,() ->
                new AlertDialog.Builder(this).setTitle("Import into " + BusinessProfiles.name(this,id) + "?")
                    .setMessage("This replaces this profile's settings with your previous messages and rules. The original is kept. Replies stay paused until you enable them.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Import",(d,w)->{
                        BusinessProfiles.importLegacy(this,id); render();
                    }).show());
        }
        if (ids.isEmpty()) body.addView(text("Enable a SIM in your phone settings and allow Phone permission to create your first business.",16));
        button("Permissions & refresh SIMs",body,() -> {
            List<String> missing = new ArrayList<>();
            for (String permission : new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS})
                if (checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED) missing.add(permission);
            if (missing.isEmpty()) render(); else requestPermissions(missing.toArray(new String[0]),100);
        });
        button("Routing diagnostics",body,() -> {
            String detail = BusinessProfiles.index(this).getString("last_routing_issue","No routing issues recorded.");
            new AlertDialog.Builder(this).setTitle("Routing diagnostics").setMessage(detail).setPositiveButton("OK",null).show();
        });
        body.addView(text("Saved eSIMs can outnumber active lines. Enable a line in Android settings to use it. A newly issued SIM needs its own profile.",14));
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results); render();
    }
}
