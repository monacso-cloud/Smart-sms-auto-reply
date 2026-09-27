package com.smartreply.beta;
import android.app.Activity;
import android.content.*;
public class SmsSentReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        int id=i.getIntExtra(BusinessProfiles.EXTRA,-1);
        if (!BusinessProfiles.exists(c,id)) return;
        String status = getResultCode()==Activity.RESULT_OK ? "Sent (delivery not confirmed)" : "SMS failed: code " + getResultCode();
        AppCallLogStore.add(c,id,i.getStringExtra("type"),i.getStringExtra("number"),
            status + " · part " + i.getIntExtra("part",1) + "/" + i.getIntExtra("parts",1));
    }
}
