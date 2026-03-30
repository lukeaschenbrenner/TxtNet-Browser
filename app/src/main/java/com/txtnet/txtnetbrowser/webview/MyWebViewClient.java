package com.txtnet.txtnetbrowser.webview;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.messaging.TextMessage;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class MyWebViewClient extends WebViewClient {
    MainBrowserScreen s;

    public MyWebViewClient(MainBrowserScreen s){
        this.s = s;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if(view == null || url == null){
            Log.e("webviewclient", "ERROR: View or url is null!");
            return false;
        }

        TextMessageHandler handler = null;

        try{
            handler = TextMessageHandler.getInstance();
        }
        catch(NullPointerException npe){
            Log.e(MyWebViewClient.class.getName(), "SMS Handler is null!");
            return true;
        }

        //                webView.clearFormData(); does this do something useful?
//        if(!url.contains("http") && !url.contains("//") && !url.contains("STOP") && !url.contains("unstop") && !url.contains("Website Cancel"))
//            url = view.getUrl() + url;

        List<String> specialUrls = Arrays.asList(new String[]{
                "about:blank", "about:blank#blocked", "file:///", "https://home/"
        });
        if(!startsWithAny(url, specialUrls)){
            TextMessage.url = url;
            assert handler != null;
            handler.sendTextMessage(url);
            s.UpdateMyText(url);
            view.getSettings().setJavaScriptEnabled(false);
            return true;
        }else if(url.startsWith("https://home/")){
            view.getSettings().setJavaScriptEnabled(true);
            String[] attempted_relative_url = url.split("https://home/", 2);
            String resolved_relative_url = attempted_relative_url.length == 2 ? attempted_relative_url[1] : "index.html";
            if (attempted_relative_url.length == 2) {
                view.loadUrl("file:///android_asset/dashboard/" + resolved_relative_url);
                return true;
            }
        }
        Log.e(MyWebViewClient.class.getName(), "Unknown scheme URL loaded, preventing the load.");
        return true;
    }

    /**
     * We must intercept all resource requests (CSS, JS) _only_ for the offline dashboard to trigger loading of tailwindCSS, alpineJS, etc.
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        final CountDownLatch latch = new CountDownLatch(1);
        if (url.startsWith("https://home/")) {
            s.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                 s.webView.getSettings().setJavaScriptEnabled(true);
                 latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                if(url.equals("https://home/")){
                    return new WebResourceResponse(
                            "text/html",
                            "utf-8",
                            s.getAssets().open("dashboard/index.html"));
                }else{
                    return new WebResourceResponse(
                            "text/html",
                            "utf-8",
                            s.getAssets().open("dashboard/" + url.split("https://home/", 2)[1]));
                }

            } catch (IOException e) {
                Log.i(MyWebViewClient.class.getName(), "Assets url missing: " + e.getMessage());
            }
        }
        return null;
    }


    public static boolean startsWithAny(String input, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (input.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

//    @Override
//    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//        handler.proceed(); // Ignore SSL certificate errors
//    }
}