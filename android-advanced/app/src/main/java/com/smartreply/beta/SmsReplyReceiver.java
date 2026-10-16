package com.smartreply.beta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.Locale;

public class SmsReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;
        Diagnostics.record(context,-1,"SMS_RECEIVED","Android SMS broadcast received");
        // Still persist STOP/HUMAN when sending is revoked. ReplySender checks SEND_SMS.

        Bundle bundle = intent.getExtras();
        int subscriptionId = SimRouter.smsSubscription(context, bundle);
        if (!BusinessProfiles.exists(context, subscriptionId) || !SimRouter.active(context, subscriptionId)) {
            ReplySender.issue(context, "SMS skipped: receiving SIM could not be identified, is inactive, or has no profile.");
            return;
        }
        try {
            Object raw = bundle.get("pdus");
            if (!(raw instanceof Object[]) || ((Object[]) raw).length == 0) {
                skip(context,subscriptionId,"","SMS has no PDU data"); return;
            }
            SmsMessage[] messages=android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (messages == null || messages.length != ((Object[]) raw).length) {
                skip(context,subscriptionId,"","Incomplete incoming SMS"); return;
            }
            handleParts(context,subscriptionId,messages);
        } catch(RuntimeException e) {
            skip(context,subscriptionId,"","Unable to decode incoming SMS: "+e.getClass().getSimpleName());
        }
    }

    void handleParts(Context context,int subscriptionId,SmsMessage[] messages){
        SharedPreferences prefs=BusinessProfiles.prefs(context,subscriptionId);
        String sender=null, canonical=null;
        StringBuilder body=new StringBuilder();
        long sourceTime=Long.MAX_VALUE;
        if(messages==null || messages.length==0)return;
        for(SmsMessage sms:messages){
            if(sms==null || sms.getDisplayMessageBody()==null){skip(context,subscriptionId,"","Incomplete incoming SMS");return;}
            String address=sms.getDisplayOriginatingAddress();
            String normal=RecipientRules.normal(prefs,address);
            if(normal.isEmpty() || (canonical!=null && !canonical.equals(normal))){
                skip(context,subscriptionId,"","Conflicting incoming SMS senders");return;
            }
            if(sender==null){sender=address;canonical=normal;}
            body.append(sms.getDisplayMessageBody());
            long date=sms.getTimestampMillis();
            if(date>0)sourceTime=Math.min(sourceTime,date);
        }
        if(body.length()==0 || sourceTime==Long.MAX_VALUE){skip(context,subscriptionId,sender,"Empty or undated incoming SMS");return;}
        handleMessage(context,subscriptionId,sender,body.toString(),sourceTime);
    }

    // Direct entry for existing local simulations. Android broadcasts always supply the source date.
    void handleMessage(Context context, int subscriptionId, String sender, String body) {
        handleMessage(context,subscriptionId,sender,body,System.currentTimeMillis());
    }

    void handleMessage(Context context,int subscriptionId,String sender,String body,long sourceTime){
        synchronized(SmsReplyReceiver.class){
            processMessage(context,subscriptionId,sender,body,sourceTime);
        }
    }
    private void processMessage(Context context,int subscriptionId,String sender,String body,long sourceTime){
        if (!BusinessProfiles.exists(context, subscriptionId) || !SimRouter.active(context, subscriptionId)
                || sender==null || body==null || body.isEmpty()) return;
        SharedPreferences prefs = BusinessProfiles.prefs(context, subscriptionId);
        String normalized=body.trim().toLowerCase(Locale.ROOT);
        long now=System.currentTimeMillis();
        if(!IncomingSmsGuard.claim(prefs,sender,body,sourceTime,now,normalized)){
            skip(context,subscriptionId,sender,"Duplicate, undated, expired SMS or receipt could not be saved");return;
        }
        if(normalized.equals("human"))return;
        boolean command=normalized.equals("stop") || normalized.equals("unsubscribe") || normalized.equals("start");
        if(!command && ConversationPolicy.optedOut(prefs,sender)){skip(context,subscriptionId,sender,"Sender opted out with STOP");return;}
        if(ConversationPolicy.paused(prefs,sender)){skip(context,subscriptionId,sender,"Human takeover active");return;}
        String policy=ReplyPolicy.reason(prefs,"sms");
        if(!"Ready".equals(policy)){skip(context,subscriptionId,sender,policy);return;}
        String filter=RecipientRules.reason(prefs,sender);
        if(!"Ready".equals(filter)){skip(context,subscriptionId,sender,filter);return;}
        String reply;
        boolean fallback=false;
        if(command){
            reply=normalized.equals("start") ? "Automatic SMS replies are active again. How can we help?"
                : "You have been unsubscribed from automatic SMS replies. Text START to enable them again.";
        }else{
            BotReplies.Answer answer=BotReplies.choose(context,prefs,normalized);
            reply=answer.text;fallback=answer.fallback;
        }
        if(reply==null || reply.trim().isEmpty() || KeywordRules.containsRule(reply)){
            skip(context,subscriptionId,sender,"Reply text is empty or contains keyword configuration; nothing sent");return;
        }
        if(!IncomingSmsGuard.reserveReply(prefs,sender,now,fallback,"SMS",30_000L)){
            skip(context,subscriptionId,sender,"Repeat protection, general reply already used, or reservation could not be saved");return;
        }
        ReplySender.send(context,subscriptionId,sender,reply.trim(),"SMS");
    }

    private void skip(Context c,int id,String number,String reason) {
        Diagnostics.record(c,id,"SKIPPED",reason);
        AppCallLogStore.add(c,id,"SMS",number,"Skipped: "+reason);
    }
}
