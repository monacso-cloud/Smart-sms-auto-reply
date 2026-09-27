package com.smartreply.beta;
import android.app.Activity;
import android.content.Context;
import android.view.*;
import android.widget.*;
public final class BasicUi {
    private BasicUi(){}
    public static int dp(Context c,int n){return (int)(n*c.getResources().getDisplayMetrics().density);}
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(1);return l;}
    public static TextView text(Context c,String s,int size){
        TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(0xff16354d);
        t.setPadding(0,dp(c,7),0,dp(c,7));return t;
    }
    public static LinearLayout page(Activity a,String title,int id){
        LinearLayout root=column(a);root.setFitsSystemWindows(true);root.setBackgroundColor(0xfff3f6fb);
        TextView top=text(a,"‹  "+title,24);top.setPadding(dp(a,20),dp(a,14),dp(a,20),dp(a,10));
        top.setOnClickListener(v->a.finish());root.addView(top);
        ScrollView scroll=new ScrollView(a);LinearLayout body=column(a);body.setPadding(dp(a,20),0,dp(a,20),dp(a,28));
        if(id>=0)body.addView(text(a,BusinessProfiles.summary(a,id),14));
        scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));a.setContentView(root);return body;
    }
    public static LinearLayout card(Context c,LinearLayout body){
        LinearLayout panel=column(c);panel.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,16));panel.setBackgroundResource(R.drawable.rd_card);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(c,14);body.addView(panel,lp);return panel;
    }
    public static Button button(Context c,LinearLayout parent,String label,Runnable action){
        Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setMinHeight(dp(c,50));
        b.setTextColor(0xffffffff);b.setBackgroundResource(R.drawable.rd_button);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(c,10);parent.addView(b,lp);
        b.setOnClickListener(v->action.run());return b;
    }
    public static EditText input(Context c,LinearLayout parent,String label,String value,boolean multiline){
        parent.addView(text(c,label,15));EditText e=new EditText(c);e.setText(value);e.setTextSize(17);
        e.setSingleLine(!multiline);if(multiline){e.setMinLines(3);e.setGravity(Gravity.TOP);}
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (multiline?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));
        e.setBackgroundResource(R.drawable.rd_field);e.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));
        parent.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
    }
    public static void error(Context c,Exception e){Toast.makeText(c,e.getMessage()==null?"Unable to save. Please try again.":e.getMessage(),Toast.LENGTH_LONG).show();}
}
