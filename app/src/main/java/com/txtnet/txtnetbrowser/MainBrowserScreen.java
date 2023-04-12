package com.txtnet.txtnetbrowser;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.txtnet.brotli4droid.Brotli4jLoader;
import com.txtnet.txtnetbrowser.blockingactivities.UnsupportedDeviceActivity;
import com.txtnet.txtnetbrowser.database.DBInstance;
import com.txtnet.txtnetbrowser.messaging.TextMessage;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;
import com.txtnet.txtnetbrowser.phonenumbers.ServerPickerActivity;
import com.txtnet.txtnetbrowser.server.ServerDisplay;
import com.txtnet.txtnetbrowser.util.AndroidLogFormatter;
//import com.txtnet.txtnetbrowser.util.EncodeFile;
import com.txtnet.txtnetbrowser.webview.MyWebView;
import com.txtnet.txtnetbrowser.webview.MyWebViewClient;

import android.content.ClipboardManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainBrowserScreen extends AppCompatActivity {
    /**
     * TODO: Add custom CSS files for commonly visited websites to save on space
     * TODO: maybe include app version in SMS sending so if the server changes we can accommodate
     * TODO: allow submitting basic web forms as post request
     * TODO: Be able to load previously requested web pages by reading messages from number, no default sms perms required
     * TODO: CHECK MEDIUM ARTICLE FOR GETTING SMS PERMISSIONS, IMPLEMENT THE DIALOG BOX SYSTEM?
     *
     * TODO 2/17/23: Replace loading screens from new WebView pages to an actual progress screen, to avoid spamming webview queue and allow for easy back button
     * TODO: Add database query view depending on country code, by contacting a master list number to return a list of known active server phone numbers for the country code
     * TODO: In phone number selector, make a FrameLayout with the textview, ping button, and checkmark icon (maybe the checkmark or x icon pushes the textview to the right?)
     *
     * TODO Before alpha launch:
     * - Add a CDMA network compatibility mode to remove all Greek symbols. How to communicate this?
     *      -- make a database of CDMA-only numbers and use an initial request text: "TxtNet vXXX charset basic"/"TxtNet vXXX charset full"
     *
     */

    public static MyWebView webView;
    public static SwipeRefreshLayout swipe;
    EditText urlEditText;
    public static ProgressBar progressBar;
    public static FrameLayout progressIndicatorBg;
    public static CircularProgressIndicator progressCircle;
    ImageButton back, forward, stop, refresh, homeButton, goButton;
    private static String[] permissions = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_PHONE_STATE};

    private static final int[] PERMISSIONS_REQUEST_ID = {1, 2, 3, 4, 5};
    public static SharedPreferences preferences;
    public static Context mContext;
    public static Logger rootLogger;
    ActivityResultLauncher<String> mGetContent;

    void showIntroActivity() {
        Intent intent = new Intent(this, AppIntroActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Log.d("APP", "STARTED INTRO");
        ActivityCompat.finishAffinity(MainBrowserScreen.this);

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


 //       if (savedInstanceState == null) { TODO: See if we should replace this null check?

            preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
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

            //Instantiate database
            DBInstance.getInstance(this);

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


            boolean isTosAccepted = preferences.getBoolean(getString(R.string.is_tosaccepted), false);
            if (!isTosAccepted) {
                TermsConditionsDialogFragment dialogFragment = new TermsConditionsDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), "terms");
            }


            urlEditText = (EditText) findViewById(R.id.web_address_edit_text);
            back = (ImageButton) findViewById(R.id.back_arrow);
            forward = (ImageButton) findViewById(R.id.forward_arrow);
            stop = (ImageButton) findViewById(R.id.stop);
            goButton = (ImageButton) findViewById(R.id.go_button);
            refresh = (ImageButton) findViewById(R.id.refresh);
            homeButton = (ImageButton) findViewById(R.id.home);
            progressBar = (ProgressBar) findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.GONE);
            progressCircle = (CircularProgressIndicator) findViewById(R.id.progress_circular);
            progressCircle.setVisibility(View.GONE);
            webView = findViewById(R.id.web_view);
//        webView.loadUrl("file:///android_asset/testfile.html");
            swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);
            progressIndicatorBg = (FrameLayout) findViewById(R.id.progress_indicator);

      //      mGetContent = registerForActivityResult(new EncodeFile(getApplicationContext()), new ActivityResultCallback<Uri>() {
      //          @Override
      //          public void onActivityResult(Uri uri) {
      //              // Handle the returned Uri
      //          }
      //      });
      //  }

        TextMessageHandler handler = TextMessageHandler.getInstance(preferences.getString(getResources().getString(R.string.phone_number), getResources().getString(R.string.default_phone)));
        Uri startIntentData = getIntent().getData();

        if (startIntentData != null) {
            String intentUrl = startIntentData.toString();
            if (intentUrl.contains("http://") || intentUrl.contains("https://")) {  // checking if the intent's data was meant to be a url
                startWebView(intentUrl);
                //load intentUrl website
            } else {
                //the app was opened before
                //webView.restoreState(savedInstanceState);
                //doing this with a separate method (below), not needed here
            }
        } else {
            startWebView(null);
            //TODO: Replace with markdown welcome page
        }


        if (BuildConfig.DEBUG) {

            File destinationFolder = this.getExternalFilesDir(null);



            rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
            File file = new File(destinationFolder, "logFile.txt");
            FileHandler handlerLog = null;
            try {
                handlerLog = new FileHandler(file.getAbsolutePath(), 5 * 1024 * 1024/*5Mb*/, 1, true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            handlerLog.setFormatter(new AndroidLogFormatter(file.getAbsolutePath(),""));

            rootLogger.addHandler(handlerLog);
            rootLogger.setUseParentHandlers(false);

            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    paramThrowable.printStackTrace(pw);
                    try {
                        sw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    rootLogger.log(Level.SEVERE, sw.toString());
                    System.exit(2);
                }
            });


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
                //if (webView.canGoForward()) {
                //    webView.goForward();
                //}


               // mGetContent.launch("encoded.br");
                Brotli4jLoader.ensureAvailability();
                Log.e("hereswhatsup", "e: " + String.valueOf(Brotli4jLoader.isAvailable()));
                //UseBrotliTest test = new UseBrotliTest();
           //     Log.e("shizukualive", String.valueOf(Shizuku.pingBinder())); // "Normal apps should use listeners rather calling this method everytime"

                //test.createFile();

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(MainBrowserScreen.this, "WARNING: The stop button is not fully functional. Please wait a minute for all messages to send.", Toast.LENGTH_SHORT).show();
                webView.stopLoading();
                assert TextMessageHandler.getInstance() != null;
                TextMessageHandler.getInstance().sendTextMessage("Website Cancel");
                TextMessageHandler.getInstance().sendTextMessage("STOP");
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TextMessageHandler.getInstance().sendTextMessage("unstop");
                    }
                }, 5000);

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
            assert TextMessageHandler.getInstance() != null;
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
        String phoneNum = preferences.getString(getResources().getString(R.string.phone_number), null);
        Log.i("phonenum", "phonenum=" + (phoneNum == null ? "null" : phoneNum));
        if(phoneNum == null){

            Intent intent = new Intent(this, ServerPickerActivity.class);
            intent.putExtra("needDefault", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            ActivityCompat.finishAffinity(MainBrowserScreen.this);
            //make Server Picker view the root view until a default phone number is selected
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
//                    case R.id.selectPhoneNumber:
//                        String phoneNumber = preferences.getString(getResources().getString(R.string.phone_number), "0");
//                        //String phoneNumber = preferences.getString(getResources().getString(R.string.phone_number), null);
//
//
//                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
//                        builder.setTitle("Phone Number (no dashes)");
//                        View viewInflated = LayoutInflater.from(v.getContext()).inflate(R.layout.phone_select_dialog, (ViewGroup) v.getParent(), false);
//                        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
//                        input.setText(phoneNumber);
//                        builder.setView(viewInflated);
//
//
//                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
//                                SharedPreferences.Editor edit = preferences.edit();
//                                edit.putString(getString(R.string.phone_number), input.getText().toString());
//                                edit.apply();
//
//                            }
//                        });
//                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//
//                        builder.show();
//                        return true;
                    case R.id.TxtNetServer:
                        Intent intent2 = new Intent(v.getContext(), ServerDisplay.class);
                        startActivity(intent2);
                        return true;
                    case R.id.serverPhoneSelect:
                        Intent intent3 = new Intent(v.getContext(), ServerPickerActivity.class);
                        startActivity(intent3);
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
        progressCircle.setMax(total);
        progressBar.setMax(total);
        progressBar.setProgress(newProgress);
        progressCircle.setProgress(newProgress);

        if (newProgress < total && progressBar.getVisibility() == ProgressBar.GONE) {
            progressIndicatorBg.setVisibility(FrameLayout.VISIBLE);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            progressCircle.setVisibility(CircularProgressIndicator.VISIBLE);
        }
        if (newProgress >= total) {
            progressCircle.setVisibility(CircularProgressIndicator.GONE);
            progressBar.setVisibility(ProgressBar.GONE);
            progressIndicatorBg.setVisibility(View.GONE);

            swipe.setRefreshing(false);
        } else {
            progressIndicatorBg.setVisibility(FrameLayout.VISIBLE);
            progressCircle.setVisibility(CircularProgressIndicator.VISIBLE);
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        //webView.loadData("<br><br><br><center><h1>Loading...</h1></center><br><center><h2>(" + newProgress + " of " + total + ")</h2></center>", "text/html; charset=utf-8", "UTF-8");
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
                assert TextMessageHandler.getInstance() != null;
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

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}
