package com.smartreply.beta;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

/** Simple per-business answer cards. Raw rule syntax is confined to legacy recovery. */
public class KeywordsActivity extends ProfileActivity {
    private SharedPreferences prefs;
    private List<KeywordRules.Rule> rules;
    @Override protected void onCreate(Bundle state){super.onCreate(state);prefs=BusinessProfiles.prefs(this,businessId);render();}
    private void render(){
        LinearLayout body=BasicUi.page(this,"Chatbot setup",businessId);
        body.addView(BasicUi.text(this,"Add the words customers use and the answer you want to send. Each card is one answer.",16));
        LinearLayout status=BasicUi.card(this,body);
        Switch enabled=new Switch(this);enabled.setText("Chatbot ON / OFF");enabled.setTextSize(19);
        enabled.setChecked(prefs.getBoolean("master_enabled",false) && prefs.getBoolean("reply_to_incoming_sms",false) && prefs.getBoolean("sms_chatbot_mode",false));
        status.addView(enabled);
        enabled.setOnCheckedChangeListener((button,on)->{
            SharedPreferences.Editor e=prefs.edit();
            if(on)e.putBoolean("master_enabled",true).putBoolean("enabled",true).putBoolean("reply_to_incoming_sms",true).putBoolean("chatbot_enabled",true).putBoolean("sms_chatbot_mode",true);
            else if(prefs.getBoolean("sms_chatbot_mode",false))e.putBoolean("reply_to_incoming_sms",false).putBoolean("chatbot_enabled",false);
            e.apply();render();
        });
        status.addView(BasicUi.text(this,"Reply schedule: "+ReplyPolicy.reason(prefs,"sms")+"\nAnswers use this business SIM.",14));
        if(BotSetup.needsRecovery(prefs)){
            LinearLayout recovery=BasicUi.card(this,body);
            recovery.addView(BasicUi.text(this,"Your saved answers are in the wrong box",20));
            recovery.addView(BasicUi.text(this,"Move them into answer cards with one tap. Your original text will be kept. This does not turn the chatbot on.",15));
            BasicUi.button(this,recovery,"Organise my saved answers",this::recover);
        }
        try{rules=BotSetup.read(prefs);}catch(IllegalArgumentException e){
            body.addView(BasicUi.text(this,e.getMessage(),16));
            BasicUi.button(this,body,"Review saved text",this::reviewLegacy);return;
        }
        BasicUi.button(this,body,"+ Add an answer",()->edit(-1,"",""));
        BasicUi.button(this,body,"Start with an example",this::example);
        LinearLayout test=BasicUi.card(this,body);test.addView(BasicUi.text(this,"Try a customer question",20));
        EditText question=BasicUi.input(this,test,"Example: Are you available today?","",false);
        question.setId(R.id.testMessageInput);
        TextView result=BasicUi.text(this,"Preview your saved answers here. No text message will be sent.",15);result.setId(R.id.testBotResult);
        Button tryButton=BasicUi.button(this,test,"Preview answer",()->{
            if(question.getText().toString().trim().isEmpty()){question.setError("Type a customer question");return;}
            result.setText(BotReplies.preview(this,prefs,question.getText().toString(),true));
        });tryButton.setId(R.id.runTestButton);test.addView(result);
        LinearLayout general=BasicUi.card(this,body);general.addView(BasicUi.text(this,"When no answer matches",20));
        String fallback=prefs.getString("chatbot_fallback",getString(R.string.default_chatbot_fallback));
        general.addView(BasicUi.text(this,KeywordRules.containsRule(fallback)?"Organise your saved answers above to replace the misplaced rules with a general reply.":fallback,16));
        BasicUi.button(this,general,"Edit general reply",this::fallback);
        body.addView(BasicUi.text(this,"Your answers · "+rules.size(),22));
        if(rules.isEmpty())body.addView(BasicUi.text(this,"No answers yet. Add one or choose an example above.",15));
        else body.addView(BasicUi.text(this,"If several answers match, the first enabled card wins. Move specific questions above general ones.",14));
        for(int n=0;n<rules.size();n++){
            final int at=n;KeywordRules.Rule r=rules.get(n);LinearLayout card=BasicUi.card(this,body);
            Switch toggle=new Switch(this);toggle.setText((n+1)+". "+r.keywords.split(",")[0]);toggle.setTextSize(19);toggle.setChecked(r.enabled);card.addView(toggle);
            toggle.setOnCheckedChangeListener((v,on)->{List<KeywordRules.Rule> next=new ArrayList<>(rules);next.set(at,new KeywordRules.Rule(r.keywords,r.reply,on));persist(next);});
            card.addView(BasicUi.text(this,"When a message includes: "+r.keywords,14));
            TextView answer=BasicUi.text(this,r.reply,16);answer.setMaxLines(5);answer.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(answer);
            BasicUi.button(this,card,"Edit answer",()->edit(at,r.keywords,r.reply));
            if(n>0)BasicUi.button(this,card,"Move up",()->{List<KeywordRules.Rule> next=new ArrayList<>(rules);Collections.swap(next,at,at-1);persist(next);});
            BasicUi.button(this,card,"Delete answer",()->new AlertDialog.Builder(this).setTitle("Delete this answer?").setMessage(r.keywords)
                .setNegativeButton("Keep",null).setPositiveButton("Delete",(d,w)->{List<KeywordRules.Rule> next=new ArrayList<>(rules);next.remove(at);persist(next);}).show());
        }
        body.addView(BasicUi.text(this,"ReplyDesk adds the automated-reply label and its name to outgoing replies. Changes are saved when you tap Save.",14));
    }
    private void persist(List<KeywordRules.Rule> next){try{BotSetup.save(prefs,next);render();}catch(RuntimeException e){BasicUi.error(this,e);}}
    private ScrollView scroll(LinearLayout form){ScrollView s=new ScrollView(this);int pad=BasicUi.dp(this,18);form.setPadding(pad,0,pad,pad);s.addView(form);return s;}
    private void edit(int at,String keys,String answer){
        LinearLayout form=BasicUi.column(this);
        EditText words=BasicUi.input(this,form,"Words or phrases customers may use",keys,true);words.setId(R.id.newKeywordsInput);words.setHint("available, availability, any cancellation, free today");
        form.addView(BasicUi.text(this,"Separate alternatives with commas. The bot matches these words anywhere in a message.",14));
        EditText reply=BasicUi.input(this,form,"Your answer",answer,true);reply.setId(R.id.newReplyInput);reply.setMinLines(5);reply.setHint("Write the message your customer should receive.");
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(at<0?"Add an answer":"Edit answer").setView(scroll(form)).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            String k=words.getText().toString().trim().replaceAll("[\\r\\n]+",",");String text=reply.getText().toString().trim();
            if(k.replace(",","").trim().isEmpty()){words.setError("Add a word or phrase");return;}
            if(text.isEmpty()){reply.setError("Write your answer");return;}
            if(KeywordRules.containsRule(k)||KeywordRules.containsRule(text)){reply.setError("Enter only the answer here. Put words in the box above.");return;}
            List<KeywordRules.Rule> next=new ArrayList<>(rules);KeywordRules.Rule rule=new KeywordRules.Rule(k,text,at<0||rules.get(at).enabled);
            if(at<0)next.add(rule);else next.set(at,rule);
            try{BotSetup.save(prefs,next);dialog.dismiss();render();}catch(RuntimeException e){BasicUi.error(this,e);}
        }));dialog.show();
    }
    private void example(){
        String[] names={"Availability / openings","Reschedule an appointment","Cancel an appointment","Prices","Opening hours","Location","Talk to staff","Book an appointment"};
        String[] words={"available,availability,any cancellation,cancellations today,cancellation today,free today,free slot,open slot,last minute,last-minute",
            "reschedule,change my appointment,change appointment,move my appointment,move appointment,change my booking",
            "cancel,cancellation","price,cost,how much","hours,open,opening time","address,location,where","human,person,staff,help","book,booking,appointment"};
        String[] replies={"Please check our booking website for current availability and last-minute openings: ",
            "To reschedule your appointment, please open your booking confirmation email and use the reschedule option. Thank you!",
            "To cancel your appointment, please open your booking confirmation email and use the cancellation option. Thank you for letting us know.",
            "Thanks for asking! Our prices start at ","We are open ","Our address is ","Please leave your message and our team will get back to you as soon as possible. Thank you!","Please book online here: "};
        new AlertDialog.Builder(this).setTitle("Choose a starting point").setItems(names,(d,w)->edit(-1,words[w],(w==0||w==3||w==4||w==5||w==7)?"":replies[w])).show();
    }
    private void fallback(){
        if(BotSetup.needsRecovery(prefs)){recover();return;}
        LinearLayout form=BasicUi.column(this);EditText input=BasicUi.input(this,form,"One message for questions you have not added yet",prefs.getString("chatbot_fallback",getString(R.string.default_chatbot_fallback)),true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("General reply").setView(scroll(form)).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            String value=input.getText().toString().trim();if(value.isEmpty()||KeywordRules.containsRule(value)){input.setError("Write one reply message. Use Add an answer for questions and answers.");return;}
            if(!prefs.edit().putString("chatbot_fallback",value).commit()){BasicUi.error(this,new Exception("Could not save"));return;}
            dialog.dismiss();render();
        }));dialog.show();
    }
    private void recover(){
        try{
            List<KeywordRules.Rule> preview=BotSetup.recoveryPreview(prefs);LinearLayout list=BasicUi.column(this);
            list.addView(BasicUi.text(this,"These answers will become cards. Existing answers are kept; recovered answers are placed first. A general reply replaces the misplaced rules. Original text is kept on this phone.",15));
            for(KeywordRules.Rule r:preview){list.addView(BasicUi.text(this,r.keywords,17));list.addView(BasicUi.text(this,r.reply,14));}
            new AlertDialog.Builder(this).setTitle("Organise "+preview.size()+" answers").setView(scroll(list)).setNegativeButton("Cancel",null)
                .setPositiveButton("Organise answers",(d,w)->{try{BotSetup.recover(this,prefs);render();}catch(RuntimeException e){BasicUi.error(this,e);}}).show();
        }catch(IllegalArgumentException e){new AlertDialog.Builder(this).setTitle("Your original text is safe").setMessage(e.getMessage()).setNegativeButton("Close",null).setPositiveButton("Review saved text",(d,w)->reviewLegacy()).show();}
    }
    private void reviewLegacy(){
        // Only shown for a damaged legacy import; ordinary setup never requires rule syntax.
        String[] keys=BotSetup.needsRecovery(prefs)?new String[]{"chatbot_fallback","sms_reply_message","chatbot_rules"}:new String[]{"chatbot_rules"};
        List<String> found=new ArrayList<>();for(String k:keys)if(!prefs.getString(k,"").isEmpty())found.add(k);
        new AlertDialog.Builder(this).setTitle("Choose saved text to review").setItems(found.toArray(new String[0]),(d,w)->{
            String key=found.get(w);LinearLayout form=BasicUi.column(this);EditText input=BasicUi.input(this,form,"Keep one keyword group and answer per line",prefs.getString(key,""),true);
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Review saved text").setView(scroll(form)).setNegativeButton("Cancel",null).setPositiveButton("Save reviewed text",null).create();
            dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{try{
                String raw=input.getText().toString();KeywordRules.parse(raw);SharedPreferences.Editor e=prefs.edit();BotSetup.backup(prefs,e,key);
                if(!e.putString(key,raw).commit())throw new IllegalStateException("Could not save");dialog.dismiss();render();
            }catch(RuntimeException ex){input.setError(ex.getMessage());}}));dialog.show();
        }).show();
    }
}
