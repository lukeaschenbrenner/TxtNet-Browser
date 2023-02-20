package com.txtnet.txtnetbrowser.server;

import static com.txtnet.txtnetbrowser.server.ServerDisplay.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
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
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.txtnet.txtnetbrowser.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;


public class TxtNetServerService extends Service {
    private NotificationManagerCompat notificationManager;
    private final int NOTIFICATION_ID = 43;

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private final String TAG = "SMSProcessService";

    private WebView webview;


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

        // make a separate thing for receiving incoming messages for responses, but we're going to manually send a message here
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent; // I don't know what any of this does!
        Bundle bundle = new Bundle(1);
      //  bundle.putString("linkToVisit", intent.getStringExtra("linkToVisit"));
        bundle.putString("linkToVisit", "https://google.com");
        Log.e("GETSTRING", bundle.getString("linkToVisit"));
        msg.setData(bundle);

        serviceHandler.sendMessage(msg);

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
        Toast.makeText(this, "We're done here!", Toast.LENGTH_SHORT).show();
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
        super.onCreate();


        HandlerThread thread = new HandlerThread("SMSProcessThread",
                Process.THREAD_PRIORITY_MORE_FAVORABLE);
        thread.start();

        Shizuku.pingBinder();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        // Adapted from https://stackoverflow.com/questions/18865035/android-using-webview-outside-an-activity-context
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
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

        webview = new WebView(this);
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

        webview.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        view.addView(webview);
//        wv.loadUrl("http://google.com");

        windowManager.addView(view, params);
        //    surfaceView.getHolder().addCallback(this);

    }

    private final class ServiceHandler extends Handler {

        private String downloadedHTML = "";
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        // Much code adapted from https://github.com/JonasCz/save-for-offline/blob/master/app/src/main/java/jonas/tool/saveForOffline/ScreenshotService.java
        private int currentStartId;
        private boolean exportCompleted = false;

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public void handleMessage(Message msg) {
            Message msgCopy = Message.obtain(msg);
            currentStartId = msg.arg1;
            //webview = new WebView(TxtNetServerService.this);
            webview.setDrawingCacheEnabled(true);
            webview.measure(412, 824); // Reference viewport size of Pixel 4 XL


   //         final Intent intent = (Intent) msg.obj; // ??????
   //         webview.loadUrl(intent.getStringExtra(Database.FILE_LOCATION));
            // Use this to create a thread/latch onto an existing thread object and process incoming SMS. In this example we stay on only one service thread and simply download a website.




            webview.post(new Runnable() {
                @Override
                public void run() {
                    webview.setWebViewClient(new WebViewClient() {

                        @Override
                        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                            Log.w(TAG, "Recieved error from WebView, description: " + description + ", Failing url: " + failingUrl);
                            //without this method, your app may crash...
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            Log.i(TAG, "Page finished, getting thumbnail");
                            //    takeWebviewScreenshot(intent.getStringExtra(Database.THUMBNAIL));
                            exportHTMLFromWebsite("NULL");
                        }

                        @Override
                        public WebResourceResponse shouldInterceptRequest (final WebView view, String url) {
                            if (url.contains(".css")) {
                                return super.shouldInterceptRequest(view, url);
                                //return null;
                                //makes all CSS requests return null instead of the actual css
                            } else {
                                return super.shouldInterceptRequest(view, url);
                            }
                        }

                    });

          //          Handler handle = new Handler(Looper.getMainLooper());
          //          handle.post(new Runnable() {
          //              public void run() {
                            webview.layout(0, 0, 412, 824);
                            webview.getSettings().setJavaScriptEnabled(true);
                            webview.getSettings().setAllowFileAccessFromFileURLs(true);

         //               }
         //           });

                    // TODO: Should have all the webview creation during object creation, we don't need to create a new webview for every request
                    Log.e("HELLO", "Stuff:" + msgCopy.getData().getString("linkToVisit"));
                    webview.loadUrl(msgCopy.getData().getString("linkToVisit"));
                }
            });


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

        private void exportHTMLFromWebsite(String exportLocation){
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
                                downloadedHTML = "<html>Error: No Content</html>";
                            } else {
                                String unescaped = html.substring(1, html.length() - 1)  // remove wrapping quotes
                                        .replace("\\\\", "\\")        // unescape \\ -> \
                                        .replace("\\\"", "\"");       // unescape \" -> "
                                downloadedHTML = unescaped;
                            }
                            Log.i("Finished HTML", downloadedHTML);
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
                    exportCompleted = true;
                    stopService();

                }
            }).start();
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
            if (exportCompleted) {
                Log.i(TAG, "Service stopped, with startId " + currentStartId + " completed");
                stopSelf(currentStartId);
            }
        }

    }

}

