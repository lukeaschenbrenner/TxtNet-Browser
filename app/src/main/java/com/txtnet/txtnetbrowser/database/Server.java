package com.txtnet.txtnetbrowser.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Server {

    @PrimaryKey
    public int uid;

    @ColumnInfo(name = "phone_number")
    public String phoneNumber;

    @ColumnInfo(name = "server_last_status")
    public boolean serverStatus;

    @ColumnInfo(name = "server_country_code")
    public int countryCode;

    public Server(int uid, String phoneNumber, boolean serverStatus, int countryCode){
        this.uid = uid;
        this.phoneNumber = phoneNumber;
        this.serverStatus = serverStatus;
        this.countryCode = countryCode;
    }
    public Server(){}
}
