package com.iitk.proxylogin;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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

import static com.iitk.proxylogin.MyTaskService.mId;

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
        void onProgress(String message);
        void onFinish(String message);
    }

    private void onPing(final String userName, final String password) {
        listener.onProgress("Pinging at..");
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                StringRequest request = new StringRequest(Request.Method.GET,
                        mContext.getString(R.string.default_ping_url),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                listener.onFinish("You are not on iitk fortinet network");
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
                                        listener.onFinish("Unknown error while ping!");
                                        Toast.makeText(mContext, "Error Code: " +
                                                        response.statusCode,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    listener.onFinish("Error while ping!");
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
        listener.onProgress("Magic request..");
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
                        listener.onFinish("Error in magic request!");
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
        listener.onProgress("Login..");
        StringRequest request = new StringRequest(Request.Method.POST,
                mContext.getString(R.string.login_url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        listener.onFinish("Logged in!");
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
                        listener.onFinish("Check your credentials!");
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
        listener.onProgress("Logging out..");
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
                            listener.onFinish("Logged out!");
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
                        listener.onFinish("Error logging out!");
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

    public void showNotification(String contentText, Class activity, boolean unDestroyable) {
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.black_logo)
                        .setContentTitle("Proxy Login")
                        .setContentText(contentText)
                        .setOngoing(unDestroyable);
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(mContext, activity);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(activity);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

// mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }

}
