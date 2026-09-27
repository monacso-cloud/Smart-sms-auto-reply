package com.smartreply.beta;

import android.app.*;
import android.content.*;
import android.telephony.*;
import android.view.*;
import android.widget.*;
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
public class ChatbotSetupTest {
    Application app;SharedPreferences p;
    private static final String FOOTER=" POWERED BY: ReplyDesk - Business SMS Bot";
    private static final String PASTED="reschedule,change my appointment=>Use your confirmation email to reschedule."+FOOTER+" "
        +"available,availability,any cancellation,cancellations today=>Check our booking website for today's availability."+FOOTER+" "
        +"cancel,cancellation=>Use your confirmation email to cancel."+FOOTER+" "
        +"price,cost,how much=>Prices start at $135 per hour."+FOOTER+" "
        +"hours,open=>8 am to 8 pm by appointment only."+FOOTER+" "
        +"address,location,where=>Piara Waters, WA."+FOOTER+" "
        +"human,person,staff,help=>Please leave your message."+FOOTER+" "
        +"book,booking,appointment=>Book on our website."+FOOTER;
    @Before public void setup(){
        app=RuntimeEnvironment.getApplication();shadowOf(app).grantPermissions(android.Manifest.permission.READ_PHONE_STATE,android.Manifest.permission.SEND_SMS);
        SubscriptionInfo first=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(11).setSimSlotIndex(0).setDisplayName("A").buildSubscriptionInfo();
        SubscriptionInfo second=ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder().setId(22).setSimSlotIndex(1).setDisplayName("B").buildSubscriptionInfo();
        shadowOf(app.getSystemService(SubscriptionManager.class)).setActiveSubscriptionInfos(first,second);
        BusinessProfiles.register(app,first);BusinessProfiles.register(app,second);p=BusinessProfiles.prefs(app,11);
    }
    @Test public void recoveryPreservesEightAnswersBacksUpOriginalAndLeavesOtherBusinessUntouched(){
        p.edit().putString("chatbot_fallback",PASTED).putString("sms_reply_message",PASTED).apply();
        SharedPreferences other=BusinessProfiles.prefs(app,22);other.edit().putString("chatbot_fallback","Business B").apply();
        BotSetup.recover(app,p);
        assertEquals(8,BotSetup.read(p).size());assertEquals(PASTED,p.getString("setup_original_chatbot_fallback",""));
        assertEquals(PASTED,p.getString("setup_original_sms_reply_message",""));assertFalse(BotSetup.needsRecovery(p));
        assertFalse(p.getBoolean("master_enabled",false));assertEquals("Business B",other.getString("chatbot_fallback",""));
        assertFalse(other.contains("chatbot_rules"));
        assertTrue(KeywordRules.find("Are you available today?",p.getString("chatbot_rules","")).startsWith("Check our booking website"));
        assertTrue(KeywordRules.find("any cancellations today?",p.getString("chatbot_rules","")).startsWith("Check our booking website"));
        assertTrue(KeywordRules.find("can I reschedule my appointment?",p.getString("chatbot_rules","")).startsWith("Use your confirmation email to reschedule"));
        BotSetup.recover(app,p);assertEquals(8,BotSetup.read(p).size());assertEquals(PASTED,p.getString("setup_original_chatbot_fallback",""));
    }
    @Test public void invalidRecoveryLeavesAllSourceSettingsIntact(){
        p.edit().putString("chatbot_fallback",PASTED).putString("sms_reply_message","price=>First answer book=>Second answer").apply();
        Map<String,?> before=new HashMap<>(p.getAll());
        try{BotSetup.recover(app,p);fail("Must reject an ambiguous import");}catch(IllegalArgumentException expected){}
        assertEquals(before,p.getAll());
    }
    @Test public void cardsPreserveMultilineAnswersOrderAndDisabledState(){
        String answer="Hello!\nSee https://example.com/booking\nThank you.";
        BotSetup.save(p,Arrays.asList(new KeywordRules.Rule("available",answer,false),new KeywordRules.Rule("available,today","Second answer")));
        assertEquals(answer,BotSetup.read(p).get(0).reply);assertFalse(BotSetup.read(p).get(0).enabled);
        assertEquals("Second answer",KeywordRules.find("Available today?",p.getString("chatbot_rules","")));
        assertEquals("First line\nSecond line",KeywordRules.parse("price=>First line\nSecond line\nbook=>Booking").get(0).reply);
    }
    private KeywordsActivity screen(){return Robolectric.buildActivity(KeywordsActivity.class,new Intent(app,KeywordsActivity.class).putExtra(BusinessProfiles.EXTRA,11)).setup().get();}
    private Button button(View root,String text){
        if(root instanceof Button && text.contentEquals(((Button)root).getText()))return (Button)root;
        if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){Button b=button(g.getChildAt(i),text);if(b!=null)return b;}}
        return null;
    }
    private void idle(){shadowOf(android.os.Looper.getMainLooper()).idle();}
    @Test public void simpleEditorSavesCardAndPreviewDoesNotSendSms(){
        KeywordsActivity a=screen();button(a.findViewById(android.R.id.content),"+ Add an answer").performClick();idle();
        AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();
        ((EditText)dialog.findViewById(R.id.newKeywordsInput)).setText("available,availability");
        ((EditText)dialog.findViewById(R.id.newReplyInput)).setText("Please check our website.");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
        assertEquals(1,BotSetup.read(p).size());assertEquals("Please check our website.",BotSetup.read(p).get(0).reply);
        ((EditText)a.findViewById(R.id.testMessageInput)).setText("Are you available today?");a.findViewById(R.id.runTestButton).performClick();idle();
        assertTrue(((TextView)a.findViewById(R.id.testBotResult)).getText().toString().contains("Please check our website."));
        assertNull(shadowOf(SmsManager.getSmsManagerForSubscriptionId(11)).getLastSentTextMessageParams());
        a.finish();assertEquals(1,BotSetup.read(p).size());
    }
    @Test public void organiseButtonRequiresConfirmationAndThenConvertsWrongField(){
        p.edit().putString("chatbot_fallback",PASTED).apply();KeywordsActivity a=screen();
        button(a.findViewById(android.R.id.content),"Organise my saved answers").performClick();idle();
        assertTrue(BotSetup.needsRecovery(p));AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();idle();assertTrue(BotSetup.needsRecovery(p));
        button(a.findViewById(android.R.id.content),"Organise my saved answers").performClick();idle();
        dialog=(AlertDialog)ShadowDialog.getLatestDialog();dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
        assertFalse(BotSetup.needsRecovery(p));assertEquals(8,BotSetup.read(p).size());a.finish();
    }
    @Test public void previewAndIncomingSmsUseSameCardAfterRecovery(){
        p.edit().putString("chatbot_fallback",PASTED).putBoolean("master_enabled",true).putBoolean("reply_to_incoming_sms",true).putBoolean("sms_chatbot_mode",true).apply();
        BotSetup.recover(app,p);String question="Are you available today?";
        String expected=ReplySender.disclosure(BotReplies.choose(app,p,question).text);
        assertTrue(BotReplies.preview(app,p,question,false).contains(expected));
        new SmsReplyReceiver().handleMessage(app,11,"+61400000000",question);
        // Content spans several SMS parts on a real device; join whichever Android call was used.
        ShadowSmsManager manager=shadowOf(SmsManager.getSmsManagerForSubscriptionId(11));
        if(manager.getLastSentTextMessageParams()!=null)assertEquals(expected,manager.getLastSentTextMessageParams().getText());
        else assertEquals(expected,String.join("",manager.getLastSentMultipartTextMessageParams().getParts()));
        assertFalse(expected.contains("=>"));
    }
    @Test public void oldSettingsHidesMisplacedRulesAndProvidesSetupButton(){
        p.edit().putString("chatbot_fallback",PASTED).apply();
        AutoReplySettingsActivity a=Robolectric.buildActivity(AutoReplySettingsActivity.class,new Intent(app,AutoReplySettingsActivity.class).putExtra(BusinessProfiles.EXTRA,11)).setup().get();
        assertEquals(View.GONE,a.findViewById(R.id.smsReplyMessageInput).getVisibility());
        a.findViewById(R.id.openChatbotSetupButton).performClick();idle();
        assertEquals(KeywordsActivity.class.getName(),shadowOf(a).getNextStartedActivity().getComponent().getClassName());
        assertEquals(PASTED,p.getString("chatbot_fallback",""));a.finish();
    }
    @Test public void missedCallEditorSavesOnlyMissedCallSettings(){
        p.edit().putString("chatbot_fallback","General reply").putString("chatbot_rules","available=>Website").apply();
        MessageEditorActivity a=Robolectric.buildActivity(MessageEditorActivity.class,new Intent(app,MessageEditorActivity.class).putExtra(BusinessProfiles.EXTRA,11).putExtra("message_kind","missed")).setup().get();
        ((EditText)a.findViewById(R.id.missedCallMessageInput)).setText("Sorry I missed your call.");
        a.findViewById(R.id.saveAutoReplyButton).performClick();idle();
        assertEquals("Sorry I missed your call.",p.getString("message",""));assertEquals("General reply",p.getString("chatbot_fallback",""));
        assertEquals("available=>Website",p.getString("chatbot_rules",""));assertNull(a.findViewById(R.id.smsReplyMessageInput));a.finish();
    }
    @Test public void unmatchedQuestionUsesOnlyGeneralReply(){
        p.edit().putBoolean("sms_chatbot_mode",true).putString("chatbot_rules","available=>Check website").putString("chatbot_fallback","Please leave your message.").apply();
        assertEquals("Please leave your message.",BotReplies.choose(app,p,"Do you have parking?").text);
        assertTrue(BotReplies.preview(app,p,"Do you have parking?",false).contains("Please leave your message."));
    }
}
