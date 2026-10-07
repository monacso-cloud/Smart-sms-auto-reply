package com.smartreply.beta;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
import java.util.*;
public class RecipientsActivity extends ProfileActivity {
    @Override public void onCreate(Bundle b){super.onCreate(b);render();}
    private void render(){
        SharedPreferences p=BusinessProfiles.prefs(this,businessId);LinearLayout body=BasicUi.page(this,"Recipients",businessId);
        LinearLayout rules=BasicUi.card(this,body);rules.addView(BasicUi.text(this,"Who receives automatic replies?",21));
        Switch only=new Switch(this);only.setText("Only saved recipients");only.setChecked("selected".equals(p.getString("sender_filter","everyone")));rules.addView(only);
        rules.addView(BasicUi.text(this,"OFF: everyone. Ignored numbers are always excluded. Applies to missed calls and incoming SMS.",14));
        EditText ignored=BasicUi.input(this,rules,"Ignored phone numbers",p.getString("ignored_numbers",""),true);
        BasicUi.button(this,rules,"Save recipient filters",()->{try{
            List<String> nums=RecipientRules.numbers(p,ignored.getText().toString());
            p.edit().putString("sender_filter",only.isChecked()?"selected":"everyone").putString("ignored_numbers",android.text.TextUtils.join("\n",nums)).apply();
            Toast.makeText(this,"Filters saved for this business",Toast.LENGTH_SHORT).show();
        }catch(Exception e){BasicUi.error(this,e);}});
        BasicUi.button(this,body,"+ Add recipient",()->edit(-1));
        JSONArray contacts=RecipientRules.contacts(p);
        for(int i=0;i<contacts.length();i++){
            final int at=i;JSONObject contact=contacts.optJSONObject(i);LinearLayout card=BasicUi.card(this,body);
            card.addView(BasicUi.text(this,contact.optString("name"),20));card.addView(BasicUi.text(this,contact.optString("phone"),16));
            BasicUi.button(this,card,"Edit",()->edit(at));
            BasicUi.button(this,card,"Remove",()->new AlertDialog.Builder(this).setTitle("Remove this recipient?").setNegativeButton("Cancel",null)
                .setPositiveButton("Remove",(d,w)->{JSONArray list=RecipientRules.contacts(p);list.remove(at);p.edit().putString("saved_recipients",list.toString()).apply();render();}).show());
        }
    }
    private void edit(int at){
        SharedPreferences p=BusinessProfiles.prefs(this,businessId);JSONArray contacts=RecipientRules.contacts(p);
        JSONObject existing=at<0?null:contacts.optJSONObject(at);LinearLayout form=BasicUi.column(this);form.setPadding(30,10,30,10);
        EditText name=BasicUi.input(this,form,"Name",existing==null?"":existing.optString("name"),false);
        EditText phone=BasicUi.input(this,form,"Phone number",existing==null?"":existing.optString("phone"),false);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Business recipient").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
            List<String> numbers=RecipientRules.numbers(p,phone.getText().toString());
            if(numbers.size()!=1)throw new IllegalArgumentException("Enter one phone number");
            String n=name.getText().toString().trim();if(n.isEmpty())throw new IllegalArgumentException("Enter a name");
            JSONObject c=new JSONObject().put("name",n).put("phone",numbers.get(0));
            if(at<0)contacts.put(c);else contacts.put(at,c);p.edit().putString("saved_recipients",contacts.toString()).apply();dialog.dismiss();render();
        }catch(Exception e){BasicUi.error(this,e);}}));dialog.show();
    }
}
