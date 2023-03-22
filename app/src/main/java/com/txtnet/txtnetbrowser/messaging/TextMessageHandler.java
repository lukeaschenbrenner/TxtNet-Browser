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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.txtnet.txtnetbrowser.BuildConfig;
import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.basest.Base10Conversions;

import androidx.appcompat.app.AlertDialog;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import android.provider.Telephony;
import android.widget.Toast;

import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.basest.Encode;
import com.txtnet.txtnetbrowser.receiver.SmsDeliveredReceiver;
import com.txtnet.txtnetbrowser.receiver.SmsSentReceiver;
import com.txtnet.txtnetbrowser.server.SmsSocket;
import com.txtnet.txtnetbrowser.server.TxtNetServerService;


public class TextMessageHandler {
    private static TextMessageHandler single_instance = null;
    public static String PHONE_NUMBER = null;
    private TextMessageHandler(String phoneNumber){
        PHONE_NUMBER = phoneNumber;
    } //TODO: above line of code is buggy, results in some app crashes. fix!
    public static TextMessageHandler getInstance(String phoneNumber){
        if(single_instance == null){
            single_instance = new TextMessageHandler(phoneNumber);
        }
        return single_instance;
    }
    public static TextMessageHandler getInstance(){
        if(single_instance == null){
            return null;
        }else{
            return single_instance;
        }
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
        else if(body.contains("Website Cancel") || body.contains("STOP") || body.contains("unstop")) {

            SmsManager sms = SmsManager.getDefault();

            sms.sendTextMessage(PHONE_NUMBER, null, body, null, null);

            return;
        }


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
            StringBuilder output = new StringBuilder();

            for (int encodedInt : encodedInts) {
                output.append(SYMBOL_TABLE[encodedInt]);
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
        private TextMessage txtmsg;

        private final String TAG = "SMSReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive called");
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
                StringBuilder Message = new StringBuilder();
                SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < messages.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]); //Returns one message, in array because multipart message due to sms max char
                    Message.append(messages[i].getMessageBody()); // Using +=, because need to add multipart from before also
                 //   if(messages[i].isStatusReportMessage()){
                 //       Log.i(TAG, "message " + i + " was a status message");
                 //   }
                 //   Log.i(TAG, messages[i].getMessageBody());
                }
                String origin = messages[0].getOriginatingAddress();

                if(PhoneNumberUtils.compare(origin, PHONE_NUMBER)) { //remove leading zeroes?
                    if (Message.toString().contains("Process starting")) {
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
                }else if(TxtNetServerService.isRunning){
                    try {
                        Log.i(TAG, "origin number: " + origin);
                        Phonenumber.PhoneNumber incomingPhone = PhoneNumberUtil.getInstance().parse(origin,"");
                        if(Message.toString().startsWith("ping TxtNet")){
                            int theirVersion = Integer.parseInt(Message.substring(Message.indexOf(" v")+2, (Message.indexOf(" ", Message.indexOf("v") + 1))));
                            String theirProtocol = (Message.substring(Message.lastIndexOf(" ")+1).replaceAll("[^a-zA-Z0-9]", "")); //only alphanumeric regex allowed
                            int myVersion = BuildConfig.VERSION_CODE;
                            String body = "TxtNet Server v" + myVersion;
                            if(theirVersion < myVersion){
                                body += "\nYour version is outdated. Download the newest release at https://bit.ly/txtnet-apk";
                            }
                            Log.i(TAG, "Received message " + Message.toString());
                            SmsManager sms = SmsManager.getDefault();
                            String outputNumber = "";
                            if(incomingPhone.hasCountryCode()){
                                outputNumber += incomingPhone.getCountryCode();
                            }
                            outputNumber += incomingPhone.getNationalNumber();

//                            /**--------- Modified code below, delete when done ----------*/
//
//                            byte[] byteArray = "https://www.google.com/search?q=what+does+air+quality+index+measure&sxsrf=AJOqlzXwE5qXE891YLoXY9jnHgblKvFbIw%3A1679462295955&source=hp&ei=l48aZLnJN7arptQPs8So6Ag&iflsig=AK50M_UAAAAAZBqdp-Sp5H_d0433EXiPD4qpUqbvHNOZ&ved=0ahUKEwj58L7M5O79AhW2lYkEHTMiCo0Q4dUDCAo&uact=5&oq=what+does+air+quality+index+measure&gs_lcp=Cgdnd3Mtd2l6EAMyBQgAEIAEMgYIABAWEB4yBggAEBYQHjIGCAAQFhAeMgYIABAWEB4yBggAEBYQHjIFCAAQhgM6BwgjEOoCECc6BAgjECc6BQgAEJECOgsIABCABBCxAxCDAToRCC4QgAQQsQMQgwEQxwEQ0QM6CwguEIAEELEDEIMBOgsILhCxAxCDARDUAjoOCC4QgAQQsQMQxwEQ0QM6BAgAEEM6CggAELEDEIMBEEM6BwgAELEDEEM6CAgAELEDEIMBOggIABCABBCxAzoLCC4QgAQQxwEQ0QM6BggAEAoQQzoFCAAQsQM6DQgAEIAEELEDEIMBEAo6BwgAEIAEEAo6CggAEIAEELEDEAo6CggAEIAEEBQQhwI6BwguEIAEEAo6BwgAEA0QgAQ6CAgAEBYQHhAPUJMpWOKkAmDgpQJoRnAAeACAAXeIAfcvkgEFNTAuMTWYAQCgAQGwAQo&sclient=gws-wiz".getBytes();
//                            int[] bytesInt = new int[byteArray.length];
//                            for(int i = 0; i < bytesInt.length; i++){
//                                bytesInt[i] = byteArray[i] & 0xFF; //bitwise AND operator, converts signed byte to unsigned
//                            }
//                            Encode encoder = new Encode();
//                            int[] encodedInts = encoder.encode_raw(256, 114, 134, 158, bytesInt);
//                            StringBuilder output = new StringBuilder();
//
//                            for (int encodedInt : encodedInts) {
//                                output.append(SYMBOL_TABLE[encodedInt]);
//                            }
//
//
//                            final int NUM_CHARS_PER_SMS = 158;
//                            ArrayList<String> smsQueue =  new ArrayList<>();
//
//                            int j;
//                            for(j = 0; j < output.length(); j += NUM_CHARS_PER_SMS){
//                                smsQueue.add(output.substring(j, j+NUM_CHARS_PER_SMS));
//                            }
//                            String[] smsFinalQueue = new String[smsQueue.size()];
//                            for(j = 0; j < smsQueue.size(); j++){
//                                int[] jArr = {j};
//                                StringBuffer sb = new StringBuffer();
//                                String[] indices = Base10Conversions.v2r(jArr);
//
//                                sb.append(indices[0]);
//                                String str = sb.toString();
//
//                                if(j == smsQueue.size()-1){
//                                    smsQueue.set(j, SYMBOL_TABLE[SYMBOL_TABLE.length-1] + SYMBOL_TABLE[SYMBOL_TABLE.length-1] + smsQueue.get(j));
//                                }else{
//                                    smsQueue.set(j, str + smsQueue.get(j));
//                                }
//                            }
//                            for(String msg : smsQueue){
//                                sms.sendTextMessage(outputNumber, null, msg, null, null);
//
//                            }
//                            /* ----------------------------------*/

                            sms.sendTextMessage(outputNumber, null, body, null, null);

                        }else if(Message.toString().contains("Website Cancel")){
                            if(TxtNetServerService.smsDataBase.containsKey(incomingPhone)){
                                Objects.requireNonNull(TxtNetServerService.smsDataBase.get(incomingPhone)).stopSend();
                            }
                        }
                        else if(TxtNetServerService.smsDataBase.containsKey(incomingPhone)){
                            //if the message ends up being a new request, we need to clear out the previous arraylist inside of the SmsSocket.
                            Objects.requireNonNull(TxtNetServerService.smsDataBase.get(incomingPhone)).addPart(Message.toString());
                        }else{
                            TxtNetServerService.smsDataBase.put(incomingPhone, new SmsSocket(incomingPhone, TxtNetServerService.instance));
                            //TODO: Fix the above code to avoid static object reference(??)
                            Objects.requireNonNull(TxtNetServerService.smsDataBase.get(incomingPhone)).addPart(Message.toString());
                            //attempt to parse it as if it was a user request. if parsing fails, stop.
                        }
                        //REMEMBER TO SEND "Process starting xxx" BEFORE THE ACTUAL CONTENT
                        //TODO: Eventually clear the smsDataBase every so often to avoid increases in memory usage



                    } catch (NumberParseException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                        boolean removeThisBool = true;
                    }

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
