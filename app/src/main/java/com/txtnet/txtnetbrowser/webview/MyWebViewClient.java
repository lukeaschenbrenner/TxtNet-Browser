package com.txtnet.txtnetbrowser.webview;

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
        }else if(url.equals("https://home/")){
            //this code block is likely never run
            view.getSettings().setJavaScriptEnabled(true);
            view.loadUrl("file:///android_asset/dashboard/index.html");
            return true;
        }
        Log.e(MyWebViewClient.class.getName(), "Unknown scheme URL loaded, preventing the load.");
        return true;
    }

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
                            s.getAssets().open("dashboard/" + url.substring("https://home/".length())));
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