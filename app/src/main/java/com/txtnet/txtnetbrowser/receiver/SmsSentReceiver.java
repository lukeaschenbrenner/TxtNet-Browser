package com.txtnet.txtnetbrowser.receiver;

import static com.txtnet.txtnetbrowser.R.string.SSRMsg1;
import static com.txtnet.txtnetbrowser.R.string.SSRMsg4;
import static com.txtnet.txtnetbrowser.R.string.SSRMsg5;
import static com.txtnet.txtnetbrowser.R.string.SSRMsg6;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.txtnet.txtnetbrowser.BuildConfig;
import com.txtnet.txtnetbrowser.MainBrowserScreen;
import com.txtnet.txtnetbrowser.R;

import java.util.logging.Level;

public class SmsSentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                //  Toast.makeText(context,
                //          "SMS Sent" + intent.getIntExtra("object", 0),
                //          Toast.LENGTH_SHORT).show();
                //TODO: Initiate loading animation here, not when the button is pressed.
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                Toast.makeText(context, SSRMsg1, Toast.LENGTH_SHORT)
                        .show();
                if (BuildConfig.DEBUG) {
                    int errorCode = intent.getIntExtra("errorCode", -1);
                    if (errorCode != -1) {
                        MainBrowserScreen.rootLogger.log(Level.INFO, context.getString(R.string.SSRMsg2) + String.valueOf(errorCode));
                    } else {
                        MainBrowserScreen.rootLogger.log(Level.INFO, context.getString(R.string.SSRMsg3));
                    }
                }


                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                Toast.makeText(context, SSRMsg4, Toast.LENGTH_SHORT)
                        .show();

                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                Toast.makeText(context, SSRMsg5, Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                Toast.makeText(context, SSRMsg6, Toast.LENGTH_SHORT).show();
                //TODO: ask user to turn off airplane mode
                break;
        }
    }
}
