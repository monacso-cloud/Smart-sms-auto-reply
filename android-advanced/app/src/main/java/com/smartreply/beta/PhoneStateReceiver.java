package com.smartreply.beta;
import android.content.*;
import android.telephony.TelephonyManager;

public class PhoneStateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if(TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(i.getAction()))
            Diagnostics.record(c,-1,"CALL_STATE",String.valueOf(i.getStringExtra(TelephonyManager.EXTRA_STATE)));
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(i.getAction())
                && TelephonyManager.EXTRA_STATE_IDLE.equals(i.getStringExtra(TelephonyManager.EXTRA_STATE))) {
            MissedCallJobService.scheduleScan(c, false);
        }
    }
}
