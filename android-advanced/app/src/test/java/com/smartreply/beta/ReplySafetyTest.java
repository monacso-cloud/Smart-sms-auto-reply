package com.smartreply.beta;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.os.*;
import android.telephony.*;
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
public class ReplySafetyTest {
    Application app; SharedPreferences p; long source;
    final String local="0400000000", international="+61400000000";
    @Before public void setup(){
        app=RuntimeEnvironment.getApplication();
        shadowOf(app).grantPermissions(Manifest.permission.READ_PHONE_STATE,Manifest.permission.SEND_SMS);
        SubscriptionInfo a=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(11).setSimSlotIndex(0).setCountryIso("au").setDisplayName("A").buildSubscriptionInfo();
        SubscriptionInfo b=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(22).setSimSlotIndex(1).setCountryIso("au").setDisplayName("B").buildSubscriptionInfo();
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(a,b);
        BusinessProfiles.register(app,a);BusinessProfiles.register(app,b);
        for(int id:new int[]{11,22})BusinessProfiles.prefs(app,id).edit().putBoolean("master_enabled",true)
            .putBoolean("reply_to_incoming_sms",true).putBoolean("reply_to_missed_calls",true)
            .putBoolean("sms_chatbot_mode",true).putString("chatbot_rules","price=>Approved price")
            .putString("message","Approved missed call").commit();
        p=BusinessProfiles.prefs(app,11);source=System.currentTimeMillis()-1000;
    }
    ShadowSmsManager manager(int id){return shadowOf(SmsManager.getSmsManagerForSubscriptionId(id));}
    void clearSms(int id){manager(id).clearLastSentTextMessageParams();manager(id).clearLastSentMultipartTextMessageParams();}
    boolean sent(int id){return manager(id).getLastSentTextMessageParams()!=null || manager(id).getLastSentMultipartTextMessageParams()!=null;}
    void clearCooldown(){
        SharedPreferences.Editor e=p.edit();
        for(String key:p.getAll().keySet())if(key.startsWith("chat_last_"))e.remove(key);
        e.commit();
    }
    void receive(String sender,String body,long stamp){new SmsReplyReceiver().handleMessage(app,11,sender,body,stamp);}
    @Test public void duplicateMatchedSmsSurvivesRestartAndCooldownAndSenderFormatting(){
        receive(local,"price",source);assertTrue(sent(11));clearSms(11);clearCooldown();
        receive(international,"price",source);assertFalse(sent(11));
    }
    @Test public void newMessageWithSameContentAndNewSourceTimeCanReplyAfterCooldown(){
        receive(local,"price",source);assertTrue(sent(11));clearSms(11);clearCooldown();
        receive(international,"price",source+1);assertTrue(sent(11));
    }
    @Test public void normalizedCooldownPreventsDifferentMessageAndCommandBurst(){
        receive(local,"price",source);clearSms(11);
        receive(international,"price please",source+1);assertFalse(sent(11));
        receive(international,"STOP",source+2);assertTrue(ConversationPolicy.optedOut(p,local));assertFalse(sent(11));
    }
    @Test public void sameEventOnDifferentSimHasIndependentClaimAndReply(){
        receive(local,"price",source);assertTrue(sent(11));
        new SmsReplyReceiver().handleMessage(app,22,local,"price",source);assertTrue(sent(22));
    }
    @Test public void duplicateStopDoesNotAcknowledgeAgainOrUndoNewerStart(){
        receive(local,"STOP",source);assertTrue(sent(11));clearSms(11);clearCooldown();
        receive(international,"STOP",source);assertFalse(sent(11));
        receive(international,"START",source+1);assertFalse(ConversationPolicy.optedOut(p,local));clearSms(11);clearCooldown();
        receive(local,"STOP",source);assertFalse(ConversationPolicy.optedOut(p,local));assertFalse(sent(11));
        receive(local,"price",source+2);assertTrue(sent(11));
    }
    @Test public void duplicateStartCannotResetFallbackOrHumanHandoff(){
        receive(local,"START",source);clearSms(11);clearCooldown();
        ConversationPolicy.setPaused(p,local,true);p.edit().putBoolean("fallback_used_"+international,true).commit();
        receive(international,"START",source);
        assertTrue(ConversationPolicy.paused(p,local));assertTrue(p.getBoolean("fallback_used_"+international,false));assertFalse(sent(11));
    }
    @Test public void startOverridesLegacyRawOptOutButOwnerResumeDoesNot(){
        p.edit().putBoolean("chat_opt_out_"+Integer.toHexString(local.hashCode()),true)
            .putBoolean("chat_opt_out_"+Integer.toHexString(international.hashCode()),true).commit();
        ConversationPolicy.setPaused(p,local,false);assertTrue(ConversationPolicy.optedOut(p,local));
        receive(international,"START",source);assertFalse(ConversationPolicy.optedOut(p,local));
        assertFalse(ConversationPolicy.optedOut(p,international));
    }
    @Test public void businessTakeoverStillOverridesStart(){
        p.edit().putBoolean("human_takeover",true).commit();
        receive(local,"START",source);assertTrue(ConversationPolicy.paused(p,local));assertFalse(sent(11));
        receive(local,"price",source+1);assertFalse(sent(11));
    }
    @Test public void fallbackOnceIsIndependentOfCooldownAndReceiverLifecycle(){
        p.edit().putBoolean("fallback_enabled",true).putString("chatbot_fallback","Approved general reply").commit();
        receive(local,"unmatched",source);assertTrue(sent(11));clearSms(11);clearCooldown();
        receive(international,"another unmatched question",source+1);assertFalse(sent(11));
    }
    @Test public void failedReceiptCommitCannotChangeCommandStateOrSend(){
        Context failing=new ContextWrapper(app){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                SharedPreferences base=super.getSharedPreferences(name,mode);
                return name.equals("replydesk_business_11") ? FailingPreferences.wrap(base) : base;
            }
        };
        new SmsReplyReceiver().handleMessage(failing,11,local,"STOP",source);
        assertFalse(ConversationPolicy.optedOut(p,local));assertFalse(sent(11));
        assertFalse(IncomingSmsGuard.claim(FailingPreferences.wrap(p),local,"HUMAN",source,System.currentTimeMillis(),"human"));
        assertFalse(ConversationPolicy.paused(p,local));
        receive(local,"STOP",source);assertTrue(ConversationPolicy.optedOut(p,local));assertTrue(sent(11));
    }
    @Test public void failedReplyReservationAndFailedCallClaimFailClosed(){
        SharedPreferences fail=FailingPreferences.wrap(p);
        assertFalse(IncomingSmsGuard.reserveReply(fail,local,System.currentTimeMillis(),false,"SMS",30_000));
        assertFalse(IncomingSmsGuard.claimCall(FailingPreferences.wrap(BusinessProfiles.index(app)),"call_1_1",source));
        assertFalse(BusinessProfiles.index(app).contains("call_1_1"));assertFalse(sent(11));
    }
    @Test public void failedDispatchIsNeverRetriedByDuplicateReceipt(){
        shadowOf(app).denyPermissions(Manifest.permission.SEND_SMS);
        receive(local,"price",source);assertFalse(sent(11));
        shadowOf(app).grantPermissions(Manifest.permission.SEND_SMS);clearCooldown();
        receive(international,"price",source);assertFalse(sent(11));
    }
    @Test public void failedSentCallbackDoesNotResubmitMessage(){
        receive(local,"price",source);assertTrue(sent(11));
        SharedPreferences deliveries=app.getSharedPreferences("replydesk_deliveries",0);
        String token=deliveries.getAll().keySet().iterator().next();
        clearSms(11);DeliveryTracker.result(app,token,0,false,"No service");
        DeliveryTracker.result(app,token,0,false,"Duplicate failure");clearCooldown();
        receive(local,"price",source);assertFalse(sent(11));
    }
    PersistableBundle call(String event){
        PersistableBundle b=new PersistableBundle();b.putString("event",event);b.putLong("date",System.currentTimeMillis());
        b.putInt(BusinessProfiles.EXTRA,11);b.putString("number",local);return b;
    }
    @Test public void duplicateMissedCallJobsSubmitOnceAcrossServiceRecreation(){
        PersistableBundle data=call("call_42_"+source);
        MissedCallJobService service=Robolectric.buildService(MissedCallJobService.class).create().get();
        service.dispatch(data);assertTrue(sent(11));clearSms(11);
        Robolectric.buildService(MissedCallJobService.class).create().get().dispatch(data);assertFalse(sent(11));
    }
    @Test public void missedCallCooldownNormalizesCallerBeforeDispatch(){
        p.edit().putInt("repeat_minutes",60).commit();
        MissedCallJobService service=Robolectric.buildService(MissedCallJobService.class).create().get();
        service.dispatch(call("call_1_"+source));assertTrue(sent(11));clearSms(11);
        PersistableBundle b=call("call_2_"+source);b.putString("number",international);service.dispatch(b);assertFalse(sent(11));
    }
    @Test public void optedOutSenderDoesNotReceiveMissedCallReply(){
        receive(local,"STOP",source);clearSms(11);
        PersistableBundle b=call("call_3_"+source);b.putString("number",international);
        Robolectric.buildService(MissedCallJobService.class).create().get().dispatch(b);assertFalse(sent(11));
    }
    @Test public void undatedExpiredAndFarFutureSmsFailClosed(){
        long now=System.currentTimeMillis();
        for(long timestamp:new long[]{0,now-IncomingSmsGuard.RETENTION_MS-1,now+2*86400_000L})receive(local,"price",timestamp);
        assertFalse(sent(11));
    }
    @Test public void routingRejectsFractionalOrConflictingSimMetadata(){
        Bundle b=new Bundle();b.putDouble("subscription",11.5);assertEquals(-1,SimRouter.smsSubscription(app,b));
        b.clear();b.putInt("subscription",11);b.putInt("slot",1);assertEquals(-1,SimRouter.smsSubscription(app,b));
        b.clear();b.putInt("slot",0);b.putInt("android.telephony.extra.SLOT_INDEX",1);assertEquals(-1,SimRouter.smsSubscription(app,b));
        b.clear();b.putDouble("slot",0.1);assertEquals(-1,SimRouter.smsSubscription(app,b));
        b.clear();b.putInt("subscription",11);b.putLong("slot",0);assertEquals(11,SimRouter.smsSubscription(app,b));
    }
    @Test public void wholeRuleConfigurationNeverLeavesAnyAutoReplyPath(){
        String rules="price=>Approved price\nbook=>Approved booking";
        assertFalse(ReplySender.send(app,11,local,rules,"SMS"));assertFalse(ReplySender.send(app,11,local,rules,"MISSED"));
        p.edit().putBoolean("sms_chatbot_mode",false).putString("sms_reply_message",rules).commit();receive(local,"hello",source);
        p.edit().putBoolean("sms_chatbot_mode",true).putBoolean("fallback_enabled",false).putString("chatbot_fallback",rules).commit();receive(local,"unmatched",source+1);
        assertFalse(sent(11));receive(local,"price",source+2);assertTrue(sent(11));
        assertEquals(ReplySender.disclosure("Approved price"),manager(11).getLastSentTextMessageParams().getText());
    }
}
