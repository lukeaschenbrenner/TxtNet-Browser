package com.txtnet.txtnetbrowser.server;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;

public class ServerWebViewClient extends WebViewClient {
        private long startTime;
        private static final String TAG = "ServerWebViewClient";
        private boolean isRedirected;
        private TxtNetServerService service;
        public ServerWebViewClient(TxtNetServerService service){
            this.service = service;
        }
        private boolean webViewSuccess = true;
  //  public ServerWebViewClient(WebView[] webViews) {
  //      super();
  //      for (WebView webView : webViews) {
  //          this.webViews.put(webView, new AtomicBoolean(false));
  //      }
  //  }

    @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.w(TAG, "Recieved error from WebView, description: " + description + ", Failing url: " + failingUrl);
            webViewSuccess = false;
            timeout = false;
            service.pageLoadFailed(view, false);

            //without this method, your app may crash...
        }
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                SslError error) {

            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    Log.d("SslError", "The certificate authority is not trusted.");
                    break;
                case SslError.SSL_EXPIRED:
                    Log.d("SslError", "The certificate has expired.");
                    break;
                case SslError.SSL_IDMISMATCH:
                    Log.d("SslError", "The certificate Hostname mismatch.");
                    break;
                case SslError.SSL_NOTYETVALID:
                    Log.d("SslError", "The certificate is not yet valid.");
                    break;
            }
            handler.proceed();
        }

        boolean timeout = true;
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
         //   try{
         //       Objects.requireNonNull(TxtNetServerService.webViewBusynessMap.get(view)).set(true);
         //   }catch(NullPointerException npe){
         //       npe.printStackTrace();
         //   }
            Log.e(TAG, "Page started with URL " + url);
            isRedirected = false;
            //startTime = System.nanoTime();
            Runnable run = new Runnable() {
                public void run() {
                    if(timeout) { // give up after 15 seconds
                        view.stopLoading(); //TODO: does this method call also call onPageFinished?
                        service.exportHTMLFromWebsite(view);
                    }
                }
            };
            timeout = true;

            HandlerThread thread = new HandlerThread("SmsProcessing",
                    Process.THREAD_PRIORITY_DEFAULT);
            thread.start();
            Looper serviceLooper = thread.getLooper();
            Handler smsExportHandler = new Handler(serviceLooper);
            smsExportHandler.postDelayed(run, 15000);
            // OR, Get the HandlerThread's Looper and use it for our Handler
          //  Handler myHandler = new Handler(Looper.myLooper());
          //  myHandler.postDelayed(run, 15000);
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            timeout = false;
            //    takeWebviewScreenshot(intent.getStringExtra(Database.THUMBNAIL));
            Log.e("Page completion", url + " " + view.getProgress() + "%");
            if ((!isRedirected && view.getProgress() >= 100) && webViewSuccess) {
                // if we overrode the URL load because of redirect, then onPageFinished will be called immediately so we need to check that progress is complete.
                // if progress still isn't complete after 15 seconds, just move on.
                //Do something you want when finished loading

                Thread th = new Thread(() -> service.exportHTMLFromWebsite(view));
                th.start();
            }
            webViewSuccess = true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest (final WebView view, String url) {
            if (url.contains(".css")) {
                return super.shouldInterceptRequest(view, url);
                //return null;
                //makes all CSS requests return null instead of the actual css
                //Disabled for now in case the JS requires valid CSS to properly render the HTML. Probably safe to uncomment, and would reduce load times.
            } else {
                return super.shouldInterceptRequest(view, url);
            }
        }

        // The following ensures that the onPageFinished method is only called once
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Uri uri = Uri.parse(url);
            //     return handleUri(uri); //false
            HashMap<String, String> headers = new HashMap<String, String>(1);
            headers.put("User-Agent", view.getSettings().getUserAgentString());
            //TODO: Allow user to choose custom user agent as a configurable value to avoid device association
            view.loadUrl(url, headers);
            isRedirected = true;
            return true;
        }


    @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest
        request) {
            final Uri uri = request.getUrl();
            Log.e("URI", uri.toString());
            //        return handleUri(uri); //false

            view.loadUrl(uri.toString());

            isRedirected = true;
            return true;
        }

    public boolean onRenderProcessGone (WebView view,
                                        RenderProcessGoneDetail detail){
            timeout = false;
            service.replaceFailedWebView(view);
            Log.e(TAG, "WebView " + view.toString() + " render process gone. Details: " + detail.toString());
            return true;
    }
/*
        private boolean handleUri(final Uri uri) {
            // Log.i(TAG, "Uri =" + uri);
            final String host = uri.getHost();
            final String scheme = uri.getScheme();
            // Based on some condition you need to determine if you are going to load the url
            // in your web view itself or in a browser.
            // You can use `host` or `scheme` or any part of the `uri` to decide.
            if (true) {
                // Returning false means that you are going to load this url in the webView itself
                //  return false;
                return false;
            } else {  //// PLACEHOLDER CONDITION, WE NEVER NEED TO DO THIS
                // Returning true means that you need to handle what to do with the url
                // e.g. open web page in a Browser
                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            }
        }
        */


}

