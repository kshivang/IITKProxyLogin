package com.iitk.proxyLogin.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.iitk.proxyLogin.R
import com.iitk.proxyLogin.service.ProxyService
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by kshivang on 22/01/17.

 */

class SplashActivity : AppCompatActivity() {

    private var isAnimationFinished = false
    private var tvProgress: TextView? = null
    private var localBroadcastManager: LocalBroadcastManager? = null

    private val proxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ("proxy.app.PROXY_PROGRESS" == intent.action) {
                tvProgress!!.text = intent.getStringExtra("Progress")
            } else if ("proxy.app.PROXY_REQUEST_CREDENTIAL" == intent.action) {
                val type = intent.getStringExtra("Type")
                tvProgress!!.text = "You are on IITK $type network"
                onFetched(true)
            } else if ("proxy.app.PROXY_LIVE_SESSION" == intent.action) {
                val lastLogin = intent.getLongExtra("Time",
                        System.currentTimeMillis())
                tvProgress!!.text = "IITK Fortinet refreshed at " + SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                        .format(Date(lastLogin))
                onFetched(false)
            } else {
                tvProgress!!.text = "You are not on IITK network"
                onFetched(false)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager!!.registerReceiver(proxyReceiver, makeProxyUpdatesIntentFilter())

        fetchType()

        val bounceAnim = AnimationUtils
                .loadAnimation(applicationContext, R.anim.bounce)

        tvProgress = findViewById<TextView>(R.id.progress_text)
        val tvTitle = findViewById<TextView>(R.id.appName)

        tvTitle.animation = bounceAnim

        bounceAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                isAnimationFinished = true
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
    }

    private fun fetchType() {
        val proxyServiceIntent = Intent(this, ProxyService::class.java)
        proxyServiceIntent.action = "proxy.service.NETWORK_TYPE"
        startService(proxyServiceIntent)
    }

    private fun onFetched(isLoginRequired: Boolean) {
        startActivity(Intent(this@SplashActivity, if (isLoginRequired)
            LoginActivity::class.java
        else
            SessionActivity::class.java))
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
        if (isAnimationFinished) {
            finish()
        }
    }

    companion object {


        fun makeProxyUpdatesIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction("proxy.app.PROXY_PROGRESS")
            intentFilter.addAction("proxy.app.PROXY_REQUEST_CREDENTIAL")
            intentFilter.addAction("proxy.app.PROXY_NOT_IITK")
            intentFilter.addAction("proxy.app.PROXY_LIVE_SESSION")
            intentFilter.addAction("proxy.app.NO_WIFI")
            return intentFilter
        }
    }
}
