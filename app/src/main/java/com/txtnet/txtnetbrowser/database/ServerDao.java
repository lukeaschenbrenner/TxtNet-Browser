package com.txtnet.txtnetbrowser.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ServerDao {
    @Query("SELECT * FROM Server")
    LiveData<List<Server>> getAll();

    @Query("SELECT * FROM Server WHERE server_country_code == :countryCode")
    LiveData<List<Server>> getAllByCountryCode(int countryCode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertServers(Server... servers);

    @Delete
    public void deleteServers(Server... users);

    @Query("DELETE FROM Server WHERE uid == :uid")
    public void deleteByID(int uid);

    @Query("SELECT server_last_status FROM Server WHERE uid == :uid LIMIT 1")
    public boolean getServerStatus(int uid);

    @Query("UPDATE Server SET server_last_status=:status WHERE uid=:uid")
    public void setServerStatus(int uid, boolean status);

}
