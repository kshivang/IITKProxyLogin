package com.iitk.proxylogin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kshivang on 26/01/17.
 *
 */

class LogHandler {

    private Context mContext;
    private OnProgressListener listener;
    private VolleyController volleyController;
    private UserLocalDatabase localDatabase;


    static LogHandler newInstance(Context context, OnProgressListener listener) {
        return new LogHandler(context, listener);
    }

    private LogHandler(Context context, OnProgressListener onProgress) {
        mContext = context;
        listener = onProgress;
        volleyController = VolleyController.getInstance(context);
        localDatabase = new UserLocalDatabase(context);
    }

    interface OnProgressListener {
        void onProgress();
        void onFinish();
    }

    private void onPing(final String userName, final String password) {
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                StringRequest request = new StringRequest(Request.Method.GET,
                        mContext.getString(R.string.default_ping_url),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                listener.onFinish();
                                Toast.makeText(mContext, "You are not on " +
                                                "iitk fortinet network!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                NetworkResponse response = error.networkResponse;
                                if (response != null) {
                                    if (response.data != null) {
                                        String st = new String(response.data);
                                        String redirectUrl = st.substring(st.indexOf("\"") + 1,
                                                st.lastIndexOf("\""));
                                        onMagicRequest(userName, password, redirectUrl);
                                    } else {
                                        listener.onFinish();
                                        Toast.makeText(mContext, "Error Code: " +
                                                        response.statusCode,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    listener.onFinish();
                                    Toast.makeText(mContext, "Some error occurred",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                {
                    @Override
                    public Priority getPriority() {
                        return Priority.HIGH;
                    }
                };
                request.setShouldCache(false);
                volleyController.addToRequestQueue(request);
            }
        }, 1000);
    }

    private void onMagicRequest(final String username, final String password, String url) {
        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String magicValue = response.substring(response
                                        .indexOf("magic\" value=") + 14,
                                response.lastIndexOf("\"><h1"));
                        onLogin(username, password, magicValue);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(mContext, "Error in onMagicRequest",
                                Toast.LENGTH_SHORT).show();
                        listener.onFinish();
                    }
                }
        ) {
            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
        request.setShouldCache(false);
        volleyController.addToRequestQueue(request);

    }

    private void onLogin(final String username, final String password, final String magic) {
        StringRequest request = new StringRequest(Request.Method.POST,
                mContext.getString(R.string.login_url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        listener.onFinish();
                        String refreshURL = response.substring(response.indexOf(".href=\"") + 7,
                                response.lastIndexOf("\";"));
                        Toast.makeText(mContext, "Fortinet Logged in!",
                                Toast.LENGTH_SHORT).show();
                        localDatabase.setLogin(true, username, password, refreshURL,
                                Calendar.getInstance().getTimeInMillis());
                        mContext.startActivity(new Intent(mContext, MainActivity.class));
                        ((Activity)mContext).finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(mContext, "Check your username and password!",
                                Toast.LENGTH_SHORT).show();
                        listener.onFinish();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                params.put("4Tredir", mContext.getString(R.string.default_ping_url));
                params.put("magic", magic);
                params.put("username", username);
                params.put("password", password);
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
        request.setShouldCache(false);
        volleyController.addToRequestQueue(request);
    }

    void onLog(@Nullable final String username, @Nullable final String password) {
        listener.onProgress();
        StringRequest request = new StringRequest(Request.Method.GET,
                mContext.getString(R.string.logout_url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        if (username != null) {
                            onPing(username, password);
                        } else {
                            Toast.makeText(mContext, R.string.logged_out_message,
                                    Toast.LENGTH_SHORT).show();
                            listener.onFinish();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if (response != null && response.statusCode == 303) {
                    Toast.makeText(mContext, "Not currently logged in!",
                            Toast.LENGTH_SHORT).show();
                    if (username != null) {
                        onPing(username, password);
                    } else {
                        listener.onFinish();
                    }
                } else {
                    onPing(username, password);
//                    Toast.makeText(LoginActivity.this, "Network not working!",
//                            Toast.LENGTH_SHORT).show();
                }
            }
        }){
            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
        request.setShouldCache(false);
        volleyController.addToRequestQueue(request);
    }

}
