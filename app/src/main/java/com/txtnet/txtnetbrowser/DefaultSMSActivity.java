package com.txtnet.txtnetbrowser;


import static com.txtnet.txtnetbrowser.R.string.DSAMsg1;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg10;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg11;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg12;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg14;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg15;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg17;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg3;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg4;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg5;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg6;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg7;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg8;
import static com.txtnet.txtnetbrowser.R.string.DSAMsg9;

import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.provider.BlockedNumberContract;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import com.txtnet.txtnetbrowser.blockingactivities.UnsupportedBlockActivity;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;

public class DefaultSMSActivity extends AppCompatActivity {

    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        String currentDefault = Telephony.Sms.getDefaultSmsPackage(this);
        boolean isDefault = getPackageName().equals(currentDefault);
        Toast.makeText(getApplicationContext(), getString(DSAMsg1) + (isDefault ? getString(R.string.DSAMsg2) : getString(DSAMsg3)), Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defaultsms);

        Button button1 = (Button) findViewById(R.id.setdefaultsms);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //openSMSappChooser(v.getContext());

                RoleManager roleManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    roleManager = getApplicationContext().getSystemService(RoleManager.class);
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                        if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                            Toast.makeText(getApplicationContext(), DSAMsg4, Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                            startActivity(i);
                        } else {
                            Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);


                            Toast.makeText(getApplicationContext(), DSAMsg5, Toast.LENGTH_LONG).show();

                            startActivityForResult(roleRequestIntent, 2); // we use the onActivityResult callback to find out if the user actually did this!


                        }
                    }
                }else {
                    if(!isDefaultSmsApp(v.getContext())){

                        getPackageManager()
                                .setComponentEnabledSetting(new ComponentName(v.getContext(), SMSActivities.SmsReceiver.class),
                                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                        PackageManager.DONT_KILL_APP);

                        String myPackageName = getPackageName();
                        Intent setSmsAppIntent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                        setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
                        Toast.makeText(getApplicationContext(), DSAMsg6, Toast.LENGTH_LONG).show();

                        startActivity(setSmsAppIntent);


                    }else{

                        getPackageManager()
                                .setComponentEnabledSetting(new ComponentName(v.getContext(), SMSActivities.SmsReceiver.class),
                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                        PackageManager.DONT_KILL_APP);
                        Snackbar.make(v, DSAMsg7, Snackbar.LENGTH_LONG).setAction("OK", null).show();

                    }

                }
            }
        });
//delete all sms
        Button button2 = (Button) findViewById(R.id.deleteallsms);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTON2", "PRESSED");
                if(!isDefaultSmsApp(v.getContext())){
                    Snackbar.make(v, DSAMsg8, Snackbar.LENGTH_LONG).setAction("OK", null).show();
                    return;
                }
                //DELETE ALL SMS
                PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber pn = null;
                try {
                    pn = pnu.parse(TextMessageHandler.PHONE_NUMBER, "US");
                } catch (NumberParseException e) {
                    e.printStackTrace();
                }
                String pnE164 = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164);
                String national = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                new Thread(new Runnable() {
                    public void run() {
                        Looper.prepare();
                        for (;;) { //as you can see, this isn't very thread safe! TODO: fix thread safety
                            // restart cursor before each delete
                            String messageid = null;
                            String numberFilter = "address='"+ pnE164 + "'";
                            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"),
                                    null, numberFilter, null, null);
                            //TODO: Content provider URI is not constant across all Android devices. Should obtain cursor programatically instead.
                            /*
                                ContentResolver cr = context.getContentResolver();
                                Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
                            */

                            if (!cursor.moveToFirst()) {
                                break; // nothing more to delete
                            }

                            // delete single record
                            messageid = cursor.getString(0);
                            int rowsDeleted = getContentResolver().delete(Uri.parse("content://sms/" + messageid), null, null);

                        }
                        Snackbar.make(v, DSAMsg9, Snackbar.LENGTH_LONG).setAction("OK", null).show();

                    }}).start();

            }
        });
//block number -- if doesn't work, should ask user to hide notifications/try and block by themselves.
        Button button3 = (Button) findViewById(R.id.block);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!isDefaultSmsApp(v.getContext())){
                    Snackbar.make(v, DSAMsg10, Snackbar.LENGTH_LONG).setAction("OK", null).show();
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                    assert BlockedNumberContract.canCurrentUserBlockNumbers(v.getContext()); //has to be true!


                    ContentValues values = new ContentValues();

                    PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
                    Phonenumber.PhoneNumber pn = null;
                    try {
                        pn = pnu.parse(TextMessageHandler.PHONE_NUMBER, "US");
                    } catch (NumberParseException e) {
                        e.printStackTrace();
                    }
                    String pnE164 = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164);
                    String national = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                    String intl = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

                    values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, intl);
                    values.put(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER, pnE164);
                    Uri uri = getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
                    Snackbar.make(v, DSAMsg11, Snackbar.LENGTH_LONG).setAction("OK", null).show();

                }else{
                    Snackbar.make(v, DSAMsg12, Snackbar.LENGTH_LONG).setAction(R.string.DSAMsg13, new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(v.getContext(), UnsupportedBlockActivity.class);
                            startActivity(intent);
                        }
                    }).show();

                }
            }
        });
//unblock number
        Button button4 = (Button) findViewById(R.id.unblock);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!isDefaultSmsApp(v.getContext())){
                    Snackbar.make(v, DSAMsg14, Snackbar.LENGTH_LONG).setAction("OK", null).show();
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                    assert BlockedNumberContract.canCurrentUserBlockNumbers(v.getContext()); //has to be true!

                    ContentValues values = new ContentValues();

                    PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
                    Phonenumber.PhoneNumber pn = null;
                    try {
                        pn = pnu.parse(TextMessageHandler.PHONE_NUMBER, "US");
                    } catch (NumberParseException e) {
                        e.printStackTrace();
                    }
                    String pnE164 = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164);
                    String national = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                    new Thread(new Runnable() {
                        public void run() {
                            Looper.prepare();

                            Log.d("DEFAULT", "Unblock process begin.");
                            int result = BlockedNumberContract.unblock(v.getContext(), pnE164);
                            if(result > 0){
                                Snackbar.make(v, DSAMsg15, Snackbar.LENGTH_LONG).setAction("OK", null).show();
                                Log.d("DEFAULT:", "Unblock success");

                            }else{
                                Snackbar.make(v, R.string.DSAMsg16, Snackbar.LENGTH_LONG).setAction("OK", null).show();
                                Log.d("DEFAULT:", "Unblock fail");
                            }
                        }}).start();


                }else{
                    Snackbar.make(v, DSAMsg17, Snackbar.LENGTH_LONG).setAction(R.string.DSAMsg18, new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(v.getContext(), UnsupportedBlockActivity.class);
                            startActivity(intent);
                        }
                    }).show();

                }
            }
        });
    }


    public static boolean isDefaultSmsApp(Context context) {
        return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
    }

}
