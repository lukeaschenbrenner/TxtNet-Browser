package com.txtnet.txtnetbrowser.server;

import static com.txtnet.txtnetbrowser.server.ServerDisplay.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Binder;
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

import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.*;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.txtnet.txtnetbrowser.Constants;
import com.txtnet.txtnetbrowser.R;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;
import org.jsoup.*;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.safety.*;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;


public class TxtNetServerService extends Service {
    private NotificationManagerCompat notificationManager;
    private final int NOTIFICATION_ID = 43;
    private int WEBVIEWS_LIMIT = 5;
    public static int MAX_SMS_PER_REQUEST;
   // private final int WEBVIEWS_LIMIT = 2; // use for testing!

    public ServiceHandler serviceHandler;
    private final String TAG = "SMSProcessService";
    private ArrayList<WebView> webViews;
    SmsSocket[] socketAssociatedWithWebViews;
    public static final HashMap<WebView, AtomicBoolean> webViewBusynessMap = new HashMap<>();
    //private WebView webview;
    private WindowManager windowManager;

    public static HashMap<PhoneNumber, SmsSocket> smsDataBase = new HashMap<>();
    public static boolean isRunning = false;

    public static TxtNetServerService instance = null;

    private Binder binder;

    @Override
    public IBinder onBind(Intent intent) {
        int tempNum = intent.getIntExtra("maxWebViews", 5);
        if(tempNum < 100){
            WEBVIEWS_LIMIT = tempNum;
        }
        MAX_SMS_PER_REQUEST = intent.getIntExtra("maxOutgoingSmsPerRequest", 100);

        LinearLayout view = new LinearLayout(this);
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        //
        //////// Populate webviews
        //
        Log.i(TAG, "Populating webviews...");
        for(int i = 0; i < WEBVIEWS_LIMIT; i++) {
            webViews.add(new WebView(this));
            webViewBusynessMap.put(webViews.get(i), new AtomicBoolean(false));
            webViews.get(i).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            view.addView(webViews.get(i));

            webViews.get(i).measure(412, 824); // Reference viewport size of Pixel 4 XL
            webViews.get(i).layout(0, 0, 412, 824);
            WebSettings ws = webViews.get(i).getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setAllowFileAccessFromFileURLs(true);
            ws.setSaveFormData(false);

            int finalI = i;
            webViews.get(i).post(new Runnable() { // careful, this runs on main thread!
                @Override
                public void run() {
                    webViews.get(finalI).setWebViewClient(new ServerWebViewClient(TxtNetServerService.this));
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
        // Adapted from https://stackoverflow.com/questions/18865035/android-using-webview-outside-an-activity-context
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;
        params.width = 0;
        params.height = 0;
        windowManager.addView(view,params);
        //    surfaceView.getHolder().addCallback(this);
        Log.i(TAG, WEBVIEWS_LIMIT + " Views added.");

        return binder;
    }



    @Override
    @SuppressLint("UnspecifiedImmutableFlag")
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);

        //Message msg = serviceHandler.obtainMessage();
        //msg.arg1 = startId;
        //serviceHandler.sendMessage(msg);
        if(intent == null){
            // The service restarted itself (possibly from a crash!)
        }
        if (intent == null || intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
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
//        String[] links = {"https://google.com", "https://bing.com", "http://example.com", "http://frogfind.com", "http://lite.cnn.com"};
//        ArrayList<Message> msgs = new ArrayList<>();
//        for(int i = 0; i < 5; i++){
//            Message msg = serviceHandler.obtainMessage();
//            msg.arg1 = startId;
//            msg.obj = intent; // I don't know what any of this does!
//            Bundle bundle = new Bundle(2);
//            //  bundle.putString("linkToVisit", intent.getStringExtra("linkToVisit"));
//            bundle.putString("linkToVisit", links[i]);
//            bundle.putSerializable(phonenumber.object)
//            msg.setData(bundle);
//            msgs.add(msg);
//
//        }
//        for(int i = 0; i < 5; i++){
//            serviceHandler.sendMessage(msgs.get(i));
//        }
//        Toast.makeText(this, "We're done here!", Toast.LENGTH_SHORT).show();


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
        isRunning = false;
        instance = null;

        // Tell the user we stopped.
       // Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Server stopped!", Toast.LENGTH_SHORT).show();
    }


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

    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(){
        Log.e("ONCREATE", "ONCREATE CALLED TXTNETSERVERSERVICE!");
        super.onCreate();
        isRunning = true;
        instance = this;
        webViews = new ArrayList<>();//[WEBVIEWS_LIMIT];
        socketAssociatedWithWebViews = new SmsSocket[WEBVIEWS_LIMIT];

        HandlerThread thread = new HandlerThread("SMSProcessThread",
                Process.THREAD_PRIORITY_MORE_FAVORABLE);
        thread.start();
        binder = new LocalBinder();

        Shizuku.pingBinder();

        // Get the HandlerThread's Looper and use it for our Handler
        Looper serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);


        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null); // could make a looper callback to handle a message indicating how many cookies were removed, but we don't care
        }else{
            cookieManager.removeAllCookie();
        }
        cookieManager.setAcceptCookie(false);


        }

    static final Object SEMAPHORE = new Object();
    public final class ServiceHandler extends Handler {
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
            PhoneNumber number = null;
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                number = msgCopy.getData().getSerializable("phoneNumber", PhoneNumber.class);
            }else{
                number = (PhoneNumber) msgCopy.getData().getSerializable("phoneNumber");
            }
            SmsSocket sock = null;
            if(smsDataBase.containsKey(number)){
                sock = smsDataBase.get(number);
            }else{
                sock = new SmsSocket(number, TxtNetServerService.this, MAX_SMS_PER_REQUEST);
                smsDataBase.put(number, sock);
            }

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
                    for (int i = 0; i < webViews.size(); i++) {
                        WebView view = webViews.get(i);
                        if (!(Objects.requireNonNull(webViewBusynessMap.get(view)).get())){ // webview is not busy, load the url
                            Objects.requireNonNull(webViewBusynessMap.get(view)).set(true);
                            socketAssociatedWithWebViews[i] = sock;
                            int finalI = i;
                            view.post(new Runnable() {
                                @Override
                                public void run() {
                                   // Log.e("newbusy", "newwebviewbusyness: " + Objects.requireNonNull(webViewBusynessMap.get(view)).get());
                                    System.out.println("Webview " + view.toString() + " is running. " + finalI);
                                    String url = sanitizeUrl(msgCopy.getData().getString("linkToVisit"));
                                    view.loadUrl(url);
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
            for(int i = 0; i < webViews.size(); i++){
                windowManager.removeView(webViews.get(i).getRootView());

            }
                webViews.clear();
                stopSelf(); // we don't care if the startID matches

            }
        }

    public void exportHTMLFromWebsite(WebView webview){ //, SmsSocket socket
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
                            //Log.i("Early HTML", html);

                            String unescaped = html.substring(1, html.length() - 1)  // remove wrapping quotes
                                    .replace("\\\\", "\\")        // unescape \\ -> \
                                    .replace("\\\"", "\"");       // unescape \" -> "
                            downloadedHTML[0] = unescaped;
                        }
                        //Jsoup.parse(StringEscapeUtils.unescapeJava(downloadedHTML[0])).html().substring(0, 10));
                        String output = sanitizeHtml(StringEscapeUtils.unescapeJava(downloadedHTML[0]));
                        SmsSocket currentSocket = socketAssociatedWithWebViews[webViews.indexOf(webview)];
                        currentSocket.sendHTML(output);
                        socketAssociatedWithWebViews[webViews.indexOf(webview)] = null;

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
                                        "if(node === null){ return (document.documentElement.outerHTML); }\n" +
                                        "else { \n"+
                                        "var html = \"<!DOCTYPE \"\n" +
                                        "         + node.name\n" +
                                        "         + (node.publicId ? ' PUBLIC \"' + node.publicId + '\"' : '')\n" +
                                        "         + (!node.publicId && node.systemId ? ' SYSTEM' : '') \n" +
                                        "         + (node.systemId ? ' \"' + node.systemId + '\"' : '')\n" +
                                        "         + '>';\n" +
                                        "return (html+document.documentElement.outerHTML); } })();",
                                vc);
                    }
                });

                //                    webview.post();
                //         stopService();

            }
        }).start();
    }

    private static final String[] tlds = {"aaa","aarp","abarth","abb","abbott","abbvie","abc","able","abogado","abudhabi","ac","academy","accenture","accountant","accountants","aco","active","actor","ad","adac","ads","adult","ae","aeg","aero","aetna","af","afamilycompany","afl","africa","ag","agakhan","agency","ai","aig","aigo","airbus","airforce","airtel","akdn","al","alfaromeo","alibaba","alipay","allfinanz","allstate","ally","alsace","alstom","am","americanexpress","americanfamily","amex","amfam","amica","amsterdam","analytics","android","anquan","anz","ao","aol","apartments","app","apple","aq","aquarelle","ar","arab","aramco","archi","army","arpa","art","arte","as","asda","asia","associates","at","athleta","attorney","au","auction","audi","audible","audio","auspost","author","auto","autos","avianca","aw","aws","ax","axa","az","azure","ba","baby","baidu","banamex","bananarepublic","band","bank","bar","barcelona","barclaycard","barclays","barefoot","bargains","baseball","basketball","bauhaus","bayern","bb","bbc","bbt","bbva","bcg","bcn","bd","be","beats","beauty","beer","bentley","berlin","best","bestbuy","bet","bf","bg","bh","bharti","bi","bible","bid","bike","bing","bingo","bio","biz","bj","black","blackfriday","blanco","blockbuster","blog","bloomberg","blue","bm","bms","bmw","bn","bnl","bnpparibas","bo",
            "boats","boehringer","bofa","bom","bond","boo","book","booking","bosch","bostik","boston","bot","boutique","box","br","bradesco","bridgestone","broadway","broker","brother","brussels","bs","bt","budapest","bugatti","build","builders","business","buy","buzz","bv","bw","by","bz","bzh","ca","cab","cafe","cal","call","calvinklein","cam","camera","camp","cancerresearch","canon","capetown","capital","capitalone","car","caravan","cards","care","career","careers","cars","cartier","casa","case","caseih","cash","casino","cat","catering","catholic","cba","cbn","cbre","cbs","cc","cd","ceb","center","ceo","cern","cf","cfa","cfd","cg","ch","chanel","channel","chase","chat","cheap","chintai","christmas","chrome","chrysler","church","ci","cipriani","circle","cisco","citadel","citi","citic","city","cityeats","ck","cl","claims","cleaning","click","clinic","clinique","clothing","cloud","club","clubmed","cm","cn","co","coach","codes","coffee","college","cologne","com","comcast","commbank","community","company","compare","computer","comsec","condos","construction","consulting","contact","contractors","cooking","cookingchannel","cool","coop","corsica","country","coupon","coupons","courses","cr","credit","creditcard","creditunion","cricket","crown","crs","cruise","cruises","csc","cu","cuisinella",
            "cv","cw","cx","cy","cymru","cyou","cz","dabur","dad","dance","data","date","dating","datsun","day","dclk","dds","de","deal","dealer","deals","degree","delivery","dell","deloitte","delta","democrat","dental","dentist","desi","design","dev","dhl","diamonds","diet","digital","direct","directory","discount","discover","dish","diy","dj","dk","dm","dnp","do","docs","doctor","dodge","dog","doha","domains","dot","download","drive","dtv","dubai","duck","dunlop","duns","dupont","durban","dvag","dvr","dz","earth","eat","ec","eco","edeka","edu","education","ee","eg","email","emerck","energy","engineer","engineering","enterprises","epost","epson","equipment","er","ericsson","erni","es","esq","estate","esurance","et","etisalat","eu","eurovision","eus","events","everbank","exchange","expert","exposed","express","extraspace","fage","fail","fairwinds","faith","family","fan","fans","farm","farmers","fashion","fast","fedex","feedback","ferrari","ferrero","fi","fiat","fidelity","fido","film","final","finance","financial","fire","firestone","firmdale","fish","fishing","fit","fitness","fj","fk","flickr","flights","flir","florist","flowers","fly","fm","fo","foo","food","foodnetwork","football","ford","forex","forsale","forum","foundation","fox","fr","free","fresenius","frl","frogans","frontdoor",
            "frontier","ftr","fujitsu","fujixerox","fun","fund","furniture","futbol","fyi","ga","gal","gallery","gallo","gallup","game","games","gap","garden","gb","gbiz","gd","gdn","ge","gea","gent","genting","george","gf","gg","ggee","gh","gi","gift","gifts","gives","giving","gl","glade","glass","gle","global","globo","gm","gmail","gmbh","gmo","gmx","gn","godaddy","gold","goldpoint","golf","goo","goodhands","goodyear","goog","google","gop","got","gov","gp","gq","gr","grainger","graphics","gratis","green","gripe","grocery","group","gs","gt","gu","guardian","gucci","guge","guide","guitars","guru","gw","gy","hair","hamburg","hangout","haus","hbo","hdfc","hdfcbank","health","healthcare","help","helsinki","here","hermes","hgtv","hiphop","hisamitsu","hitachi","hiv","hk","hkt","hm","hn","hockey","holdings","holiday","homedepot","homegoods","homes","homesense","honda","honeywell","horse","hospital","host","hosting","hot","hoteles","hotels","hotmail","house","how","hr","hsbc","ht","hu","hughes","hyatt","hyundai","ibm","icbc","ice","icu","id","ie","ieee","ifm","ikano","il","im","imamat","imdb","immo","immobilien","in","industries","infiniti","info","ing","ink","institute","insurance","insure","int","intel","international","intuit","investments","io","ipiranga","iq","ir","irish","is","iselect",
            "ismaili","ist","istanbul","it","itau","itv","iveco","iwc","jaguar","java","jcb","jcp","je","jeep","jetzt","jewelry","jio","jlc","jll","jm","jmp","jnj","jo","jobs","joburg","jot","joy","jp","jpmorgan","jprs","juegos","juniper","kaufen","kddi","ke","kerryhotels","kerrylogistics","kerryproperties","kfh","kg","kh","ki","kia","kim","kinder","kindle","kitchen","kiwi","km","kn","koeln","komatsu","kosher","kp","kpmg","kpn","kr","krd","kred","kuokgroup","kw","ky","kyoto","kz","la","lacaixa","ladbrokes","lamborghini","lamer","lancaster","lancia","lancome","land","landrover","lanxess","lasalle","lat","latino","latrobe","law","lawyer","lb","lc","lds","lease","leclerc","lefrak","legal","lego","lexus","lgbt","li","liaison","lidl","life","lifeinsurance","lifestyle","lighting","like","lilly","limited","limo","lincoln","linde","link","lipsy","live","living","lixil","lk","llc","loan","loans","locker","locus","loft","lol","london","lotte","lotto","love","lpl","lplfinancial","lr","ls","lt","ltd","ltda","lu","lundbeck","lupin","luxe","luxury","lv","ly","ma","macys","madrid","maif","maison","makeup","man","management","mango","map","market","marketing","markets","marriott","marshalls","maserati","mattel","mba","mc","mckinsey","md","me","med","media","meet","melbourne","meme","memorial","men","menu",
            "meo","merckmsd","metlife","mg","mh","miami","microsoft","mil","mini","mint","mit","mitsubishi","mk","ml","mlb","mls","mm","mma","mn","mo","mobi","mobile","mobily","moda","moe","moi","mom","monash","money","monster","mopar","mormon","mortgage","moscow","moto","motorcycles","mov","movie","movistar","mp","mq","mr","ms","msd","mt","mtn","mtr","mu","museum","mutual","mv","mw","mx","my","mz","na","nab","nadex","nagoya","name","nationwide","natura","navy","nba","nc","ne","nec","net","netbank","netflix","network","neustar","new","newholland","news","next","nextdirect","nexus","nf","nfl","ng","ngo","nhk","ni","nico","nike","nikon","ninja","nissan","nissay","nl","no","nokia","northwesternmutual","norton","now","nowruz","nowtv","np","nr","nra","nrw","ntt","nu","nyc","nz","obi","observer","off","office","okinawa","olayan","olayangroup","oldnavy","ollo","om","omega","one","ong","onl","online","onyourside","ooo","open","oracle","orange","org","organic","origins","osaka","otsuka","ott","ovh","pa","page","panasonic","panerai","paris","pars","partners","parts","party","passagens","pay","pccw","pe","pet","pf","pfizer","pg","ph","pharmacy","phd","philips","phone","photo","photography","photos","physio","piaget","pics","pictet","pictures","pid","pin","ping","pink","pioneer","pizza","pk","pl","place",
            "play","playstation","plumbing","plus","pm","pn","pnc","pohl","poker","politie","porn","post","pr","pramerica","praxi","press","prime","pro","prod","productions","prof","progressive","promo","properties","property","protection","pru","prudential","ps","pt","pub","pw","pwc","py","qa","qpon","quebec","quest","qvc","racing","radio","raid","re","read","realestate","realtor","realty","recipes","red","redstone","redumbrella","rehab","reise","reisen","reit","reliance","ren","rent","rentals","repair","report","republican","rest","restaurant","review","reviews","rexroth","rich","richardli","ricoh","rightathome","ril","rio","rip","rmit","ro","rocher","rocks","rodeo","rogers","room","rs","rsvp","ru","rugby","ruhr","run","rw","rwe","ryukyu","sa","saarland","safe","safety","sakura","sale","salon","samsclub","samsung","sandvik","sandvikcoromant","sanofi","sap","sapo","sarl","sas","save","saxo","sb","sbi","sbs","sc","sca","scb","schaeffler","schmidt","scholarships","school","schule","schwarz","science","scjohnson","scor","scot","sd","se","search","seat","secure","security","seek","select","sener","services","ses","seven","sew","sex","sexy","sfr","sg","sh","shangrila","sharp","shaw","shell","shia","shiksha","shoes","shop","shopping","shouji","show","showtime","shriram","si","silk","sina","singles",
            "site","sj","sk","ski","skin","sky","skype","sl","sling","sm","smart","smile","sn","sncf","so","soccer","social","softbank","software","sohu","solar","solutions","song","sony","soy","space","spiegel","sport","spot","spreadbetting","sr","srl","srt","st","stada","staples","star","starhub","statebank","statefarm","statoil","stc","stcgroup","stockholm","storage","store","stream","studio","study","style","su","sucks","supplies","supply","support","surf","surgery","suzuki","sv","swatch","swiftcover","swiss","sx","sy","sydney","symantec","systems","sz","tab","taipei","talk","taobao","target","tatamotors","tatar","tattoo","tax","taxi","tc","tci","td","tdk","team","tech","technology","tel","telecity","telefonica","temasek","tennis","teva","tf","tg","th","thd","theater","theatre","tiaa",
            "tickets","tienda","tiffany","tips","tires","tirol","tj","tjmaxx","tjx","tk","tkmaxx","tl","tm","tmall","tn","to","today","tokyo","tools","top","toray","toshiba","total","tours","town","toyota","toys","tr","trade","trading","training","travel","travelchannel","travelers","travelersinsurance","trust","trv","tt","tube","tui","tunes","tushu","tv","tvs","tw","tz","ua","ubank","ubs","uconnect","ug","uk","unicom","university","uno","uol","ups","us","uy","uz","va","vacations","vana","vanguard","vc","ve","vegas","ventures","verisign","versicherung","vet","vg","vi","viajes","video","vig","viking","villas","vin","vip","virgin","visa","vision","vista","vistaprint","viva","vivo","vlaanderen","vn","vodka","volkswagen","volvo","vote","voting","voto","voyage","vu","vuelos","wales","walmart","walter","wang","wanggou","warman","watch","watches","weather","weatherchannel","webcam","weber","website","wed","wedding","weibo","weir","wf","whoswho","wien","wiki","williamhill","win","windows","wine","winners","wme","wolterskluwer","woodside","work","works","world","wow","ws","wtc","wtf","xbox","xerox","xfinity","xihuan","xin","xyz","yachts","yahoo","yamaxun","yandex","ye","yodobashi","yoga","yokohama","you","youtube","yt","yun","za","zappos","zara","zero","zip","zippo","zm","zone","zuerich","zw"};
    private static final String[] protocols = {"http", "https"};
    private static final String searchSite = "http://frogfind.com/?q=";


    public String sanitizeUrl(String url){
        boolean containsTld = false;
        boolean containsProtocol = false;
        for(String tld : tlds){
            if(url.contains(tld)){
                containsTld = true;
                break;
            }
        }
        for(String protocol : protocols){
            if(url.contains(protocol)){
                containsProtocol = true;
                break;
            }
        }
        if(!containsTld){
            url = searchSite + url;
        }else if(!containsProtocol){
            url = protocols[0] + url;
        }
        try{
            URI uri = new URI(url);
            url = uri.toString();
        } catch (URISyntaxException e) {
            url = null;
        //} catch (UnsupportedEncodingException e) {
        //    e.printStackTrace();
        }
        return url;

    }
    private static final String[] VALID_TAGS = {"div", "base", "a", "abbr", "address", "article", "aside", "audio", "b", "bdi", "bdo", "blockquote", "body", "br", "button", "caption", "center", "cite", "code", "col", "colgroup", "dd", "del", "details", "dfn", "dialog", "dl", "em", "fieldset", "figure", "footer", "form", "font", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "html", "i", "input", "ins", "keygen", "legend", "li", "link", "main", "mark", "menu", "menuitem", "meter", "nav", "noscript", "object", "ol", "optgroup", "option", "output", "p", "param", "pre", "progress", "q", "rb", "rp", "rt", "rtc", "ruby", "s", "samp", "section", "select", "small", "source", "span", "strong", "sub", "summary", "sup", "table", "tbody", "td", "template", "textarea", "tfoot", "th", "thead", "time", "title", "tr", "track", "u", "ul", "wbr", "img"};
    private static final String[] VALID_ATTRIBUTES = {"title", "alt", "href", "width", "height", "cellpadding", "cellspacing", "border", "bgcolor", "valign", "align", "halign", "colspan", "size", "color", "action", "method", "type", "size", "name", "value", "alink", "link", "text", "vlink", "checked", "maxlength", "for", "start", "selected", "valuetype", "multiple", "rules", "summary", "headers", "align", "bgcolor", "char", "charoff", "height", "scope", "valign", "width", "color", "face", "span", "datetime", "cols", "rows", "readonly", "label", "nowrap", "align", "border", "char", "cite", "compact", "disabled", "longdesc", "name", "value", "valign", "vspace"};

    static String sanitizeHtml(String input){
        //StringEscapeUtils.unescapeJava(downloadedHTML[0])
        Safelist list = new Safelist();
        list.addProtocols("a", "href", "http", "https", "#");
        list.addTags(VALID_TAGS);
        //for(String tag : VALID_TAGS){
        //    list.addAttributes(tag, VALID_ATTRIBUTES);
        //}
        list.addAttributes(":all", VALID_ATTRIBUTES);

        Cleaner c = new Cleaner(list);

        List<String> childList = new ArrayList<>();
        NodeVisitor myNodeVisitor = new MyNodeVisitor(childList);

        //Log.i("HTML", "Input HTML: " + input);

        Document base = Jsoup.parse(input);
        base.charset(StandardCharsets.UTF_8);
        base = c.clean(base);
        //Log.i("HTML", "After clean HTML: " + base.outerHtml());

        Elements tags = base.getAllElements();
        for (Element e : tags) {
            if (e.tagName().equals("div")) {
                e.unwrap();
            }
        }
        //Log.i("HTML", "After unwrap: " + base.outerHtml());

        base.traverse(myNodeVisitor);

        //Log.i("HTML", "After visiting: " + base.outerHtml());


        String outerHTML = base.outerHtml();
        HtmlCompressor comp = new HtmlCompressor();
        comp.setRemoveComments(true);
        comp.setRemoveQuotes(false);
        comp.setRemoveStyleAttributes(true);
        comp.setRemoveIntertagSpaces(true);
        comp.setEnabled(true);

        //Log.i("HTML", "After compress: " + comp.compress(outerHTML));

        return comp.compress(outerHTML);

    }
    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (this.getClass().getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public class LocalBinder extends Binder {
        TxtNetServerService getService() {
            return TxtNetServerService.this;
        }
    }
}
class MyNodeVisitor implements NodeVisitor{
    public MyNodeVisitor(List<String> childList) {
        //for now do nothing
    }

    @Override
    public void head(Node node, int depth) {
        if(node instanceof Comment || node instanceof DocumentType){
            node.remove(); // no need for any extra characters
        }else if(node instanceof Element){
            Element e = (Element)node;
                for(Attribute attr : e.attributes()){
                    if(e.tagName().equals("img") && attr.toString().startsWith("src")){
                        e.attributes().remove(attr.getKey());
                    }
                    else if(attr.getKey().equals("src") || attr.getKey().equals("action")){
                        String value = attr.getValue();
                        if(!value.startsWith("http") && !value.startsWith("www") && value.charAt(0) != '/'){
                            //convert relative url to correct format
                            attr.setValue("./" + attr.getValue());

                        }else if(!value.startsWith("http") && !value.startsWith("www")){
                            attr.setValue("." + attr.getValue());
                        }
                    }
                }


        }
    }

}


