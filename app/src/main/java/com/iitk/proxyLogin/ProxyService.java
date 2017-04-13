package com.iitk.proxyLogin;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

/**
 * Created by kshivang on 12/04/17.
 *
 */

public class ProxyService extends Service {

    private Binder mBinder;
    private static final String TAG = ProxyService.class.getSimpleName();
    private LocalBroadcastManager localBroadcastManager;
    private ProxyServiceHandler mServiceHandler;
    private ProxyApp proxyApp;
    private SharedPreferences sp;
    private Context context;

    public ProxyService() {
        this.context = this;
        this.mBinder = new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.proxyApp = ProxyApp.getInstance();
        this.sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.context);
        HandlerThread thread = new HandlerThread("ServiceStartArguments", 10);
        thread.start();
        this.mServiceHandler = new ProxyServiceHandler(thread.getLooper(), this);
    }

    private class ProxyServiceHandler extends Handler {
        private final String TAG;
        private ProxyService service;

        public ProxyServiceHandler(Looper looper, ProxyService proxyService) {
            super(looper);
            this.TAG = ProxyServiceHandler.class.getSimpleName();
            this.service = proxyService;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    handleGetNetworkType(msg.arg1, (Intent)msg.obj);
                case 2:
                    handleFortinetLogout(msg.arg1, (Intent)msg.obj);
                case 3:
                    handleFortinetLogin(msg.arg1, (Intent)msg.obj);
            }
        }

        private void handleGetNetworkType(int startId, Intent intent) {

            ProxyApp.broadcastProgress("Looking for IITK network ...",
                    ProxyService.this.localBroadcastManager);

            onGet(getString(R.string.check_iitk_url), new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i(TAG, "onResponse: in iitk network");
                    ProxyApp.broadcastProgress("IITK network found...",
                            ProxyService.this.localBroadcastManager);

                    onGet(getString(R.string.check_iron_port_url), new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.i(TAG, "onResponse: in iitk ironport login");
                            ProxyApp.broadcastProgress("IITK ironport network found...",
                                    ProxyService.this.localBroadcastManager);
                            requestCredentials("ironport");
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            ProxyApp.broadcastProgress("IITK fortinet network found...",
                                    ProxyService.this.localBroadcastManager);
                            Log.i(TAG, "onResponse: in iitk fortinet login");
                            onGet(getString(R.string.fortinet_keep_alive_url),
                                    new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    ProxyApp.broadcastLiveSession(System.currentTimeMillis(),
                                            ProxyService.this.localBroadcastManager);
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    ProxyApp.broadcastProgress("IITK fortinet: Signed out.",
                                            ProxyService.this.localBroadcastManager);
                                    requestCredentials("fortinet");
                                }
                            });
                        }
                    });

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ProxyApp.broadcastNotIITK(localBroadcastManager);
                }
            });
        }

        private void handleFortinetLogout (final int startId, Intent intent) {
            onFortinetLogout(new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i(TAG, "onResponse: in iitk fortinet session logged out");
                    stopSelf(startId);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i(TAG, "onResponse: in iitk fortinet session not present");
                    stopSelf(startId);
                }
            });
        }

        private void handleFortinetLogin(int arg1, Intent obj) {

        }
    }

    private void requestCredentials(String type) {
        SummaryNotification sn = new SummaryNotification(this.context,
                "Need IITK credentials!", null, null, R.mipmap.ic_launcher, null);
        Intent resultIntent = new Intent(ProxyService.this.context, LoginActivity.class);
        resultIntent.setAction(type);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = TaskStackBuilder.create(this.context)
                .addNextIntentWithParentStack(resultIntent).getPendingIntent(1001,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        sn.setNotificationId(1001);
        sn.setContentIntent(pendingIntent);
        sn.show();
        ProxyApp.broadcastRequestCredential(type, localBroadcastManager);
    }

    private void onGet(String url, Response.Listener<String> onResponse,
                       Response.ErrorListener onError) {
        proxyApp.addToRequestQueue(new StringRequest(Request.Method.GET,
                url, onResponse, onError), TAG);
    }

    private void onFortinetLogout(Response.Listener<String> onResponse,
                                  Response.ErrorListener onError) {
        onGet(this.context.getString(R.string.logout_url), onResponse, onError);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = this.mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        if (intent.getAction().equals("proxy.service.NETWORK_TYPE")) {
            if (intent.getBooleanExtra("Finish", false)) {
                stopSelf(startId);
                return Service.START_NOT_STICKY;
            }
            msg.what = 1;
            this.mServiceHandler.sendMessage(msg);
        }
        return Service.START_REDELIVER_INTENT;
    }
}
