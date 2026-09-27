package com.smartreply.beta;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class DashboardActivity extends ProfileActivity {
    @Override public void onCreate(Bundle b){super.onCreate(b);}
    @Override public void onResume(){super.onResume();DeliveryTracker.cleanup(this);ScheduledSmsStore.restore(this,false);render();}
    private void render(){
        SharedPreferences p=BusinessProfiles.prefs(this,businessId);
        LinearLayout root=BasicUi.column(this);root.setFitsSystemWindows(true);root.setBackgroundColor(0xfff3f6fb);
        TextView title=BasicUi.text(this,"‹  "+BusinessProfiles.name(this,businessId),25);title.setPadding(24,18,24,8);title.setOnClickListener(v->finish());root.addView(title);
        ScrollView scroll=new ScrollView(this);LinearLayout body=BasicUi.column(this);body.setPadding(24,0,24,24);scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(BasicUi.text(this,p.getString("sim_label","")+" · ReplyDesk Basic",15));
        LinearLayout status=BasicUi.card(this,body);
        boolean available=SimRouter.active(this,businessId);
        status.addView(BasicUi.text(this,!available?"SIM inactive":p.getBoolean("master_enabled",false)?"Automatic replies enabled":"Automatic replies paused",21));
        status.addView(BasicUi.text(this,"SMS: "+ReplyPolicy.reason(p,"sms")+"\nMissed calls: "+ReplyPolicy.reason(p,"missed_call"),14));
        BasicUi.button(this,status,"Check permissions & recent events",()->startActivity(new Intent(this,DiagnosticsActivity.class)));
        Switch master=new Switch(this);master.setText("Enable this business's auto replies");master.setTextSize(16);master.setChecked(p.getBoolean("master_enabled",false));status.addView(master);
        master.setOnCheckedChangeListener((b,on)->{p.edit().putBoolean("master_enabled",on).putBoolean("enabled",on).apply();render();});
        SimpleDateFormat day=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);Calendar yesterday=Calendar.getInstance();yesterday.add(Calendar.DAY_OF_YEAR,-1);
        body.addView(BasicUi.text(this,"Sent today  "+p.getInt("sent_"+day.format(new Date()),0)+"     Yesterday  "+p.getInt("sent_"+day.format(yesterday.getTime()),0)+"     Total  "+p.getInt("sent_total",0),15));
        boolean enabled=p.getBoolean("master_enabled",false), sms=p.getBoolean("reply_to_incoming_sms",p.getBoolean("chatbot_enabled",false));
        boolean bot=p.getBoolean("sms_chatbot_mode",p.getBoolean("chatbot_enabled",false));
        task(body,"Missed-call auto reply","Your message after an unanswered call",enabled&&p.getBoolean("reply_to_missed_calls",true),on->{
            SharedPreferences.Editor e=p.edit().putBoolean("reply_to_missed_calls",on);if(on)e.putBoolean("master_enabled",true).putBoolean("enabled",true);e.apply();render();},AutoReplySettingsActivity.class);
        task(body,"SMS auto reply","One saved message for incoming SMS",enabled&&sms&&!bot,on->{
            SharedPreferences.Editor e=p.edit();if(on)e.putBoolean("master_enabled",true).putBoolean("reply_to_incoming_sms",true).putBoolean("sms_chatbot_mode",false);else if(!bot)e.putBoolean("reply_to_incoming_sms",false);e.apply();render();},AutoReplySettingsActivity.class);
        task(body,"FAQ chatbot","Keyword answers and numbered menus",enabled&&sms&&bot,on->{
            SharedPreferences.Editor e=p.edit();if(on)e.putBoolean("master_enabled",true).putBoolean("reply_to_incoming_sms",true).putBoolean("sms_chatbot_mode",true);else if(bot)e.putBoolean("reply_to_incoming_sms",false);e.apply();render();},KeywordsActivity.class);
        LinearLayout schedule=BasicUi.card(this,body);schedule.addView(BasicUi.text(this,"Scheduled SMS",22));
        schedule.addView(BasicUi.text(this,"Choose recipients, message, date and time",15));BasicUi.button(this,schedule,"Open scheduled messages",()->openProfile(ScheduledSmsActivity.class));
        BasicUi.button(this,body,"Menu & quick replies",()->openProfile(MenuSettingsActivity.class));
        BasicUi.button(this,body,"Reply days & hours",()->openProfile(ScheduleActivity.class));
        BasicUi.button(this,body,"History & sending results",()->openProfile(CallLogsActivity.class));
        BasicUi.button(this,body,"Preview a bot reply",()->openProfile(TestBotActivity.class));
        LinearLayout nav=new LinearLayout(this);root.addView(nav);
        String[] labels={"Tasks","Recipients","Templates","Settings"};
        for(int i=0;i<labels.length;i++){
            final int at=i;Button b=new Button(this);b.setAllCaps(false);b.setText(labels[i]);b.setTextSize(12);
            nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->{if(at==1)openProfile(RecipientsActivity.class);else if(at==2)openProfile(TemplatesActivity.class);else if(at==3)settings();});
        }
        setContentView(root);
    }
    private void settings(){
        new android.app.AlertDialog.Builder(this).setTitle("Business settings").setItems(new String[]{"Messages & reply modes","Delay & repeat protection","Reply days & hours","Permissions & diagnostics","Switch business"},(d,w)->{
            if(w==0)openProfile(AutoReplySettingsActivity.class);if(w==1)openProfile(MainActivity.class);if(w==2)openProfile(ScheduleActivity.class);
            if(w==3)startActivity(new Intent(this,DiagnosticsActivity.class));if(w==4)finish();
        }).show();
    }
    private interface Toggle{void set(boolean value);}
    private void task(LinearLayout body,String name,String detail,boolean on,Toggle changed,Class<?> editor){
        LinearLayout card=BasicUi.card(this,body);Switch toggle=new Switch(this);toggle.setText(name);toggle.setTextSize(20);toggle.setChecked(on);card.addView(toggle);
        card.addView(BasicUi.text(this,detail,15));toggle.setOnCheckedChangeListener((b,v)->changed.set(v));
        BasicUi.button(this,card,"Edit",()->openProfile(editor));
    }
}
