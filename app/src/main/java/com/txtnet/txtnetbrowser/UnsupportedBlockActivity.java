package com.txtnet.txtnetbrowser;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class UnsupportedBlockActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.unsupported_block);

        final Button button = (Button) findViewById(R.id.revertSMSbutton);

        button.setOnClickListener(new View.OnClickListener()

        {
            public void onClick (View v){
                getPackageManager()
                        .setComponentEnabledSetting(new ComponentName(v.getContext(), SMSActivities.SmsReceiver.class),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);

                //getPackageManager().setComponentEnabledSetting(new ComponentName(v.getContext(), SMSActivities.SmsReceiver.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            }

        });

    }

}

