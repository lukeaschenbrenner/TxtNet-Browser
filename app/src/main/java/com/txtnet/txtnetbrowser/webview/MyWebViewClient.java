package com.txtnet.txtnetbrowser.webview;

import android.net.http.SslError;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.messaging.TextMessage;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;

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

        TextMessageHandler handler = TextMessageHandler.getInstance();

//        if(!url.contains("http") && !url.contains("//") && !url.contains("STOP") && !url.contains("unstop") && !url.contains("Website Cancel"))
//            url = view.getUrl() + url;
        TextMessage.url = url;

        handler.sendTextMessage(url);

        s.UpdateMyText(url);

        return true;
    }
//    @Override
//    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//        handler.proceed(); // Ignore SSL certificate errors
//    }
}