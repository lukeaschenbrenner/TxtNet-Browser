package com.txtnet.txtnetbrowser.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;

//import org.apache.commons.compress.archivers.ArchiveOutputStream;
//import org.apache.commons.compress.compressors.brotli.*;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EncodeFile extends ActivityResultContract<String, Uri> {
    Context context;
    public EncodeFile(Context context){
        this.context = context;
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull String filename){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/x-br");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        //      intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

   /*     ActivityResultLauncher<Intent> activityResultLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == CREATE_FILE) {
                            // ToDo : Do your stuff...
                        } else if(result.getResultCode() == 321) {
                            // ToDo : Do your stuff...
                        }
                    }
                });
*/
        // Intent intent = new Intent(this, SampleActivity.class);
        //      activityResultLaunch.launch(intent);

        // Intent creation
        int REQUEST_CODE_ARBITRARY = 1;
//        intent.setType("*text/plain");
        return intent;
    }

    @Override
    public Uri parseResult(int resultCode, @Nullable Intent intent){

        final int CREATE_FILE = 1;

        final ActivityResultRegistry mRegistry;
        ActivityResultLauncher<String> mGetContent;

        //UseBrotliTest(@NonNull ActivityResultRegistry registry) {
        //    mRegistry = registry;
        //}


        //mGetContent = mRegistry.register("key", owner, new ActivityResultContracts.GetContent(),
            //    new ActivityResultCallback<Uri>() {
        //            @Override
        //            public void onActivityResult(Uri uri) {
                        // Handle the returned Uri


                        // Example file to copy
                        //File sourceFile = getDatabasePath("MyDatabaseName");

                        InputStream stream = null;
                        OutputStream os = null;

                        // Note: you may use try-with resources if your API is 19+
                        try {
                            // InputStream constructor takes File, String (path), or FileDescriptor
                            //is = new FileInputStream(sourceFile);
                            // data.getData() holds the URI of the path selected by the picker
                            os = context.getContentResolver().openOutputStream(intent.getData());

                            //  byte[] buffer = new byte[1024];
                            //  int length;
                            //  while ((length = is.read(buffer)) > 0) {
                            //      os.write(buffer, 0, length);
                            //  }
        //                    ArchiveOutputStream o = (ArchiveOutputStream) os;
                            //BrotliCompressorInputStream

                                    Encoder.Parameters params = new Encoder.Parameters().setQuality(4);

                            BrotliOutputStream brotliOutputStream = new BrotliOutputStream(os, params);
                            String exampleString = "<html><head>\n" +
                                    "    <title>Example Domain</title>\n" +
                                    "\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">\n" +
                                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                                    "    <style type=\"text/css\">\n" +
                                    "    body {\n" +
                                    "        background-color: #f0f0f2;\n" +
                                    "        margin: 0;\n" +
                                    "        padding: 0;\n" +
                                    "        font-family: -apple-system, system-ui, BlinkMacSystemFont, \"Segoe UI\", \"Open Sans\", \"Helvetica Neue\", Helvetica, Arial, sans-serif;\n" +
                                    "        \n" +
                                    "    }\n" +
                                    "    div {\n" +
                                    "        width: 600px;\n" +
                                    "        margin: 5em auto;\n" +
                                    "        padding: 2em;\n" +
                                    "        background-color: #fdfdff;\n" +
                                    "        border-radius: 0.5em;\n" +
                                    "        box-shadow: 2px 3px 7px 2px rgba(0,0,0,0.02);\n" +
                                    "    }\n" +
                                    "    a:link, a:visited {\n" +
                                    "        color: #38488f;\n" +
                                    "        text-decoration: none;\n" +
                                    "    }\n" +
                                    "    @media (max-width: 700px) {\n" +
                                    "        div {\n" +
                                    "            margin: 0 auto;\n" +
                                    "            width: auto;\n" +
                                    "        }\n" +
                                    "    }\n" +
                                    "    </style>    \n" +
                                    "</head>\n" +
                                    "\n" +
                                    "<body class=\"vsc-initialized\">\n" +
                                    "<div>\n" +
                                    "    <h1>Example Domain</h1>\n" +
                                    "    <p>This domain is for use in illustrative examples in documents. You may use this\n" +
                                    "    domain in literature without prior coordination or asking for permission.</p>\n" +
                                    "    <p><a href=\"https://www.iana.org/domains/example\">More information...</a></p>\n" +
                                    "</div>\n" +
                                    "\n" +
                                    "\n" +
                                    "</body></html>";

                            stream = new ByteArrayInputStream(exampleString.getBytes(StandardCharsets.UTF_8));

                            int read = stream.read();
                            while (read > -1) { // -1 means EOF
                                brotliOutputStream.write(read);
                                read = stream.read();
                            }

                            // It's important to close the BrotliOutputStream object. This also closes the underlying FileOutputStream
                            brotliOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                os.close();
                                stream.close();
                                // is.close();
                            } catch (IOException e) {
                                //
                            }
                        }




            return intent.getData();
        // return result.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);;
    }






}
