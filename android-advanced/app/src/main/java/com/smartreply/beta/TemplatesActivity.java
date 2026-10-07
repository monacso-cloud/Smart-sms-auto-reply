package com.smartreply.beta;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
public class TemplatesActivity extends ProfileActivity {
    interface Selection{void choose(String value);}
    private static JSONArray templates(Context c,int id){try{return new JSONArray(BusinessProfiles.prefs(c,id).getString("message_templates","[]"));}catch(Exception e){return new JSONArray();}}
    public static void choose(Activity c,int id,Selection selected){
        JSONArray list=templates(c,id);if(list.length()==0){Toast.makeText(c,"Add a message in the Templates tab first",Toast.LENGTH_LONG).show();return;}
        String[] names=new String[list.length()];for(int n=0;n<names.length;n++)names[n]=list.optJSONObject(n).optString("name");
        new AlertDialog.Builder(c).setTitle("Message templates").setItems(names,(d,w)->selected.choose(list.optJSONObject(w).optString("text"))).show();
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);render();}
    private void render(){
        LinearLayout body=BasicUi.page(this,"Message templates",businessId);BasicUi.button(this,body,"+ New template",()->edit(-1));
        JSONArray list=templates(this,businessId);
        if(list.length()==0)body.addView(BasicUi.text(this,"Save messages here to reuse in replies or scheduled SMS.",17));
        for(int n=0;n<list.length();n++){
            final int at=n;JSONObject item=list.optJSONObject(n);LinearLayout card=BasicUi.card(this,body);
            card.addView(BasicUi.text(this,item.optString("name"),21));card.addView(BasicUi.text(this,item.optString("text"),16));
            BasicUi.button(this,card,"Use template",()->new AlertDialog.Builder(this).setTitle("Use this message for")
                .setItems(new String[]{"Missed-call reply","Plain SMS reply","Unmatched bot reply","Copy text"},(d,w)->{
                    String text=item.optString("text");if(w==3)((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("ReplyDesk template",text));
                    else BusinessProfiles.prefs(this,businessId).edit().putString(new String[]{"message","sms_reply_message","chatbot_fallback"}[w],text).apply();
                    Toast.makeText(this,w==3?"Copied":"Reply text updated; existing ON/OFF settings kept",Toast.LENGTH_LONG).show();
                }).show());
            BasicUi.button(this,card,"Edit",()->edit(at));
            BasicUi.button(this,card,"Delete template",()->new AlertDialog.Builder(this).setTitle("Delete this template?").setNegativeButton("Cancel",null)
                .setPositiveButton("Delete",(d,w)->{JSONArray items=templates(this,businessId);items.remove(at);BusinessProfiles.prefs(this,businessId).edit().putString("message_templates",items.toString()).apply();render();}).show());
        }
    }
    private void edit(int at){
        JSONArray list=templates(this,businessId);JSONObject old=at<0?null:list.optJSONObject(at);
        LinearLayout form=BasicUi.column(this);form.setPadding(30,10,30,10);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        EditText name=BasicUi.input(this,form,"Template name",old==null?"":old.optString("name"),false);
        EditText text=BasicUi.input(this,form,"Message",old==null?"":old.optString("text"),true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Message template").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
            if(name.getText().toString().trim().isEmpty()||text.getText().toString().trim().isEmpty())throw new IllegalArgumentException("Enter a name and message");
            JSONObject item=new JSONObject().put("name",name.getText().toString().trim()).put("text",text.getText().toString().trim());
            if(at<0)list.put(item);else list.put(at,item);BusinessProfiles.prefs(this,businessId).edit().putString("message_templates",list.toString()).apply();dialog.dismiss();render();
        }catch(Exception e){BasicUi.error(this,e);}}));dialog.show();
    }
}
