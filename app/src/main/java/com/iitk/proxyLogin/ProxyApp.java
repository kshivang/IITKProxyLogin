package com.iitk.proxyLogin;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Locale;

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

    public static void broadcastProgress(@Nullable String type, String progress,
                                         LocalBroadcastManager localBroadcastManager) {
        if (type != null) new LocalDatabase(mInstance).setLastIdentified(type);

        SummaryNotification sn = new SummaryNotification(mInstance,
                progress, null, null, R.drawable.black_logo, null);
        Intent resultIntent = new Intent(mInstance, SessionActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = TaskStackBuilder.create(mInstance)
                .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        sn.setNotificationId(1001);
        sn.setContentIntent(pendingIntent);
        sn.mBuilder.setOngoing(true);
        sn.setTag(TAG);
        sn.show();

        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_PROGRESS")
                .putExtra("Progress", progress));
    }

    public static void broadcastRequestCredential(String type,
            LocalBroadcastManager localBroadcastManager) {

        LocalDatabase localDatabase = new LocalDatabase(mInstance);

        if (localDatabase.getUsername() == null || localDatabase.getPassword() == null) {
            SummaryNotification sn = new SummaryNotification(mInstance,
                    "Need IITK credentials!", null, null, R.drawable.black_logo, null);
            Intent resultIntent = new Intent(mInstance, LoginActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = TaskStackBuilder.create(mInstance)
                    .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            sn.setNotificationId(1001);
            sn.setContentIntent(pendingIntent);
            sn.mBuilder.setOngoing(true);
            sn.addAction(R.drawable.ic_login, "login", pendingIntent);
            sn.setTag(TAG);
            sn.show();
        } else {
            Intent proxyServiceIntent = new Intent(mInstance, ProxyService.class);
            switch (localDatabase.getLastIdentified()) {
                case "fortinet":
                    proxyServiceIntent.setAction("proxy.service.FORTINET_LOGIN");
                    proxyServiceIntent.putExtra("username", localDatabase.getUsername());
                    proxyServiceIntent.putExtra("password", localDatabase.getPassword());
                    break;
                case "ironport":
                    proxyServiceIntent.setAction("porxy.service.IRONPORT_LOGIN");
                    proxyServiceIntent.putExtra("username", localDatabase.getUsername());
                    proxyServiceIntent.putExtra("password", localDatabase.getPassword());
                    break;
                default:
                    proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
            }
        }

        new LocalDatabase(mInstance).setLastIdentified(type);
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_REQUEST_CREDENTIAL")
                .putExtra("Type", type));
    }

    public static void broadcastNotIITK(LocalBroadcastManager localBroadcastManager) {
        SummaryNotification.cancelNotification(mInstance, TAG, 1001);
        new LocalDatabase(mInstance).setLastIdentified("non IITK");
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_NOT_IITK"));
    }

    public static void broadcastLiveSession(long lastLogin,
                                            LocalBroadcastManager localBroadcastManager) {
        LocalDatabase localDatabase = new LocalDatabase(mInstance);

        long nextUpdate = lastLogin + (localDatabase.getLastIdentified().equals("fortinet") ?
                localDatabase.getFortinetRefresh() : localDatabase.getIronPortRefresh());

        localDatabase.setRefreshTime(nextUpdate, lastLogin);

        Intent proxyServiceIntent = new Intent(mInstance,
                ProxyService.class);
        proxyServiceIntent.setAction("proxy.service.SETUP_ALARM");
        mInstance.startService(proxyServiceIntent);

        SummaryNotification sn = new SummaryNotification(mInstance,
                "Logged in to IITK " + localDatabase.getLastIdentified() + " network", null, null,
                R.drawable.black_logo, null);
        Intent resultIntent = new Intent(mInstance, SessionActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = TaskStackBuilder.create(mInstance)
                .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        sn.addLineToBigView("Session refreshed at " + new SimpleDateFormat("HH:mm a",
                Locale.ENGLISH).format(lastLogin) + ".");
        sn.addLineToBigView("Will refresh at " + new SimpleDateFormat("HH:mm a",
                Locale.ENGLISH).format(nextUpdate));
        sn.setNotificationId(1001);
        sn.setContentIntent(pendingIntent);
        sn.mBuilder.setOngoing(true);
        sn.setTag(TAG);
        sn.show();

        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_LIVE_SESSION")
                .putExtra("Time", lastLogin));
    }

    public static void broadcastCheckSession(LocalBroadcastManager localBroadcastManager) {
        Intent proxyServiceIntent = new Intent(mInstance,
                ProxyService.class);
        proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
        mInstance.startService(proxyServiceIntent);
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_CHECK_SESSION"));
    }

    public static void broadcastIncorrectPassword(LocalBroadcastManager localBroadcastManager) {
        SummaryNotification sn = new SummaryNotification(mInstance,
                "Incorrect credentials!", null, null, R.drawable.black_logo, null);

        Intent resultIntent = new Intent(mInstance, LoginActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = TaskStackBuilder.create(mInstance)
                .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        sn.setNotificationId(1001);
        sn.setContentIntent(pendingIntent);
        sn.mBuilder.setOngoing(false);
        sn.addAction(R.drawable.ic_login, "login", pendingIntent);
        sn.setTag(TAG);
        sn.show();
        localBroadcastManager.sendBroadcast(new Intent("proxy.app.PROXY_INCORRECT_PASSWORD"));
    }

    public static void broadcastWifiChange(LocalBroadcastManager localBroadcastManager) {

        if (new LocalDatabase(mInstance).isWifiPresent()) {
            broadcastProgress(null, "Wifi connection found", localBroadcastManager);

            SummaryNotification sn = new SummaryNotification(mInstance,
                    "Wifi connection found", null, null,
                    R.drawable.black_logo, null);
            Intent resultIntent = new Intent(mInstance, SessionActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = TaskStackBuilder.create(mInstance)
                    .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            sn.setNotificationId(1001);
            sn.setContentIntent(pendingIntent);
            sn.mBuilder.setOngoing(true);
            sn.setTag(TAG);
            sn.show();
        } else {
            broadcastProgress(null, "Wifi connection not found", localBroadcastManager);
            SummaryNotification sn = new SummaryNotification(mInstance,
                    "No wifi connection found", null, null,
                    R.drawable.black_logo, null);
            Intent resultIntent = new Intent(mInstance, SessionActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = TaskStackBuilder.create(mInstance)
                    .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            sn.setNotificationId(1001);
            sn.setContentIntent(pendingIntent);
            sn.mBuilder.setOngoing(true);
            sn.setTag(TAG);
            sn.show();
        }
    }
}
