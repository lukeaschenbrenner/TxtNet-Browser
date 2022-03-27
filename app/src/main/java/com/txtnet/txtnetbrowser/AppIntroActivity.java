package com.txtnet.txtnetbrowser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;
import com.github.appintro.AppIntroPageTransformerType;
import com.txtnet.txtnetbrowser.R;

import java.security.Permissions;

public class AppIntroActivity extends AppIntro {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntroFragment.createInstance(
                "Welcome to TxtNet Browser!",
                "This app will allow you to browse the web without an internet connection.",
                R.mipmap.ic_launcher,
                R.color.green,
                R.color.white,
                R.color.white));


        addSlide(AppIntroFragment.createInstance(
                "Permissions",
                "Certain permissions are required to continue. Without them, this app will not function.",
                R.drawable.permissions_request_phone,
                R.color.green,
                R.color.white,
                R.color.white));

        addSlide(AppIntroFragment.createInstance(
                "Thank you!",
                "Enjoy the app.",
                R.drawable.forward_arrow,
                R.color.green,
                R.color.white,
                R.color.white));

        // Fade Transition
        setTransformer(AppIntroPageTransformerType.Fade.INSTANCE);

        // Show/hide status bar
        showStatusBar(true);

        //Speed up or down scrolling
        setScrollDurationFactor(2);

        //Enable the color "fade" animation between two slides (make sure the slide implements SlideBackgroundColorHolder)
        setColorTransitionsEnabled(true);

        //Prevent the back button from exiting the slides
        setSystemBackButtonLocked(true);

        //Activate wizard mode (Some aesthetic changes)
        setWizardMode(true);

        setSkipButtonEnabled(false);

        //Enable/disable page indicators
        setIndicatorEnabled(true);


        String[] permissions = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_PHONE_STATE};
        askForPermissions( permissions,
                2,
                true);
}

        @Override
        protected void onSkipPressed(Fragment currentFragment) {
                super.onSkipPressed(currentFragment);
                finish();
        }

        @Override
        protected void onDonePressed(Fragment currentFragment) {
                super.onDonePressed(currentFragment);
                Intent intent = new Intent(this, MainBrowserScreen.class);
                startActivity(intent);
                finish();
        }
        }