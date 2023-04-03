package com.txtnet.txtnetbrowser.phonenumbers;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.room.Delete;
import androidx.room.Query;

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

    public void deleteServers(Server... users){
        mRepository.delete(users);
    }
    public void deleteByID(int uid){
        mRepository.deleteByID(uid);
    }

//    @Query("SELECT server_last_status FROM Server WHERE uid == :uid LIMIT 1")
//    public boolean getServerStatus(int uid);
//
//    @Query("UPDATE Server SET server_last_status=:status WHERE uid=:uid")
//    public void setServerStatus(int uid, boolean status);
//    LiveData<List<Server>> getAllByCountryCode(int countryCode);
}
