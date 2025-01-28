package com.txtnet.txtnetbrowser.webview;

import android.net.http.SslError;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
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

        if(!(url.equals("about:blank") || url.equals("about:blank#blocked") || url.startsWith("file:///"))){
            TextMessage.url = url;
            assert handler != null;
            handler.sendTextMessage(url);
        }
        if(!url.startsWith("file:///")){
            s.UpdateMyText(url);
        }else{
            return false;
        }

        return true;
    }



//    @Override
//    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//        handler.proceed(); // Ignore SSL certificate errors
//    }
}