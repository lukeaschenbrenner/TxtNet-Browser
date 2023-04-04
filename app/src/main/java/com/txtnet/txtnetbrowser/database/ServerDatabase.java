package com.txtnet.txtnetbrowser.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Server.class}, version = 1)
public abstract class ServerDatabase extends RoomDatabase {
    public abstract ServerDao serverDao();
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private static volatile ServerDatabase INSTANCE;

    static ServerDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ServerDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ServerDatabase.class, "server-database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
