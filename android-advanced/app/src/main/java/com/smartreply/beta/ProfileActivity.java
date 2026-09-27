package com.smartreply.beta;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

/** Pins an editor to its explicit business ID, including across recreation. */
public abstract class ProfileActivity extends Activity {
    protected int businessId;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        businessId = getIntent().getIntExtra(BusinessProfiles.EXTRA, -1);
        if (!BusinessProfiles.exists(this, businessId)) {
            startActivity(new Intent(this, BusinessListActivity.class));
            finish();
            // Non-exported activities are only opened with a registered ID.
            throw new IllegalStateException("Missing business profile");
        }
    }
    @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        if (BusinessProfiles.LEGACY.equals(name)) return BusinessProfiles.prefs(this, businessId);
        return super.getSharedPreferences(name, mode);
    }
    @Override public void setContentView(int layout) {
        View content = getLayoutInflater().inflate(layout, null);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setFitsSystemWindows(true);
        wrapper.setBackgroundColor(0xfff3f6fb);
        TextView label = new TextView(this);
        label.setText("‹  " + BusinessProfiles.summary(this, businessId));
        label.setTextSize(16); label.setTextColor(0xff164451);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        label.setPadding(pad, pad, pad, pad);
        label.setOnClickListener(v -> finish());
        wrapper.addView(label);
        content.setFitsSystemWindows(false);
        wrapper.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        super.setContentView(wrapper);
    }
    protected void openProfile(Class<?> target) {
        startActivity(new Intent(this, target).putExtra(BusinessProfiles.EXTRA, businessId));
    }
}
