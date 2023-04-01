package com.txtnet.txtnetbrowser.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"phone_number", "server_country_code"},
        unique = true)})
public class Server {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @NonNull
    @ColumnInfo(name = "phone_number")
    public String phoneNumber;

    @ColumnInfo(name = "server_last_status")
    public boolean serverStatus;

    @ColumnInfo(name = "server_country_code")
    public int countryCode;

    public Server(int uid, @NonNull String phoneNumber, boolean serverStatus, int countryCode){
        this.uid = uid;
        this.phoneNumber = phoneNumber;
        this.serverStatus = serverStatus;
        this.countryCode = countryCode;
    }

}
