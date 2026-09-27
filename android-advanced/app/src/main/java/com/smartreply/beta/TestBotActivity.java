package com.smartreply.beta;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
public class TestBotActivity extends ProfileActivity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_test_bot);
        EditText input=findViewById(R.id.testMessageInput);TextView result=findViewById(R.id.testBotResult);
        findViewById(R.id.runTestButton).setOnClickListener(v->{
            if(input.getText().toString().trim().isEmpty()){input.setError("Type a customer question");return;}
            SharedPreferences p=BusinessProfiles.prefs(this,businessId);
            result.setText(BotReplies.preview(this,p,input.getText().toString(),false)+"\n\nReply schedule: "+ReplyPolicy.reason(p,"sms"));
        });
    }
}
