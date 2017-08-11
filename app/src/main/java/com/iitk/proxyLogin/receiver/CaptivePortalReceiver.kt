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

class CaptivePortalReceiver : WakefulBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val proxyServiceIntent = Intent(context, ProxyService::class.java)
        proxyServiceIntent.action = "proxy.service.CAPTIVE_PORTAL"
        Log.i(TAG, "#### Launching ProxyService for " +
                intent.action + " @ " + SystemClock.elapsedRealtime())
        WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent)
    }

    companion object {
        private val TAG: String = CaptivePortalReceiver::class.java.simpleName

    }
}
