package com.txtnet.txtnetbrowser.blockingactivities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.SMSActivities;

public class ShizukuIncompatible extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shizuku_incompatible);

        final Button button = (Button) findViewById(R.id.shizukuOpenServerButton);
        SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick (View v){
                //add to shared preferences not to show this screen again, and restart the app
                SharedPreferences.Editor edit = preferences.edit();
                edit.putBoolean("outdatedAdbAccepted", Boolean.TRUE);
                edit.apply();

                PackageManager packageManager = v.getContext().getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(v.getContext().getPackageName());
                ComponentName componentName = intent.getComponent();
                Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                v.getContext().startActivity(mainIntent);
                Runtime.getRuntime().exit(0);
            }

        });

    }
}