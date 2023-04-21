package com.txtnet.txtnetbrowser;

import static com.txtnet.txtnetbrowser.MainBrowserScreen.preferences;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class TermsConditionsDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {



        LayoutInflater inflater = requireActivity().getLayoutInflater();


        View view = inflater.inflate(R.layout.dialog_termsconditions, null);
        TextView textView = (TextView) view.findViewById(R.id.termsconditions_textview);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView.setScrollbarFadingEnabled(false);
        }


                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(inflater.inflate(R.layout.dialog_termsconditions, null))
                .setTitle(R.string.TCDFMsg1)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.accept), (dialog, which) -> {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(getString(R.string.is_tosaccepted), Boolean.TRUE);
                    editor.commit();
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getActivity().finishAffinity();
                    }
                });
                builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                            getActivity().finishAffinity();
                        return false;
                    }});

        return builder.create();

    }
}