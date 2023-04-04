package com.txtnet.txtnetbrowser.phonenumbers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.database.DBInstance;
import com.txtnet.txtnetbrowser.database.Server;
import com.txtnet.txtnetbrowser.databinding.ActivityServerPickerBinding;

import java.util.List;

public class ServerPickerActivity extends AppCompatActivity {

    private ActivityServerPickerBinding binding;
    //DBInstance database;
    private Toolbar toolbar;

    private ServerPickerModel mViewModel;
    public static final int NEW_SERVER_ACTIVITY_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // database = DBInstance.getInstance(this);
        binding = ActivityServerPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = binding.toolbar;

        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        Intent intent = getIntent();
        boolean needsDefault = intent.getBooleanExtra("needDefault", false);
        if(needsDefault){
            ViewGroup view = (ViewGroup) findViewById(android.R.id.content);
            Snackbar.make(view.getRootView(), "Please enter a phone number and select it as default!", Snackbar.LENGTH_LONG).setAction("OK", null).show();
        }


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
        DividerItemDecoration divider = new DividerItemDecoration(
                recyclerView.getContext(), DividerItemDecoration.VERTICAL
        );
        divider.setDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.line_divider));
        recyclerView.addItemDecoration(divider);

        mViewModel = new ViewModelProvider(this).get(ServerPickerModel.class);

        mViewModel.getAllServers().observe(this, servers -> {
            adapter.submitList(servers);
        });

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.toolbar_layout);
        collapsingToolbar.setTitle(getString(R.string.title_activity_server_picker));

//        FloatingActionButton fab2 = findViewById(R.id.fab);
//        fab2.setOnClickListener( view -> {
//            Intent intent = new Intent(ServerPickerActivity.this, NewServerActivity.class);
//            startActivityForResult(intent, NEW_SERVER_ACTIVITY_REQUEST_CODE);
//        });

       // List<Server> serverList = database.getDB().serverDao().getAll();
     //   for(Server server : serverList){
     //       Log.i("SERVER", "SERVER: " + server.phoneNumber);
     //   }
        SwipeHelper swipeHelper = new SwipeHelper(this, recyclerView) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                ServerViewHolder holder = (ServerViewHolder) viewHolder;
                underlayButtons.add(new SwipeHelper.UnderlayButton(
                        "Delete",
                        0,
                        Color.parseColor("#FF3C30"),
                        new SwipeHelper.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int pos) {
                                Log.i("holder", "holder is default? " + (holder.isDefault ? "true" : "false"));
                                if(holder.isDefault){
                                    SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences(holder.itemView.getContext().getPackageName() + "_preferences", MODE_PRIVATE);
                                    SharedPreferences.Editor edit = prefs.edit();
                                    edit.remove(getResources().getString(R.string.phone_number));
                                    edit.apply();

                                }
                                mViewModel.deleteByID(holder.id);
                            }
                        }
                ));
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server_picker, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_addnumber) {
            Intent intent = new Intent(ServerPickerActivity.this, NewServerActivity.class);
            startActivityForResult(intent, NEW_SERVER_ACTIVITY_REQUEST_CODE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_SERVER_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
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