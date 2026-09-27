package com.smartreply.beta;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.telephony.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=31)
public class BasicSchedulingTest {
    Application app;
    SubscriptionInfo first,second;
    @Before public void setup(){
        app=RuntimeEnvironment.getApplication();shadowOf(app).grantPermissions(Manifest.permission.READ_PHONE_STATE,Manifest.permission.SEND_SMS);
        ShadowAlarmManager.setCanScheduleExactAlarms(true);
        first=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(11).setSimSlotIndex(0).setDisplayName("A").setCountryIso("au").buildSubscriptionInfo();
        second=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(22).setSimSlotIndex(1).setDisplayName("B").setCountryIso("au").buildSubscriptionInfo();
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(first,second);
        BusinessProfiles.register(app,first);BusinessProfiles.register(app,second);
    }
    String create(){return ScheduledSmsStore.save(app,11,null,Arrays.asList("+61400000000"),"Appointment reminder",System.currentTimeMillis()+120_000);}
    @Test public void scheduleSurvivesReloadAndCanBeCancelled(){
        String id=create();assertEquals(1,ScheduledSmsStore.list(app,11).length());assertEquals(0,ScheduledSmsStore.list(app,22).length());
        ScheduledSmsStore.restore(app,false);
        assertFalse(shadowOf(app.getSystemService(AlarmManager.class)).getScheduledAlarms().isEmpty());
        assertTrue(ScheduledSmsStore.cancel(app,id));
        assertNull(ScheduledSmsStore.claim(app,id,1,System.currentTimeMillis()+120_001));
        assertEquals("Cancelled",ScheduledSmsStore.get(app,id).optString("state"));
    }
    @Test public void editsInvalidateOldAlarmAndOnlyOneClaimIsAllowed(){
        String id=create();long when=System.currentTimeMillis()+180_000;
        ScheduledSmsStore.save(app,11,id,Arrays.asList("+61400000000"),"Updated",when);
        assertNull(ScheduledSmsStore.claim(app,id,1,when));
        assertNotNull(ScheduledSmsStore.claim(app,id,2,when));
        assertNull(ScheduledSmsStore.claim(app,id,2,when));
        assertFalse(ScheduledSmsStore.cancel(app,id));
    }
    @Test public void expiredScheduleFailsWithoutSending(){
        String id=create();long when=ScheduledSmsStore.get(app,id).optLong("time");
        assertNull(ScheduledSmsStore.claim(app,id,1,when+ScheduledSmsStore.MAX_LATENESS+1));
        assertEquals("Failed",ScheduledSmsStore.get(app,id).optString("state"));
        assertNull(shadowOf(SmsManager.getSmsManagerForSubscriptionId(11)).getLastSentTextMessageParams());
    }
    @Test public void alarmPermissionRevocationBlocksAndGrantRestoresPending(){
        String id=create();ShadowAlarmManager.setCanScheduleExactAlarms(false);ScheduledSmsStore.restore(app,false);
        assertEquals("Blocked",ScheduledSmsStore.get(app,id).optString("state"));
        ShadowAlarmManager.setCanScheduleExactAlarms(true);ScheduledSmsStore.restore(app,false);
        assertEquals("Pending",ScheduledSmsStore.get(app,id).optString("state"));
    }
    @Test public void rebootDoesNotResendAClaimedMessage(){
        String id=create();long when=ScheduledSmsStore.get(app,id).optLong("time");
        assertNotNull(ScheduledSmsStore.claim(app,id,1,when));ScheduledSmsStore.restore(app,true);
        assertEquals("Unconfirmed",ScheduledSmsStore.get(app,id).optString("state"));
        assertNull(ScheduledSmsStore.claim(app,id,1,when));
    }
    @Test public void multipartCallbacksCountOneMessageOnly(){
        String token=DeliveryTracker.begin(app,11,"+61400000000","SMS",2,"",-1);
        DeliveryTracker.result(app,token,0,true,"");assertEquals(0,BusinessProfiles.prefs(app,11).getInt("sent_total",0));
        DeliveryTracker.result(app,token,1,true,"");DeliveryTracker.result(app,token,1,true,"");
        assertEquals(1,BusinessProfiles.prefs(app,11).getInt("sent_total",0));assertEquals(0,BusinessProfiles.prefs(app,22).getInt("sent_total",0));
    }
    @Test public void failedPartFailsRecipientAndNeverBecomesSent(){
        String id=create();ScheduledSmsStore.claim(app,id,1,ScheduledSmsStore.get(app,id).optLong("time"));
        ScheduledSmsStore.recipient(app,id,0,"Submitted");
        String token=DeliveryTracker.begin(app,11,"+61400000000","SCHEDULED",2,id,0);
        DeliveryTracker.result(app,token,0,false,"No service");DeliveryTracker.result(app,token,1,true,"");
        assertEquals("Failed",ScheduledSmsStore.get(app,id).optString("state"));assertEquals(0,BusinessProfiles.prefs(app,11).getInt("sent_total",0));
    }
    @Test public void recipientFiltersMatchAustralianLocalAndInternationalNumbers(){
        SharedPreferences p=BusinessProfiles.prefs(app,11);
        p.edit().putString("ignored_numbers","0400 000 000").apply();assertNotEquals("Ready",RecipientRules.reason(p,"+61400000000"));
        p.edit().putString("ignored_numbers","").putString("sender_filter","selected")
            .putString("saved_recipients","[{\"name\":\"Test\",\"phone\":\"0400000000\"}]").apply();
        assertEquals("Ready",RecipientRules.reason(p,"+61400000000"));assertNotEquals("Ready",RecipientRules.reason(p,"+61411111111"));
        assertEquals("Ready",RecipientRules.reason(BusinessProfiles.prefs(app,22),"+61411111111"));
    }
    @Test public void slotOnlySmsRoutesUniquelyButNeverOverridesConflictingIds(){
        Bundle b=new Bundle();b.putInt("slot",1);assertEquals(22,SimRouter.smsSubscription(app,b));
        b.putInt("subscription",11);b.putInt("android.telephony.extra.SUBSCRIPTION_INDEX",22);
        assertEquals(-1,SimRouter.smsSubscription(app,b));
    }
    @Test public void overnightScheduleUsesTheStartingDay(){
        SharedPreferences p=BusinessProfiles.prefs(app,11);p.edit().putBoolean("master_enabled",true).putBoolean("reply_to_incoming_sms",true)
            .putString("schedule_mode","custom_hours").putInt("schedule_start_minutes",18*60).putInt("schedule_end_minutes",6*60)
            .putBoolean("schedule_day_1",true).putBoolean("schedule_day_2",false).apply();
        Calendar t=Calendar.getInstance();t.set(2026,Calendar.SEPTEMBER,29,2,0); // Tuesday, in Monday's overnight window.
        assertEquals("Ready",ReplyPolicy.reason(p,"sms",t));t.set(Calendar.HOUR_OF_DAY,19);assertNotEquals("Ready",ReplyPolicy.reason(p,"sms",t));
    }
    @Test public void basicScreensOpenWithBoundBusiness(){
        for(Class<? extends Activity> type:Arrays.asList(DashboardActivity.class,ScheduledSmsActivity.class,RecipientsActivity.class,TemplatesActivity.class,MainActivity.class)){
            Activity a=Robolectric.buildActivity(type,new Intent(app,type).putExtra(BusinessProfiles.EXTRA,11)).setup().get();
            assertNotNull(a.findViewById(android.R.id.content));a.finish();
        }
    }
    @Test public void missingPermissionAndPausedPolicyAreRecorded(){
        shadowOf(app).denyPermissions(Manifest.permission.SEND_SMS);
        new SmsReplyReceiver().onReceive(app,new Intent("android.provider.Telephony.SMS_RECEIVED"));
        assertTrue(Diagnostics.report(app).contains("SEND_SMS permission missing"));
    }
    @Test public void dueMessageUsesPinnedSimAndBecomesSentOnlyAfterCallback(){
        String id=create();ShadowSubscriptionManager.setDefaultSmsSubscriptionId(22);
        ScheduledSmsStore.dispatch(app,id,1);
        assertEquals("Pending",ScheduledSmsStore.get(app,id).optString("state"));
        ScheduledSmsStore.dispatch(app,id,1,ScheduledSmsStore.get(app,id).optLong("time"));
        assertEquals("Submitted",ScheduledSmsStore.get(app,id).optString("state"));
        ShadowSmsManager.TextSmsParams sent=shadowOf(SmsManager.getSmsManagerForSubscriptionId(11)).getLastSentTextMessageParams();
        assertNotNull(sent);assertEquals("Appointment reminder",sent.getText());
        assertNull(shadowOf(SmsManager.getSmsManagerForSubscriptionId(22)).getLastSentTextMessageParams());
        Intent callback=shadowOf(sent.getSentIntent()).getSavedIntent();
        DeliveryTracker.result(app,callback.getStringExtra("delivery_token"),0,true,"");
        assertEquals("Sent",ScheduledSmsStore.get(app,id).optString("state"));
        ScheduledSmsStore.dispatch(app,id,1);assertEquals(1,BusinessProfiles.prefs(app,11).getInt("sent_total",0));
    }
    @Test public void dueMessageFailsIfItsSimWasRemovedAndDoesNotUseDefault(){
        String id=create();ShadowSubscriptionManager.setDefaultSmsSubscriptionId(22);
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(second);
        ScheduledSmsStore.dispatch(app,id,1,ScheduledSmsStore.get(app,id).optLong("time"));
        assertEquals("Failed",ScheduledSmsStore.get(app,id).optString("state"));
        assertNull(shadowOf(SmsManager.getSmsManagerForSubscriptionId(22)).getLastSentTextMessageParams());
    }
}
