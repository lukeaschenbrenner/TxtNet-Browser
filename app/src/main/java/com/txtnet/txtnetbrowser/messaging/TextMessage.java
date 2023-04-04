package com.txtnet.txtnetbrowser.messaging;


import static com.txtnet.txtnetbrowser.basest.Base10Conversions.SYMBOL_TABLE;

import android.content.Context;
import android.util.Log;

import com.txtnet.txtnetbrowser.basest.Base10Conversions;
import com.txtnet.txtnetbrowser.basest.Decode;
import com.txtnet.txtnetbrowser.basest.Encode;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.util.Index;

import org.brotli.dec.BrotliInputStream;

/**
 * A buffer to store the SMS Text Message data.
 */
public class TextMessage {
    private final String TAG = "TextMessage";
    private int howManyAdded = 0;
    public String[] textBuffer = null;
    public static String url;
    //public boolean isUserRequest = false;
    Context context;
    /**
     * Constructs a new TextMessage object that allows for the use of a String buffer to represent a text message.
     * @param sizeOfParts size of the buffer (in terms of parts)
     */
    public TextMessage(int sizeOfParts, Context context){
        this.context = context;
        if(sizeOfParts < 0){
            Log.e(TAG, "******* ERROR: SIZE OF PARTS IS NEGATIVE");
            return;
        }
        textBuffer = new String[sizeOfParts];
    }

    public void addPart(int index, String part) throws Exception {
        if(index < 0 || index > textBuffer.length || part == null){
            Log.e(TAG, "******* ERROR: EITHER PART WAS NULL OR INDEX WAS OUT OF BOUNDS");
            return;
        }

        if(textBuffer[index] == null){
            if(part.length() == 157){
                //twilio chopped off the last \n character
                part =  part + "\n";
            }
            textBuffer[index] = part;
            howManyAdded++;
            MainBrowserScreen.onProgressChanged(howManyAdded, textBuffer.length);

        }


        if(howManyAdded == textBuffer.length){


            //we have them all! render the page.
            StringBuilder reassembled = new StringBuilder();
            for(String value : textBuffer){
                if(value == null){
                    throw new Exception("One of the strings is missing.");
                }
                reassembled.append(value);
            }
            //  Log.d("reassembled", reassembled);
            String[] reassembledAsArray = Base10Conversions.explode(reassembled.toString());
            //  Log.d("reassembledasarray", Arrays.toString(reassembledAsArray));
            int[] nums = new int[reassembledAsArray.length];
            for(int i = 0; i < nums.length; i++){
                nums[i] = Index.findIndex(SYMBOL_TABLE, reassembledAsArray[i]);
            }
            int garbageData = 0;
            for(int p = nums.length-1; nums[p] == 114; p--){
                garbageData++;
            }


            //  Log.d("nums", Arrays.toString(nums));
            Encode decoder = new Encode();//not actually encoding the data! I'm decoding the data, but I use encode and reverse the parameters. Output identical when compared to python
            // int[] decoded = decoder.encode_raw(114, 256, 158, 134, nums);
            int[] decoded = decoder.encode_raw(114, 256, 158, 134, nums);
            //  Log.d("bytes", Arrays.toString(decoded));


            decoded = Arrays.copyOfRange(decoded, 0, decoded.length-garbageData);
            //   Log.d("bytes", Arrays.toString(decoded));
            byte[] decodedbytes = new byte[decoded.length];
            for(int i = 0; i < decoded.length; i++){
                decodedbytes[i] = (byte) decoded[i]; //trying to encode an int array (0 to 256) as a byte array (-128 to 127). Should be okay?
            }


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

        }
    }

    //unused method
    byte[] integersToBytes(int[] values) throws IOException { // Don't know if this method works?
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for(int i=0; i < values.length; ++i)
        {
            //dos.writeInt(values[i]);
            dos.writeByte(values[i]);
        }

        return baos.toByteArray();
    }

    //don't need because brotli doesn't output ints in each line read like I initially thought, it outputs strings
    public static int[] convertIntegers(List<Integer> integers) //int arraylist to int array
    {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next();
        }
        return ret;
    }

    /**
     * Returns the text message to the activity in question. Not implemented-- don't need right now
     */
    private void flush(){

    }



}
