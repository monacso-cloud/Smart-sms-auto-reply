package com.smartreply.beta;

import android.Manifest;
import android.app.Application;
import android.content.*;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telephony.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class BusinessRoutingTest {
    private Application app;
    private SubscriptionInfo telstra, vodafone;
    @Before public void setup() {
        app = RuntimeEnvironment.getApplication();
        shadowOf(app).grantPermissions(Manifest.permission.READ_PHONE_STATE,Manifest.permission.SEND_SMS);
        telstra=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(11).setSimSlotIndex(0)
            .setDisplayName("Telstra").buildSubscriptionInfo();
        vodafone=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(22).setSimSlotIndex(1)
            .setDisplayName("Vodafone").setIsEmbedded(true).buildSubscriptionInfo();
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(telstra,vodafone);
        BusinessProfiles.register(app,telstra); BusinessProfiles.register(app,vodafone);
    }
    @Test public void settingsOptOutAndCooldownAreSeparateForSameCustomer() {
        SharedPreferences a=BusinessProfiles.prefs(app,11), b=BusinessProfiles.prefs(app,22);
        a.edit().putString("message","Clinic").putBoolean("chat_opt_out_customer",true)
            .putLong("last_reply_customer",99).putString("schedule_mode","off").apply();
        b.edit().putString("message","Academy").putBoolean("master_enabled",true)
            .putBoolean("reply_to_missed_calls",true).apply();
        assertEquals("Clinic",a.getString("message",""));
        assertEquals("Academy",b.getString("message",""));
        assertFalse(b.getBoolean("chat_opt_out_customer",false));
        assertEquals(0,b.getLong("last_reply_customer",0));
        assertFalse(ReplyPolicy.shouldReply(a,"missed_call"));
        assertTrue(ReplyPolicy.shouldReply(b,"missed_call"));
    }
    @Test public void importsOnlyOnceAndNeverEnablesSecondBusiness() {
        SharedPreferences legacy=app.getSharedPreferences(BusinessProfiles.LEGACY,0);
        legacy.edit().putString("message","Original text").putBoolean("master_enabled",true).putInt("subscription_id",22).apply();
        assertTrue(BusinessProfiles.importLegacy(app,11));
        assertEquals("Original text",BusinessProfiles.prefs(app,11).getString("message",""));
        assertFalse(BusinessProfiles.prefs(app,11).getBoolean("master_enabled",true));
        assertFalse(BusinessProfiles.importLegacy(app,22));
        assertFalse(BusinessProfiles.prefs(app,22).contains("message"));
        assertEquals("Original text",legacy.getString("message",""));
    }
    @Test public void routingAcceptsLongAndIntegerRejectsMissingOrConflictingIds() {
        Bundle b=new Bundle(); assertEquals(-1,SimRouter.smsSubscription(b));
        b.putLong("subscription",11); assertEquals(11,SimRouter.smsSubscription(b));
        b.putInt("android.telephony.extra.SUBSCRIPTION_INDEX",11); assertEquals(11,SimRouter.smsSubscription(b));
        b.putInt("android.telephony.extra.SUBSCRIPTION_INDEX",22); assertEquals(-1,SimRouter.smsSubscription(b));
        b.clear(); b.putInt("slot",0); assertEquals(-1,SimRouter.smsSubscription(b));
    }
    @Test public void sendsOnReceivingSimEvenWhenDefaultIsOtherBusiness() {
        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(22);
        assertTrue(ReplySender.send(app,11,"0400000000","Clinic reply","SMS"));
        assertNotNull(shadowOf(SmsManager.getSmsManagerForSubscriptionId(11)).getLastSentTextMessageParams());
        assertNull(shadowOf(SmsManager.getSmsManagerForSubscriptionId(22)).getLastSentTextMessageParams());
    }
    @Test public void removedSimDoesNotFallBackToActiveSim() {
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(vodafone);
        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(22);
        assertFalse(ReplySender.send(app,11,"0400000000","Clinic reply","SMS"));
        assertNull(shadowOf(SmsManager.getSmsManagerForSubscriptionId(22)).getLastSentTextMessageParams());
        assertTrue(BusinessProfiles.exists(app,11));
    }
    @Test public void callAccountMapsToExactActiveSubscription() {
        ComponentName component=new ComponentName("com.android.phone","PhoneService");
        PhoneAccountHandle handle=new PhoneAccountHandle(component,"opaque-account");
        shadowOf(app.getSystemService(TelephonyManager.class)).setPhoneAccountHandleSubscriptionId(handle,22);
        assertEquals(22,SimRouter.callSubscription(app,component.flattenToString(),"opaque-account"));
        assertEquals(-1,SimRouter.callSubscription(app,null,"22"));
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(telstra);
        assertEquals(-1,SimRouter.callSubscription(app,component.flattenToString(),"opaque-account"));
    }
    @Test public void editorsStayBoundToTheirBusinessWhenBothAreOpen() {
        AutoReplySettingsActivity a=Robolectric.buildActivity(AutoReplySettingsActivity.class,
            new Intent(app,AutoReplySettingsActivity.class).putExtra(BusinessProfiles.EXTRA,11)).setup().get();
        AutoReplySettingsActivity b=Robolectric.buildActivity(AutoReplySettingsActivity.class,
            new Intent(app,AutoReplySettingsActivity.class).putExtra(BusinessProfiles.EXTRA,22)).setup().get();
        ((EditText)a.findViewById(R.id.missedCallMessageInput)).setText("Clinic only");
        a.findViewById(R.id.saveAutoReplyButton).performClick();
        ((EditText)b.findViewById(R.id.missedCallMessageInput)).setText("Academy only");
        b.findViewById(R.id.saveAutoReplyButton).performClick();
        assertEquals("Clinic only",BusinessProfiles.prefs(app,11).getString("message",""));
        assertEquals("Academy only",BusinessProfiles.prefs(app,22).getString("message",""));
    }
    @Test public void missingMetadataDoesNotChangeAnyBusinessOptOut() {
        Intent i=new Intent("android.provider.Telephony.SMS_RECEIVED");
        new SmsReplyReceiver().onReceive(app,i);
        assertTrue(BusinessProfiles.index(app).getString("last_routing_issue","").contains("SMS skipped"));
    }
    @Test public void inboundMessagesAndStopCommandsStayInTheirOwnBusiness() {
        for (int id : new int[]{11,22}) BusinessProfiles.prefs(app,id).edit()
            .putBoolean("master_enabled",true).putBoolean("reply_to_incoming_sms",true)
            .putString("sms_reply_message",id==11 ? "Clinic reply" : "Academy reply").apply();
        SmsReplyReceiver receiver=new SmsReplyReceiver();
        String caller="0400000000";
        receiver.handleMessage(app,11,caller,"hello");
        receiver.handleMessage(app,22,caller,"hello");
        assertTrue(shadowOf(SmsManager.getSmsManagerForSubscriptionId(11)).getLastSentTextMessageParams().getText().contains("Clinic reply"));
        assertTrue(shadowOf(SmsManager.getSmsManagerForSubscriptionId(22)).getLastSentTextMessageParams().getText().contains("Academy reply"));
        BusinessProfiles.prefs(app,11).edit().putString("schedule_mode","off").apply();
        receiver.handleMessage(app,11,caller,"STOP");
        String key="chat_opt_out_"+Integer.toHexString(caller.hashCode());
        assertTrue(BusinessProfiles.prefs(app,11).getBoolean(key,false));
        assertFalse(BusinessProfiles.prefs(app,22).getBoolean(key,false));
        receiver.handleMessage(app,11,caller,"START");
        assertFalse(BusinessProfiles.prefs(app,11).getBoolean(key,false));
    }
}
