package com.txtnet.txtnetbrowser.phonenumbers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.database.DBInstance;
import com.txtnet.txtnetbrowser.database.Server;
import com.txtnet.txtnetbrowser.databinding.ActivityServerPickerBinding;

import java.util.List;

public class ServerPickerActivity extends AppCompatActivity {

    private ActivityServerPickerBinding binding;
    //DBInstance database;

    private ServerPickerModel mViewModel;
    public static final int NEW_WORD_ACTIVITY_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // database = DBInstance.getInstance(this);
        binding = ActivityServerPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

//        FloatingActionButton fab = binding.fab;
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//               // database.getDB().serverDao().insertServers(new Server(123, "+13202222222", true, 1));
//            }
//        });


        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String phoneNumber = prefs.getString(getResources().getString(R.string.phone_number), getResources().getString(R.string.default_phone));

        final ServerListAdapter adapter = new ServerListAdapter(new ServerListAdapter.ServerDiff(), phoneNumber);

        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        mViewModel = new ViewModelProvider(this).get(ServerPickerModel.class);

        mViewModel.getAllServers().observe(this, servers -> {
            adapter.submitList(servers);
        });

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.toolbar_layout);
        collapsingToolbar.setTitle(getString(R.string.title_activity_server_picker));

        FloatingActionButton fab2 = findViewById(R.id.fab);
        fab2.setOnClickListener( view -> {
            Intent intent = new Intent(ServerPickerActivity.this, NewServerActivity.class);
            startActivityForResult(intent, NEW_WORD_ACTIVITY_REQUEST_CODE);
        });

       // List<Server> serverList = database.getDB().serverDao().getAll();
     //   for(Server server : serverList){
     //       Log.i("SERVER", "SERVER: " + server.phoneNumber);
     //   }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_WORD_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            Server server = new Server(0, data.getStringExtra(NewServerActivity.EXTRA_REPLY), true, 1);
            mViewModel.insert(server);
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    R.string.empty_not_saved,
                    Toast.LENGTH_LONG).show();
        }
    }

}