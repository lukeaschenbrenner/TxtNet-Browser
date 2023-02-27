package com.txtnet.txtnetbrowser.server;

import static com.txtnet.txtnetbrowser.server.ServerDisplay.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.txtnet.txtnetbrowser.Constants;
import com.txtnet.txtnetbrowser.R;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;


public class TxtNetServerService extends Service {
    private NotificationManagerCompat notificationManager;
    private final int NOTIFICATION_ID = 43;
    private final int WEBVIEWS_LIMIT = 5;
   // private final int WEBVIEWS_LIMIT = 2; // use for testing!

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private final String TAG = "SMSProcessService";
    private WebView[] webViews;
    public static final HashMap<WebView, AtomicBoolean> webViewBusynessMap = new HashMap<>();
    //private WebView webview;
    private WindowManager windowManager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    @SuppressLint("UnspecifiedImmutableFlag")
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        //Message msg = serviceHandler.obtainMessage();
        //msg.arg1 = startId;
        //serviceHandler.sendMessage(msg);
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            //do nothing for now
        }
        else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            if(serviceHandler != null){
                serviceHandler.stopService();
            }else{
                stopSelfResult(startId);
                    //TODO: Check if this else statement actually works(?)
            }
            return START_NOT_STICKY;
        }

        PendingIntent pendingIntent = null;
        Context context = getApplicationContext();
        Intent intentNotif = new Intent(this, ServerDisplay.class); //allows notif to execute code with same perms as our app
        intentNotif.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // https://developer.android.com/guide/components/activities/tasks-and-back-stack
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intentNotif, PendingIntent.FLAG_MUTABLE);
        }else{
            pendingIntent = PendingIntent.getActivity(this, 0, intentNotif, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TxtNet Server Is Running").setContentText("Press this notification to open the server management screen.")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_monochrome);
        builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).setVisibility(NotificationCompat.VISIBILITY_SECRET);

        notificationManager = NotificationManagerCompat.from(context.getApplicationContext());
        Notification notif = builder.build();
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        startForeground(NOTIFICATION_ID, notif);



        //////////////// TEST CASES /////////////////////////
        // make a separate thing for receiving incoming messages for responses, but we're going to manually send a message here
        String[] links = {"https://google.com", "https://bing.com", "http://example.com", "http://frogfind.com", "http://lite.cnn.com"};
        ArrayList<Message> msgs = new ArrayList<>();
        for(int i = 0; i < 5; i++){
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent; // I don't know what any of this does!
            Bundle bundle = new Bundle(1);
            //  bundle.putString("linkToVisit", intent.getStringExtra("linkToVisit"));
            bundle.putString("linkToVisit", links[i]);
            msg.setData(bundle);
            msgs.add(msg);

        }
        for(int i = 0; i < 5; i++){
            serviceHandler.sendMessage(msgs.get(i));
        }
        Toast.makeText(this, "We're done here!", Toast.LENGTH_SHORT).show();


        /*
                if (intent != null) {
            String action = intent.getAction();
            if(action!=null)

            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startForegroundService();
                    Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    Toast.makeText(getApplicationContext(),
         */

        return START_STICKY;
     ///alternative:   return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notificationManager.cancel(NOTIFICATION_ID);

        // Tell the user we stopped.
       // Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "OnDestroy called!", Toast.LENGTH_SHORT).show();
    }

    //@Override
   // public IBinder onBind(Intent intent) {
    //    return mBinder;
    //}

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    //private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    /*
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LocalServiceActivities.Controller.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_sample)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);

    } */

    public void onCreate(){
        Log.e("ONCREATE", "ONCREATE CALLED TXTNETSERVERSERVICE!");
        super.onCreate();

        webViews = new WebView[WEBVIEWS_LIMIT];

        HandlerThread thread = new HandlerThread("SMSProcessThread",
                Process.THREAD_PRIORITY_MORE_FAVORABLE);
        thread.start();

        Shizuku.pingBinder();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);



        // Adapted from https://stackoverflow.com/questions/18865035/android-using-webview-outside-an-activity-context
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;
        params.width = 0;
        params.height = 0;

        LinearLayout view = new LinearLayout(this);
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null); // could make a looper callback to handle a message indicating how many cookies were removed, but we don't care
        }else{
            cookieManager.removeAllCookie();
        }
        cookieManager.setAcceptCookie(false);

        //
        //////// Populate webviews
        //
        for(int i = 0; i < WEBVIEWS_LIMIT; i++) {
            webViews[i] = new WebView(this);
            webViewBusynessMap.put(webViews[i], new AtomicBoolean(false));
            webViews[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            view.addView(webViews[i]);

            webViews[i].measure(412, 824); // Reference viewport size of Pixel 4 XL
            webViews[i].layout(0, 0, 412, 824);
            WebSettings ws = webViews[i].getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setAllowFileAccessFromFileURLs(true);
            ws.setSaveFormData(false);

            int finalI = i;
            webViews[i].post(new Runnable() { // careful, this runs on main thread!
                @Override
                public void run() {
                    webViews[finalI].setWebViewClient(new ServerWebViewClient());
                }
            });
            //TODO: Add requesting permission to overlay

/*
In activity:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }
 */


//        wv.loadUrl("http://google.com");
        }
        windowManager.addView(view,params);
                //    surfaceView.getHolder().addCallback(this);


        }

    static final Object SEMAPHORE = new Object();
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        // Much code adapted from https://github.com/JonasCz/save-for-offline/blob/master/app/src/main/java/jonas/tool/saveForOffline/ScreenshotService.java
        private int currentStartId;


        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public void handleMessage(Message msg) {
            Message msgCopy = Message.obtain(msg); // the message data is cleared when the servicehandler is quit, which happens before the runnable is executed
            Log.e("msgs", "Message recieved: " + msgCopy.getData().getString("linkToVisit"));
            currentStartId = msg.arg1;
            //webview = new WebView(TxtNetServerService.this);
        //    webview.setDrawingCacheEnabled(true); // If we want to export the website as a bitmap, we should disable hardware-accelerated rendering using this line


   //         final Intent intent = (Intent) msg.obj; // ??????
   //         webview.loadUrl(intent.getStringExtra(Database.FILE_LOCATION));
            // Use this to create a thread/latch onto an existing thread object and process incoming SMS. In this example we stay on only one service thread and simply download a website.



          //          Handler handle = new Handler(Looper.getMainLooper());
          //          handle.post(new Runnable() {
          //              public void run() {


         //               }
         //           });

//                    Log.e("URL from message:", "Stuff:" + msgCopy.getData().getString("linkToVisit"));

            //check if webviews are busy
            boolean shouldExit = false;
            while(!shouldExit) {
                Log.e("TAG", "Not exiting loop, waiting for webview!");
                synchronized (SEMAPHORE) {
                    for (int i = 0; i < webViews.length; i++) {
                        WebView view = webViews[i];
                        if (!(Objects.requireNonNull(webViewBusynessMap.get(view)).get())){ // webview is not busy, load the url
                            Objects.requireNonNull(webViewBusynessMap.get(view)).set(true);
                            int finalI = i;
                            view.post(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e("newbusy", "newwebviewbusyness: " + Objects.requireNonNull(webViewBusynessMap.get(view)).get());
                                    System.out.println("Webview " + view.toString() + " is running. " + finalI);
                                    view.loadUrl(msgCopy.getData().getString("linkToVisit"));
                                }
                            });
                            shouldExit = true;
                            break;
                        }
                    }
                    if(!shouldExit){
                        try {
                            Log.e("WAIT", "SEMAPHORE WAIT TIME");
                            SEMAPHORE.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }

            }





//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                // Restore interrupt status.
//                Thread.currentThread().interrupt();
//            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //stopSelf(msg.arg1);
        }


  //      @JavascriptInterface
  //      public void onData(){
  //          webview.post(new Runnable() {
  //              @Override
  //              public void run() {
  //                  webview.evaluateJavascript("javascript:executeNext()",null);
  //              }
  //          });

  //      }

        private void saveBitmapToFile(Bitmap bitmap, File outputFile) {
            if (bitmap == null) {
                return;
            }
            Objects.requireNonNull(outputFile.getParentFile()).mkdirs();

            try {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "IoException while saving bitmap to file", e);
            }
            Log.i(TAG, "Saved Bitmap to file: " + outputFile.getPath());
        }

        private void stopService() {
            Log.i(TAG, "stopService called");
            if (serviceHandler.hasMessages(currentStartId)) {
                serviceHandler.removeMessages(currentStartId);
            }
                Log.i(TAG, "Service stopped, with startId " + currentStartId + " completed");
            for(int i = 0; i < webViews.length; i++){
                windowManager.removeView(webViews[i].getRootView());

            }
                webViews = new WebView[0];
                stopSelf(); // we don't care if the startID matches
            }
        }

    public static void exportHTMLFromWebsite(WebView webview, String exportLocation){
        final String[] downloadedHTML = {""}; // must be one element final array because using inner class
        String TAG = "ServerWebViewClient";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);  //allow webview to render, otherwise output may be blank or partial
                } catch (InterruptedException e) {
                    //should never happen
                    Log.e(TAG, "InterruptedException when downloading website ", e);
                }
                //     saveBitmapToFile(webview.getDrawingCache(), new File(outputFileLocation));
                //                            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",

                ValueCallback<String> vc = new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String html) {
                        if ("null".equals(html)) {
                            downloadedHTML[0] = "<html>Error: No Content</html>";
                        } else {
                            Log.i("Early HTML", html);

                            String unescaped = html.substring(1, html.length() - 1)  // remove wrapping quotes
                                    .replace("\\\\", "\\")        // unescape \\ -> \
                                    .replace("\\\"", "\"");       // unescape \" -> "
                            downloadedHTML[0] = unescaped;
                        }
                        Log.i("Finished HTML", Jsoup.parse(StringEscapeUtils.unescapeJava(downloadedHTML[0])).html().substring(0, 10));
                        try{
                            Objects.requireNonNull(TxtNetServerService.webViewBusynessMap.get(webview)).set(false);
                            synchronized (SEMAPHORE){
                                SEMAPHORE.notifyAll();
                            }
                        }catch(NullPointerException npe){
                            npe.printStackTrace();
                        }
                        //  stopService();
                    }
                };
                webview.post(new Runnable() {
                    @Override
                    public void run() {
                        webview.evaluateJavascript(
                                "(function() { " +
                                        "var node = document.doctype;\n" +
                                        "var html = \"<!DOCTYPE \"\n" +
                                        "         + node.name\n" +
                                        "         + (node.publicId ? ' PUBLIC \"' + node.publicId + '\"' : '')\n" +
                                        "         + (!node.publicId && node.systemId ? ' SYSTEM' : '') \n" +
                                        "         + (node.systemId ? ' \"' + node.systemId + '\"' : '')\n" +
                                        "         + '>';" +
                                        "return (html+document.documentElement.outerHTML); })();",
                                vc);
                    }
                });

                //                    webview.post();
                //         stopService();

            }
        }).start();
    }
}



