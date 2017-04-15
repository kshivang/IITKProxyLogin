package com.iitk.proxyLogin;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.compat.BuildConfig;

/**
 * Created by kshivang on 22/01/17.
 */

class LocalDatabase {

    private SharedPreferences localDatabase;
    private static final String KEY_USERNAME = "u";
    private static final String KEY_PASSWORD = "p";
    private static final String KEY_RESET_URL = "r";
    private static final String KEY_RESET_TIME = "t";
    private static final String KEY_NEXT_RESET_TIME = "n";
    private static final String KEY_BROADCAST_MESSAGE = "m";
    private static final String KEY_LAST_IDENTIFIED = "i";
    private static final String KEY_FORTINET_UPDATE = "fu";
    private static final String KEY_IRONPORT_UPDATE = "iu";
    private static final String KEY_WIFI_STATE = "ws";
    private static final String KEY_WIFI_STATE_INTENT = "wsi";
    private Context mContext;

    LocalDatabase(Context context) {
        localDatabase = context.getSharedPreferences(BuildConfig.APPLICATION_ID,
                Context.MODE_PRIVATE);
        mContext = context;
    }

    void setLogin(@Nullable String username, @Nullable String password) {
        SharedPreferences.Editor spEditor = localDatabase.edit();
        spEditor.putString(KEY_USERNAME, username);
        spEditor.putString(KEY_PASSWORD, password);
        spEditor.apply();
    }

    void setRefreshTime(Long nextResetTime, Long resetTime) {
        SharedPreferences.Editor spEditor = localDatabase.edit();
        spEditor.putLong(KEY_NEXT_RESET_TIME, nextResetTime);
        spEditor.putLong(KEY_RESET_TIME, resetTime);
        spEditor.apply();
    }

    String getRefreshURL() {
        return localDatabase.getString(KEY_RESET_URL, mContext
                .getString(R.string.fortinet_keep_alive_url));
    }

    long getRefreshTime() {
        return localDatabase.getLong(KEY_RESET_TIME, -1L);
    }

    long getNextRefreshTime() {
        return localDatabase.getLong(KEY_NEXT_RESET_TIME, -1L);
    }

    String getUsername() {
        return localDatabase.getString(KEY_USERNAME, null);
    }

    String getPassword() {
        return localDatabase.getString(KEY_PASSWORD, null);
    }

    void setBroadcastMessage(@Nullable String message) {
        SharedPreferences.Editor spEditor = localDatabase.edit();
        spEditor.putString(KEY_BROADCAST_MESSAGE, message);
        spEditor.apply();
    }

    String getBroadcastMessage() {
        return localDatabase.getString(KEY_BROADCAST_MESSAGE, null);
    }

    void setLastIdentified(String type) {
        localDatabase.edit().putString(KEY_LAST_IDENTIFIED, type).apply();
    }

    String getLastIdentified() {
        return localDatabase.getString(KEY_LAST_IDENTIFIED, "non IITK");
    }

    long getIronPortRefresh() {
        return localDatabase.getLong(KEY_IRONPORT_UPDATE, 10 * 60000);
    }

    long getFortinetRefresh() {
        return localDatabase.getLong(KEY_FORTINET_UPDATE, 2 * 60000);
    }

    boolean isWifiPresent() {
        return  localDatabase.getBoolean(KEY_WIFI_STATE, false);
    }

    void setWifiState(boolean state) {
        localDatabase.edit().putBoolean(KEY_WIFI_STATE, state).apply();
    }
}
