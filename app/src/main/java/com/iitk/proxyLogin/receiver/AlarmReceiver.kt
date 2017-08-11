package com.iitk.proxyLogin.receiver

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.support.v4.content.WakefulBroadcastReceiver
import android.util.Log

import com.iitk.proxyLogin.service.ProxyService

/**
 * Created by kshivang on 15/04/17.

 */

class AlarmReceiver : WakefulBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if ("proxy.service.ALARM_BROADCAST" == intent.action) {
            val proxyServiceIntent = Intent(context, ProxyService::class.java)
            proxyServiceIntent.action = "proxy.service.ALARM_BROADCAST"
            Log.i(TAG, "#### Launching ProxyService for " +
                    intent.action + " @ " + SystemClock.elapsedRealtime())
            WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent)
        }
    }

    companion object {
        private val TAG: String = AlarmReceiver::class.java.simpleName

    }
}
