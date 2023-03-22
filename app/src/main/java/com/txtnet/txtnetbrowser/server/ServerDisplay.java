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
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.BlockedNumberContract;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;


import com.txtnet.txtnetbrowser.Constants;
import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.SMSActivities;
import com.txtnet.txtnetbrowser.UnsupportedBlockActivity;
import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.ShizukuProvider;
import rikka.shizuku.SystemServiceHelper;

public class ServerDisplay extends AppCompatActivity implements Shizuku.OnRequestPermissionResultListener {
    public static final int OPEN_NEW_ACTIVITY = 123456;
    public final static String CHANNEL_ID = "SERVER_NOTIFS";
    private final static int SHIZUKU_CODE = 31;


    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_NEW_ACTIVITY) {
            // request setting secure perm
            Settings.Global.putInt(getContentResolver(), "SMS_OUTGOING_CHECK_MAX_COUNT".toLowerCase(), 20);

            try {
                Log.e("Setting", String.valueOf(Settings.Global.getInt(getContentResolver(), "SMS_OUTGOING_CHECK_MAX_COUNT".toLowerCase())));
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }

      //  String currentDefault = Telephony.Sms.getDefaultSmsPackage(this);
      //  boolean isDefault = getPackageName().equals(currentDefault);
      //  Toast.makeText(getApplicationContext(), "TxtNet SMS is " + (isDefault ? "now the default SMS app." : "still not the default SMS app."), Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serverscreen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        createNotificationChannel(this);

        Button button1 = (Button) findViewById(R.id.startServiceButton);
        Button button2 = (Button) findViewById(R.id.endServiceButton);

        final TextView tv = (TextView) findViewById(R.id.serverStatusText);
        if(TxtNetServerService.isRunning){
            tv.setText(R.string.server_started);
        }else{
            tv.setText(R.string.server_stopped);
        }

        button1.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("PrivateApi")
            public void onClick(View v) {

                // It turns out that SMS_OUTGOING_CHECK_MAX_COUNT and SMS_OUTGOING_CHECK_MAX_INTERVAL_MS are no longer secure settings as of 9/14/2012 ( https://cs.android.com/android/_/android/platform/frameworks/opt/telephony/+/3ca3a570c0ad836dc42378e4359dbf28c6ef71db:src/java/com/android/internal/telephony/SmsUsageMonitor.java;l=258;bpv=1;bpt=0;drc=4658a1a8c23111d5cc89feb040ce547a7b65dfb0;dlc=c38bb60d867c5d61d90b7179a9ed2b2d1848124f )
                //     still, just the WRITE_SETTINGS permission DOES NOT allow us to write global settings.
                if(ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_DENIED)
                {
                    Log.e("perm", "No permission!");
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        boolean isGranted;
                        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                            isGranted = checkSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED;
                        } else {
                            isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
                        }
                        if (!isGranted) {
                            Log.e("rationale_shouldshow", String.valueOf(Shizuku.shouldShowRequestPermissionRationale()));

                            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                                requestPermissions(new String[]{ShizukuProvider.PERMISSION}, SHIZUKU_CODE);
                            } else {
                                Shizuku.requestPermission(31);
                            }
                        }else{
                            grantPermissions();
                        }
                    } else {
                        Toast.makeText(v.getContext(), "TOD0: Add Instructions for manual ADB on Android 4.4-6 (cant use shizuku)", Toast.LENGTH_LONG).show();
                        //TODO: Add Instructions for manual ADB on Android 4.4-6 (cant use shizuku)
                    }
                }
                else{ // permission granted, let's roll
                    if(ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_DENIED){
                        grantPermissions();
                    }

                //    Intent intent = new Intent(v.getContext(), TxtNetServerService.class);
                //    intent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);

                 //   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 //       v.getContext().getApplicationContext().startForegroundService(intent);
                 //   }else{
                 //       v.getContext().getApplicationContext().startService(intent);
                 //   }


                    doBindService();
                    final TextView tv = (TextView) findViewById(R.id.serverStatusText);
                    tv.setText(R.string.server_started);
                }





                /*
                    From the android source code website, default values are:
                        DEFAULT_SMS_CHECK_PERIOD = 60000;      // 1 minute
                        DEFAULT_SMS_MAX_COUNT = 30; // default number of SMS sent in checking period without user permission.
                 */



                /*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(v.getContext())) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:com.txtnet.txtnetbrowser"));
                        startActivityForResult(intent, OPEN_NEW_ACTIVITY);
                    }
                }
                */



            }

        });
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), TxtNetServerService.class);
                intent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                v.getContext().getApplicationContext().startService(intent);
                doUnbindService();
                final TextView tv = (TextView) findViewById(R.id.serverStatusText);
                tv.setText(R.string.server_stopped);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int result = grantResults[i];

            if (permission.equals(ShizukuProvider.PERMISSION)) {
                onRequestPermissionResult(requestCode, result);
            }
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, int grantResult) {
        boolean isGranted = grantResult == PackageManager.PERMISSION_GRANTED;
        if(!isGranted)
            Toast.makeText(this, "Shizuku permissions denied!", Toast.LENGTH_LONG).show();
        else {
            Toast.makeText(this, "Shizuku permissions granted!", Toast.LENGTH_LONG).show();
            grantPermissions();
        }
    }

    @SuppressLint("PrivateApi")
    public void grantPermissions(){
        Class<?> iPmClass = null;
        Class<?> iPmStub = null;
        try {
            iPmClass = Class.forName("android.content.pm.IPackageManager");
            iPmStub = Class.forName("android.content.pm.IPackageManager$Stub");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Method asInterfaceMethod = null;
        Method grantRuntimePermissionMethod = null;
        try {
            assert iPmStub != null;
            asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder.class);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        Object iPmInstance = null;
        try {
           // assert grantRuntimePermissionMethod != null;
          //  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          //      grantRuntimePermissionMethod = HiddenApiBypass.getDeclaredMethod(iPmClass, "grantRuntimePermission", String.class, String.class, int.class);
          //  }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    HiddenApiBypass.addHiddenApiExemptions("");
                }
                grantRuntimePermissionMethod = iPmClass.getMethod("grantRuntimePermission", String.class, String.class, int.class);

            assert asInterfaceMethod != null;
            iPmInstance = asInterfaceMethod.invoke(null, new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")));

           // }
           // grantRuntimePermissionMethod.invoke(iPmInstance, "com.txtnet.txtnetbrowser", Manifest.permission.WRITE_SETTINGS, 0);
            grantRuntimePermissionMethod.invoke(iPmInstance, "com.txtnet.txtnetbrowser", Manifest.permission.WRITE_SECURE_SETTINGS, 0);


        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();

        }
        try{
            // We now can send no more than
            boolean put1 = Settings.Global.putInt(getContentResolver(), "sms_outgoing_check_max_count", 1_000_000); // 1 million SMS messages every
            boolean put2 = Settings.Global.putInt(getContentResolver(), "sms_outgoing_check_interval_ms", 30000); // 30 seconds
            // Something tells me we won't hit this limit.
            Toast.makeText(this, "Done. You may need to reboot. This should only be done once.", Toast.LENGTH_LONG).show();
            Log.e("put1", String.valueOf(put1));
            Log.e("put2", String.valueOf(put2));

        }catch(SecurityException se){
            Toast.makeText(this, "Permission WRITE_SECURE_SETTINGS not obtained!", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    // Don't attempt to unbind from the service unless the client has received some
    // information about the service's state.
    private boolean mShouldUnbind;

    // To invoke the bound service, first make sure that this value
    // is not null.
    private TxtNetServerService mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((TxtNetServerService.LocalBinder)service).getService();

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
        }
    };

    void doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        Intent intent = new Intent(ServerDisplay.this, TxtNetServerService.class);
        intent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);

        if (bindService(intent,
                mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                   startForegroundService(intent);
               }else{
                   startService(intent);
               }

        } else {
            Log.e("MY_APP_TAG", "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            // Release information about the service's state.
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }
}

