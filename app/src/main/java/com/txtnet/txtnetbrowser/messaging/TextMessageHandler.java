package com.txtnet.txtnetbrowser.messaging;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import com.txtnet.txtnetbrowser.basest.Base10Conversions;

import androidx.appcompat.app.AlertDialog;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.provider.Telephony;

import com.txtnet.txtnetbrowser.MainBrowserScreen;


public class TextMessageHandler {
    private static TextMessageHandler single_instance = null;
    private TextMessageHandler(){}
    public static TextMessageHandler getInstance(){
        if(single_instance == null){
            single_instance = new TextMessageHandler();
        }
        return single_instance;
    }

    private final String TAG = "TextMessageHandler";
    public static final String PHONE_NUMBER = "8884842216"; //twilio
    //public static final String PHONE_NUMBER = "0014158397780"; //plivo



    /**
     *
     * @param body Body of the message a person is trying to send.
     * @param to Who the person is sending the text message to. Must be 10 digits.
     */
    public void sendTextMessage(String body){

        if(body == null || PHONE_NUMBER == null){
            Log.e(TAG, "***** ERROR EITHER BODY OR TO IS NULL!");
            return;
        }
        PendingIntent sentIntent = null, deliveryIntent = null;
        //TODO: Fix the above and make them useful eg. loading animation!

        SmsManager smsManager = SmsManager.getDefault();
        if(!body.contains("http") && !body.contains("."))
        {
            MainBrowserScreen.webView.loadDataWithBaseURL(null, "<br><br><h2>Invalid URL, please try again.</h2>", "text/html", "utf-8", null);
        }//TODO: make url validator system
        if(body.length() > 160) {
            //Because the body of the message can be larger than tha 140 bit limit presented, the message must be split up.
            ArrayList<String> parts = smsManager.divideMessage(body);
            smsManager.sendMultipartTextMessage(PHONE_NUMBER, null, parts, null, null);
        }
        else{
            smsManager.sendTextMessage(PHONE_NUMBER,null, body,sentIntent,deliveryIntent);
        }

        //TODO: loading page?
        //MainBrowserScreen.webView.loadDataWithBaseURL(null, "<h1>Loading...</h1>", "text/html", "utf-8", null);

    }

    public static class SMSReceiver extends BroadcastReceiver {
        private static TextMessage txtmsg;

        private final String TAG = "SMSReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            ///**Not decoding entire concatenated string at once!
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Bundle extras = intent.getExtras();
                if(extras == null) {
                    Log.e("error", "Message received but empty!");
                    return;
                }
                Object[] pdus = (Object[]) extras.get("pdus");

                String textOrder = "";
      /*          for (Object pdu : pdus) {
                    //need to figure out first pdu so you can substring
                    SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                    String origin = msg.getOriginatingAddress();
                    String body = msg.getMessageBody();

                    if(PhoneNumberUtils.compare(origin, PHONE_NUMBER)) { //remove leading zeroes?
                        if(body.contains("Process starting")){
                            txtmsg = new TextMessage(Integer.parseInt(body.substring(0, body.indexOf(" "))));
                        }else{
                            for(int single : Base10Conversions.r2v(body.substring(0, 2))){
                                textOrder += Integer.toString(single);
                            }
                }
                */
                String Message = "";
                SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < messages.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]); //Returns one message, in array because multipart message due to sms max char
                    Message += messages[i].getMessageBody(); // Using +=, because need to add multipart from before also
                }
                String origin = messages[0].getOriginatingAddress();

                if(PhoneNumberUtils.compare(origin, PHONE_NUMBER)) { //remove leading zeroes?
                    if (Message.contains("Process starting")) {
                        Log.d("amt msgs", Message.substring(0, Message.indexOf(" ")));
                        txtmsg = new TextMessage(Integer.parseInt(Message.substring(0, Message.indexOf(" "))), context);
                    } else {
                        // for (int single : Base10Conversions.r2v(Message.substring(0, 2))) {
                        //     Log.d("order number: ", String.valueOf(single));
                        //
                        //     textOrder += Integer.toString(single);
                        //}

                        try {
                            int messageNumber = Base10Conversions.r2v(Message.substring(0, 2));
                            String messageBody = Message.substring(2);
                            Log.d("msg body: ", Message.substring(2));
                            // txtmsg.addPart(Integer.parseInt(textOrder), Message.substring(2));
                            txtmsg.addPart(messageNumber, messageBody);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    //    MainBrowserScreen.webView.loadDataWithBaseURL(null, body, "text/html", "utf-8", null);
                }

            }
        }
    }

///////////////////////////////////////////////////////
    //old merged methods:

    //static FullTextMessage fullTextMessage;

    public void textToTwilio(String whatToSend) throws Exception{
        ArrayList<String> texts = new ArrayList<String>();
        String phone_Num = PHONE_NUMBER;
        String send_msg = whatToSend;
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phone_Num, null, send_msg, null, null);

    }

    public void sendStringToTwilio(String whatToSend){
        String send_msg = whatToSend;
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(PHONE_NUMBER, null, send_msg, null, null);
    }
    private void generateAlertDialog(String message, Context context){
        new AlertDialog.Builder(context)
                .setTitle("Error!")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void saveFile(String name, String content, Context ctx) {
        String filename = name;
        String string = content;
        FileOutputStream outputStream;
        try {
            outputStream = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
