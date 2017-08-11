package com.iitk.proxyLogin.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.iitk.proxyLogin.R
import com.iitk.proxyLogin.misc.LocalDatabase
import com.iitk.proxyLogin.service.ProxyService
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by kshivang on 13/04/17.

 */

class SessionActivity : AppCompatActivity() {

    private var tvProgress: TextView? = null
    private var btRefresh: Button? = null
    private var btLogout: Button? = null
    private var localDatabase: LocalDatabase? = null
    private var localBroadcastManager: LocalBroadcastManager? = null

    private val proxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "proxy.app.PROXY_PROGRESS" -> tvProgress!!.text = intent.getStringExtra("Progress")
                "proxy.app.PROXY_REQUEST_CREDENTIAL" -> onFetched(true)
                "proxy.app.PROXY_LIVE_SESSION" -> onFetched(false)
                "proxy.app.PROXY_CHECK_SESSION" -> tvProgress!!.text = "retrying.."
                "proxy.app.PROXY_INCORRECT_PASSWORD" -> {
                    localDatabase!!.setLogin(null, null)
                    onFetched(true)
                }
                else -> onFetched(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localDatabase = LocalDatabase(this)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        setContentView(R.layout.activity_session)
        tvProgress = findViewById<View>(R.id.primaryText) as TextView
        btLogout = findViewById<View>(R.id.logout) as Button
        btRefresh = findViewById<View>(R.id.refresh) as Button

        onRefreshUI(localDatabase!!.lastIdentified)
    }

    override fun onPause() {
        super.onPause()
        if (this.localBroadcastManager != null) {
            localBroadcastManager!!.unregisterReceiver(proxyReceiver)
        }
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager!!.registerReceiver(proxyReceiver, makeProxyUpdatesIntentFilter())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            this.moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    internal fun onRefreshUI(lastIdentified: String) {
        enableButtons(true)
        if (localDatabase!!.isWifiPresent) {
            when (lastIdentified) {
                "fortinet" -> {
                    Toast.makeText(this, "Fortinet session refreshed!", Toast.LENGTH_SHORT).show()
                    btLogout!!.visibility = View.VISIBLE
                    tvProgress!!.text = "IITK fortinet network: will refresh at " + SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                            .format(localDatabase!!.nextRefreshTime)
                }
                "ironport" -> {
                    Toast.makeText(this, "Ironport session refreshed!", Toast.LENGTH_SHORT).show()
                    btLogout!!.visibility = View.VISIBLE
                    tvProgress!!.text = "IITK ironport network: will refresh at " + SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                            .format(localDatabase!!.nextRefreshTime)
                }
                else -> {
                    tvProgress!!.text = "You are not on IITK network"
                    btLogout!!.visibility = View.INVISIBLE
                }
            }
        } else {
            //            enableButtons(false);
            tvProgress!!.text = "No wifi connection found!"
        }

    }

    private fun onFetched(isLoginRequired: Boolean) {
        if (isLoginRequired) {
            if (localDatabase!!.username == null || localDatabase!!.password == null) {
                startActivity(Intent(this@SessionActivity,
                        LoginActivity::class.java))
            } else {
                val proxyServiceIntent = Intent(this, ProxyService::class.java)
                when (localDatabase!!.lastIdentified) {
                    "fortinet" -> {
                        proxyServiceIntent.action = "proxy.service.FORTINET_LOGIN"
                        proxyServiceIntent.putExtra("username", localDatabase!!.username)
                        proxyServiceIntent.putExtra("password", localDatabase!!.password)
                    }
                    "ironport" -> {
                        proxyServiceIntent.action = "porxy.service.IRONPORT_LOGIN"
                        proxyServiceIntent.putExtra("username", localDatabase!!.username)
                        proxyServiceIntent.putExtra("password", localDatabase!!.password)
                    }
                    else -> proxyServiceIntent.action = "proxy.service.NETWORK_TYPE"
                }
            }
        } else {
            onRefreshUI(localDatabase!!.lastIdentified)
        }
    }

    private fun enableButtons(isEnable: Boolean) {
        btRefresh!!.isEnabled = isEnable
        btLogout!!.isEnabled = isEnable
    }

    fun onRefreshClick(view: View) {
        val proxyServiceIntent = Intent(this, ProxyService::class.java)
        when (localDatabase!!.lastIdentified) {
            "fortinet" -> proxyServiceIntent.action = "proxy.service.FORTINET_REFRESH"
            "ironport" -> proxyServiceIntent.action = "proxy.service.IRONPORT_REFRESH"
            else -> proxyServiceIntent.action = "proxy.service.NETWORK_TYPE"
        }
        startService(proxyServiceIntent)
        enableButtons(false)
    }

    fun onLogoutClick(view: View) {
        val proxyServiceIntent = Intent(this, ProxyService::class.java)
        when (localDatabase!!.lastIdentified) {
            "fortinet" -> proxyServiceIntent.action = "proxy.service.FORTINET_LOGOUT"
            "ironport" -> proxyServiceIntent.action = "proxy.service.IRONPORT_LOGOUT"
            else -> view.visibility = View.INVISIBLE
        }
        startService(proxyServiceIntent)
        enableButtons(false)
    }

    companion object {

        fun makeProxyUpdatesIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction("proxy.app.PROXY_PROGRESS")
            intentFilter.addAction("proxy.app.PROXY_REQUEST_CREDENTIAL")
            intentFilter.addAction("proxy.app.PROXY_NOT_IITK")
            intentFilter.addAction("proxy.app.PROXY_LIVE_SESSION")
            intentFilter.addAction("proxy.app.PROXY_CHECK_SESSION")
            intentFilter.addAction("proxy.app.PROXY_INCORRECT_PASSWORD")
            intentFilter.addAction("proxy.app.WIFI_STATE_CHANGE")
            return intentFilter
        }
    }
}
