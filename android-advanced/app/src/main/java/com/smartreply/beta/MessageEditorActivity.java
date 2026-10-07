package com.smartreply.beta;

import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

/** A single reply type per screen: missed calls or plain SMS. */
public class MessageEditorActivity extends ProfileActivity {
    private SharedPreferences prefs;
    private EditText message;
    private boolean missed;
    private String key;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);prefs=BusinessProfiles.prefs(this,businessId);
        missed=!"plain".equals(getIntent().getStringExtra("message_kind"));key=missed?"message":"sms_reply_message";
        LinearLayout body=BasicUi.page(this,missed?"Missed-call reply":"Plain SMS reply",businessId);
        body.addView(BasicUi.text(this,missed?"Write the one message a caller receives when you miss their call.":"Send the same message for incoming SMS. For different answers to different questions, use Chatbot setup.",16));
        Switch enabled=new Switch(this);enabled.setText(missed?"Reply to missed calls":"Use plain SMS reply");enabled.setTextSize(18);
        enabled.setChecked(prefs.getBoolean("master_enabled",false) && (missed?prefs.getBoolean("reply_to_missed_calls",true):prefs.getBoolean("reply_to_incoming_sms",false)&&!prefs.getBoolean("sms_chatbot_mode",false)));body.addView(enabled);
        String original=prefs.getString(key,getString(missed?R.string.default_message:R.string.default_plain_sms_reply));
        if(KeywordRules.containsRule(original)){
            body.addView(BasicUi.text(this,"This box contains chatbot rules. Open Chatbot setup to organise your answers before saving a reply message.",16));
            BasicUi.button(this,body,"Open chatbot setup",()->openProfile(KeywordsActivity.class));
        }
        message=BasicUi.input(this,body,missed?"Your missed-call message":"Your automatic SMS message",original,true);
        message.setMinLines(6);message.setId(missed?R.id.missedCallMessageInput:R.id.plainSmsMessageInput);
        message.setVisibility(KeywordRules.containsRule(original)?View.GONE:View.VISIBLE);
        Switch menu=new Switch(this);menu.setText("Include my numbered menu");menu.setChecked(prefs.getBoolean("missed_call_include_menu",prefs.getBoolean("menu_enabled",false)));
        Spinner delay=new Spinner(this),repeat=new Spinner(this);int[] delays={0,5,10,15,30,60},intervals={0,15,60,360,720,1440};
        if(missed){
            body.addView(menu);body.addView(BasicUi.text(this,"Requires Menu & quick replies to be enabled.",13));
            body.addView(BasicUi.text(this,"Delay before replying",16));
            delay.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"No delay","5 seconds","10 seconds","15 seconds","30 seconds","60 seconds"}));
            for(int n=0;n<delays.length;n++)if(delays[n]==prefs.getInt("delay_seconds",0))delay.setSelection(n);body.addView(delay);
            body.addView(BasicUi.text(this,"Send again to the same caller",16));
            repeat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Every missed call","Every 15 minutes","Every hour","Every 6 hours","Every 12 hours","Every day"}));
            for(int n=0;n<intervals.length;n++)if(intervals[n]==prefs.getInt("repeat_minutes",0))repeat.setSelection(n);body.addView(repeat);
        }
        BasicUi.button(this,body,"Preview this message",()->{
            String value=message.getText().toString();
            if(value.trim().isEmpty()||KeywordRules.containsRule(value)){Toast.makeText(this,"Write one reply message first",Toast.LENGTH_LONG).show();return;}
            String preview=missed?MissedCallJobService.appendNumberedMenu(value,prefs,menu.isChecked()):value;
            new android.app.AlertDialog.Builder(this).setTitle("Preview only — no SMS sent").setMessage(ReplySender.disclosure(preview)).setPositiveButton("Close",null).show();
        });
        Button save=BasicUi.button(this,body,"Save message",()->{
            String value=message.getText().toString().trim();if(value.isEmpty()||KeywordRules.containsRule(value)){Toast.makeText(this,"Write one reply message. Use Chatbot setup for questions and answers.",Toast.LENGTH_LONG).show();return;}
            SharedPreferences.Editor e=prefs.edit().putString(key,value);
            if(missed)e.putBoolean("reply_to_missed_calls",enabled.isChecked()).putBoolean("missed_call_include_menu",menu.isChecked()).putInt("delay_seconds",delays[delay.getSelectedItemPosition()]).putInt("repeat_minutes",intervals[repeat.getSelectedItemPosition()]);
            else if(enabled.isChecked())e.putBoolean("sms_chatbot_mode",false).putBoolean("reply_to_incoming_sms",true);
            else if(!prefs.getBoolean("sms_chatbot_mode",false))e.putBoolean("reply_to_incoming_sms",false);
            if(enabled.isChecked())e.putBoolean("master_enabled",true).putBoolean("enabled",true);
            if(e.commit())Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();else Toast.makeText(this,"Could not save. Please try again.",Toast.LENGTH_LONG).show();
        });save.setId(R.id.saveAutoReplyButton);
        body.addView(BasicUi.text(this,"Reply days, hours and recipient filters still apply. The receiving SIM is used for the reply.",14));
    }
    @Override protected void onResume(){
        super.onResume();if(message==null)return;
        String stored=prefs.getString(key,"");
        if(KeywordRules.containsRule(message.getText().toString())&&!KeywordRules.containsRule(stored)){message.setText(stored);message.setVisibility(View.VISIBLE);}
    }
}
