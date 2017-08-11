package com.iitk.proxyLogin.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.iitk.proxyLogin.R
import com.iitk.proxyLogin.misc.LocalDatabase
import com.iitk.proxyLogin.service.ProxyService

/**
 * Created by kshivang on 22/01/17.

 */

class LoginActivity : AppCompatActivity() {

    private var tilUserName: TextInputLayout? = null
    private var tilPassword: TextInputLayout? = null
    private var progressBar: ProgressBar? = null
    private var tvProgress: TextView? = null
    private var localDatabase: LocalDatabase? = null
    private var btLogin: Button? = null

    private var localBroadcastManager: LocalBroadcastManager? = null

    private val proxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "proxy.app.PROXY_PROGRESS" -> tvProgress!!.text = intent.getStringExtra("Progress")
                "proxy.app.PROXY_REQUEST_CREDENTIAL" -> onFetched(true)
                "proxy.app.PROXY_LIVE_SESSION" -> onFetched(false)
                "proxy.app.PROXY_CHECK_SESSION" -> tvProgress!!.text = "retrying.."
                "proxy.app.PROXY_INCORRECT_PASSWORD" -> onIncorrectPassword()
                "proxy.app.WIFI_STATE_CHANGE" -> {
                    if (localDatabase!!.isWifiPresent) {
                        btLogin!!.isEnabled = true
                        tvProgress!!.text = "Wifi connection found"
                    } else {
                        btLogin!!.isEnabled = false
                        tvProgress!!.text = "No wifi connection found"
                        progressBar!!.visibility = View.INVISIBLE
                    }
                    onFetched(false)
                }
                else -> onFetched(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localDatabase = LocalDatabase(this)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager!!.registerReceiver(proxyReceiver, makeProxyUpdatesIntentFilter())

        setContentView(R.layout.activity_login)

        tilUserName = findViewById<TextInputLayout>(R.id.userNameInput)
        tilPassword = findViewById<TextInputLayout>(R.id.passwordInput)
        progressBar = findViewById<ProgressBar>(android.R.id.progress)
        tvProgress = findViewById<TextView>(R.id.primaryText)
        btLogin = findViewById<Button>(R.id.login)

        val etUserName = tilUserName!!.editText
        val etPassword = tilPassword!!.editText

        if (etUserName != null && etPassword != null) {
            etUserName.setText(if (localDatabase!!.username != null)
                localDatabase!!.username
            else
                "")
            etPassword.setText(if (localDatabase!!.password != null)
                localDatabase!!.password
            else
                "")
            etPassword.setOnEditorActionListener(TextView.OnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    onLoginClick(btLogin!!)
                    return@OnEditorActionListener true
                }
                false
            })
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            this.moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
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

    fun onLoginClick(view: View) {
        hideKeyBoard(view)
        val etUserName = tilUserName!!.editText
        val etPassword = tilPassword!!.editText
        if (etUserName != null && etPassword != null &&
                !TextUtils.isEmpty(etUserName.text)
                && !TextUtils.isEmpty(etPassword.text)) {
            btLogin!!.isEnabled = false
            progressBar!!.visibility = View.VISIBLE
            val proxyServiceIntent = Intent(this,
                    ProxyService::class.java)
            proxyServiceIntent.putExtra("username", etUserName.text.toString())
            proxyServiceIntent.putExtra("password", etPassword.text.toString())
            when (localDatabase!!.lastIdentified) {
                "fortinet" -> proxyServiceIntent.action = "proxy.service.FORTINET_LOGIN"
                "ironport" -> proxyServiceIntent.action = "proxy.service.IRONPORT_LOGIN"
                else -> proxyServiceIntent.action = "proxy.service.NETWORK_TYPE"
            }
            startService(proxyServiceIntent)
        } else {
            btLogin!!.isEnabled = true
            progressBar!!.visibility = View.INVISIBLE
            Toast.makeText(this, "Username and Password are required fields!",
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun onFetched(isLoginRequired: Boolean) {
        if (isLoginRequired) {
            onLoginClick(btLogin!!)
        } else {
            progressBar!!.visibility = View.INVISIBLE
            startActivity(Intent(this@LoginActivity, SessionActivity::class.java))
        }
    }

    private fun onIncorrectPassword() {
        progressBar!!.visibility = View.INVISIBLE
        tvProgress!!.text = "Incorrect credentials!"
        btLogin!!.isEnabled = true
    }

    fun hideKeyBoard(view: View?) {
        if (view != null) {
            val inputMethodManager = getSystemService(
                    Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
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
