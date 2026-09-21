package com.smartreply.beta;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.HashSet;
import java.util.Set;

public class RuleEditorActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";
    private LinearLayout content;
    private EditText nameInput;
    private EditText phrasesInput;
    private EditText responseInput;
    private Spinner departmentSpinner;
    private Spinner categorySpinner;
    private Spinner prioritySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        build();
    }

    private void build() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244,247,251));

        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this); back.setText("‹"); back.setTextSize(26); back.setOnClickListener(v->finish());
        header.addView(back,new LinearLayout.LayoutParams(dp(56),dp(52)));
        TextView title=text("Add / Edit Auto Reply",22,true); header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(header);

        ScrollView scroll=new ScrollView(this);
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(8),dp(20),dp(28));

        String incomingName=getIntent().getStringExtra("rule_name");
        if(incomingName==null) incomingName="";

        nameInput=input("RULE NAME",incomingName);
        departmentSpinner=spinner("DEPARTMENT",new String[]{"General","Sales","Customer Service","Bookings","Accounts","Payments","Technical Support","Complaints","Delivery","HR","Management","Custom"});
        categorySpinner=spinner("CATEGORY",new String[]{"General","Sales","Bookings","Services","Accounts","Payments","Delivery","Technical Support","Complaints","HR","Custom"});

        phrasesInput=largeEdit("CUSTOMER MIGHT SAY / KEYWORDS","price\nhow much\ncost\nwhat does it cost");
        button("+ Add phrase",v->phrasesInput.append("\n"));
        button("+ Paste multiple phrases",v->showInfo("Paste one phrase per line. There is no small phrase limit."));

        responseInput=largeEdit("AUTOMATIC REPLY","");

        CheckBox booking=check("Add booking link");
        CheckBox website=check("Add business website");
        CheckBox signature=check("Add signature");
        CheckBox notify=check("Notify staff");
        CheckBox staff=check("Mark staff assistance required");
        CheckBox stop=check("Stop Bot after this response");
        CheckBox wait=check("Wait for customer response");

        prioritySpinner=spinner("PRIORITY",new String[]{"Low","Normal","High","Critical"});

        button("TEST RULE",v->testRule());
        buttonPrimary("SAVE",v->saveRule());
        button("DUPLICATE",v->showInfo("A duplicate copy will be created with a new name."));
        button("DELETE",v->confirmDelete());

        TextView note=text("ReplyDesk stores the technical routing format internally. Customers never need to edit keyword=>reply syntax.",14,false);
        note.setPadding(0,dp(16),0,0); content.addView(note);

        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void testRule() {
        String phrase=phrasesInput.getText().toString().trim();
        String response=responseInput.getText().toString().trim();
        if(phrase.isEmpty() || response.isEmpty()) {
            showInfo("Add at least one customer phrase and an automatic reply first.");
            return;
        }
        String first=phrase.split("\r?\n")[0].trim();
        showInfo("Test result: MATCHED ✓\n\nMatched phrase:\n"+first+"\n\nBot response:\n"+response+"\n\nNo SMS was sent.");
    }

    private void saveRule() {
        String name=nameInput.getText().toString().trim();
        String phrases=phrasesInput.getText().toString().trim();
        String response=responseInput.getText().toString().trim();
        if(name.isEmpty()){nameInput.setError("Enter a rule name");return;}
        if(phrases.isEmpty()){phrasesInput.setError("Add at least one customer phrase");return;}
        if(response.isEmpty()){responseInput.setError("Enter an automatic reply");return;}

        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        Set<String> names=new HashSet<>(p.getStringSet("rule_names",new HashSet<>()));
        String key=safeKey(name);

        for(String existing:names){
            if(!existing.equals(name)){
                String other=p.getString("rule_"+safeKey(existing)+"_phrases","");
                String duplicate=findDuplicatePhrase(phrases,other);
                if(duplicate!=null){
                    new AlertDialog.Builder(this)
                            .setTitle("Duplicate phrase detected")
                            .setMessage("This phrase is already used by another Auto Reply:\n\n"+duplicate+"\n\nExisting Rule: "+existing+"\nNew Rule: "+name)
                            .setNegativeButton("Remove",null)
                            .setNeutralButton("View Existing Rule",null)
                            .setPositiveButton("Keep",(d,w)->commitRule(p,names,name,key,phrases,response))
                            .show();
                    return;
                }
            }
        }
        commitRule(p,names,name,key,phrases,response);
    }

    private void commitRule(SharedPreferences p,Set<String> names,String name,String key,String phrases,String response){
        names.add(name);
        p.edit()
                .putStringSet("rule_names",names)
                .putString("rule_"+key+"_phrases",phrases)
                .putString("rule_"+key+"_response",response)
                .putString("rule_"+key+"_department",String.valueOf(departmentSpinner.getSelectedItem()))
                .putString("rule_"+key+"_category",String.valueOf(categorySpinner.getSelectedItem()))
                .putString("rule_"+key+"_priority",String.valueOf(prioritySpinner.getSelectedItem()))
                .apply();
        Toast.makeText(this,"Auto Reply saved",Toast.LENGTH_SHORT).show();
        finish();
    }

    private String findDuplicatePhrase(String current,String other){
        Set<String> a=new HashSet<>(); for(String s:current.split("\r?\n")) if(!s.trim().isEmpty()) a.add(ChatbotRules.normalize(s));
        for(String s:other.split("\r?\n")) if(a.contains(ChatbotRules.normalize(s)) && !s.trim().isEmpty()) return s.trim();
        return null;
    }

    private void confirmDelete(){
        new AlertDialog.Builder(this).setTitle("Delete Auto Reply?").setMessage("This removes this rule from the Auto Reply Library.")
                .setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->finish()).show();
    }

    private String safeKey(String value){return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+","_");}

    private EditText input(String label,String value){content.addView(label(label));EditText e=new EditText(this);e.setText(value);e.setTextSize(16);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(0,dp(5),0,dp(10));content.addView(e,lp);return e;}
    private EditText largeEdit(String label,String value){content.addView(label(label));EditText e=new EditText(this);e.setText(value);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setPadding(dp(12),dp(12),dp(12),dp(12));e.setBackgroundColor(Color.WHITE);e.setMinLines(5);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(180));lp.setMargins(0,dp(5),0,dp(12));content.addView(e,lp);return e;}
    private Spinner spinner(String label,String[] values){content.addView(label(label));Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(0,dp(5),0,dp(10));content.addView(s,lp);return s;}
    private CheckBox check(String label){CheckBox c=new CheckBox(this);c.setText(label);c.setTextSize(15);content.addView(c,new LinearLayout.LayoutParams(-1,dp(50)));return c;}
    private void button(String label,android.view.View.OnClickListener l){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(l);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(6),0,0);content.addView(b,lp);}
    private void buttonPrimary(String label,android.view.View.OnClickListener l){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(18,103,229));b.setOnClickListener(l);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(0,dp(10),0,0);content.addView(b,lp);}
    private TextView label(String v){TextView t=text(v,15,true);t.setPadding(0,dp(8),0,0);return t;}
    private TextView text(String v,int sp,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextSize(sp);t.setTextColor(Color.rgb(11,31,58));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
    private void showInfo(String m){new AlertDialog.Builder(this).setMessage(m).setPositiveButton("OK",null).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
