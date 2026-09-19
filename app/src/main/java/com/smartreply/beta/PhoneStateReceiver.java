package com.smartreply.beta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class PhoneStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (!TelephonyManager.EXTRA_STATE_IDLE.equals(state)) return;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MissedCallReplyWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }
}
