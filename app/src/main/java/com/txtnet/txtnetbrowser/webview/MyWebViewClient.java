package com.txtnet.txtnetbrowser.webview;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.txtnet.txtnetbrowser.MainBrowserScreen;
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

        handler.sendTextMessage(url);


        s.UpdateMyText(url);

        return true;
    }
}