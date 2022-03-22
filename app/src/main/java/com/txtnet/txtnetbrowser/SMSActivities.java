package com.txtnet.txtnetbrowser;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class SMSActivities{
    public static class ComposeSmsActivity extends Activity {

    }

    public static class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
    public static class MmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    public static class HeadlessSmsSendService extends Service {

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}

