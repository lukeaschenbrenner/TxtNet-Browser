package com.txtnet.txtnetbrowser.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Server.class}, version = 1)
public abstract class ServerDatabase extends RoomDatabase {
    public abstract ServerDao serverDao();
}
