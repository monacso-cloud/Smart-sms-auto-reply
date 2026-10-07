package com.smartreply.beta;
import android.content.*;
import java.util.concurrent.*;
public class ScheduledSmsReceiver extends BroadcastReceiver {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    @Override public void onReceive(Context c,Intent i){
        PendingResult pending=goAsync();Context app=c.getApplicationContext();
        WORKER.execute(()->{try{ScheduledSmsStore.dispatch(app,i.getStringExtra("job"),i.getIntExtra("version",-1));}
            catch(Exception e){Diagnostics.record(app,-1,"SCHEDULE_ERROR",e.getClass().getSimpleName());}
            finally{pending.finish();}});
    }
}
