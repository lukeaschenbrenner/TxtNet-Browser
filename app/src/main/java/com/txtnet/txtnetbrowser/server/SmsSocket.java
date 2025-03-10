package com.txtnet.txtnetbrowser.server;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED;
import static com.txtnet.txtnetbrowser.basest.Base10Conversions.SYMBOL_TABLE;
import static com.txtnet.txtnetbrowser.basest.Base10Conversions.v2r;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.i18n.phonenumbers.Phonenumber;
import com.txtnet.brotli4droid.Brotli4jLoader;
import com.txtnet.brotli4droid.encoder.BrotliOutputStream;
import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.basest.Base10Conversions;
import com.txtnet.txtnetbrowser.basest.Encode;
import com.txtnet.txtnetbrowser.receiver.SmsSentReceiver;
import com.txtnet.txtnetbrowser.util.CRC5;
import com.txtnet.txtnetbrowser.util.Index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmsSocket {
    //private final int COMPRESSION_QUALITY_LEVEL = 11;
    public ArrayList<String> inputRequestBuffer;
    int finalBufferLength = 406;
    public StringBuilder decodedUrl = new StringBuilder();
    public ArrayList<String> outputMessagesBuffer;
    private Phonenumber.PhoneNumber phoneNumber;
    private AtomicBoolean shouldSend = new AtomicBoolean(true);
    private TxtNetServerService service = null;
    private boolean isSending = false;
    private int MAX_SMS_PER_REQUEST;
    private int SMS_SERVER_SEND_INTERVAL_MS;
    private final SmsManager sms = SmsManager.getDefault();
    PendingIntent sentPI;

    public SmsSocket(Phonenumber.PhoneNumber number, TxtNetServerService service, int maxSmsPerRequest, int smsSendIntervalMs) {
        this();
        this.phoneNumber = number;
        this.service = service;
        MAX_SMS_PER_REQUEST = maxSmsPerRequest;
        SMS_SERVER_SEND_INTERVAL_MS = smsSendIntervalMs;
        sentPI = PendingIntent.getBroadcast(service.getApplication().getApplicationContext(), 0,new Intent("SMS_SENT"), FLAG_IMMUTABLE);
    }
    public SmsSocket(){
        inputRequestBuffer = new ArrayList<>();
        outputMessagesBuffer = new ArrayList<>();
        MAX_SMS_PER_REQUEST = 1; //this constructor should never be used
        SMS_SERVER_SEND_INTERVAL_MS = 5000;
    }

    public ArrayList<String> createEncodedQueue(String plainTextHTML){
        ByteArrayOutputStream brotliOutput = new ByteArrayOutputStream();
        Brotli4jLoader.ensureAvailability();
        //Log.i(TAG, "Brotli4J is available? " + (Brotli4jLoader.isAvailable() ? "true" : "false"));
        //Encoder.Parameters params = new Encoder.Parameters().setQuality(COMPRESSION_QUALITY_LEVEL);

        BufferedWriter brotliWriter = null;
        try {
            brotliWriter = new BufferedWriter(new OutputStreamWriter(new BrotliOutputStream(brotliOutput)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader bufReader = new BufferedReader(new StringReader(plainTextHTML));
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

        //Log.i(TAG, smsQueue.size() + "<smsqueue indexcharacters>" + indexCharacters.length);
        for(int k = 0; k < indices.length; k++){
            //if(k == smsQueue.size()-1){
            //    smsQueue.set(k, SYMBOL_TABLE[SYMBOL_TABLE.length-1] + SYMBOL_TABLE[SYMBOL_TABLE.length-1] + smsQueue.get(k));
            //}else{
            String currentSMS = smsQueue.get(k);

            Encode decoder = new Encode();//Encoder with parameters reversed, actually decoding

            String[] payloadData = Base10Conversions.explode(currentSMS);
            int[] payloadDataAsInts = new int[payloadData.length];

            for(int i = 0; i < payloadDataAsInts.length; i++){
                payloadDataAsInts[i] = Index.findIndex(SYMBOL_TABLE, payloadData[i]);
            }
            int[] decoded = decoder.encode_raw(114, 256, 158, 134, payloadDataAsInts);
            byte[] decodedBytes = new byte[decoded.length];
            for(int i = 0; i < decoded.length; i++){
                decodedBytes[i] = (byte) decoded[i]; //trying to encode an int array (0 to 256) as a byte array (-128 to 127). Should be okay?
            }
            int crc5 = CRC5.computeCRC5(decodedBytes);
            int crc5WithIndex = CRC5.packWithCRC5(indices[k], crc5);

            smsQueue.set(k, (v2r(new int[]{crc5WithIndex})[0]) + smsQueue.get(k));

            //}
        }
        return smsQueue;
    }

    public void sendHTML(String html){
        this.sendHTML(html, "Untitled");
    }

    public void sendHTML(String html, String pageTitle) {

        ArrayList<String> smsQueue = createEncodedQueue(html);

        String outputNumber = "";
        if(phoneNumber.hasCountryCode()){
            outputNumber += phoneNumber.getCountryCode();
        }
        outputNumber += phoneNumber.getNationalNumber();


        String processStarting = " Process starting,";

        if(smsQueue.size() > MAX_SMS_PER_REQUEST){
            Log.w(TAG, "Request made with SMS count > MAX_SMS_PER_REQUEST");
            String encodedErrorMsg = createEncodedQueue(service.getString(R.string.request_sms_outgoing_exceeded_error)).get(0);
            sms.sendTextMessage(outputNumber, null, "1" + processStarting, null, null);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sms.sendTextMessage(outputNumber, null, encodedErrorMsg, null, null);
            return;
        }

        int numberOfDigitsSMSQueue = smsQueue.isEmpty() ? 0 : (int) (Math.log10(smsQueue.size()) + 1);
        String safeTitle = ServerUtils.TS0338SafeString(pageTitle, true);
        String titleClipped = safeTitle.substring(0, Math.min(safeTitle.length(), (160 - (processStarting.length() + numberOfDigitsSMSQueue))));
        sms.sendTextMessage(outputNumber, null, (smsQueue.size() + processStarting + titleClipped), null, null);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isSending = true;

        try{
            ContextCompat.registerReceiver(service.getApplication().getApplicationContext(),
                            SmsSentReceiver.class.newInstance(), new IntentFilter("SMS_SENT"), RECEIVER_NOT_EXPORTED);

        }catch(IllegalAccessException | InstantiationException e){
            Log.e(this.TAG, e.toString());
        }

        sendMessagesSynchronously(smsQueue, outputNumber, shouldSend, SMS_SERVER_SEND_INTERVAL_MS);
  //        if(!shouldSend.get()){
//            Log.i(TAG, "not shouldsend!");
//            shouldSend.set(true);
//        }
//        if(wasFalse){
//            Log.i(TAG, "it was false!");
//        }
    }
    public void stopSend(){
        if(isSending){
            shouldSend.set(false);
        }
        inputRequestBuffer.clear();
        //TxtNetServerService.smsDataBase.remove(phoneNumber);  << to eventually clear memory leaks, could have this
    }



    private void sendMessagesSynchronously(List<String> smsQueue, String outputNumber, AtomicBoolean shouldSend, int intervalMs) {
        for (int i = 0; i < smsQueue.size(); i++) {
            long startTime = System.currentTimeMillis();
            if (!shouldSend.get()) {
                Log.e(this.toString(), "shouldSend has been set to FALSE. Stopping SMS sending.");
                isSending = false; // possibly not thread-safe, but are SmsSocket instances and SMS_Sender_Threads 1:1?
                boolean sendingManualStopOccurred = shouldSend.compareAndSet(false, true);
                return;
            }
            sms.sendTextMessage(outputNumber, null, smsQueue.get(i), sentPI, null);

            long elapsedTime = System.currentTimeMillis() - startTime;

            long remainingWaitTime = intervalMs - elapsedTime;
            if (remainingWaitTime > 0) {
                try {
                    Thread.sleep(remainingWaitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        try {
            Thread.sleep(10000); // Sleep for 10 seconds after the final message
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally{
            isSending = false; // possibly not thread-safe, but are SmsSocket instances and SMS_Sender_Threads 1:1?
            boolean sendingManualStopOccurred = shouldSend.compareAndSet(false, true);

        }
    }




    private final String TAG = "SmsSocketMessage";
    private int howManyAdded = 0;
    //public String[] textBuffer = null;
    public String url;
    //Context context;


    /*
    * This method is what the server uses to add multipart client URL requests. If an HTTP GET URL is too long, they send it in multiple (non-Brotli-compressed) parts.
    *  */
    public void addPart(String message) throws Exception {
        if(message == null){
            Log.e(TAG, "ERR: Message is null.");
            return;
        }


        if(message.matches("^([0-9]){2}(?s).*$")){ // message is the first message
            //TODO: There is a bug in this condition. The first SMS sent from the client to the server contains the number of total messages to expect
            //  embedded within the first 2 characters as a 2-digit uint, where the index usually is for all the other messages. We treat this message with index 0 and use the 2 digits to set the buffer size.
            //  due to how the symbol table is written, it takes over index 99 (decimal) to reach a base114-encoded message that contains two consecutive integers. So right now, this is not an issue.
            //  BUT in the future, if this index is somehow expanded (eg. switched to base114), a new solution needs to be found or the queue will reset every time it reaches base114-index "00".

            // || (message.startsWith("àà") && (!inputRequestBuffer.isEmpty()) && inputRequestBuffer.size() > 0)){
            inputRequestBuffer.clear();
            finalBufferLength = Integer.parseInt(message.substring(0, 2)) + 1;
            Log.w(TAG, "First message detected with finalBufferLength " + finalBufferLength);
            //reassembled = new String[inputRequestBuffer.size()];
            inputRequestBuffer.add(message);

        }else {
            inputRequestBuffer.add(message);
        }
       // if(reassembled == null){
       //     reassembled = new String[inputRequestBuffer.size()];
       // }



        if(inputRequestBuffer.size() >= finalBufferLength) { // || finalBufferLength == 1
            String[] reassembled = new String[inputRequestBuffer.size()];

            for (String str : inputRequestBuffer) {
                int textOrder = -1;

                if (str.startsWith(SYMBOL_TABLE[SYMBOL_TABLE.length - 1] + SYMBOL_TABLE[SYMBOL_TABLE.length - 1])) {
                    textOrder = finalBufferLength - 1; // àà means this is the final message of a multi-part message
                    //inputRequestBuffer.size()-1;
//                    Log.i(TAG, "FIRST ONE!!" + textOrder);

                } else if (str.substring(0, 2).matches("([0-9]){2}")) {// PROBLEM: we should know ahead of time how big our buffer should eventually be for the link we received. similar to the code for "Process Starting". This wastes space adding an entire text for every request though.
                    // We could append to beginning of every message number of texts eg. 12@@... and then take the numDigits and create a custom encoding for that one specific text.
                    // However, that's very difficult to accomplish without knowing the number of digits we will need before encoding the first text, to predict how many bytes can fit into that text.
                    // Instead, we simply append the number instead of @@, which allows us to have 100 SMS messages in one request before encoding may possibly use two digits at the beginning. Considering the actual max is 12996, this is a far cry from the potential indexing allowed by base 114 2-character index.
                    // Not a problem right now for URLs, but will become an issue later when trying to transmit binary data
                    textOrder = 0;
                    //TODO: THERE IS A BUG IN THIS BLOCK CONDITION. SEE ABOVE
//                    Log.i(TAG, "MATCHES!!" + textOrder);


                } else {
                    textOrder = Base10Conversions.r2v(str.substring(0, 2));
//                    Log.i(TAG, "MIDDLE ONE!!" + textOrder);

                }

                String text = str.substring(2);
                reassembled[textOrder] = text;
            }
            inputRequestBuffer.clear();

            //(int)(Math.log10(n)+1); << if we varied the number of input SMS digits, this would be the way to determine the number of digits of a number
            StringBuilder stringReassembled = new StringBuilder();
            for (int i = 0; i < reassembled.length; i++) {
                String part = reassembled[i];
                stringReassembled.append(part);
            }
            String str_reassembled = stringReassembled.toString();

//            Log.i(TAG, "Str_reassembled = " + str_reassembled);

            int[] nums = new int[str_reassembled.length()];
            char[] chr = str_reassembled.toCharArray();
            for (int i = 0; i < str_reassembled.length(); i++) {
                char myChr = chr[i];
                nums[i] = Index.findIndex(SYMBOL_TABLE, String.valueOf(myChr)); //TODO: HOW DOES THIS WORK?
                //nums[myChr] = (Arrays.asList(SYMBOL_TABLE).indexOf(String.valueOf(chr)));
            }
            int garbageData = 0;

            for (int p = nums.length - 1; nums[p] == 114; p--) {
                garbageData++;
            }
            int[] urlWithGarbage = null;


            try {
                urlWithGarbage = new Encode().encode_raw(114, 256, 158, 134, nums); //actually decoding, not encoding
                //assert urlWithGarbage != null;
                if (urlWithGarbage == null) {
                    Log.e(TAG, "Server ERROR: input url was not able to be decoded successfully and parsed");
                }
                int[] urlEncoded = Arrays.copyOfRange(urlWithGarbage, 0, urlWithGarbage.length - garbageData);
                byte[] urlBytes = new byte[urlEncoded.length];
                for (int i = 0; i < urlEncoded.length; i++) {
                    urlBytes[i] = (byte) (urlEncoded[i]);
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


            } catch (Exception e) {
                Log.e(TAG, "Error: Decoding user message failed.");
                inputRequestBuffer.clear();
                TxtNetServerService.smsDataBase.remove(phoneNumber);


            }finally{
                finalBufferLength = Integer.MAX_VALUE;
            }
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
