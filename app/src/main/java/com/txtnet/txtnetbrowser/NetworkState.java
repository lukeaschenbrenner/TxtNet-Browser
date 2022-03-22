package com.txtnet.txtnetbrowser;

import android.content.Context;
import android.net.ConnectivityManager;

public class NetworkState {

    public static boolean connectionAvailable(Context context){

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() !=null;
    }
}
