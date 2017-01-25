package com.iitk.proxylogin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.iitk.proxylogin.MyTaskService.mId;

/**
 * Created by kshivang on 22/01/17.
 *
 */

public class MainActivity extends AppCompatActivity {

    private TextView tvPrimaryText;
    private UserLocalDatabase localDatabase;
    private VolleyController volleyController;

    public static final String TASK_TAG_REFRESH = "refresh_session";

    private GcmNetworkManager gcmNetworkManager;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPrimaryText = (TextView) findViewById(R.id.primaryText);
        gcmNetworkManager = GcmNetworkManager.getInstance(this);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MyTaskService.ACTION_DONE)) {
                    String tag = intent.getStringExtra(MyTaskService.EXTRA_TAG);
                    int result = intent.getIntExtra(MyTaskService.EXTRA_RESULT, -1);
                    switch (tag){
                        case TASK_TAG_REFRESH:
                            if (result == GcmNetworkManager.RESULT_SUCCESS) {
                                timerStart();
                            } else {
                                timer.cancel();
                                ConnectivityManager connManager = (ConnectivityManager)
                                        getSystemService(Context.CONNECTIVITY_SERVICE);
                                NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                                if (activeNetwork != null && activeNetwork.getType() ==
                                        ConnectivityManager.TYPE_WIFI) {
                                    tvPrimaryText.setText(R.string.some_error);
                                } else {
                                    tvPrimaryText.setText(getString(R.string.no_wifi_found));
                                }
                            }
                            break;
                    }
                }
            }
        };
        localDatabase = new UserLocalDatabase(MainActivity.this);
        volleyController = VolleyController.getInstance(MainActivity.this);


        Date d = new Date(localDatabase.getRefreshTime());
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a",
                Locale.ENGLISH);
        String currentDateTimeString = sdf.format(d);
        showNotification("Refreshed at " + currentDateTimeString, MainActivity.class,  true);

        ConnectivityManager connManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            liveSessionInBackground();
            timerStart();
        } else {
            tvPrimaryText.setText(getString(R.string.no_wifi_found));
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTaskService.ACTION_DONE);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(receiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();

        ConnectivityManager connManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            if (localDatabase.getBroadcastMessage() == null) {
                timerStart();
            } else {
                showMessage(localDatabase.getBroadcastMessage());
            }
        } else {
            showMessage(getString(R.string.no_wifi_found));
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(receiver);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {
            this.moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private CountDownTimer timer;

    private void timerStart() {
        Long lastRefreshTime = localDatabase.getRefreshTime();
        if (lastRefreshTime != -1L) {
            long since = Calendar.getInstance()
                    .getTimeInMillis() - lastRefreshTime;

            if (since > 0 && since < 90000) {
                if (timer != null) {
                    timer.cancel();
                }
                timer = new CountDownTimer(90000 - since, 1000) {
                    @Override
                    public void onTick(long l) {
                        long sec = l / 1000;
                        tvPrimaryText.setText("Auto-refresh in " + sec + " sec!");
                    }

                    @Override
                    public void onFinish() {
                        tvPrimaryText.setText(R.string.refresh_current_session);
                    }
                };
                timer.start();
            } else {
                tvPrimaryText.setText(R.string.refresh_current_session);
            }
        } else {
            tvPrimaryText.setText(getString(R.string.some_error));
        }

    }

    private void liveSessionInBackground() {
        gcmNetworkManager.cancelAllTasks(MyTaskService.class);

        PeriodicTask task = new PeriodicTask.Builder()
                .setService(MyTaskService.class)
                .setTag(TASK_TAG_REFRESH)
                .setPeriod(7L)
                .setRequiredNetwork(Task.NETWORK_STATE_UNMETERED)
                .setUpdateCurrent(true)
                .build();

        gcmNetworkManager.schedule(task);
    }

    private void onRefreshSession() {
        StringRequest request = new StringRequest(Request.Method.GET,
                localDatabase.getRefreshURL(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String refreshURL = response.substring(
                                response.indexOf(".href=\"") + 7,
                                response.lastIndexOf("\";"));
                        localDatabase.setRefreshURL(refreshURL, Calendar
                                .getInstance().getTimeInMillis());
                        Date d = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a",
                                Locale.ENGLISH);
                        String currentDateTimeString = sdf.format(d);
                        showNotification("Refreshed at " + currentDateTimeString,
                                MainActivity.class,  true);
                        timerStart();
                        liveSessionInBackground();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        gcmNetworkManager.cancelTask(TASK_TAG_REFRESH, MyTaskService.class);
                        ConnectivityManager connManager = (ConnectivityManager)
                                getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                        timer.cancel();
                        if (activeNetwork != null && activeNetwork.getType() ==
                                ConnectivityManager.TYPE_WIFI) {
                            onReLogin();
                        } else {
                            showMessage(getString(R.string.no_wifi_found));
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

    public void onReLogin(){

    }

    public void onRefreshClick(View view) {
        if (timer != null) {
            timer.onFinish();
            timer.cancel();
        }
        onRefreshSession();
    }

    public void onLogoutClick(View view) {
        if (timer != null) {
            timer.cancel();
        }
        localDatabase.setLogin(false, null, null, null, null);
        gcmNetworkManager.cancelAllTasks(MyTaskService.class);
        showNotification("Logout", LoginActivity.class, false);
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    public void showMessage(String message) {
        showNotification(message, MainActivity.class, false);
        tvPrimaryText.setText(message);
    }

    private void showNotification(String contentText, Class activity, boolean unDestroyable) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Proxy Login")
                        .setContentText(contentText)
                        .setOngoing(unDestroyable);
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, activity);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
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
