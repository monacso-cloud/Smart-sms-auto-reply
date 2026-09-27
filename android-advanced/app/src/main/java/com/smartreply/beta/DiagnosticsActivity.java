package com.smartreply.beta;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.util.*;
public class DiagnosticsActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);}
    @Override public void onResume(){super.onResume();render();}
    private void render(){
        LinearLayout body=BasicUi.page(this,"Permissions & diagnostics",-1);
        BasicUi.button(this,body,"Refresh",()->render());
        BasicUi.button(this,body,"Allow Phone & SMS permissions",()->{
            List<String> missing=new ArrayList<>();
            for(String p:new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CALL_LOG,Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS})if(checkSelfPermission(p)!=0)missing.add(p);
            if(!missing.isEmpty())requestPermissions(missing.toArray(new String[0]),100);else Toast.makeText(this,"Phone and SMS permissions are allowed",Toast.LENGTH_SHORT).show();
        });
        BasicUi.button(this,body,"Open Android app settings",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        BasicUi.button(this,body,"Allow scheduled SMS alarms",()->{
            if(Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
            else Toast.makeText(this,"Alarm access is already available",Toast.LENGTH_SHORT).show();
        });
        BasicUi.button(this,body,"Send a test SMS",()->test());
        BasicUi.button(this,body,"Copy diagnostic report",()->{
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("ReplyDesk diagnostics",Diagnostics.report(this)));
            Toast.makeText(this,"Copied. You can paste it into your support message.",Toast.LENGTH_LONG).show();
        });
        body.addView(BasicUi.text(this,"Test SMS checks sending only. An incoming call or SMS is needed to test automatic replies. RCS chats do not trigger the SMS receiver.",15));
        TextView report=BasicUi.text(this,Diagnostics.report(this),15);report.setTextIsSelectable(true);body.addView(report);
    }
    private void test(){
        List<android.telephony.SubscriptionInfo> active=SimRouter.active(this);
        if(active.isEmpty()){Toast.makeText(this,"No active SIM found. Check Phone permission.",Toast.LENGTH_LONG).show();return;}
        LinearLayout form=BasicUi.column(this);form.setPadding(30,10,30,10);
        List<String> names=new ArrayList<>();for(android.telephony.SubscriptionInfo s:active)names.add(SimRouter.label(s));
        Spinner sim=new Spinner(this);sim.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));form.addView(sim);
        EditText to=BasicUi.input(this,form,"Phone number to receive one test SMS","",false);
        form.addView(BasicUi.text(this,"This sends a real SMS. Your carrier's SMS charges apply.",14));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Test selected SIM").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Send test",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
            android.telephony.SubscriptionInfo info=active.get(sim.getSelectedItemPosition());BusinessProfiles.register(this,info);
            int id=info.getSubscriptionId();List<String> numbers=RecipientRules.numbers(BusinessProfiles.prefs(this,id),to.getText().toString());
            if(numbers.size()!=1)throw new IllegalArgumentException("Enter one test phone number");
            ReplySender.send(this,id,numbers.get(0),"ReplyDesk test SMS. Please check which number sent this message.","TEST");dialog.dismiss();render();
        }catch(Exception e){BasicUi.error(this,e);}}));dialog.show();
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);render();}
}
