package com.txtnet.txtnetbrowser;


import android.app.Activity;
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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.logging.Logger;

import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;

public class DefaultSMSActivity extends Activity {

    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data){

        String currentDefault = Telephony.Sms.getDefaultSmsPackage(this);
        boolean isDefault = getPackageName().equals(currentDefault);
        Toast.makeText(getApplicationContext(), "Is default: " + isDefault, Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defaultsms);

        Button button1 = (Button) findViewById(R.id.setdefaultsms);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTON1", "PRESSED");
                //openSMSappChooser(v.getContext());

                RoleManager roleManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    roleManager = getApplicationContext().getSystemService(RoleManager.class);
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                        if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                            Toast.makeText(getApplicationContext(), "Please select your SMS app to change to.", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                            startActivity(i);
                        } else {
                            Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);

                            String currentDefault = Telephony.Sms.getDefaultSmsPackage(v.getContext());
                            boolean isDefault = getPackageName().equals(currentDefault);
                            Toast.makeText(getApplicationContext(), "Is default: " + isDefault, Toast.LENGTH_SHORT).show();

                            startActivityForResult(roleRequestIntent, 2);


                        }
                    }
                }else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
                    if(!isDefaultSmsApp(v.getContext())){

                        getPackageManager()
                                .setComponentEnabledSetting(new ComponentName(v.getContext(), SMSActivities.SmsReceiver.class),
                                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                        PackageManager.DONT_KILL_APP);

                        String myPackageName = getPackageName();
                        Intent setSmsAppIntent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                        setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
                        startActivity(setSmsAppIntent);


                    }else{
                        Toast.makeText(getApplicationContext(), "Reverting to default SMS app...: ", Toast.LENGTH_SHORT).show();

                        getPackageManager()
                                .setComponentEnabledSetting(new ComponentName(v.getContext(), SMSActivities.SmsReceiver.class),
                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                        PackageManager.DONT_KILL_APP);
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
                    Toast.makeText(getApplicationContext(), "Not the default SMS app...: ", Toast.LENGTH_SHORT).show();
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
                        for (;;) {
                            // restart cursor before each delete
                            String messageid = null;
                            String numberFilter = "address='"+ pnE164 + "'";
                            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"),
                                    null, numberFilter, null, null);

                            if (!cursor.moveToFirst()) {
                                break; // nothing more to delete
                            }

                            // delete single record
                            messageid = cursor.getString(0);
                            int rowsDeleted = getContentResolver().delete(Uri.parse("content://sms/" + messageid), null, null);

                        }
                        Toast.makeText(getApplicationContext(), "Delete complete!", Toast.LENGTH_SHORT).show();

                    }}).start();

            }
        });
//block number -- if doesn't work, should ask user to hide notifications/try and block by themselves.
        Button button3 = (Button) findViewById(R.id.block);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!isDefaultSmsApp(v.getContext())){
                    Toast.makeText(getApplicationContext(), "Not the default SMS app...: ", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getApplicationContext(), "Block successful. ", Toast.LENGTH_SHORT).show();

                }else{
                    Toast.makeText(getApplicationContext(), "Your phone doesn't support blocking, try to hide the notif manually", Toast.LENGTH_SHORT).show();

                }
            }
        });
//unblock number
        Button button4 = (Button) findViewById(R.id.unblock);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!isDefaultSmsApp(v.getContext())){
                    Toast.makeText(getApplicationContext(), "Not the default SMS app...: ", Toast.LENGTH_SHORT).show();
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
                            int result = BlockedNumberContract.unblock(v.getContext(), pnE164);
                            if(result > 0){
                                Toast.makeText(getApplicationContext(), "Phone number unblocked!", Toast.LENGTH_SHORT).show();

                            }else{
                                Toast.makeText(getApplicationContext(), "Phone number already unblocked.", Toast.LENGTH_SHORT).show();

                            }
                        }}).start();


                }else{
                    Toast.makeText(getApplicationContext(), "Your phone doesn't support blocking, try to hide the notif manually", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }


    public static boolean isDefaultSmsApp(Context context) {
        return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
    }

}