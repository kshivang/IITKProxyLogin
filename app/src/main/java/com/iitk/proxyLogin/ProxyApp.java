package com.iitk.proxyLogin;

import android.app.Application;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by kshivang on 12/04/17.
 *
 */

public class ProxyApp extends Application {

    public static final String TAG = ProxyApp.class.getSimpleName();
    private static ProxyApp mInstance;
    private static RequestQueue mRequestQueue;
    private static LocalDatabase localDatabase;


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
        localDatabase = new LocalDatabase(this);
//        if (!isBackgroundProcess()) {
//
//        }
    }

//    private boolean isBackgroundProcess() {
//        String currentProcessName = BuildConfig.VERSION_NAME;
//        int pid = Process.myPid();
//        List<RunningAppProcessInfo> runningProcessInfo =
//                ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE))
//                        .getRunningAppProcesses();
//        if (!(runningProcessInfo == null || runningProcessInfo.isEmpty())) {
//            for (RunningAppProcessInfo processInfo : runningProcessInfo) {
//                if (processInfo.pid == pid) {
//                    currentProcessName = processInfo.processName;
//                    break;
//                }
//            }
//        }
//        return currentProcessName.contains("background");
//    }

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

    public static void broadcastProgress(String progress,
            LocalBroadcastManager localBroadcastManager) {
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_PROGRESS")
                .putExtra("Progress", progress));
    }

    public static void broadcastRequestCredential(String type,
            LocalBroadcastManager localBroadcastManager) {
        localDatabase.setLastIdentified(type);
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_REQUEST_CREDENTIAL")
                .putExtra("Type", type));
    }

    public static void broadcastNotIITK(LocalBroadcastManager localBroadcastManager) {
        localDatabase.setLastIdentified("non IITK");
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_NOT_IITK"));
    }

    public static void broadcastLiveSession(long lastLogin,
                                            LocalBroadcastManager localBroadcastManager) {
        localDatabase.setRefreshURL(
                "https://gateway.iitk.ac.in:1003/keepalive?0f0103060f243720", lastLogin);
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_LIVE_SESSION")
                .putExtra("Time", lastLogin));
    }

    public static void broadcastCheckSession(LocalBroadcastManager localBroadcastManager) {
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_CHECK_SESSION"));
    }
}
