package com.smartreply.beta;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.telephony.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=31,shadows=IncomingSmsBroadcastTest.DecodedSms.class)
public class IncomingSmsBroadcastTest {
    Application app;long source;
    @Implements(SmsMessage.class)
    public static class DecodedSms {
        static final Map<Integer,SmsMessage> messages=new HashMap<>();
        String sender,body;long date;
        @Implementation protected static SmsMessage createFromPdu(byte[] pdu,String format){return pdu==null || pdu.length==0 ? null : messages.get((int)pdu[0]);}
        @Implementation protected String getDisplayOriginatingAddress(){return sender;}
        @Implementation protected String getDisplayMessageBody(){return body;}
        @Implementation protected long getTimestampMillis(){return date;}
    }
    @Before public void setup(){
        DecodedSms.messages.clear();app=RuntimeEnvironment.getApplication();
        shadowOf(app).grantPermissions(Manifest.permission.READ_PHONE_STATE,Manifest.permission.SEND_SMS);
        SubscriptionInfo info=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(11).setSimSlotIndex(0).setCountryIso("au").setDisplayName("A").buildSubscriptionInfo();
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(info);BusinessProfiles.register(app,info);
        BusinessProfiles.prefs(app,11).edit().putBoolean("master_enabled",true).putBoolean("reply_to_incoming_sms",true)
            .putBoolean("sms_chatbot_mode",true).putString("chatbot_rules","price please=>Only approved answer").commit();
        source=System.currentTimeMillis()-1000;
    }
    byte[] part(int id,String sender,String body,long date){
        SmsMessage m=Shadow.newInstanceOf(SmsMessage.class);DecodedSms s=Shadow.extract(m);s.sender=sender;s.body=body;s.date=date;
        DecodedSms.messages.put(id,m);return new byte[]{(byte)id};
    }
    Intent intent(Object...pdus){return new Intent("android.provider.Telephony.SMS_RECEIVED").putExtra("subscription",11).putExtra("format","3gpp").putExtra("pdus",pdus);}
    ShadowSmsManager manager(){return shadowOf(SmsManager.getSmsManagerForSubscriptionId(11));}
    @Test public void multipartBroadcastSendsOneCompleteAnswerAndDuplicateSendsNothing(){
        Intent i=intent(part(1,"0400000000","price ",source),part(2,"+61400000000","please",source+1));
        new SmsReplyReceiver().onReceive(app,i);
        assertNotNull(manager().getLastSentTextMessageParams());
        assertEquals(ReplySender.disclosure("Only approved answer"),manager().getLastSentTextMessageParams().getText());
        assertEquals(1,app.getSharedPreferences("replydesk_deliveries",0).getAll().size());
        manager().clearLastSentTextMessageParams();
        new SmsReplyReceiver().onReceive(app,i);
        assertNull(manager().getLastSentTextMessageParams());assertEquals(1,app.getSharedPreferences("replydesk_deliveries",0).getAll().size());
    }
    @Test public void mixedSendersNeverJoinIntoKeywordMatch(){
        new SmsReplyReceiver().onReceive(app,intent(part(1,"0400000000","price ",source),part(2,"0411111111","please",source)));
        assertNull(manager().getLastSentTextMessageParams());
    }
    @Test public void malformedPartNeverSendsPartialReply(){
        new SmsReplyReceiver().onReceive(app,intent(part(1,"0400000000","price please",source),new byte[]{99}));
        assertNull(manager().getLastSentTextMessageParams());
    }
    @Test public void allUndatedPartsAreRejected(){
        new SmsReplyReceiver().onReceive(app,intent(part(1,"0400000000","price please",0)));
        assertNull(manager().getLastSentTextMessageParams());
    }
    @Test public void inactiveOrConflictingMetadataCannotUseDefaultSim(){
        Intent i=intent(part(1,"0400000000","price please",source));i.putExtra("slot",1);
        new SmsReplyReceiver().onReceive(app,i);assertNull(manager().getLastSentTextMessageParams());
        i.removeExtra("slot");shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos();
        new SmsReplyReceiver().onReceive(app,i);assertNull(manager().getLastSentTextMessageParams());
    }
    @Test public void stopBroadcastPersistsWhileSendingPermissionIsRevoked(){
        shadowOf(app).denyPermissions(Manifest.permission.SEND_SMS);
        new SmsReplyReceiver().onReceive(app,intent(part(1,"0400000000","STOP",source)));
        assertTrue(ConversationPolicy.optedOut(BusinessProfiles.prefs(app,11),"+61400000000"));
        assertNull(manager().getLastSentTextMessageParams());
        shadowOf(app).grantPermissions(Manifest.permission.SEND_SMS);
        new SmsReplyReceiver().onReceive(app,intent(part(2,"+61400000000","price please",source+1)));
        assertNull(manager().getLastSentTextMessageParams());
    }
}
