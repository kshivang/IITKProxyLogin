package com.iitk.proxyLogin;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.compat.BuildConfig;

/**
 * Created by kshivang on 22/01/17.
 */

class UserLocalDatabase {

    private SharedPreferences userLocalDatabase;
    private static final String KEY_LOGIN = "l";
    private static final String KEY_USERNAME = "u";
    private static final String KEY_PASSWORD = "p";
    private static final String KEY_RESET_URL = "r";
    private static final String KEY_RESET_TIME = "t";
    private static final String KEY_BROADCAST_MESSAGE = "m";

    UserLocalDatabase(Context context) {
        userLocalDatabase = context.getSharedPreferences(BuildConfig.APPLICATION_ID,
                Context.MODE_PRIVATE);
    }

    void setLogin(boolean logStatus, @Nullable String username,
                  @Nullable String password, @Nullable String resetUrl,
                  @Nullable Long time) {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putBoolean(KEY_LOGIN, logStatus);
        if (logStatus) {
            spEditor.putString(KEY_USERNAME, username);
            spEditor.putString(KEY_PASSWORD, password);
            spEditor.putString(KEY_RESET_URL, resetUrl);
            spEditor.putString(KEY_BROADCAST_MESSAGE, null);
            if (time != null)
                spEditor.putLong(KEY_RESET_TIME, time);
        } else {
            spEditor.remove(KEY_USERNAME);
            spEditor.remove(KEY_PASSWORD);
            spEditor.remove(KEY_RESET_URL);
            spEditor.remove(KEY_RESET_TIME);
        }
        spEditor.apply();
    }

    void setRefreshURL(String refreshURL, Long resetTime) {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString(KEY_RESET_URL, refreshURL);
        spEditor.putLong(KEY_RESET_TIME, resetTime);
        spEditor.putString(KEY_BROADCAST_MESSAGE, null);
        spEditor.apply();
    }

    String getRefreshURL() {
        return userLocalDatabase.getString(KEY_RESET_URL, "");
    }

    long getRefreshTime() {
        return userLocalDatabase.getLong(KEY_RESET_TIME, -1L);
    }

    String getUsername() {
        return userLocalDatabase.getString(KEY_USERNAME, "");
    }

    String getPassword() {
        return userLocalDatabase.getString(KEY_PASSWORD, "");
    }

    void setBroadcastMessage(@Nullable String message) {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString(KEY_BROADCAST_MESSAGE, message);
        spEditor.apply();
    }

    String getBroadcastMessage() {
        return userLocalDatabase.getString(KEY_BROADCAST_MESSAGE, null);
    }

    boolean isLogin() {
        return userLocalDatabase.getBoolean(KEY_LOGIN, false);
    }

}
