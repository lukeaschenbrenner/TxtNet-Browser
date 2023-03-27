package com.txtnet.txtnetbrowser;

import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

import com.txtnet.txtnetbrowser.database.DBInstance;
import com.txtnet.txtnetbrowser.database.Server;
import com.txtnet.txtnetbrowser.databinding.ActivityServerPickerBinding;

import java.util.List;

public class ServerPickerActivity extends AppCompatActivity {

    private ActivityServerPickerBinding binding;
    DBInstance database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = DBInstance.getInstance(this);
        binding = ActivityServerPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                database.getDB().serverDao().insertServers(new Server(123, "+13202222222", true, 1));
            }
        });
        List<Server> serverList = database.getDB().serverDao().getAll();
        for(Server server : serverList){
            Log.i("SERVER", "SERVER: " + server.phoneNumber);
        }
    }
}