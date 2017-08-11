package com.iitk.proxyLogin.receiver

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.support.v4.content.WakefulBroadcastReceiver
import android.util.Log

import com.iitk.proxyLogin.misc.LocalDatabase
import com.iitk.proxyLogin.service.ProxyService

/**
 * Created by kshivang on 15/04/17.

 */

class WifiStateReceiver : WakefulBroadcastReceiver() {

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

    override fun onReceive(context: Context, intent: Intent) {
        val proxyServiceIntent = Intent(context, ProxyService::class.java)
        proxyServiceIntent.action = "proxy.service.WIFI_STATE_CHANGE"
        proxyServiceIntent.putExtra("networkIntent", intent)
        val localDatabase = LocalDatabase(context)
        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            if (networkInfo.isConnected) {
                // Wifi is connected
                Log.i(TAG, "Wifi is connected: " + networkInfo.toString())
                localDatabase.setWifiState(true)
                WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent)
                return
            }
        }
        try {
            val conMan = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = conMan.activeNetworkInfo
            if (netInfo != null && netInfo.type == ConnectivityManager.TYPE_WIFI) {
                Log.i(TAG, "Have Wifi Connection")
                localDatabase.setWifiState(true)
            } else {
                Log.i(TAG, "Do not Have Wifi Connection")
                localDatabase.setWifiState(false)
            }
            WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    companion object {

        private val TAG: String

        init {
            TAG = WifiStateReceiver::class.java.simpleName
        }
    }
}
