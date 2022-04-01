package com.txtnet.txtnetbrowser;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.IntentCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import com.github.appintro.AppIntroFragment;
import com.txtnet.txtnetbrowser.messaging.TextMessage;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;
import com.txtnet.txtnetbrowser.webview.MyWebView;
import com.txtnet.txtnetbrowser.webview.MyWebViewClient;

import android.content.ClipboardManager;

import java.io.IOException;
import java.io.InputStream;

public class MainBrowserScreen extends AppCompatActivity {
    /**
     * TODO: Add custom CSS files for commonly visited websites to save on space
     * TODO: maybe include app version in SMS sending so if the server changes we can accommodate
     * TODO: allow submitting basic web forms as post request
     * TODO: Be able to load previously requested web pages by reading messages from number, no default sms perms required
     * TODO: CHECK MEDIUM ARTICLE FOR GETTING SMS PERMISSIONS, IMPLEMENT THE DIALOG BOX SYSTEM?
     */

    public static MyWebView webView;
    public static SwipeRefreshLayout swipe;
    EditText urlEditText;
    public static ProgressBar progressBar;
    ImageButton back, forward, stop, refresh, homeButton, goButton;
    private static String[] permissions = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_PHONE_STATE};

    private static final int[] PERMISSIONS_REQUEST_ID = {1, 2, 3, 4, 5};
    public static SharedPreferences preferences;
    public static Context mContext;

    void showIntroActivity() {
        Intent intent = new Intent(this, AppIntroActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Log.d("APP", "STARTED INTRO");
        ActivityCompat.finishAffinity(MainBrowserScreen.this);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences( getPackageName() + "_preferences", MODE_PRIVATE);
        mContext = this;

        boolean isAccessed = preferences.getBoolean(getString(R.string.is_accessed), false);
        if (!isAccessed) {
            SharedPreferences.Editor edit = preferences.edit();
            edit.putBoolean(getString(R.string.is_accessed), Boolean.TRUE);
            edit.apply();
            showIntroActivity();
            return;
        }



        setContentView(R.layout.activity_main);

        boolean missingPerms = false;
        //above is the intro screen, but below we manually check for permissions just in case
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (checkSelfPermission(permissions[i]) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_ID[i]);
                }
            }
            for (int i = 0; i < permissions.length; i++) {
                if (checkSelfPermission(permissions[i]) == PackageManager.PERMISSION_DENIED) {
                    missingPerms = true;
                }
            }
        }


        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) || missingPerms) { //disable the app
            Intent intent = new Intent(this, UnsupportedDeviceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.d("APP", "UNSUPPORTED");
            startActivity(intent);
            ActivityCompat.finishAffinity(MainBrowserScreen.this);
        }



        Uri startIntentData = getIntent().getData();


        urlEditText = (EditText) findViewById(R.id.web_address_edit_text);
        back = (ImageButton) findViewById(R.id.back_arrow);
        forward = (ImageButton) findViewById(R.id.forward_arrow);
        stop = (ImageButton) findViewById(R.id.stop);
        goButton = (ImageButton) findViewById(R.id.go_button);
        refresh = (ImageButton) findViewById(R.id.refresh);
        homeButton = (ImageButton) findViewById(R.id.home);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        webView = findViewById(R.id.web_view);
//        webView.loadUrl("file:///android_asset/testfile.html");
        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);



        TextMessageHandler handler = TextMessageHandler.getInstance();

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
            //TODO: Replace with markdown welcome page
        }
    }

    public void startWebView(String url) {

        registerForContextMenu(webView);

        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (webView.getUrl() != null) {
                    loadUrl(TextMessage.url);
                }
            }
        });

//            webView.setWebChromeClient(new WebChromeClient() {
//                @Override
//                public void onProgressChanged(WebView view, int newProgress) {
//                    super.onProgressChanged(view, newProgress);
//                    progressBar.setProgress(newProgress);
//                    if (newProgress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
//                        progressBar.setVisibility(ProgressBar.VISIBLE);
//                    }
//                    if (newProgress == 100) {
//                        progressBar.setVisibility(ProgressBar.GONE);
//                        swipe.setRefreshing(false);
//                    } else {
//                        progressBar.setVisibility(ProgressBar.VISIBLE);
//                    }
//                }
//            });
        webView.setWebViewClient(new MyWebViewClient(this));


        urlEditText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            loadUrl(urlEditText.getText().toString());
                            return true;
                        }
                        return false;
                    }
                });

        urlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocused) {
                if (isFocused) {
                    urlEditText.setSelection(urlEditText.getText().length());

                } else {
                    //This sets the cursor of the edit text back to the front so that the url is visible when focus changes.
                    urlEditText.setSelection(0);

                }
            }
        });


        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                        //InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        //inputMethodManager.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
                        //TextMessageHandler.getInstance().sendTextMessage(urlEditText.getText().toString());

                        loadUrl(urlEditText.getText().toString());


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
                TextMessageHandler.getInstance().sendTextMessage("Website Cancel");
                TextMessageHandler.getInstance().sendTextMessage("STOP");
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TextMessageHandler.getInstance().sendTextMessage("unstop");
                    }
                }, 1000);

            }
        });

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrl(TextMessage.url);
            }
        });
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                webView.loadUrl("file:///android_asset/welcome.md.html");
                //urlEditText.setText(url);

                //TextMessageHandler.getInstance().sendTextMessage(urlEditText.getText().toString());
            }
        });

        if (url != null) {
            TextMessageHandler.getInstance().sendTextMessage(url);
            urlEditText.setText(url);
        }else{
            webView.loadUrl("file:///android_asset/welcome.md.html");
        }

    }

    public void UpdateMyText(String mystr) {
        urlEditText.setText(mystr);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            Toast.makeText(this, "No permissions granted!", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission error: not granted: " + permissions[i], Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Permissions are accepted! App is now functional.", Toast.LENGTH_SHORT).show();

        //enableApp here... we know the permissions are granted by this line.
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean isAccessed = prefs.getBoolean(getString(R.string.is_accessed), false);
        if (!isAccessed) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(getString(R.string.is_accessed), Boolean.TRUE);
            edit.apply();
            showIntroActivity();
        } else {
            super.onResume();
            findViewById(R.id.web_view).requestFocus();
        }
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
        inflater.inflate(R.menu.menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.options:
                        Intent intent = new Intent(v.getContext(), DefaultSMSActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.selectPhoneNumber:
                        String phoneNumber = preferences.getString(getResources().getString(R.string.phone_number), getResources().getString(R.string.default_phone));

                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle("Phone Number (no dashes)");
                        View viewInflated = LayoutInflater.from(v.getContext()).inflate(R.layout.phone_select_dialog, (ViewGroup) v.getParent(), false);
                        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                        input.setText(phoneNumber);
                        builder.setView(viewInflated);


                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                SharedPreferences.Editor edit = preferences.edit();
                                edit.putString(getString(R.string.phone_number), input.getText().toString());
                                edit.apply();

                            }
                        });
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                        return true;
                    default:
                        return false;
                }
            }
        });


        popup.show();

    }



    //If making a separate activity that implements OnMenuItemClickListener:
//    public void showMenu(View v) {
//        PopupMenu popup = new PopupMenu(this, v);
//
//        // This activity implements OnMenuItemClickListener
//        popup.setOnMenuItemClickListener(onMenuItemClick(popup.));
//        popup.inflate(R.menu.menu);
//        popup.show();
//    }
//


    //OLD CODE DONT USE METHOD
    public boolean clickedOptions(View v) {
     /*   moreOptionsView.setVisibility(View.INVISIBLE);
        int visibility = tabsListView.getVisibility();
        if(visibility == View.VISIBLE){
            tabsListView.setVisibility(View.INVISIBLE);
        }
        else{
            tabsListView.setVisibility(View.VISIBLE);
        }
        */

        return true;

    }

    public static void onProgressChanged(int newProgress, int total) {
        progressBar.setMax(total);
        progressBar.setProgress(newProgress);
        if (newProgress < total && progressBar.getVisibility() == ProgressBar.GONE) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }
        if (newProgress >= total) {
            progressBar.setVisibility(ProgressBar.GONE);
            swipe.setRefreshing(false);
        } else {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }
    }

    public void loadUrl(String urlToLoad){
        hideKeyboard(MainBrowserScreen.this);
        //?????? do we need hidekeyboard?
        MainBrowserScreen.webView.requestFocus();
        try {

            if (!NetworkState.connectionAvailable(this, this)) {
                Toast.makeText(MainBrowserScreen.this, R.string.check_connection, Toast.LENGTH_SHORT).show();
            } else {

                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
                TextMessage.url = urlToLoad;
                TextMessageHandler.getInstance().sendTextMessage(urlToLoad);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        WebView webView = (WebView) v;
        WebView.HitTestResult result = webView.getHitTestResult();

        if (result != null) {
            if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                String linkToCopy = result.getExtra();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", linkToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Link copied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
