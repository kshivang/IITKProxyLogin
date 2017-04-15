package com.iitk.proxyLogin;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by kshivang on 15/04/17.
 *
 */

public class WifiStateReceiver extends WakefulBroadcastReceiver {

    private static final String TAG;

    static {
        TAG = WifiStateReceiver.class.getSimpleName();
    }

//    @Override
//    public void onReceive(final Context context, final Intent intent) {
//        Intent proxyServiceIntent = new Intent(context, ProxyService.class);
//        proxyServiceIntent.setAction("proxy.service.WIFI_STATE_CHANGE");
//        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
//            NetworkInfo networkInfo =
//                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//            if (networkInfo.isConnected()) {
//                // Wifi is connected
//                Log.i("onReceive", "Wifi is connected: " + String.valueOf(networkInfo));
//                proxyServiceIntent.putExtra("isConnected", true);
////                WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent);
//                context.sendBroadcast(proxyServiceIntent);
//            }
//        } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
//            NetworkInfo networkInfo =
//                    intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
//            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
//                    !networkInfo.isConnected()) {
//                // Wifi is disconnected
//                Log.i("onReceive", "Wifi is disconnected: " + String.valueOf(networkInfo));
//                proxyServiceIntent.putExtra("isConnected", false);
//                context.sendBroadcast(proxyServiceIntent);
////                WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent);
//            }
//        }
//    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent proxyServiceIntent = new Intent(context, ProxyService.class);
        proxyServiceIntent.setAction("proxy.service.WIFI_STATE_CHANGE");
        proxyServiceIntent.putExtra("networkIntent", intent);
        LocalDatabase localDatabase = new LocalDatabase(context);
        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo networkInfo =
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                // Wifi is connected
                Log.i(TAG, "Wifi is connected: " + String.valueOf(networkInfo));
                localDatabase.setWifiState(true);
                WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent);
                return;
            }
        }
        try {
            ConnectivityManager conMan = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i(TAG, "Have Wifi Connection");
                localDatabase.setWifiState(true);
            } else {
                Log.i(TAG, "Do not Have Wifi Connection");
                localDatabase.setWifiState(false);
            }
            WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
