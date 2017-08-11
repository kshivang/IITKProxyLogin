package com.iitk.proxyLogin.misc

import android.content.Context
import android.content.SharedPreferences
import android.support.compat.BuildConfig

import com.iitk.proxyLogin.R

/**
 * Created by kshivang on 22/01/17.

 */

class LocalDatabase(private val mContext: Context) {

    private val localDatabase: SharedPreferences

    init {
        localDatabase = mContext.getSharedPreferences(BuildConfig.APPLICATION_ID,
                Context.MODE_PRIVATE)
    }

    fun setLogin(username: String?, password: String?) {
        val spEditor = localDatabase.edit()
        spEditor.putString(KEY_USERNAME, username)
        spEditor.putString(KEY_PASSWORD, password)
        spEditor.apply()
    }

    fun setRefreshTime(nextResetTime: Long?, resetTime: Long?) {
        val spEditor = localDatabase.edit()
        spEditor.putLong(KEY_NEXT_RESET_TIME, nextResetTime!!)
        spEditor.putLong(KEY_RESET_TIME, resetTime!!)
        spEditor.apply()
    }

    val refreshURL: String
        get() = localDatabase.getString(KEY_RESET_URL, mContext
                .getString(R.string.fortinet_keep_alive_url))

    val refreshTime: Long
        get() = localDatabase.getLong(KEY_RESET_TIME, -1L)

    val nextRefreshTime: Long
        get() = localDatabase.getLong(KEY_NEXT_RESET_TIME, -1L)

    val username: String
        get() = localDatabase.getString(KEY_USERNAME, null)

    val password: String
        get() = localDatabase.getString(KEY_PASSWORD, null)

    var broadcastMessage: String?
        get() = localDatabase.getString(KEY_BROADCAST_MESSAGE, null)
        set(message) {
            val spEditor = localDatabase.edit()
            spEditor.putString(KEY_BROADCAST_MESSAGE, message)
            spEditor.apply()
        }

    var lastIdentified: String
        get() = localDatabase.getString(KEY_LAST_IDENTIFIED, "non IITK")
        set(type) = localDatabase.edit().putString(KEY_LAST_IDENTIFIED, type).apply()

    val ironPortRefresh: Long
        get() = localDatabase.getLong(KEY_IRONPORT_UPDATE, (10 * 60000).toLong())

    val fortinetRefresh: Long
        get() = localDatabase.getLong(KEY_FORTINET_UPDATE, (2 * 60000).toLong())

    val isWifiPresent: Boolean
        get() = localDatabase.getBoolean(KEY_WIFI_STATE, false)

    fun setWifiState(state: Boolean) {
        localDatabase.edit().putBoolean(KEY_WIFI_STATE, state).apply()
    }

    companion object {
        private val KEY_USERNAME = "u"
        private val KEY_PASSWORD = "p"
        private val KEY_RESET_URL = "r"
        private val KEY_RESET_TIME = "t"
        private val KEY_NEXT_RESET_TIME = "n"
        private val KEY_BROADCAST_MESSAGE = "m"
        private val KEY_LAST_IDENTIFIED = "i"
        private val KEY_FORTINET_UPDATE = "fu"
        private val KEY_IRONPORT_UPDATE = "iu"
        private val KEY_WIFI_STATE = "ws"
        private val KEY_WIFI_STATE_INTENT = "wsi"
    }
}
