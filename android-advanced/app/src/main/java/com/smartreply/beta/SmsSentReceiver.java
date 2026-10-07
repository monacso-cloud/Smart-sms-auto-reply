package com.smartreply.beta;
import android.app.Activity;
import android.content.*;
import android.telephony.SmsManager;
public class SmsSentReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        int code=getResultCode();
        String reason=code==SmsManager.RESULT_ERROR_NO_SERVICE ? "No mobile service" :
            code==SmsManager.RESULT_ERROR_RADIO_OFF ? "Mobile radio is off" :
            code==SmsManager.RESULT_ERROR_LIMIT_EXCEEDED ? "Android SMS sending limit reached" : "Android error code "+code;
        DeliveryTracker.result(c,i.getStringExtra("delivery_token"),i.getIntExtra("part",0),code==Activity.RESULT_OK,reason);
    }
}
