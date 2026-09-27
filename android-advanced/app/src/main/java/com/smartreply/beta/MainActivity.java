package com.smartreply.beta;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
public class MainActivity extends ProfileActivity {
    @Override public void onCreate(Bundle b){super.onCreate(b);
        SharedPreferences p=BusinessProfiles.prefs(this,businessId);LinearLayout body=BasicUi.page(this,"Delay & repeat protection",businessId);
        body.addView(BasicUi.text(this,"Missed-call replies",23));
        body.addView(BasicUi.text(this,"Delay before replying",16));Spinner delay=new Spinner(this);
        delay.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"No delay","15 seconds","30 seconds","60 seconds"}));
        int[] delays={0,15,30,60};for(int n=0;n<delays.length;n++)if(delays[n]==p.getInt("delay_seconds",0))delay.setSelection(n);body.addView(delay);
        body.addView(BasicUi.text(this,"Send again to the same caller",16));Spinner repeat=new Spinner(this);
        repeat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Every missed call","Every 15 minutes","Every hour","Every 6 hours","Every 12 hours","Every day"}));
        int[] intervals={0,15,60,360,720,1440};for(int n=0;n<intervals.length;n++)if(intervals[n]==p.getInt("repeat_minutes",0))repeat.setSelection(n);body.addView(repeat);
        body.addView(BasicUi.text(this,"Reply SIM: same as receiving SIM\n"+p.getString("sim_label",""),16));
        BasicUi.button(this,body,"Save",()->{p.edit().putInt("delay_seconds",delays[delay.getSelectedItemPosition()]).putInt("repeat_minutes",intervals[repeat.getSelectedItemPosition()]).apply();Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();});
    }
}
