package com.smartreply.beta;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import org.json.*;
import java.text.DateFormat;
import java.util.*;
public class ScheduledSmsActivity extends ProfileActivity {
    private String filter="Pending";
    @Override public void onCreate(Bundle b){super.onCreate(b);if(b!=null)filter=b.getString("filter","Pending");}
    @Override public void onResume(){super.onResume();ScheduledSmsStore.restore(this,false);render();}
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("filter",filter);}
    private void render(){
        LinearLayout body=BasicUi.page(this,"Scheduled SMS",businessId);
        BasicUi.button(this,body,"+ Schedule a message",()->edit(null));
        body.addView(BasicUi.text(this,"Uses this business's SIM. Phone must be on with SMS service. Messages over 15 minutes late are not sent. Reply ON/OFF switches apply to auto replies; scheduled messages are managed here.",14));
        LinearLayout tabs=new LinearLayout(this);body.addView(tabs);
        for(String name:new String[]{"Pending","Sent","Failed","All"}){
            Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextColor(name.equals(filter)?0xff008b94:0xff526477);
            tabs.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->{filter=name;render();});
        }
        JSONArray jobs=ScheduledSmsStore.list(this,businessId);int count=0;
        for(int n=jobs.length()-1;n>=0;n--){
            JSONObject job=jobs.optJSONObject(n);String state=job.optString("state");
            boolean pending=ScheduledSmsStore.editable(job)||state.equals("Submitted")||state.equals("Dispatching");
            if(!filter.equals("All") && !(filter.equals("Pending")&&pending) && !(filter.equals("Sent")&&state.equals("Sent"))
                && !(filter.equals("Failed")&&(state.equals("Failed")||state.equals("Unconfirmed"))))continue;
            count++;LinearLayout card=BasicUi.card(this,body);
            card.addView(BasicUi.text(this,state+" · "+job.optJSONArray("numbers").length()+" recipient(s)",20));
            card.addView(BasicUi.text(this,time(job),16));card.addView(BasicUi.text(this,job.optString("message"),16));
            if(!job.optString("reason").isEmpty())card.addView(BasicUi.text(this,job.optString("reason"),14));
            BasicUi.button(this,card,"Details",()->details(job));
            if(ScheduledSmsStore.editable(job)){
                BasicUi.button(this,card,"Edit message or time",()->edit(job));
                BasicUi.button(this,card,"Cancel scheduled message",()->new AlertDialog.Builder(this).setTitle("Cancel this scheduled SMS?")
                    .setNegativeButton("Keep",null).setPositiveButton("Cancel SMS",(d,w)->{
                        boolean cancelled=ScheduledSmsStore.cancel(this,job.optString("id"));
                        if(!cancelled)Toast.makeText(this,"Sending has already started; it can no longer be cancelled",Toast.LENGTH_LONG).show();render();
                    }).show());
            }
        }
        if(count==0)body.addView(BasicUi.text(this,"No messages in this section.",17));
    }
    private String time(JSONObject job){
        DateFormat f=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT);
        f.setTimeZone(TimeZone.getTimeZone(job.optString("zone",TimeZone.getDefault().getID())));
        return f.format(new Date(job.optLong("time")))+" · "+job.optString("zone");
    }
    private void details(JSONObject job){
        StringBuilder s=new StringBuilder(BusinessProfiles.summary(this,businessId)+"\n"+time(job)+"\n\n");
        JSONArray recipients=job.optJSONArray("numbers"),states=job.optJSONArray("recipients");
        for(int n=0;n<recipients.length();n++)s.append(recipients.optString(n)).append(" · ").append(states==null?job.optString("state"):states.optString(n)).append("\n");
        s.append("\nSent means Android reported sending all parts; recipient delivery is not confirmed.");
        new AlertDialog.Builder(this).setTitle("Scheduled SMS details").setMessage(s).setPositiveButton("OK",null).show();
    }
    private void edit(JSONObject job){
        if(!ScheduledSmsStore.canSchedule(this)){
            new AlertDialog.Builder(this).setTitle("Allow Alarms & reminders")
                .setMessage("ReplyDesk needs this permission to trigger your SMS at the selected time. Return here after allowing it.")
                .setNegativeButton("Cancel",null).setPositiveButton("Open settings",(d,w)->{
                    if(Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
                }).show();return;
        }
        LinearLayout form=BasicUi.column(this);form.setPadding(30,10,30,10);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        form.addView(BasicUi.text(this,BusinessProfiles.summary(this,businessId),15));
        StringBuilder numbers=new StringBuilder();if(job!=null)for(int i=0;i<job.optJSONArray("numbers").length();i++){if(i>0)numbers.append("\n");numbers.append(job.optJSONArray("numbers").optString(i));}
        EditText to=BasicUi.input(this,form,"Recipients — one number per line",numbers.toString(),true);
        BasicUi.button(this,form,"Choose saved recipients",()->{
            JSONArray contacts=RecipientRules.contacts(BusinessProfiles.prefs(this,businessId));
            if(contacts.length()==0){Toast.makeText(this,"Add recipients in the Recipients tab first",Toast.LENGTH_LONG).show();return;}
            String[] labels=new String[contacts.length()];boolean[] checked=new boolean[labels.length];
            for(int i=0;i<labels.length;i++)labels[i]=contacts.optJSONObject(i).optString("name")+" · "+contacts.optJSONObject(i).optString("phone");
            new AlertDialog.Builder(this).setTitle("Recipients").setMultiChoiceItems(labels,checked,(d,which,value)->checked[which]=value)
                .setPositiveButton("Add",(d,w)->{StringBuilder text=new StringBuilder(to.getText());for(int i=0;i<checked.length;i++)if(checked[i]){
                    if(text.length()>0)text.append("\n");text.append(contacts.optJSONObject(i).optString("phone"));}to.setText(text);}).setNegativeButton("Cancel",null).show();
        });
        EditText message=BasicUi.input(this,form,"Message",job==null?"":job.optString("message"),true);
        BasicUi.button(this,form,"Choose a message template",()->TemplatesActivity.choose(this,businessId,value->message.setText(value)));
        Calendar when=Calendar.getInstance();when.setTimeInMillis(job==null?System.currentTimeMillis()+5*60_000:job.optLong("time"));when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);
        TextView date=BasicUi.text(this,"",18);form.addView(date);
        Runnable update=()->date.setText(DateFormat.getDateTimeInstance().format(when.getTime())+"\n"+TimeZone.getDefault().getID());update.run();
        BasicUi.button(this,form,"Choose date",()->new DatePickerDialog(this,(v,y,m,d)->{when.set(y,m,d);update.run();},when.get(Calendar.YEAR),when.get(Calendar.MONTH),when.get(Calendar.DAY_OF_MONTH)).show());
        BasicUi.button(this,form,"Choose time",()->new TimePickerDialog(this,(v,h,m)->{when.set(Calendar.HOUR_OF_DAY,h);when.set(Calendar.MINUTE,m);update.run();},when.get(Calendar.HOUR_OF_DAY),when.get(Calendar.MINUTE),false).show());
        form.addView(BasicUi.text(this,"One-time SMS. Carrier charges apply to each recipient and message part.",14));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(job==null?"Schedule SMS":"Edit scheduled SMS").setView(scroll)
            .setNegativeButton("Cancel",null).setPositiveButton("Save schedule",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
            List<String> recipients=RecipientRules.numbers(BusinessProfiles.prefs(this,businessId),to.getText().toString());
            ScheduledSmsStore.save(this,businessId,job==null?null:job.optString("id"),recipients,message.getText().toString(),when.getTimeInMillis());
            dialog.dismiss();filter="Pending";render();
        }catch(Exception e){BasicUi.error(this,e);}}));dialog.show();
    }
}
