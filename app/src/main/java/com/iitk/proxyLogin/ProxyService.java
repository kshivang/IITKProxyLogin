package com.iitk.proxyLogin;

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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.lang.System.currentTimeMillis;

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
            switch (msg.what) {
                case 1:
                    handleNewLogin(msg.arg1, (Intent)msg.obj);

            }
        }

        private void handleNewLogin(int startId, Intent intent) {

            onPing(getString(R.string.check_iitk_url), new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    onLogout(new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            //todo broadcast logout from fortinet

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            //todo broadcast not logout from fortinet

                        }
                    });
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //todo broadcast not iitk network
                }
            });



            long lastLogin = currentTimeMillis();
            ProxyService.this.sp.edit().putLong(getString(R.string.lastLogin),
                    lastLogin).apply();
            String lastLoginString = new SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                    .format(new Date(lastLogin));
            new SummaryNotification(ProxyService.this.context, "New sign in at" +
                    lastLoginString, null, null, R.mipmap.ic_launcher, null).show();

            ProxyApp.broadcastNewLogin(ProxyService.this.localBroadcastManager);
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
            ProxyService.this.stopSelf(startId);
        }

        private void onPing(String url, Response.Listener<String> onResponse,
                            Response.ErrorListener onError) {
            proxyApp.addToRequestQueue(new StringRequest(Request.Method.GET,
                    url, onResponse, onError), TAG);
        }

        private void onLogout(Response.Listener<String> onResponse,
                              Response.ErrorListener onError) {
            proxyApp.addToRequestQueue(new StringRequest(Request.Method.GET,
                    context.getString(R.string.logout), onResponse, onError), TAG);
        }
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

        return super.onStartCommand(intent, flags, startId);
    }
}
