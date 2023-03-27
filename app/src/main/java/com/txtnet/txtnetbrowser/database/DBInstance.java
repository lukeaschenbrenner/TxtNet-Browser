package com.txtnet.txtnetbrowser.database;

import android.content.Context;

import androidx.room.Room;

import com.txtnet.txtnetbrowser.messaging.TextMessageHandler;

public class DBInstance {

    private static DBInstance single_instance;
    private ServerDatabase db;
    private DBInstance(Context context){
         db = Room.databaseBuilder(context.getApplicationContext(),
                ServerDatabase.class, "server-database").build();
    }
    public ServerDatabase getDB(){
        return db;
    }
    public static DBInstance getInstance(Context context){
        if(single_instance == null){
            single_instance = new DBInstance(context);
        }
        return single_instance;
    }
    public static DBInstance getInstance(){
        if(single_instance == null){
            return null;
        }else{
            return single_instance;
        }
    }
}
