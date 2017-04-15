package com.iitk.proxyLogin;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

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
    private Context context;

    public ProxyService() {
        this.context = this;
        this.mBinder = new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.proxyApp = ProxyApp.getInstance();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.context);
        HandlerThread thread = new HandlerThread("ServiceStartArguments", 10);
        thread.start();
        this.mServiceHandler = new ProxyServiceHandler(thread.getLooper(), this);
    }

    private class ProxyServiceHandler extends Handler {
        private final String TAG;
        private ProxyService service;
        private Context context;

        ProxyServiceHandler(Looper looper, ProxyService proxyService) {
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
                    break;
                case 2:
                    handleFortinetLogout(msg.arg1, (Intent)msg.obj);
                    break;
                case 3:
                    handleFortinetLogin(msg.arg1, (Intent)msg.obj);
                    break;
                case 4:
                    handleFortinetRefresh(msg.arg1, (Intent)msg.obj);
                    break;
                case 5:
                    handleIronPortLogout(msg.arg1, (Intent)msg.obj);
                    break;
                case 6:
                    handleIronPortLogin(msg.arg1, (Intent)msg.obj);
                    break;
                default:
                    handleIronPortRefresh(msg.arg1, (Intent)msg.obj);
            }
        }

        private void handleIronPortRefresh(int startId, Intent intent) {
            // todo ironport refresh tobe implemented
            stopSelf(startId);
        }

        private void handleGetNetworkType(final int startId, Intent intent) {

            ProxyApp.broadcastProgress(null, "Looking for IITK network ...",
                    service.localBroadcastManager);

            onGet(getString(R.string.check_iitk_url), new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i(TAG, "onResponse: in iitk network");
                    ProxyApp.broadcastProgress(null, "IITK network found...",
                            service.localBroadcastManager);

                    onGet(getString(R.string.check_iron_port_url),
                            new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.i(TAG, "onResponse: in iitk ironport login");
                            ProxyApp.broadcastProgress("ironport",
                                    "IITK ironport network found...",
                                    service.localBroadcastManager);
                            requestCredentials("ironport");
                            stopSelf(startId);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            ProxyApp.broadcastProgress("fortinet",
                                    "IITK fortinet network found...",
                                    service.localBroadcastManager);
                            Log.i(TAG, "onResponse: in iitk fortinet login");
                            onGet(getString(R.string.fortinet_keep_alive_url),
                                    new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    ProxyApp.broadcastLiveSession(System.currentTimeMillis(),
                                            service.localBroadcastManager);
                                    Log.i(TAG, "onResponse: in iitk fortinet refreshed");
                                    stopSelf(startId);
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    ProxyApp.broadcastProgress(null,
                                            "IITK fortinet: Signed out.",
                                            service.localBroadcastManager);
                                    Log.i(TAG, "onResponse: in iitk fortinet need login");
                                    requestCredentials("fortinet");
                                    stopSelf(startId);
                                }
                            });
                        }
                    });

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ProxyApp.broadcastNotIITK(localBroadcastManager);
                    stopSelf(startId);
                }
            });
        }

        private void handleFortinetLogout(final int startId, Intent intent) {
            onGet(service.getString(R.string.logout_url),
                    new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    ProxyApp.broadcastProgress(null, "IITK fortinet: Signed out.",
                            service.localBroadcastManager);
                    Log.i(TAG, "onResponse: in iitk fortinet session logged out");
                    requestCredentials("fortinet");
                    stopSelf(startId);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i(TAG, "onResponse: in iitk fortinet session not present");
                    ProxyApp.broadcastCheckSession(
                            service.localBroadcastManager);
                    stopSelf(startId);
                }
            });
        }

        private void handleFortinetLogin(final int startId, final Intent intent) {
            final String username = intent.getStringExtra("username");
            final String password = intent.getStringExtra("password");
            Log.i(TAG, "handleFortinetLogin: Username:password " + username + ":" + password);
            onGet(service.getString(R.string.default_ping_url),
                    new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i(TAG, "onResponse: already connected");
                    ProxyApp.broadcastCheckSession(service.localBroadcastManager);
                    stopSelf(startId);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse response = error.networkResponse;
                    if (response != null)  {
                        if (response.data != null) {
                            String st = new String(response.data);
                            String magicUrl = st.substring(st.indexOf("\"") + 1,
                                    st.lastIndexOf("\""));
                            Log.i(TAG, "onErrorResponse: doing magic request at " + magicUrl);
                            onGet(magicUrl, new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    String magicValue = response.substring(response
                                                    .indexOf("magic\" value=") + 14,
                                            response.lastIndexOf("\"><h1"));
                                    Log.i(TAG, "onResponse: trying fortnet login with magic code"
                                            + magicValue);
                                    Map<String, String> params = new HashMap<>();
                                    params.put("4Tredir",
                                            service.getString(R.string.default_ping_url));
                                    params.put("magic", magicValue);
                                    params.put("username", username);
                                    params.put("password", password);

                                    onPost(service.getString(R.string.fortinet_login_url),
                                            new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            Log.i(TAG, "onResponse: fortinet successful login");
                                            new LocalDatabase(service.context)
                                                    .setLogin(username, password);
                                            ProxyApp.broadcastLiveSession(
                                                    System.currentTimeMillis(),
                                                    service.localBroadcastManager);
                                            stopSelf(startId);
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.i(TAG, "onResponse: fortinet incorrect " +
                                                    "login credentials");
                                            ProxyApp.broadcastIncorrectPassword(
                                                    service.localBroadcastManager);
                                            stopSelf(startId);
                                        }
                                    }, params);


                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.i(TAG, "onErrorResponse: error while magic request");
                                    ProxyApp.broadcastCheckSession(service.localBroadcastManager);
                                    stopSelf(startId);
                                }
                            });
                        } else {
                            Log.i(TAG, "onErrorResponse: some error occurred retrying");
                            ProxyApp.broadcastCheckSession(service.localBroadcastManager);
                            stopSelf(startId);
                        }
                    } else {
                        Log.i(TAG, "onErrorResponse: some error occurred retrying");
                        ProxyApp.broadcastCheckSession(service.localBroadcastManager);
                        stopSelf(startId);
                    }
                }
            });
        }

        private void handleIronPortLogout(int startId, Intent intent) {
            //todo IronPort Logout to be implemented
            ProxyApp.broadcastProgress(null, "IITK fortinet: Signed out.",
                    service.localBroadcastManager);
            Log.i(TAG, "handleIronPortLogout: in iitk ironport session logged out");
            requestCredentials("ironport");
            stopSelf(startId);
        }

        private void handleIronPortLogin(int startId, Intent intent) {
            //todo IronPort Login to be implemented
            ProxyApp.broadcastLiveSession(
                    System.currentTimeMillis(),
                    service.localBroadcastManager);
            Log.i(TAG, "handleIronPortLogin: iitk ironport session logedin");
            stopSelf(startId);
        }

        private void handleFortinetRefresh (final int startId, Intent intent) {
            onGet(getString(R.string.fortinet_keep_alive_url),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            ProxyApp.broadcastLiveSession(System.currentTimeMillis(),
                                    service.localBroadcastManager);
                            Log.i(TAG, "onResponse: in iitk fortinet refreshed");
                            stopSelf(startId);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i(TAG, "onResponse: in iitk fortinet need login");
                            ProxyApp.broadcastCheckSession(
                                    service.localBroadcastManager);
                            stopSelf(startId);
                        }
                    });
        }
    }

    private void requestCredentials(String type) {
        SummaryNotification sn = new SummaryNotification(this.context,
                "Need IITK credentials!", null, null, R.drawable.black_logo, null);
        Intent resultIntent = new Intent(this.context, LoginActivity.class);
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

    private void onPost(String url, Response.Listener<String> onResponse,
                         Response.ErrorListener onError,
                        final Map<String, String> params) {
        proxyApp.addToRequestQueue(
                new StringRequest(Request.Method.POST, url, onResponse, onError) 
                {
                   @Override
                   protected Map<String, String> getParams()
                           throws AuthFailureError {
                       return params;
                   }
                }
                , TAG);
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
            msg.what = 1;
        } else if (intent.getAction().equals("proxy.service.FORTINET_LOGOUT")) {
            msg.what = 2;
        } else if (intent.getAction().equals("proxy.service.FORTINET_LOGIN")) {
            msg.what = 3;
        } else if (intent.getAction().equals("proxy.service.FORTINET_REFRESH")){
            msg.what = 4;
        } else if (intent.getAction().equals("proxy.service.IRONPORT_LOGOUT")) {
            msg.what = 5;
        } else if (intent.getAction().equals("proxy.service.IRONPORT_LOGIN")){
            msg.what = 6;
        } else {
            msg.what = 7;
        }
        this.mServiceHandler.sendMessage(msg);
        return Service.START_REDELIVER_INTENT;
    }
}
