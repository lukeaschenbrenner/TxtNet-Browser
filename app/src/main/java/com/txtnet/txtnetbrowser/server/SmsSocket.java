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
import com.txtnet.brotli4droid.encoder.BrotliOutputStream;
import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.basest.Base10Conversions;
import com.txtnet.txtnetbrowser.basest.Decode;
import com.txtnet.txtnetbrowser.basest.Encode;
import com.txtnet.txtnetbrowser.receiver.SmsDeliveredReceiver;
import com.txtnet.txtnetbrowser.receiver.SmsSentReceiver;
import com.txtnet.txtnetbrowser.util.Index;

import org.brotli.dec.BrotliInputStream;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmsSocket {
    public ArrayList<String> inputRequestBuffer;
    public StringBuilder decodedUrl = new StringBuilder();
    public ArrayList<String> outputMessagesBuffer;
    private Phonenumber.PhoneNumber phoneNumber;
    private AtomicBoolean shouldSend = new AtomicBoolean(true);
    private TxtNetServerService service = null;

    public SmsSocket(Phonenumber.PhoneNumber number, TxtNetServerService service) {
        this();
        this.phoneNumber = number;
        this.service = service;
    }
    public SmsSocket(){
        inputRequestBuffer = new ArrayList<>();
        outputMessagesBuffer = new ArrayList<>();
    }

    public void sendHTML(String html){

        ByteArrayOutputStream brotliOutput = new ByteArrayOutputStream();

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
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        Encode smsEncoder = new Encode();
        byte[] htmlBytes = brotliOutput.toByteArray();
        int[] htmlBytesAsIntArray = new int[htmlBytes.length];
        for(int i = 0; i < htmlBytes.length; i++){
            htmlBytesAsIntArray[i] = (int)(htmlBytes[i]);
        }

        int[] encodedSms = smsEncoder.encode_raw(256, 114, 143, 158, htmlBytesAsIntArray);
        StringBuilder smsEncodedOutputBuilder = new StringBuilder();
        for(int value : encodedSms){
            smsEncodedOutputBuilder.append(SYMBOL_TABLE[value]);
        }
        String smsEncodedOutput = smsEncodedOutputBuilder.toString();
        final int NUM_CHARS_PER_SMS = 158;
        ArrayList<String> smsQueue = new ArrayList<>();
        for(int i = 0; i < smsEncodedOutput.length(); i += NUM_CHARS_PER_SMS){
            smsQueue.add(smsEncodedOutput.substring(i, i + NUM_CHARS_PER_SMS));
        }
        for(int j = 0; j < smsQueue.size(); j++){
            String str = (v2r(new int[]{j}))[0] + smsQueue.get(j);
            smsQueue.set(j, str);
        }
        int howManyTextsToExpect = (smsQueue.size());

        SmsManager sms = SmsManager.getDefault();

        String outputNumber = "";
        if(phoneNumber.hasCountryCode()){
            outputNumber += phoneNumber.getCountryCode();
        }
        outputNumber += phoneNumber.getNationalNumber();

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
    String[] reassembled = null;


    public void addPart(String message) throws Exception {
        if(message == null){
            Log.e(TAG, "ERR: Message is null.");
            return;
        }

        if(message.startsWith("@@") || (message.startsWith("àà") && (!inputRequestBuffer.isEmpty()))){
            inputRequestBuffer.clear();
            reassembled = new String[inputRequestBuffer.size()];
        }
        if(reassembled == null){
            reassembled = new String[inputRequestBuffer.size()];
        }

        inputRequestBuffer.add(message);

        for(String str : inputRequestBuffer){
            int textOrder = -1;

            if(message.startsWith("àà")){
                textOrder = inputRequestBuffer.size()-1;
            }else{
                textOrder = Base10Conversions.r2v(str.substring(0, 2));
            }
            String text = str.substring(2);
            reassembled[textOrder] = text;
        }
        StringBuilder stringReassembled = new StringBuilder();
        for(String part : reassembled){
            stringReassembled.append(part);
        }
        String str_reassembled = stringReassembled.toString();
        int[] nums = new int[str_reassembled.length()];
        char[] chr = str_reassembled.toCharArray();
        for(int i = 0; i < str_reassembled.length(); i++){
            char myChr = chr[i];
            nums[myChr] = Index.findIndex(SYMBOL_TABLE, String.valueOf(chr));
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
        if(message.startsWith("àà")){
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
