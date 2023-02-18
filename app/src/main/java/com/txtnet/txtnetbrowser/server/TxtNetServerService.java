package com.txtnet.txtnetbrowser.server;

import static com.txtnet.txtnetbrowser.server.ServerDisplay.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.txtnet.txtnetbrowser.R;

import rikka.shizuku.Shizuku;


public class TxtNetServerService extends Service {
    private NotificationManagerCompat notificationManager;
    private final int NOTIFICATION_ID = 43;

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    @SuppressLint("UnspecifiedImmutableFlag")
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        //Message msg = serviceHandler.obtainMessage();
        //msg.arg1 = startId;
        //serviceHandler.sendMessage(msg);


        PendingIntent pendingIntent = null;
        Context context = getApplicationContext();
        Intent intentNotif = new Intent(this, ServerDisplay.class); //allows notif to execute code with same perms as our app
        intentNotif.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // https://developer.android.com/guide/components/activities/tasks-and-back-stack
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intentNotif, PendingIntent.FLAG_MUTABLE);
        }else{
            pendingIntent = PendingIntent.getActivity(this, 0, intentNotif, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TxtNet Server Is Running").setContentText("Press this notification to open the server management screen.")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_monochrome);
        builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).setVisibility(NotificationCompat.VISIBILITY_SECRET);

        notificationManager = NotificationManagerCompat.from(context.getApplicationContext());
        Notification notif = builder.build();
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        startForeground(NOTIFICATION_ID, notif);

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        Toast.makeText(this, "We're done here!", Toast.LENGTH_SHORT).show();


        /*
                if (intent != null) {
            String action = intent.getAction();
            if(action!=null)

            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startForegroundService();
                    Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    Toast.makeText(getApplicationContext(),
         */

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notificationManager.cancel(NOTIFICATION_ID);

        // Tell the user we stopped.
       // Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "We're done here!", Toast.LENGTH_SHORT).show();
    }

    //@Override
   // public IBinder onBind(Intent intent) {
    //    return mBinder;
    //}

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    //private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    /*
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LocalServiceActivities.Controller.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_sample)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);

    } */

    public void onCreate(){
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        Shizuku.pingBinder();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

}

