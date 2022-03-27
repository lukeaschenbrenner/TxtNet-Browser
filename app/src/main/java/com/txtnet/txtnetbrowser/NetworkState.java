package com.txtnet.txtnetbrowser;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class NetworkState {

    public static boolean connectionAvailable(Context context, Activity activity){

       // ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
       // return connectivityManager.getActiveNetworkInfo() !=null;

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            int networkType;
            String[] permission = {Manifest.permission.READ_PHONE_STATE};
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, permission, 5);
            }
            try{
                networkType = telephonyManager.getNetworkType();
            }catch(NullPointerException e){
                return false;
            }

            Log.i("MAINACTIVITY", "#### isCellularAvailable(): Network type is: " + networkType);

//        switch (networkType)
//        {
            // Return true for networks that suits your purpose
//            case TelephonyManager.NETWORK_TYPE_1xRTT:    return true;
//            case TelephonyManager.NETWORK_TYPE_CDMA:     return true;
//            case TelephonyManager.NETWORK_TYPE_EDGE:     return true;
//            case TelephonyManager.NETWORK_TYPE_EHRPD:    return true;
//            case TelephonyManager.NETWORK_TYPE_EVDO_0:   return true;
//            case TelephonyManager.NETWORK_TYPE_EVDO_A:   return true;
//            case TelephonyManager.NETWORK_TYPE_EVDO_B:   return true;
//            case TelephonyManager.NETWORK_TYPE_GPRS:     return true;
//            case TelephonyManager.NETWORK_TYPE_GSM:      return true;
//            case TelephonyManager.NETWORK_TYPE_HSDPA:    return true;
//            case TelephonyManager.NETWORK_TYPE_HSPA:     return true;
//            case TelephonyManager.NETWORK_TYPE_HSPAP:    return true;
//            case TelephonyManager.NETWORK_TYPE_HSUPA:    return true;
//            case TelephonyManager.NETWORK_TYPE_IDEN:     return true;
//            case TelephonyManager.NETWORK_TYPE_IWLAN:    return true;
//            case TelephonyManager.NETWORK_TYPE_LTE:      return true;
//            case TelephonyManager.NETWORK_TYPE_NR:     return true;       // Not supported by my API
//            case TelephonyManager.NETWORK_TYPE_TD_SCDMA: return true;
//            case TelephonyManager.NETWORK_TYPE_UMTS:     return true;

            // Return false for unacceptable networks, UNKNOWN id no network e.g. airplane mode.
//            case TelephonyManager.NETWORK_TYPE_UNKNOWN: return false;

            // Future unknown network types, handle as you please.
            //           default: return false;
            //     }
            return (networkType != 0);

    }
}
