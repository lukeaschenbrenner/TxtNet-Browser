package com.txtnet.txtnetbrowser.webview;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;

import com.txtnet.txtnetbrowser.R;

public class MyWebView extends WebView {
    //private static final String TAG = "CosmosWebView";



    private WebViewClient webViewClient;

    public MyWebView(Context context) {
        super(context);
        this.setWebViewClient(webViewClient);

        getSettings().setJavaScriptEnabled(false);
        getSettings().setUseWideViewPort(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setSupportZoom(true);
        //getSettings().setBlockNetworkLoads(true);
        setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        setBackgroundColor(Color.WHITE);
        getSettings().setDomStorageEnabled(true);
        getSettings().setAllowFileAccess(true);
        getSettings().setSupportMultipleWindows(false);
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setWebViewClient(webViewClient);
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setWebViewClient(webViewClient);
    }

   // @Override
   // public void goBack() {
   //     ListView settingsListView = (ListView)findViewById(R.id.settingsListView);
   //     if(settingsListView.getVisibility() == View.VISIBLE){
   //         settingsListView.setVisibility(View.INVISIBLE);
   //     }
    //    else{
    //        super.goBack();
    //    }
   // }
}