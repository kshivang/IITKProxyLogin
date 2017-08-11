package com.iitk.proxyLogin.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.WakefulBroadcastReceiver
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.iitk.proxyLogin.R
import com.iitk.proxyLogin.misc.LocalDatabase
import com.iitk.proxyLogin.misc.ProxyApp
import com.iitk.proxyLogin.receiver.AlarmReceiver
import java.util.*

/**
 * Created by kshivang on 12/04/17.

 */

class ProxyService : Service() {

    private val mBinder: Binder
    private var localBroadcastManager: LocalBroadcastManager? = null
    private var mServiceHandler: ProxyServiceHandler? = null
    private var proxyApp: ProxyApp? = null
    private var localDatabase: LocalDatabase? = null
    private val context: Context

    init {
        this.context = this
        this.mBinder = Binder()
    }

    override fun onCreate() {
        super.onCreate()
        this.proxyApp = ProxyApp.instance
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this)
        this.localDatabase = LocalDatabase(this)
        val thread = HandlerThread("ServiceStartArguments", 10)
        thread.start()
        this.mServiceHandler = ProxyServiceHandler(thread.looper, this)
    }

    private inner class ProxyServiceHandler internal constructor(looper: Looper, private val service: ProxyService) : Handler(looper) {
        private val TAG: String

        init {
            this.TAG = ProxyServiceHandler::class.java.simpleName
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                1 -> handleGetNetworkType(msg.arg1, msg.obj as Intent)
                2 -> handleFortinetLogout(msg.arg1, msg.obj as Intent)
                3 -> handleFortinetLogin(msg.arg1, msg.obj as Intent)
                4 -> handleFortinetRefresh(msg.arg1, msg.obj as Intent)
                5 -> handleIronPortLogout(msg.arg1, msg.obj as Intent)
                6 -> handleIronPortLogin(msg.arg1, msg.obj as Intent)
                7 -> handleIronPortRefresh(msg.arg1, msg.obj as Intent)
                8 -> handleSetupAlarm(msg.arg1, msg.obj as Intent)
                9 -> handleAlarmAction(msg.arg1, msg.obj as Intent)
                10 -> {
                    handleGetNetworkType(msg.arg1, msg.obj as Intent)
                    WakefulBroadcastReceiver.completeWakefulIntent(msg.obj as Intent)
                }
                11 -> handleWifiChange(msg.arg1, msg.obj as Intent)
            }
        }

        private fun handleWifiChange(startId: Int, intent: Intent) {
            Log.i(TAG, "handleWifiChange: " + localDatabase!!.isWifiPresent)
            ProxyApp.broadcastWifiChange(localBroadcastManager)
            if (localDatabase!!.isWifiPresent) {
                val currentTime = System.currentTimeMillis()
                localDatabase!!.setRefreshTime(currentTime + 5000, currentTime)
                handleSetupAlarm(startId, intent)
            }
            stopSelf(startId)
            WakefulBroadcastReceiver.completeWakefulIntent(intent)
        }

        private fun handleAlarmAction(startId: Int, intent: Intent) {
            if (LocalDatabase(service.context).lastIdentified == "fortinet") {
                handleFortinetRefresh(startId, intent)
            } else {
                handleIronPortRefresh(startId, intent)
            }
            WakefulBroadcastReceiver.completeWakefulIntent(intent)
        }

        private fun handleSetupAlarm(startId: Int, intent: Intent) {
            val alarmManager = this@ProxyService
                    .getSystemService(Service.ALARM_SERVICE) as AlarmManager
            val proxyBroadcastIntent = Intent(this@ProxyService.context,
                    AlarmReceiver::class.java)
            proxyBroadcastIntent.action = "proxy.service.ALARM_BROADCAST"
            Log.i(TAG, "handleSetupAlarm: Alarm for " + localDatabase!!.nextRefreshTime)
            alarmManager.set(AlarmManager.RTC, localDatabase!!.nextRefreshTime,
                    PendingIntent.getBroadcast(service.context, 5001, proxyBroadcastIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT))
            stopSelf(startId)
        }

        private fun handleIronPortRefresh(startId: Int, intent: Intent) {
            onGet(getString(R.string.ironport_url),
                    Response.Listener<String> {
                        if (localDatabase!!.username != null && localDatabase!!.password != null) {
                            val params = HashMap<String, String>()
                            params.put("sid", "0")
                            params.put("username", localDatabase!!.username)
                            params.put("password", localDatabase!!.password)
                            onPost(getString(R.string.ironport_url),
                                    Response.Listener<String> { }, Response.ErrorListener { }, params)
                        } else {
                            ProxyApp.broadcastRequestCredential("ironport",
                                    localBroadcastManager)
                        }
                    }, Response.ErrorListener { ProxyApp.broadcastCheckSession(localBroadcastManager) })
            stopSelf(startId)
        }

        private fun handleGetNetworkType(startId: Int, intent: Intent) {

            ProxyApp.broadcastProgress(null, "Looking for IITK network ...",
                    service.localBroadcastManager)

            onGet(getString(R.string.check_iitk_url), Response.Listener<String> {
                Log.i(TAG, "onResponse: in iitk network")
                ProxyApp.broadcastProgress(null, "IITK network found...",
                        service.localBroadcastManager)

                onGet(getString(R.string.check_iron_port_url),
                        Response.Listener<String> {
                            Log.i(TAG, "onResponse: in iitk ironport login")
                            ProxyApp.broadcastProgress("ironport",
                                    "IITK ironport network found...",
                                    service.localBroadcastManager)
                            requestCredentials("ironport")
                            stopSelf(startId)
                        }, Response.ErrorListener {
                    ProxyApp.broadcastProgress("fortinet",
                            "IITK fortinet network found...",
                            service.localBroadcastManager)
                    Log.i(TAG, "onResponse: in iitk fortinet login")
                    onGet(getString(R.string.fortinet_keep_alive_url),
                            Response.Listener<String> {
                                ProxyApp.broadcastLiveSession(System.currentTimeMillis(),
                                        service.localBroadcastManager)
                                Log.i(TAG, "onResponse: in iitk fortinet refreshed")
                                stopSelf(startId)
                            }, Response.ErrorListener {
                        ProxyApp.broadcastProgress(null,
                                "IITK fortinet: Signed out.",
                                service.localBroadcastManager)
                        Log.i(TAG, "onResponse: in iitk fortinet need login")
                        requestCredentials("fortinet")
                        stopSelf(startId)
                    })
                })
            }, Response.ErrorListener {
                ProxyApp.broadcastNotIITK(localBroadcastManager)
                stopSelf(startId)
            })
        }

        private fun handleFortinetLogout(startId: Int, intent: Intent) {
            onGet(service.getString(R.string.logout_url),
                    Response.Listener<String> {
                        ProxyApp.broadcastProgress(null, "IITK fortinet: Signed out.",
                                service.localBroadcastManager)
                        Log.i(TAG, "onResponse: in iitk fortinet session logged out")
                        requestCredentials("fortinet")
                        stopSelf(startId)
                    }, Response.ErrorListener {
                Log.i(TAG, "onResponse: in iitk fortinet session not present")
                ProxyApp.broadcastCheckSession(
                        service.localBroadcastManager)
                stopSelf(startId)
            })
        }

        private fun handleFortinetLogin(startId: Int, intent: Intent) {
            val username = intent.getStringExtra("username")
            val password = intent.getStringExtra("password")
            Log.i(TAG, "handleFortinetLogin: Username:password $username:$password")
            onGet(service.getString(R.string.default_ping_url),
                    Response.Listener<String> {
                        Log.i(TAG, "onResponse: already connected")
                        ProxyApp.broadcastCheckSession(service.localBroadcastManager)
                        stopSelf(startId)
                    }, Response.ErrorListener { error ->
                val response = error.networkResponse
                if (response != null) {
                    if (response.data != null) {
                        val st = String(response.data)
                        val magicUrl = st.substring(st.indexOf("\"") + 1,
                                st.lastIndexOf("\""))
                        Log.i(TAG, "onErrorResponse: doing magic request at " + magicUrl)
                        onGet(magicUrl, Response.Listener<String> { response ->
                            val magicValue = response.substring(response
                                    .indexOf("magic\" value=") + 14,
                                    response.lastIndexOf("\"><h1"))
                            Log.i(TAG, "onResponse: trying fortnet login with magic code" + magicValue)
                            val params = HashMap<String, String>()
                            params.put("4Tredir",
                                    service.getString(R.string.default_ping_url))
                            params.put("magic", magicValue)
                            params.put("username", username)
                            params.put("password", password)

                            onPost(service.getString(R.string.fortinet_login_url),
                                    Response.Listener<String> {
                                        Log.i(TAG, "onResponse: fortinet successful login")
                                        LocalDatabase(service.context)
                                                .setLogin(username, password)
                                        ProxyApp.broadcastLiveSession(
                                                System.currentTimeMillis(),
                                                service.localBroadcastManager)
                                        stopSelf(startId)
                                    }, Response.ErrorListener {
                                Log.i(TAG, "onResponse: fortinet incorrect " + "login credentials")
                                ProxyApp.broadcastIncorrectPassword(
                                        service.localBroadcastManager)
                                stopSelf(startId)
                            }, params)
                        }, Response.ErrorListener {
                            Log.i(TAG, "onErrorResponse: error while magic request")
                            ProxyApp.broadcastCheckSession(service.localBroadcastManager)
                            stopSelf(startId)
                        })
                    } else {
                        Log.i(TAG, "onErrorResponse: some error occurred retrying")
                        ProxyApp.broadcastCheckSession(service.localBroadcastManager)
                        stopSelf(startId)
                    }
                } else {
                    Log.i(TAG, "onErrorResponse: some error occurred retrying")
                    ProxyApp.broadcastCheckSession(service.localBroadcastManager)
                    stopSelf(startId)
                }
            })
        }

        private fun handleIronPortLogout(startId: Int, intent: Intent) {
            //todo IronPort Logout to be implemented
            ProxyApp.broadcastProgress(null, "IITK fortinet: Signed out.",
                    service.localBroadcastManager)
            Log.i(TAG, "handleIronPortLogout: in iitk ironport session logged out")
            requestCredentials("ironport")
            stopSelf(startId)
        }

        private fun handleIronPortLogin(startId: Int, intent: Intent) {
            //todo IronPort Login to be implemented
            ProxyApp.broadcastLiveSession(
                    System.currentTimeMillis(),
                    service.localBroadcastManager)
            Log.i(TAG, "handleIronPortLogin: iitk ironport session logedin")
            stopSelf(startId)
        }

        private fun handleFortinetRefresh(startId: Int, intent: Intent) {
            onGet(getString(R.string.fortinet_keep_alive_url),
                    Response.Listener<String> {
                        ProxyApp.broadcastLiveSession(System.currentTimeMillis(),
                                service.localBroadcastManager)
                        Log.i(TAG, "onResponse: in iitk fortinet refreshed")
                        stopSelf(startId)
                    }, Response.ErrorListener {
                Log.i(TAG, "onResponse: in iitk fortinet need login")
                ProxyApp.broadcastCheckSession(
                        service.localBroadcastManager)
                stopSelf(startId)
            })
        }
    }

    private fun requestCredentials(type: String) {
        ProxyApp.broadcastRequestCredential(type, localBroadcastManager)
    }

    private fun onGet(url: String, onResponse: Response.Listener<String>,
                      onError: Response.ErrorListener) {
        if (localDatabase!!.isWifiPresent) {
            proxyApp!!.addToRequestQueue(StringRequest(Request.Method.GET,
                    url, onResponse, onError), TAG)
        } else {
            ProxyApp.broadcastWifiChange(localBroadcastManager)
        }
    }

    private fun onPost(url: String, onResponse: Response.Listener<String>,
                       onError: Response.ErrorListener,
                       params: Map<String, String>) {
        if (localDatabase!!.isWifiPresent) {
            proxyApp!!.addToRequestQueue(
                    object : StringRequest(Request.Method.POST, url, onResponse, onError) {
                        @Throws(AuthFailureError::class)
                        override fun getParams(): Map<String, String> {
                            return params
                        }
                    }, TAG)
        } else {
            ProxyApp.broadcastWifiChange(localBroadcastManager)
        }
    }

    private fun checkWifiPresent(): Boolean {
        return true
    }

    override fun onBind(intent: Intent): IBinder? {
        return this.mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val msg = this.mServiceHandler!!.obtainMessage()
        msg.arg1 = startId
        msg.obj = intent
        when (intent.action) {
            "proxy.service.NETWORK_TYPE" -> msg.what = 1
            "proxy.service.FORTINET_LOGOUT" -> msg.what = 2
            "proxy.service.FORTINET_LOGIN" -> msg.what = 3
            "proxy.service.FORTINET_REFRESH" -> msg.what = 4
            "proxy.service.IRONPORT_LOGOUT" -> msg.what = 5
            "proxy.service.IRONPORT_LOGIN" -> msg.what = 6
            "proxy.service.IRONPORT_REFRESH" -> msg.what = 7
            "proxy.service.SETUP_ALARM" -> msg.what = 8
            "proxy.service.ALARM_BROADCAST" -> msg.what = 9
            "proxy.service.CAPTIVE_PORTAL" -> msg.what = 10
            "proxy.service.WIFI_STATE_CHANGE" -> msg.what = 11
            else -> msg.what = 11
        }
        this.mServiceHandler!!.sendMessage(msg)
        return Service.START_REDELIVER_INTENT
    }

    companion object {
        private val TAG = ProxyService::class.java.simpleName
    }
}
