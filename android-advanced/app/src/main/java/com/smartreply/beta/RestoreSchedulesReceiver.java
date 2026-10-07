package com.smartreply.beta;
import android.content.*;
public class RestoreSchedulesReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        String a=i.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)
            ||"android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(a))
            ScheduledSmsStore.restore(c,Intent.ACTION_BOOT_COMPLETED.equals(a));
    }
}
