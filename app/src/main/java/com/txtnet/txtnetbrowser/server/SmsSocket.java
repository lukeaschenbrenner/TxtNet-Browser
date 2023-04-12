package com.txtnet.txtnetbrowser.server;

import static com.txtnet.txtnetbrowser.basest.Base10Conversions.SYMBOL_TABLE;
import static com.txtnet.txtnetbrowser.basest.Base10Conversions.v2r;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.i18n.phonenumbers.Phonenumber;
import com.txtnet.brotli4droid.Brotli4jLoader;
import com.txtnet.brotli4droid.decoder.BrotliInputStream;
import com.txtnet.brotli4droid.encoder.BrotliOutputStream;
import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.basest.Base10Conversions;
import com.txtnet.txtnetbrowser.basest.Decode;
import com.txtnet.txtnetbrowser.basest.Encode;
import com.txtnet.txtnetbrowser.receiver.SmsDeliveredReceiver;
import com.txtnet.txtnetbrowser.receiver.SmsSentReceiver;
import com.txtnet.txtnetbrowser.util.Index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmsSocket {
    public ArrayList<String> inputRequestBuffer;
    int finalBufferLength = 9999;
    public StringBuilder decodedUrl = new StringBuilder();
    public ArrayList<String> outputMessagesBuffer;
    private Phonenumber.PhoneNumber phoneNumber;
    private AtomicBoolean shouldSend = new AtomicBoolean(true);
    private TxtNetServerService service = null;
    private int MAX_SMS_PER_REQUEST;
    public SmsSocket(Phonenumber.PhoneNumber number, TxtNetServerService service, int maxSmsPerRequest) {
        this();
        this.phoneNumber = number;
        this.service = service;
        MAX_SMS_PER_REQUEST = maxSmsPerRequest;
    }
    public SmsSocket(){
        inputRequestBuffer = new ArrayList<>();
        outputMessagesBuffer = new ArrayList<>();
    }

    public void sendHTML(String html){

        ByteArrayOutputStream brotliOutput = new ByteArrayOutputStream();
        Brotli4jLoader.ensureAvailability();
        //Log.i(TAG, "Brotli4J is available? " + (Brotli4jLoader.isAvailable() ? "true" : "false"));
        BufferedWriter brotliWriter = null;
        try {
            brotliWriter = new BufferedWriter(new OutputStreamWriter(new BrotliOutputStream(brotliOutput)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader bufReader = new BufferedReader(new StringReader(html));
        String line=null;
        assert brotliWriter != null;

        try{
            while((line=bufReader.readLine()) != null) {
                brotliWriter.write(line);
            }
            brotliWriter.flush();
            brotliWriter.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        Encode smsEncoder = new Encode();
        byte[] htmlBytes = brotliOutput.toByteArray();


//        //******* TEST ********
//        BufferedReader brot2 = null;
//        try {
//            brot2 = new BufferedReader(new InputStreamReader(new BrotliInputStream(new ByteArrayInputStream(htmlBytes))));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String linee = "";
//        try{
//            while((line=brot2.readLine()) != null) {
//                linee += line;
//            }
//            brot2.close();
//            Log.i("LINEE", "LINEE: " + linee);
//        }catch(IOException ioe){
//            ioe.printStackTrace();
//        }
//        // ************************


        //Log.i(TAG, "HTMLBYTES: " + Arrays.toString(htmlBytes));
        int[] htmlBytesAsIntArray = new int[htmlBytes.length];
        for(int i = 0; i < htmlBytes.length; i++){
            htmlBytesAsIntArray[i] = (htmlBytes[i] & 0xFF);
        }
        //Log.i(TAG, Arrays.toString(htmlBytesAsIntArray));

        int[] encodedSms = smsEncoder.encode_raw(256, 114, 134, 158, htmlBytesAsIntArray);
        StringBuilder smsEncodedOutputBuilder = new StringBuilder();
        //Log.i(TAG, Arrays.toString(encodedSms));
        for(int value : encodedSms){
            smsEncodedOutputBuilder.append(SYMBOL_TABLE[value]);
        }
        String smsEncodedOutput = smsEncodedOutputBuilder.toString();
        final int NUM_CHARS_PER_SMS = 158;
        ArrayList<String> smsQueue = new ArrayList<>();
        for(int i = 0; i < smsEncodedOutput.length(); i += NUM_CHARS_PER_SMS){
            smsQueue.add(smsEncodedOutput.substring(i, i + NUM_CHARS_PER_SMS));
        }
        int[] indices = new int[smsQueue.size()];
        for(int j = 0; j < indices.length; j++){
            indices[j] = j;
        }
        String[] indexCharacters = v2r(indices);
        //Log.i(TAG, smsQueue.size() + "<smsqueue indexcharacters>" + indexCharacters.length);
        for(int k = 0; k < indexCharacters.length; k++){
            //if(k == smsQueue.size()-1){
            //    smsQueue.set(k, SYMBOL_TABLE[SYMBOL_TABLE.length-1] + SYMBOL_TABLE[SYMBOL_TABLE.length-1] + smsQueue.get(k));
            //}else{
                smsQueue.set(k, indexCharacters[k] + smsQueue.get(k));
            //}
        }

        int howManyTextsToExpect = (smsQueue.size());

        SmsManager sms = SmsManager.getDefault();

        String outputNumber = "";
        if(phoneNumber.hasCountryCode()){
            outputNumber += phoneNumber.getCountryCode();
        }
        outputNumber += phoneNumber.getNationalNumber();

        if(howManyTextsToExpect > MAX_SMS_PER_REQUEST){
            Log.w(TAG, "Request made with SMS count > MAX_SMS_PER_REQUEST");
            sms.sendTextMessage(outputNumber, null, service.getString(R.string.request_sms_outgoing_exceeded_error), null, null);
            return;
        }


        sms.sendTextMessage(outputNumber, null, howManyTextsToExpect + " Process starting", null, null);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int currentMessageID = 0;
        while(shouldSend.get() && currentMessageID < smsQueue.size()){
            sms.sendTextMessage(outputNumber, null, smsQueue.get(currentMessageID), null, null);
            currentMessageID++;
        }
    }
    public void stopSend(){
        shouldSend.set(false);
    }




    private final String TAG = "SmsSocketMessage";
    private int howManyAdded = 0;
    //public String[] textBuffer = null;
    public String url;
    //Context context;


    public void addPart(String message) throws Exception {
        if(message == null){
            Log.e(TAG, "ERR: Message is null.");
            return;
        }

        if(message.matches("([0-9]){2}(?s).*")){ // message is the first message
            // || (message.startsWith("àà") && (!inputRequestBuffer.isEmpty()) && inputRequestBuffer.size() > 0)){
            inputRequestBuffer.clear();
            finalBufferLength = Integer.parseInt(message.substring(0, 2));
            //reassembled = new String[inputRequestBuffer.size()];
        }
       // if(reassembled == null){
       //     reassembled = new String[inputRequestBuffer.size()];
       // }


        inputRequestBuffer.add(message);

        if(inputRequestBuffer.size() >= finalBufferLength){ // || finalBufferLength == 1
            finalBufferLength = 9999;
            String[] reassembled = new String[inputRequestBuffer.size()];

            for(String str : inputRequestBuffer){
                int textOrder = -1;

                if(message.startsWith(SYMBOL_TABLE[SYMBOL_TABLE.length-1] + SYMBOL_TABLE[SYMBOL_TABLE.length-1])){
                    textOrder = finalBufferLength-1; // àà means this is the final message of a multi-part message
                    //inputRequestBuffer.size()-1;
                }
                else if(message.matches("([0-9]){2}(?s).*")){// PROBLEM: we should know ahead of time how big our buffer should eventually be for the link we received. similar to the code for "Process Starting". This wastes space adding an entire text for every request though.
                    // We could append to beginning of every message number of texts eg. 12@@... and then take the numDigits and create a custom encoding for that one specific text.
                    // However, that's very difficult to accomplish without knowing the number of digits we will need before encoding the first text, to predict how many bytes can fit into that text.
                    // Instead, we simply append the number instead of @@, which allows us to have 100 SMS messages in one request before encoding may possibly use two digits at the beginning. Considering the actual max is 12996, this is a far cry from the potential indexing allowed by base 114 2-character index.
                    // Not a problem right now for URLs, but will become an issue later when trying to transmit binary data
                    textOrder = 0;


                }else{
                    textOrder = Base10Conversions.r2v(str.substring(0, 2));
                }
                String text = str.substring(2);
                reassembled[textOrder] = text;
            }
            inputRequestBuffer.clear();

            //(int)(Math.log10(n)+1); << if we varied the number of input SMS digits, this would be the way to determine the number of digits of a number
            StringBuilder stringReassembled = new StringBuilder();
            for(int i = 0; i < reassembled.length; i++){
                String part = reassembled[i];
                stringReassembled.append(part);
            }
            String str_reassembled = stringReassembled.toString();
            int[] nums = new int[str_reassembled.length()];
            char[] chr = str_reassembled.toCharArray();
            for(int i = 0; i < str_reassembled.length(); i++){
                char myChr = chr[i];
                nums[i] = Index.findIndex(SYMBOL_TABLE, String.valueOf(myChr)); //TODO: HOW DOES THIS WORK?
                //nums[myChr] = (Arrays.asList(SYMBOL_TABLE).indexOf(String.valueOf(chr)));
            }
            int garbageData = 0;

            for(int p = nums.length-1; nums[p] == 114; p--){
                garbageData++;
            }
            int[] urlWithGarbage = null;


            try{
                    urlWithGarbage = new Encode().encode_raw(114, 256, 158, 134, nums); //actually decoding, not encoding
            }catch(Exception e){
                Log.e(TAG, "Error: Decoding user message failed.");
                inputRequestBuffer.clear();
                TxtNetServerService.smsDataBase.remove(phoneNumber);
            }
            assert urlWithGarbage != null;
            int[] urlEncoded = Arrays.copyOfRange(urlWithGarbage, 0, urlWithGarbage.length - garbageData);
            byte[] urlBytes = new byte[urlEncoded.length];
            for(int i = 0; i < urlEncoded.length; i++){
                urlBytes[i] = (byte)(urlEncoded[i]);
            }

            String decodedUrlString = new String(urlBytes, StandardCharsets.UTF_8);
            decodedUrl.append(decodedUrlString);
            //if(message.startsWith("àà")){ //(message.startsWith("àà") && (!inputRequestBuffer.isEmpty())
            // we don't know that the messages will guaranteed come in in order!
            String finalDecString = decodedUrl.toString();
            decodedUrl = new StringBuilder();


            Message msg = service.serviceHandler.obtainMessage();
            msg.arg1 = 12345; // custom startID
            Bundle bundle = new Bundle(2);
            //  bundle.putString("linkToVisit", intent.getStringExtra("linkToVisit"));
            bundle.putString("linkToVisit", finalDecString);
            bundle.putSerializable("phoneNumber", phoneNumber);
            msg.setData(bundle);
            service.serviceHandler.sendMessage(msg);

        }







        // }




//////////////////////////////

        /*


//////////////////////

if user data was brotli compressed, use this. but user requests are not brotli compressed, only encoded!

            try{
                InputStream targetStream = new ByteArrayInputStream(decodedbytes);

                BufferedReader rd = (new BufferedReader(new InputStreamReader(new BrotliInputStream(targetStream))));
                //ArrayList<Integer> intsArray = new ArrayList<>();
                StringBuilder result = new StringBuilder();
                String line2;

                while((line2 = rd.readLine()) != null){
                    // Log.d("result", String.valueOf(line2));
                    result.append(line2);
                }


                //Don't need the next 2 lines of code if we assume that brotli is able to give me a UTF-8 HTML string
                //byte[] almostLast = integersToBytes(convertIntegers(intsArray));
                //String s = new String(almostLast, StandardCharsets.UTF_8);

                //Finally, load the data with result.toString()
                //   Log.d("APPARENT URL: ", url);
                MainBrowserScreen.webView.loadDataWithBaseURL(url, result.toString(), "text/html", "utf-8", null);

            } catch (Exception e) {
                e.printStackTrace();
            }
*/
        }
    }
