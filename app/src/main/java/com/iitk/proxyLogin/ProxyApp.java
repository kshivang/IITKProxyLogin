package com.iitk.proxyLogin;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.List;

/**
 * Created by kshivang on 12/04/17.
 *
 */

public class ProxyApp extends Application {

    public static final String TAG = ProxyApp.class.getSimpleName();
    private static ProxyApp mInstance;
    private static RequestQueue mRequestQueue;


    public static synchronized ProxyApp getInstance() {
        ProxyApp proxyApp;
        synchronized (ProxyApp.class) {
            proxyApp = mInstance;
        }
        return proxyApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        if (!isBackgroundProcess()) {

        }
    }

    private boolean isBackgroundProcess() {
        String currentProcessName = BuildConfig.VERSION_NAME;
        int pid = Process.myPid();
        List<RunningAppProcessInfo> runningProcessInfo =
                ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE))
                        .getRunningAppProcesses();
        if (!(runningProcessInfo == null || runningProcessInfo.isEmpty())) {
            for (RunningAppProcessInfo processInfo : runningProcessInfo) {
                if (processInfo.pid == pid) {
                    currentProcessName = processInfo.processName;
                    break;
                }
            }
        }
        return currentProcessName.contains("background");
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        if (TextUtils.isEmpty(tag)) {
            tag = TAG;
        }
        req.setTag(tag);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            Log.i(TAG, "Cancelling all queries for TAG : " + tag);
            mRequestQueue.cancelAll(tag);
        }
    }

    public static void broadcastRequestComplete(
            LocalBroadcastManager localBroadcastManager) {
        localBroadcastManager.sendBroadcast(
                new Intent("ProxyLogin.app.PROXY_UPDATE_INSIGHTS"));
    }

    public static void broadcastNewLogin(
            LocalBroadcastManager localBroadcastManager) {
        localBroadcastManager.sendBroadcast(
                new Intent("ProxyLogin.app.PROXY_NEW_LOGIN"));
    }
}
