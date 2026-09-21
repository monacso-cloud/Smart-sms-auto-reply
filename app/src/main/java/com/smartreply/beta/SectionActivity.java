package com.smartreply.beta;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends Activity {
    private static final String PREFS = "smart_reply_settings";
    private static final int PERMISSION_REQUEST = 1001;
    private LinearLayout content;
    private String page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        page = getIntent().getStringExtra("page");
        if (page == null) page = "sms_bot";
        render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244,247,251));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8),dp(8),dp(12),dp(8));
        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(26);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(56),dp(52)));
        TextView title = text(titleFor(page),22,true);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(8),dp(20),dp(28));
        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        buildPage(page);
        root.addView(bottomNav());
        setContentView(root);
    }

    private void buildPage(String key) {
        switch (key) {
            case "missed_call": buildMissedCall(); break;
            case "sms_bot": buildSmsBot(); break;
            case "library": buildLibrary(); break;
            case "staff": buildStaff(); break;
            case "business_hours": buildBusinessHours(); break;
            case "after_hours": buildAfterHours(); break;
            case "conversations": buildConversationList(); break;
            case "activity": buildActivity(); break;
            case "test_bot": buildTestBot(); break;
            case "templates": buildTemplates(); break;
            case "contacts": buildSimpleInfo("Contacts / Customers","Customer records and searchable conversation contacts will live here."); break;
            case "scheduled": buildSimpleInfo("Scheduled Messages","Scheduled business messages and future-send controls will live here."); break;
            case "business_profile": buildBusinessProfile(); break;
            case "automation_schedule": buildAutomationSchedule(); break;
            case "bot_settings": buildBotSettings(); break;
            case "permissions": buildPermissions(); break;
            case "account": buildSimpleInfo("Subscription / Account","Subscription status, billing plan and account controls."); break;
            case "help": buildHelp(); break;
            case "feature_request": buildFeatureRequest(); break;
            case "department_routing": buildDepartmentRouting(); break;
            case "departments": buildDepartments(); break;
            case "unmatched": buildUnmatched(); break;
            default: buildSimpleInfo(titleFor(key),"This section is ready for expansion.");
        }
    }

    private void buildMissedCall() {
        SharedPreferences p = prefs();
        Switch on = toggle("Missed Call Auto Reply", p.getBoolean("enabled", false));
        EditText msg = largeEdit("Auto-reply message", p.getString("message", getString(R.string.default_message)));
        Spinner delay = spinner("Delay before sending", new String[]{"No delay","15 seconds","30 seconds","1 minute"}, delayIndex(p.getInt("delay_seconds",30)));
        Spinner repeat = spinner("Prevent repeated missed-call replies", new String[]{"Reply every missed call","15 minutes","1 hour","6 hours","12 hours","24 hours"}, repeatIndex(p.getInt("repeat_minutes",0)));
        CheckBox booking = check("Add booking link", p.getBoolean("missed_add_booking",false));
        CheckBox signature = check("Add signature", p.getBoolean("missed_add_signature",false));
        button("Preview", v -> showMessage("Preview", msg.getText().toString()));
        button("Send Test SMS", v -> open("permissions"));
        buttonPrimary("Save", v -> {
            p.edit().putBoolean("enabled",on.isChecked()).putString("message",msg.getText().toString().trim())
                    .putInt("delay_seconds",delayValue(delay.getSelectedItemPosition()))
                    .putInt("repeat_minutes",repeatValue(repeat.getSelectedItemPosition()))
                    .putBoolean("missed_add_booking",booking.isChecked()).putBoolean("missed_add_signature",signature.isChecked()).apply();
            toast("Missed-call settings saved");
        });
    }

    private void buildSmsBot() {
        SharedPreferences p = prefs();
        Switch bot = toggle("SMS Auto Bot",p.getBoolean("chatbot_enabled",false));
        button("Auto Reply Library",v->open("library"));
        button("Departments",v->open("departments"));
        button("Categories",v->open("library"));
        button("Unmatched Messages",v->open("unmatched"));
        button("Staff Handover",v->open("staff"));
        button("Bot Behaviour",v->open("bot_settings"));
        button("Test Bot",v->open("test_bot"));
        TextView note = text("Normal users never need to edit keyword=>reply syntax. ReplyDesk builds routing rules internally.",14,false);
        note.setPadding(0,dp(16),0,dp(10));
        content.addView(note);
        bot.setOnCheckedChangeListener((b,c)->p.edit().putBoolean("chatbot_enabled",c).apply());
    }

    private void buildLibrary() {
        EditText search = input("Search Auto Replies","");
        horizontalChips(new String[]{"All","Sales","Bookings","Services","Accounts","Payments","Delivery","Technical Support","Complaints","HR","General","Custom"});
        buttonPrimary("+ ADD AUTO REPLY",v->editRule(""));
        addRuleCard("Prices","12 customer phrases","Active");
        addRuleCard("Booking","19 customer phrases","Active");
        addRuleCard("Refunds","28 customer phrases","Active");
        addRuleCard("Delivery Status","35 customer phrases","Active");
        TextView scalability = text("Designed for hundreds or thousands of rules. Search, departments and categories keep large libraries manageable.",14,false);
        scalability.setPadding(0,dp(16),0,0);
        content.addView(scalability);
    }

    private void addRuleCard(String name,String phrases,String status) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        b.setText(name+"\n"+phrases+"  •  "+status);
        b.setTextSize(16);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(76)); lp.setMargins(0,dp(8),0,0);
        b.setOnClickListener(v->editRule(name));
        content.addView(b,lp);
    }

    private void editRule(String name) {
        Intent i=new Intent(this,RuleEditorActivity.class);
        i.putExtra("rule_name",name);
        startActivity(i);
    }

    private void buildDepartments() {
        input("Search department","");
        buttonPrimary("+ ADD DEPARTMENT",v->toast("Department editor will open here"));
        for(String d:new String[]{"Sales","Customer Service","Bookings","Accounts","Payments","Technical Support","Complaints","Delivery","HR","Management","General"}) {
            Button b=new Button(this); b.setAllCaps(false); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); b.setText(d+"    •    0 rules");
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58)); lp.setMargins(0,dp(6),0,0); content.addView(b,lp);
        }
    }

    private void buildUnmatched() {
        SharedPreferences p=prefs();
        spinner("When no rule matches",new String[]{"Send fallback reply","Ask customer to choose menu options","Flag for staff","Stop Bot","Send online booking link"},0);
        EditText fallback=largeEdit("Fallback message",p.getString("chatbot_fallback",getString(R.string.default_chatbot_fallback)));
        buttonPrimary("Save",v->{p.edit().putString("chatbot_fallback",fallback.getText().toString()).apply();toast("Unmatched-message settings saved");});
    }

    private void buildStaff() {
        SharedPreferences p=prefs();
        EditText phrases=largeEdit("Staff assistance phrases",p.getString("staff_phrases","human\nstaff\nstaff member\nspeak to someone\ntalk to someone\nreceptionist\nhelp\n4"));
        check("Flag conversation as STAFF REQUIRED",true);
        check("Notify business",true);
        check("Pause Bot for that conversation",true);
        check("Do not allow Bot to interrupt staff conversation",true);
        Spinner resume=spinner("Resume Bot",new String[]{"Manually","30 minutes","1 hour","2 hours","4 hours","Next day","Custom"},0);
        buttonPrimary("Save",v->{p.edit().putString("staff_phrases",phrases.getText().toString()).putInt("staff_resume",resume.getSelectedItemPosition()).apply();toast("Staff assistance settings saved");});
    }

    private void buildBusinessHours() {
        SharedPreferences p=prefs();
        for(String day:new String[]{"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"}) {
            input(day,p.getString("hours_"+day,day.equals("Sunday")?"Closed":"8:00 AM - 8:00 PM")).setOnFocusChangeListener((v,f)->{if(!f)p.edit().putString("hours_"+day,((EditText)v).getText().toString()).apply();});
        }
        check("Holiday Mode",p.getBoolean("holiday_mode",false));
        check("Away Mode",p.getBoolean("away_mode",false));
        check("Temporary Closure",p.getBoolean("temporary_closure",false));
        button("Special Hours",v->toast("Special-hours editor will open here"));
        buttonPrimary("Save",v->toast("Business hours saved"));
    }

    private void buildAfterHours() {
        SharedPreferences p=prefs();
        spinner("After-hours behaviour",new String[]{"Bot continues normally","Use after-hours message","Pause Bot"},1);
        EditText msg=largeEdit("After-hours message",p.getString("after_hours_message","Thanks for your message. We are currently closed and will respond during business hours."));
        check("Add booking link",p.getBoolean("after_booking",true));
        check("Show emergency/contact information if configured",p.getBoolean("after_emergency",false));
        check("Allow staff assistance",p.getBoolean("after_staff",false));
        buttonPrimary("Save",v->{p.edit().putString("after_hours_message",msg.getText().toString()).apply();toast("After-hours settings saved");});
    }

    private void buildConversationList() {
        horizontalChips(new String[]{"ALL","BOT HANDLED","STAFF REQUIRED","UNMATCHED","MISSED CALLS","FAILED"});
        input("Search conversations","");
        addConversation("Sarah","Are you available today?","Bot Handled ✓","10:42 AM");
        addConversation("John","I need to speak to someone","STAFF REQUIRED","10:38 AM");
        addConversation("Unknown","Question not matched","UNMATCHED","9:57 AM");
    }

    private void addConversation(String name,String preview,String status,String time) {
        Button b=new Button(this); b.setAllCaps(false); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        b.setText(name+"    "+time+"\n"+preview+"\n"+status);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(90)); lp.setMargins(0,dp(8),0,0); content.addView(b,lp);
    }

    private void buildActivity() {
        horizontalChips(new String[]{"Missed Calls","Incoming SMS","Bot Replies","Staff Replies","Failed","Unmatched"});
        input("Search activity","");
        spinner("Filter by status",new String[]{"All statuses","Sent","Delivered","Failed","Unmatched","Staff Required"},0);
        TextView t=text("Full-screen activity list\n\nDate/time • Customer/phone • Incoming message • Matched rule • Bot response • SMS status • SMS parts • Staff status",15,false);
        t.setPadding(dp(12),dp(16),dp(12),dp(16)); t.setBackgroundColor(Color.WHITE); content.addView(t);
    }

    private void buildTestBot() {
        EditText incoming=largeEdit("Type a customer message","Are you available today?");
        TextView result=text("No test run yet",15,false); result.setPadding(dp(14),dp(14),dp(14),dp(14)); result.setBackgroundColor(Color.WHITE); content.addView(result);
        buttonPrimary("TEST BOT — no SMS charge",v->{
            String rules=prefs().getString("chatbot_rules",getString(R.string.default_chatbot_rules));
            String reply=ChatbotRules.findReply(incoming.getText().toString(),rules);
            if(reply==null) result.setText("Result: NO MATCH\n\nFallback / staff-handover logic can be tested here without sending an SMS.");
            else result.setText("Result: MATCHED ✓\n\nBot Response:\n"+reply);
        });
    }

    private void buildTemplates() {
        for(String name:new String[]{"Booking","Cancellation","Reschedule","Payment","Follow-up","Review request","Directions","Thank you","Custom"}) {
            button(name,v->toast(name+" template editor"));
        }
        buttonPrimary("+ ADD CUSTOM TEMPLATE",v->toast("Custom template editor"));
    }

    private void buildAutomationSchedule() {
        SharedPreferences p=prefs();
        Switch master=toggle("Use Auto Reply Schedule",p.getBoolean("schedule_enabled",false));
        Spinner mode=spinner("Auto Reply Mode",new String[]{"Always on","Only during selected hours","Only outside selected hours","Manual only"},p.getInt("schedule_mode",0));
        EditText start=input("Start time",p.getString("schedule_start","8:00 AM"));
        EditText end=input("End time",p.getString("schedule_end","8:00 PM"));
        CheckBox missed=check("Auto reply to missed calls",p.getBoolean("schedule_missed_calls",true));
        CheckBox incoming=check("Auto reply to incoming SMS",p.getBoolean("schedule_incoming_sms",true));
        CheckBox unmatched=check("Reply to unmatched incoming messages",p.getBoolean("schedule_unmatched_sms",true));
        CheckBox after=check("Use different after-hours reply",p.getBoolean("schedule_after_hours",true));
        button("Business Hours",v->open("business_hours"));
        button("After-Hours Reply",v->open("after_hours"));
        buttonPrimary("Save",v->{
            p.edit().putBoolean("schedule_enabled",master.isChecked())
                    .putInt("schedule_mode",mode.getSelectedItemPosition())
                    .putString("schedule_start",start.getText().toString().trim())
                    .putString("schedule_end",end.getText().toString().trim())
                    .putBoolean("schedule_missed_calls",missed.isChecked())
                    .putBoolean("schedule_incoming_sms",incoming.isChecked())
                    .putBoolean("schedule_unmatched_sms",unmatched.isChecked())
                    .putBoolean("schedule_after_hours",after.isChecked()).apply();
            toast("Automation schedule saved");
        });
    }

    private void buildBusinessProfile() {
        SharedPreferences p=prefs();
        TextView logoStatus=text(p.getString("business_logo_uri","").isEmpty()?"No business logo selected":"Business logo selected ✓",15,true);
        logoStatus.setPadding(dp(14),dp(12),dp(14),dp(12)); logoStatus.setBackgroundColor(Color.WHITE); content.addView(logoStatus);
        button("Choose / Change Business Logo",v->{
            Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.setType("image/*"); pick.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(pick,2002);
        });
        EditText name=input("Business Name",p.getString("business_name",""));
        EditText type=input("Business Type",p.getString("business_type",""));
        EditText phone=input("Phone",p.getString("business_phone",""));
        EditText website=input("Website",p.getString("business_website",""));
        EditText booking=input("Booking URL",p.getString("booking_url",""));
        EditText address=largeEdit("Address",p.getString("business_address",""));
        EditText email=input("Email",p.getString("business_email",""));
        EditText signature=input("Signature",p.getString("business_signature",""));
        EditText staff=largeEdit("Default Staff Assistance Message",p.getString("default_staff_message","A staff member will respond as soon as possible."));
        buttonPrimary("Save",v->{p.edit().putString("business_name",name.getText().toString()).putString("business_type",type.getText().toString())
                .putString("business_phone",phone.getText().toString()).putString("business_website",website.getText().toString())
                .putString("booking_url",booking.getText().toString()).putString("business_address",address.getText().toString())
                .putString("business_email",email.getText().toString()).putString("business_signature",signature.getText().toString())
                .putString("default_staff_message",staff.getText().toString()).apply();toast("Business profile saved");});
    }

    private void buildBotSettings() {
        check("Prevent duplicate replies",true);
        spinner("Maximum automatic replies per conversation",new String[]{"3","5","10","20","Unlimited"},1);
        spinner("Do not reply again within",new String[]{"1 minute","5 minutes","10 minutes","30 minutes","Custom"},2);
        check("Stop Bot when staff takes over",true);
        check("Ignore OTP / verification messages",true);
        button("Automation Schedule",v->open("automation_schedule"));
        button("Blocked numbers",v->toast("Blocked-number list"));
        check("STOP keyword handling",true);
        spinner("Reply delay",new String[]{"No delay","15 seconds","30 seconds","1 minute","Custom"},2);
        check("Auto-reply notification",true);
        check("Failed-message notification",true);
        buttonPrimary("Save",v->toast("Bot safety settings saved"));
    }

    private void buildPermissions() {
        TextView status=text(permissionSummary(),15,false); status.setPadding(dp(14),dp(14),dp(14),dp(14)); status.setBackgroundColor(Color.WHITE); content.addView(status);
        buttonPrimary(hasAllPermissions()?"Open Android App Permissions":"Review & Grant Permissions",v->{
            if(hasAllPermissions()) startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName())));
            else showDisclosure();
        });
        button("Refresh diagnostics",v->render());
    }

    private void showDisclosure() {
        new AlertDialog.Builder(this)
                .setTitle("Business SMS automation permissions")
                .setMessage("ReplyDesk is a user-configured business SMS tool. You control automation, message content, response rules, timing and SIM. Phone/Call log access is used for missed-call triggers; Send SMS sends your configured response; Receive SMS detects incoming-message triggers.")
                .setNegativeButton("Not now",null)
                .setPositiveButton("Continue",(d,w)->requestRequiredPermissions()).show();
    }

    private void requestRequiredPermissions() {
        List<String> missing=new ArrayList<>();
        for(String perm:new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CALL_LOG,Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS})
            if(checkSelfPermission(perm)!=PackageManager.PERMISSION_GRANTED) missing.add(perm);
        if(!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]),PERMISSION_REQUEST);
    }

    private boolean hasAllPermissions() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED;
    }

    private String permissionSummary() {
        return (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED?"✓":"✕")+" Phone\n"
                +(checkSelfPermission(Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED?"✓":"✕")+" Call log\n"
                +(checkSelfPermission(Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED?"✓":"✕")+" Send SMS\n"
                +(checkSelfPermission(Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED?"✓":"✕")+" Receive SMS";
    }

    private void buildDepartmentRouting() {
        SharedPreferences p=prefs();

        TextView intro=text("Route customer messages and calls to the right department — even when staff use different phones.",16,true);
        intro.setPadding(0,0,0,dp(10));
        content.addView(intro);

        Switch enabled=toggle("Department Routing",p.getBoolean("routing_enabled",false));
        Spinner mode=spinner("Routing Mode",new String[]{"SMS forwarding only","Call forwarding only","SMS + Call routing"},p.getInt("routing_mode",2));

        EditText department=input("Department Name",p.getString("routing_department","Sales"));
        EditText numbers=largeEdit("External Numbers — one per line",p.getString("routing_numbers",""));
        CheckBox smsForward=check("Forward matched SMS to department numbers",p.getBoolean("routing_sms_forward",true));
        CheckBox staffNotify=check("Notify all department numbers",p.getBoolean("routing_notify_all",true));
        CheckBox includeCustomer=check("Include customer number and original message",p.getBoolean("routing_include_customer",true));
        CheckBox pauseBot=check("Pause Bot when department takes over",p.getBoolean("routing_pause_bot",true));

        spinner("Call Routing Strategy",new String[]{"Ring first number","Ring numbers in order","Ring all available staff","Forward only after missed call","Manual handover"},p.getInt("routing_call_strategy",1));
        EditText fallback=input("Fallback / Overflow Number",p.getString("routing_fallback_number",""));

        button("Add Another Department",v->toast("Additional department routing profile"));
        buttonPrimary("Save Routing",v->{
            p.edit()
                    .putBoolean("routing_enabled",enabled.isChecked())
                    .putInt("routing_mode",mode.getSelectedItemPosition())
                    .putString("routing_department",department.getText().toString().trim())
                    .putString("routing_numbers",numbers.getText().toString().trim())
                    .putBoolean("routing_sms_forward",smsForward.isChecked())
                    .putBoolean("routing_notify_all",staffNotify.isChecked())
                    .putBoolean("routing_include_customer",includeCustomer.isChecked())
                    .putBoolean("routing_pause_bot",pauseBot.isChecked())
                    .putString("routing_fallback_number",fallback.getText().toString().trim())
                    .apply();
            toast("Department routing settings saved");
        });

        TextView note=text("SMS forwarding can be handled by ReplyDesk. Reliable live call routing to several external numbers needs a cloud telephony/business-number service or carrier-supported call forwarding; a normal Android handset cannot universally act as a multi-number PBX by itself.",13,false);
        note.setPadding(0,dp(12),0,0);
        content.addView(note);
    }

    private void buildHelp() {
        buildSimpleInfo("Help & Support","Help articles, setup guidance, diagnostics and support options.");
        button("Suggest a Feature / Improve ReplyDesk",v->open("feature_request"));
    }

    private void buildFeatureRequest() {
        SharedPreferences p=prefs();

        TextView intro=text("Tell us what would make ReplyDesk work better for your business.",16,true);
        intro.setPadding(0,0,0,dp(8));
        content.addView(intro);

        TextView body=text("Different businesses work in different ways. Tell us what is difficult, what function you need, and how you would like it to work. Your request can help shape future ReplyDesk features.",14,false);
        body.setPadding(0,0,0,dp(14));
        content.addView(body);

        EditText businessName=input("Business Name",p.getString("business_name",""));
        EditText businessType=input("Business Type",p.getString("business_type",""));
        EditText problem=largeEdit("What is difficult or time-consuming in your business?",p.getString("feature_problem",""));
        EditText feature=largeEdit("What feature would make your business easier?",p.getString("feature_request",""));
        EditText workflow=largeEdit("How would you like it to work?",p.getString("feature_workflow",""));
        Spinner importance=spinner("How important is this feature?",new String[]{"Nice to have","Useful","Important","Very important","Critical for our workflow"},p.getInt("feature_importance",2));
        CheckBox contact=check("You may contact me about this request",p.getBoolean("feature_contact_ok",true));
        EditText email=input("Contact Email",p.getString("business_email",""));
        EditText phone=input("Contact Phone",p.getString("business_phone",""));

        button("Save Draft",v->{
            saveFeatureDraft(p,businessName,businessType,problem,feature,workflow,importance,contact,email,phone);
            toast("Feature request draft saved");
        });

        buttonPrimary("SEND FEATURE REQUEST",v->{
            saveFeatureDraft(p,businessName,businessType,problem,feature,workflow,importance,contact,email,phone);
            String message="ReplyDesk Feature Request\n\n"
                    +"Business: "+businessName.getText().toString().trim()+"\n"
                    +"Business type: "+businessType.getText().toString().trim()+"\n"
                    +"Importance: "+String.valueOf(importance.getSelectedItem())+"\n\n"
                    +"What is difficult now?\n"+problem.getText().toString().trim()+"\n\n"
                    +"Requested feature\n"+feature.getText().toString().trim()+"\n\n"
                    +"How it should work\n"+workflow.getText().toString().trim()+"\n\n"
                    +"Contact permission: "+(contact.isChecked()?"Yes":"No")+"\n"
                    +"Email: "+email.getText().toString().trim()+"\n"
                    +"Phone: "+phone.getText().toString().trim();

            Intent share=new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT,"ReplyDesk Feature Request");
            share.putExtra(Intent.EXTRA_TEXT,message);
            startActivity(Intent.createChooser(share,"Send feature request"));
        });

        TextView note=text("Before public release, this page can be connected directly to ReplyDesk support so requests arrive without the customer needing to copy technical information.",13,false);
        note.setPadding(0,dp(12),0,0);
        content.addView(note);
    }

    private void saveFeatureDraft(SharedPreferences p, EditText businessName, EditText businessType,
                                  EditText problem, EditText feature, EditText workflow, Spinner importance,
                                  CheckBox contact, EditText email, EditText phone) {
        p.edit()
                .putString("feature_business_name",businessName.getText().toString().trim())
                .putString("feature_business_type",businessType.getText().toString().trim())
                .putString("feature_problem",problem.getText().toString().trim())
                .putString("feature_request",feature.getText().toString().trim())
                .putString("feature_workflow",workflow.getText().toString().trim())
                .putInt("feature_importance",importance.getSelectedItemPosition())
                .putBoolean("feature_contact_ok",contact.isChecked())
                .putString("feature_contact_email",email.getText().toString().trim())
                .putString("feature_contact_phone",phone.getText().toString().trim())
                .apply();
    }

    private void buildSimpleInfo(String heading,String body) {
        TextView t=text(body,16,false); t.setPadding(dp(14),dp(14),dp(14),dp(14)); t.setBackgroundColor(Color.WHITE); content.addView(t);
    }

    private EditText input(String label,String value) {
        content.addView(label(label));
        EditText e=new EditText(this); e.setText(value); e.setTextSize(16); e.setSingleLine(true); e.setPadding(dp(12),dp(10),dp(12),dp(10)); e.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.setMargins(0,dp(5),0,dp(10)); content.addView(e,lp); return e;
    }

    private EditText largeEdit(String label,String value) {
        content.addView(label(label));
        EditText e=new EditText(this); e.setText(value); e.setTextSize(16); e.setGravity(Gravity.TOP|Gravity.START); e.setPadding(dp(12),dp(12),dp(12),dp(12)); e.setBackgroundColor(Color.WHITE); e.setMinLines(5);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(170)); lp.setMargins(0,dp(5),0,dp(12)); content.addView(e,lp); return e;
    }

    private Spinner spinner(String label,String[] values,int selected) {
        content.addView(label(label));
        Spinner s=new Spinner(this); s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values)); s.setSelection(Math.max(0,Math.min(selected,values.length-1)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.setMargins(0,dp(5),0,dp(10)); content.addView(s,lp); return s;
    }

    private Switch toggle(String label,boolean checked) {
        Switch s=new Switch(this); s.setText(label); s.setTextSize(18); s.setChecked(checked); s.setPadding(dp(12),dp(10),dp(12),dp(10)); s.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(60)); lp.setMargins(0,0,0,dp(10)); content.addView(s,lp); return s;
    }

    private CheckBox check(String label,boolean checked) {
        CheckBox c=new CheckBox(this); c.setText(label); c.setChecked(checked); c.setTextSize(15); c.setPadding(dp(8),dp(4),dp(8),dp(4)); content.addView(c,new LinearLayout.LayoutParams(-1,dp(50))); return c;
    }

    private TextView label(String value) {
        TextView t=text(value,15,true); t.setPadding(0,dp(8),0,0); return t;
    }

    private void horizontalChips(String[] labels) {
        HorizontalScrollView hsv=new HorizontalScrollView(this); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(String s:labels){Button b=new Button(this);b.setText(s);b.setAllCaps(false);row.addView(b,new LinearLayout.LayoutParams(-2,dp(48)));}
        hsv.addView(row); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.setMargins(0,dp(6),0,dp(8)); content.addView(hsv,lp);
    }

    private void button(String name,android.view.View.OnClickListener l) {
        Button b=new Button(this); b.setText(name+"   ›"); b.setAllCaps(false); b.setTextSize(16); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); b.setOnClickListener(l);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.setMargins(0,dp(6),0,0); content.addView(b,lp);
    }

    private void buttonPrimary(String name,android.view.View.OnClickListener l) {
        Button b=new Button(this); b.setText(name); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.rgb(18,103,229)); b.setOnClickListener(l);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.setMargins(0,dp(12),0,dp(4)); content.addView(b,lp);
    }

    private LinearLayout bottomNav() {
        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setBackgroundColor(Color.WHITE);
        String[][] items={{"HOME","home"},{"CONVERSATIONS","conversations"},{"AUTO BOT","sms_bot"},{"ACTIVITY","activity"},{"SETTINGS","bot_settings"}};
        for(String[] i:items){Button b=new Button(this);b.setText(i[0]);b.setAllCaps(false);b.setTextSize(11);b.setOnClickListener(v->{if("home".equals(i[1])){Intent in=new Intent(this,MainActivity.class);in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(in);}else open(i[1]);});nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1f));}
        return nav;
    }

    private void open(String p) { Intent i=new Intent(this,SectionActivity.class); i.putExtra("page",p); startActivity(i); }

    private String titleFor(String p) {
        switch(p){
            case "missed_call":return "Missed Call Auto Reply"; case "sms_bot":return "SMS Auto Bot"; case "library":return "Auto Reply Library";
            case "staff":return "Staff Assistance"; case "business_hours":return "Business Hours"; case "after_hours":return "After-Hours Reply";
            case "conversations":return "Conversations"; case "activity":return "Activity & Call Logs"; case "test_bot":return "Test Bot";
            case "templates":return "Message Templates"; case "contacts":return "Contacts / Customers"; case "scheduled":return "Scheduled Messages";
            case "business_profile":return "Business Profile"; case "bot_settings":return "Bot Settings"; case "permissions":return "Permissions & Diagnostics";
            case "account":return "Subscription / Account"; case "help":return "Help & Support"; case "feature_request":return "Suggest a Feature"; case "department_routing":return "Department Routing"; case "departments":return "Departments"; case "unmatched":return "Unmatched Messages"; case "automation_schedule":return "Automation Schedule";
        } return "ReplyDesk";
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==2002 && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            Uri uri=data.getData();
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            prefs().edit().putString("business_logo_uri",uri.toString()).apply();
            toast("Business logo saved");
            render();
        }
    }

    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private TextView text(String v,int sp,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextSize(sp);t.setTextColor(Color.rgb(11,31,58));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void showMessage(String title,String msg){new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("OK",null).show();}
    private int delayIndex(int v){return v==0?0:v==15?1:v==60?3:2;} private int delayValue(int i){return new int[]{0,15,30,60}[Math.max(0,Math.min(i,3))];}
    private int repeatIndex(int v){int[] a={0,15,60,360,720,1440};for(int i=0;i<a.length;i++)if(a[i]==v)return i;return 0;} private int repeatValue(int i){int[] a={0,15,60,360,720,1440};return a[Math.max(0,Math.min(i,a.length-1))];}
}
