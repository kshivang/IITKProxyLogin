package com.iitk.proxyLogin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by kshivang on 15/04/17.
 */

public class WifiStateReceiver extends BroadcastReceiver {

    private static final String TAG;

    static {
        TAG = WifiStateReceiver.class.getSimpleName();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, ProxyService.class);
        try {
            LocalDatabase localDatabase = new LocalDatabase(context);
            ConnectivityManager conMan = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            serviceIntent.setAction("proxy.service.WIFI_STATE_CHANGE");
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "Have Wifi Connection");
                localDatabase.setWifiState(true);
            } else {
                localDatabase.setWifiState(false);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
