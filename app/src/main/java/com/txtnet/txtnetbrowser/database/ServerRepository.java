package com.txtnet.txtnetbrowser.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ServerRepository {
    private ServerDao mServerDao;
    private LiveData<List<Server>> mAllServers;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    public ServerRepository(Application application) {
        ServerDatabase db = ServerDatabase.getDatabase(application);
        mServerDao = db.serverDao();
        mAllServers = mServerDao.getAll();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    public LiveData<List<Server>> getAllServers() {
        return mAllServers;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    public void insert(Server... server) {
        ServerDatabase.databaseWriteExecutor.execute(() -> {
            mServerDao.insertServers(server);
        });
    }

}
