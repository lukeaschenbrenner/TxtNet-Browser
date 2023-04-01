package com.txtnet.txtnetbrowser.phonenumbers;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.txtnet.txtnetbrowser.database.Server;
import com.txtnet.txtnetbrowser.database.ServerRepository;

import java.util.List;

public class ServerPickerModel extends AndroidViewModel {
   // private final MutableLiveData<>
    private ServerRepository mRepository;

    private final LiveData<List<Server>> mAllServers;
  //  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication().getBaseContext());

    public ServerPickerModel(Application application){
        super(application);
        mRepository = new ServerRepository(application);
        mAllServers = mRepository.getAllServers();
    }
    LiveData<List<Server>> getAllServers() {return mAllServers;}

    public void insert(Server... servers){
        mRepository.insert(servers);
    }

}
