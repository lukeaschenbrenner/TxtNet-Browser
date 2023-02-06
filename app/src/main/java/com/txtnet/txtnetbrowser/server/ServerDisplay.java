package com.txtnet.txtnetbrowser.server;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;


import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.SMSActivities;
import com.txtnet.txtnetbrowser.UnsupportedBlockActivity;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ServerDisplay extends AppCompatActivity {
    public final static String CHANNEL_ID = "SERVER_NOTIFS";


    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        String currentDefault = Telephony.Sms.getDefaultSmsPackage(this);
        boolean isDefault = getPackageName().equals(currentDefault);
        Toast.makeText(getApplicationContext(), "TxtNet SMS is " + (isDefault ? "now the default SMS app." : "still not the default SMS app."), Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serverscreen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            createNotificationChannel(this);
        }

        Button button1 = (Button) findViewById(R.id.startServiceButton);
        Button button2 = (Button) findViewById(R.id.endServiceButton);

        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(v.getContext(), TxtNetServerService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.getContext().startForegroundService(intent);
                }else{
                    v.getContext().startService(intent);
                }


            }

        });
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.getContext().stopService(new Intent(v.getContext(), TxtNetServerService.class));
            }
        });

    }
    public static void createNotificationChannel(Context c) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager notificationManager = c.getSystemService(NotificationManager.class);
            NotificationChannel ch = notificationManager.getNotificationChannel(CHANNEL_ID);
            if(ch == null){
                //CharSequence name = getString(R.string.channel_name);
                CharSequence name = "Server Background Service";
                //  String description = getString(R.string.channel_description);
                String description = "Allows TxtNet server to be run in the background.";
                int importance = NotificationManager.IMPORTANCE_LOW; // To support devices running Android 7.1 (API level 25) or lower, you must also call
                // setPriority() for each notification, using a priority constant from the NotificationCompat class.
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);

                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                notificationManager.createNotificationChannel(channel);
            }

        }
    }



}
