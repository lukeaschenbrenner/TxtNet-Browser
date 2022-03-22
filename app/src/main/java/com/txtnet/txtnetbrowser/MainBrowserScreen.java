package com.txtnet.txtnetbrowser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import android.content.Context;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.txtnet.txtnetbrowser.messaging.TextMessage;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;
import com.txtnet.txtnetbrowser.webview.MyWebViewClient;

public class MainBrowserScreen extends AppCompatActivity {
    /** TODO: Add custom CSS files for commonly visited websites to save on space *
     TODO: fancy encoded post request, maybe include app version
     TODO: allow submitting basic web forms as post request
     TODO: Be able to load previously requested web pages by reading messages from number, no default sms perms required*/

    WebView webView;
    SwipeRefreshLayout swipe;
    EditText editText;
    ProgressBar progressBar;
    ImageButton back, forward, stop, refresh, homeButton;
    Button goButton;
    private static final int[] PERMISSIONS_REQUEST_ID = {1, 2, 3, 4, 5};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS};
            for(int i = 0; i < permissions.length; i++){
                if(checkSelfPermission(permissions[i]) == PackageManager.PERMISSION_DENIED){
                    ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_ID[i]);
                }
            }
        }

        TextMessageHandler handler = TextMessageHandler.getInstance();

        setContentView(R.layout.activity_main);


        Uri startIntentData = getIntent().getData();


        editText = (EditText) findViewById(R.id.web_address_edit_text);
        back = (ImageButton) findViewById(R.id.back_arrow);
        forward = (ImageButton) findViewById(R.id.forward_arrow);
        stop = (ImageButton) findViewById(R.id.stop);
        goButton = (Button) findViewById(R.id.go_button);
        refresh = (ImageButton) findViewById(R.id.refresh);
        homeButton = (ImageButton) findViewById(R.id.home);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        webView = (WebView) findViewById(R.id.web_view);
//        webView.loadUrl("file:///android_asset/testfile.html");
        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);

        if (startIntentData != null) {
            String intentUrl = startIntentData.toString();
            if (intentUrl.contains("http://") || intentUrl.contains("https://")) {  // checking if the intent's data was meant to be a url
                startWebView(intentUrl);
                //load intentUrl website
            } else {
                //the app was opened before
                webView.restoreState(savedInstanceState);
            }
        } else {
            startWebView(null);
        }
    }
        public void startWebView(String url){

            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setSupportZoom(true);
            webView.getSettings().setSupportMultipleWindows(true);
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            webView.setBackgroundColor(Color.WHITE);

            swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    webView.reload();
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
                    progressBar.setProgress(newProgress);
                    if (newProgress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                    if (newProgress == 100) {
                        progressBar.setVisibility(ProgressBar.GONE);
                        swipe.setRefreshing(false);
                    } else {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                }
            });
        webView.setWebViewClient(new CosmosWebViewClient(this));
        webView.setWebViewClient(new MyWebViewClient());


            urlEditText.setOnEditorActionListener(
                    new EditText.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
// If triggered by an enter key, close the keyboard and load the site
                            if (actionId == EditorInfo.IME_ACTION_GO) {
                                hideAllExpandableViews(MainBrowserScreen.this);
                                hideKeyboard(MainBrowserScreen.this);
                                //?????? do we need hidekeyboard?
                                MainBrowserScreen.webView.requestFocus();

                                try {
                                    TextMessage.url = urlEditText.getText().toString();
                                    handler.sendTextMessage(urlEditText.getText().toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return true;
                            }
                            return false;
                        }
                    });

            urlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean isFocused) {
                    if(isFocused){
                        urlEditText.setSelection(urlEditText.getText().length());
                        searchBar.getChildAt(1).setVisibility(View.GONE);
                        searchBar.getChildAt(2).setVisibility(View.GONE);
                        hideAllExpandableViews(MainBrowserScreen.this);
                    }
                    else{
                        //This sets the cursor of the edit text back to the front so that the url is visible when focus changes.
                        urlEditText.setSelection(0);
                        searchBar.getChildAt(1).setVisibility(View.VISIBLE);
                        searchBar.getChildAt(2).setVisibility(View.VISIBLE);
                    }
                }
            });







            goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (!NetworkState.connectionAvailable(MainBrowserScreen.this)) {
                        Toast.makeText(MainBrowserScreen.this, R.string.check_connection, Toast.LENGTH_SHORT).show();
                    } else {

                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        webView.loadUrl("https://" + editText.getText().toString());
                        editText.setText("");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.stopLoading();
            }
        });

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl("http://google.com");
            }
        });

        if(url != null){
            webView.loadUrl(url);
        }
    }

    public void UpdateMyText(String mystr) {
        urlEditText.setText(mystr);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(grantResults.length == 0) {
            Toast.makeText(this, "No permissions granted!", Toast.LENGTH_SHORT).show();
            return;
        }
        for(int i = 0; i < grantResults.length; i++) {
            if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(this, "Permission error: not granted: " + permissions[i], Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Permissions are accepted! App is now functional.", Toast.LENGTH_SHORT).show();

        //enableApp here... we know the permissions are granted by this line.
    }

    public void onResume(){
        findViewById(R.id.rootWebView).requestFocus();
        super.onResume();
    }

    public void clickedSettings(View v){
     /*   moreOptionsView.setVisibility(View.INVISIBLE);
        int visibility = tabsListView.getVisibility();
        if(visibility == View.VISIBLE){
            tabsListView.setVisibility(View.INVISIBLE);
        }
        else{
            tabsListView.setVisibility(View.VISIBLE);
        }
        */
        Intent intent = new Intent(this, DefaultSMSActivity.class);
        startActivity(intent);


    }

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.actions);
        popup.show();
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());
        popup.show();
    }

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.actions);
        popup.show();
    }

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.actions);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.archive:
                archive(item);
                return true;
            case R.id.delete:
                delete(item);
                return true;
            default:
                return false;
        }
    }



}
