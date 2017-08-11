package com.iitk.proxyLogin.misc

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.LocalBroadcastManager
import android.text.TextUtils
import android.util.Log

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.iitk.proxyLogin.R
import com.iitk.proxyLogin.activity.LoginActivity
import com.iitk.proxyLogin.activity.SessionActivity
import com.iitk.proxyLogin.notfication.SummaryNotification
import com.iitk.proxyLogin.service.ProxyService

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by kshivang on 12/04/17.

 */

class ProxyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }

    val requestQueue: RequestQueue
        get() {
            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(applicationContext)
            }
            return mRequestQueue!!
        }

    fun <T> addToRequestQueue(req: Request<T>, tag: String) {
        var tag = tag
        if (TextUtils.isEmpty(tag)) {
            tag = TAG
        }
        req.tag = tag
        requestQueue.add(req)
    }

    fun cancelPendingRequests(tag: Any) {
        if (mRequestQueue != null) {
            Log.i(TAG, "Cancelling all queries for TAG : " + tag)
            mRequestQueue!!.cancelAll(tag)
        }
    }

    companion object {

        val TAG = ProxyApp::class.java.simpleName
        private var mInstance: ProxyApp? = null
        private var mRequestQueue: RequestQueue? = null


        val instance: ProxyApp
            @Synchronized get() {
                var proxyApp: ProxyApp? = null
                synchronized(ProxyApp::class.java) {
                    proxyApp = mInstance!!
                }
                return proxyApp!!
            }

        fun broadcastProgress(type: String?, progress: String,
                              localBroadcastManager: LocalBroadcastManager) {
            if (type != null) LocalDatabase(mInstance!!).lastIdentified = type

            val sn = SummaryNotification(mInstance!!,
                    progress, null, null!!, R.drawable.black_logo, null)
            val resultIntent = Intent(mInstance, SessionActivity::class.java)
            resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = TaskStackBuilder.create(mInstance)
                    .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            sn.setNotificationId(1001)
            sn.setContentIntent(pendingIntent)
            sn.mBuilder.setOngoing(true)
            sn.setTag(TAG)
            sn.show()

            localBroadcastManager.sendBroadcast(Intent("proxy.app.PROXY_PROGRESS")
                    .putExtra("Progress", progress))
        }

        fun broadcastRequestCredential(type: String,
                                       localBroadcastManager: LocalBroadcastManager) {

            val localDatabase = LocalDatabase(mInstance!!)

            if (localDatabase.username == null || localDatabase.password == null) {
                val sn = SummaryNotification(mInstance!!,
                        "Need IITK credentials!", null, null!!, R.drawable.black_logo, null)
                val resultIntent = Intent(mInstance, LoginActivity::class.java)
                resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val pendingIntent = TaskStackBuilder.create(mInstance)
                        .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                sn.setNotificationId(1001)
                sn.setContentIntent(pendingIntent)
                sn.mBuilder.setOngoing(true)
                sn.addAction(R.drawable.ic_login, "login", pendingIntent)
                sn.setTag(TAG)
                sn.show()
            } else {
                val proxyServiceIntent = Intent(mInstance, ProxyService::class.java)
                when (localDatabase.lastIdentified) {
                    "fortinet" -> {
                        proxyServiceIntent.action = "proxy.service.FORTINET_LOGIN"
                        proxyServiceIntent.putExtra("username", localDatabase.username)
                        proxyServiceIntent.putExtra("password", localDatabase.password)
                    }
                    "ironport" -> {
                        proxyServiceIntent.action = "porxy.service.IRONPORT_LOGIN"
                        proxyServiceIntent.putExtra("username", localDatabase.username)
                        proxyServiceIntent.putExtra("password", localDatabase.password)
                    }
                    else -> proxyServiceIntent.action = "proxy.service.NETWORK_TYPE"
                }
            }

            LocalDatabase(mInstance!!).lastIdentified = type
            localBroadcastManager.sendBroadcast(Intent("proxy.app.PROXY_REQUEST_CREDENTIAL")
                    .putExtra("Type", type))
        }

        fun broadcastNotIITK(localBroadcastManager: LocalBroadcastManager) {
            SummaryNotification.cancelNotification(mInstance!!, TAG, 1001)
            LocalDatabase(mInstance!!).lastIdentified = "non IITK"
            localBroadcastManager.sendBroadcast(Intent("proxy.app.PROXY_NOT_IITK"))
        }

        fun broadcastLiveSession(lastLogin: Long,
                                 localBroadcastManager: LocalBroadcastManager) {
            val localDatabase = LocalDatabase(mInstance!!)

            val nextUpdate = lastLogin + if (localDatabase.lastIdentified == "fortinet")
                localDatabase.fortinetRefresh
            else
                localDatabase.ironPortRefresh

            localDatabase.setRefreshTime(nextUpdate, lastLogin)

            val proxyServiceIntent = Intent(mInstance,
                    ProxyService::class.java)
            proxyServiceIntent.action = "proxy.service.SETUP_ALARM"
            mInstance!!.startService(proxyServiceIntent)

            val sn = SummaryNotification(mInstance!!,
                    "Logged in to IITK " + localDatabase.lastIdentified + " network", null, null!!,
                    R.drawable.black_logo, null)
            val resultIntent = Intent(mInstance, SessionActivity::class.java)
            resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = TaskStackBuilder.create(mInstance)
                    .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            sn.addLineToBigView("Session refreshed at " + SimpleDateFormat("HH:mm a",
                    Locale.ENGLISH).format(lastLogin) + ".")
            sn.addLineToBigView("Will refresh at " + SimpleDateFormat("HH:mm a",
                    Locale.ENGLISH).format(nextUpdate))
            sn.setNotificationId(1001)
            sn.setContentIntent(pendingIntent)
            sn.mBuilder.setOngoing(true)
            sn.setTag(TAG)
            sn.show()

            localBroadcastManager.sendBroadcast(Intent("proxy.app.PROXY_LIVE_SESSION")
                    .putExtra("Time", lastLogin))
        }

        fun broadcastCheckSession(localBroadcastManager: LocalBroadcastManager) {
            val proxyServiceIntent = Intent(mInstance,
                    ProxyService::class.java)
            proxyServiceIntent.action = "proxy.service.NETWORK_TYPE"
            mInstance!!.startService(proxyServiceIntent)
            localBroadcastManager.sendBroadcast(Intent("proxy.app.PROXY_CHECK_SESSION"))
        }

        fun broadcastIncorrectPassword(localBroadcastManager: LocalBroadcastManager) {
            val sn = SummaryNotification(mInstance!!,
                    "Incorrect credentials!", null, null!!, R.drawable.black_logo, null)

            val resultIntent = Intent(mInstance, LoginActivity::class.java)
            resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = TaskStackBuilder.create(mInstance)
                    .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            sn.setNotificationId(1001)
            sn.setContentIntent(pendingIntent)
            sn.mBuilder.setOngoing(false)
            sn.addAction(R.drawable.ic_login, "login", pendingIntent)
            sn.setTag(TAG)
            sn.show()
            localBroadcastManager.sendBroadcast(Intent("proxy.app.PROXY_INCORRECT_PASSWORD"))
        }

        fun broadcastWifiChange(localBroadcastManager: LocalBroadcastManager) {

            if (LocalDatabase(mInstance!!).isWifiPresent) {
                broadcastProgress(null, "Wifi connection found", localBroadcastManager)

                val sn = SummaryNotification(mInstance!!,
                        "Wifi connection found", null, null!!,
                        R.drawable.black_logo, null)
                val resultIntent = Intent(mInstance, SessionActivity::class.java)
                resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val pendingIntent = TaskStackBuilder.create(mInstance)
                        .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                sn.setNotificationId(1001)
                sn.setContentIntent(pendingIntent)
                sn.mBuilder.setOngoing(true)
                sn.setTag(TAG)
                sn.show()
            } else {
                broadcastProgress(null, "Wifi connection not found", localBroadcastManager)
                val sn = SummaryNotification(mInstance!!,
                        "No wifi connection found", null, null!!,
                        R.drawable.black_logo, null)
                val resultIntent = Intent(mInstance, SessionActivity::class.java)
                resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val pendingIntent = TaskStackBuilder.create(mInstance)
                        .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                sn.setNotificationId(1001)
                sn.setContentIntent(pendingIntent)
                sn.mBuilder.setOngoing(true)
                sn.setTag(TAG)
                sn.show()
            }
        }
    }
}
