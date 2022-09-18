package com.txtnet.txtnetbrowser.messaging;


import static com.txtnet.txtnetbrowser.basest.Base10Conversions.SYMBOL_TABLE;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.basest.Base10Conversions;

import androidx.appcompat.app.AlertDialog;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.provider.Telephony;
import android.widget.Toast;

import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.basest.Encode;
import com.txtnet.txtnetbrowser.receiver.SmsDeliveredReceiver;
import com.txtnet.txtnetbrowser.receiver.SmsSentReceiver;


public class TextMessageHandler {
    private static TextMessageHandler single_instance = null;
    public static String PHONE_NUMBER = null;
    private TextMessageHandler(){
        PHONE_NUMBER = MainBrowserScreen.preferences.getString(MainBrowserScreen.mContext.getResources().getString(R.string.phone_number), MainBrowserScreen.mContext.getResources().getString(R.string.default_phone));
    }
    public static TextMessageHandler getInstance(){
        if(single_instance == null){
            single_instance = new TextMessageHandler();
        }
        return single_instance;
    }

    private final String TAG = "TextMessageHandler";
    //public static String PHONE_NUMBER = null; //twilio
    //public static final String PHONE_NUMBER = "0014158397780"; //plivo



    /**
     *
     * @param body Body of the message a person is trying to send.
     */
    public void sendTextMessage(String body){
        PHONE_NUMBER = MainBrowserScreen.preferences.getString(MainBrowserScreen.mContext.getResources().getString(R.string.phone_number), MainBrowserScreen.mContext.getResources().getString(R.string.default_phone));

        if(body == null || PHONE_NUMBER == null){
            Log.e(TAG, "***** ERROR EITHER BODY OR TO IS NULL!");
            return;
        }
       // PendingIntent sentIntent = null, deliveryIntent = null;


        SmsManager smsManager = SmsManager.getDefault();
     //   if(!body.contains("http") && !body.contains(".") && !body.contains("STOP") && !body.contains("unstop") && !body.contains("Website Cancel"))
     //   {
     //       MainBrowserScreen.webView.loadDataWithBaseURL(null, "<br><br><h2>Invalid URL, please try again.</h2>", "text/html", "utf-8", null);
     //   }//TODO: make url validator system
     //   else{


            byte[] byteArray = body.getBytes();
            int[] bytesInt = new int[byteArray.length];
            for(int i = 0; i < bytesInt.length; i++){
                bytesInt[i] = byteArray[i] & 0xFF; //bitwise AND operator, converts signed byte to unsigned
            }
            Encode encoder = new Encode();
            int[] encodedInts = encoder.encode_raw(256, 114, 134, 158, bytesInt);
            String output = "";

            for(int i = 0; i < encodedInts.length; i++){
                output += SYMBOL_TABLE[encodedInts[i]];
            }


            final int NUM_CHARS_PER_SMS = 158;
            ArrayList<String> smsQueue =  new ArrayList<>();

            int j;
            for(j = 0; j < output.length(); j += NUM_CHARS_PER_SMS){
                smsQueue.add(output.substring(j, j+NUM_CHARS_PER_SMS));
            }
            String[] smsFinalQueue = new String[smsQueue.size()];
            for(j = 0; j < smsQueue.size(); j++){
                int[] jArr = {j};
                StringBuffer sb = new StringBuffer();
                String[] indices = Base10Conversions.v2r(jArr);

                sb.append(indices[0]);
                String str = sb.toString();

                if(j == smsQueue.size()-1){
                    smsQueue.set(j, SYMBOL_TABLE[SYMBOL_TABLE.length-1] + SYMBOL_TABLE[SYMBOL_TABLE.length-1] + smsQueue.get(j));
                }else{
                    smsQueue.set(j, str + smsQueue.get(j));
                }
            }


            int howManyTextsToExpect = (int) Math.ceil(smsQueue.size());


            ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
            PendingIntent sentPI = PendingIntent.getBroadcast(MainBrowserScreen.mContext, 0,
                    new Intent(MainBrowserScreen.mContext, SmsSentReceiver.class), PendingIntent.FLAG_IMMUTABLE);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(MainBrowserScreen.mContext, 0,
                    new Intent(MainBrowserScreen.mContext, SmsDeliveredReceiver.class), PendingIntent.FLAG_IMMUTABLE);
            try {
                SmsManager sms = SmsManager.getDefault();
                for (int i = 0; i < smsQueue.size(); i++) {
                    sentPendingIntents.add(i, sentPI);

                    deliveredPendingIntents.add(i, deliveredPI);
                }

                for(int i = 0; i < smsQueue.size(); i++){
                    Handler handler = new Handler();
                    int finalI = i;
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            sms.sendTextMessage(PHONE_NUMBER, null, smsQueue.get(finalI), sentPendingIntents.get(finalI), deliveredPendingIntents.get(finalI));
                        }
                    }, 1000L * i);

                }


            } catch (Exception e) {

                e.printStackTrace();
                Toast.makeText(MainBrowserScreen.mContext, "SMS sending failed...",
                        Toast.LENGTH_SHORT).show();
            }
     //   }

        // else if(body.length() > 160) {
       //     //Because the body of the message can be larger than tha 140 bit limit presented, the message must be split up.
       //     ArrayList<String> parts = smsManager.divideMessage(body);
       //     smsManager.sendMultipartTextMessage(PHONE_NUMBER, null, parts, null, null);
       // }
       // else{
       //     smsManager.sendTextMessage(PHONE_NUMBER,null, body,sentIntent,deliveryIntent);
       // }

        //TODO: loading page?
        MainBrowserScreen.webView.loadDataWithBaseURL(null, "<br><br><br><center><h1>Loading...</h1></center>", "text/html", "utf-8", null);

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
                           // Log.d("msg body: ", Message.substring(2));
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
