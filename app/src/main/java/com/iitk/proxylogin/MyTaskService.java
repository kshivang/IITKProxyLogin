package com.iitk.proxylogin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by kshivang on 25/01/17.
 *
 */

public class MyTaskService extends GcmTaskService {
    final static String ACTION_DONE = "done";
    final static String EXTRA_TAG = "tag";
    final static String EXTRA_RESULT = "result";
    final static int mId = 32;

    private VolleyController volleyController;
    private UserLocalDatabase localDatabase;

    final static int TIME_OUT = 10;


    @Override
    public void onInitializeTasks() {
        // When your package is removed or updated, all of its network tasks are cleared by
        // the GcmNetworkManager. You can override this method to reschedule them in the case of
        // an updated package. This is not called when your application is first installed.
        //
        // This is called on your application's main thread.

        // TODO: In a real app, this should be implemented to re-schedule important tasks.
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        volleyController = VolleyController.getInstance(this);
        localDatabase = new UserLocalDatabase(this);
        final String tag = taskParams.getTag();

        int result = GcmNetworkManager.RESULT_SUCCESS;
        switch (tag) {
            case MainActivity.TASK_TAG_REFRESH:
                showNotification("Refreshing..", MainActivity.class, true);
                result = onGetRequest(localDatabase.getRefreshURL(), new NetworkCallback() {
                    @Override
                    public int onResponse(RequestFuture<String> future) {
                        try {
                            String response = future.get(TIME_OUT, TimeUnit.SECONDS);
                            if (response.contains("Authentication Refresh in ")) {
                                String refreshURL = response.substring(
                                        response.indexOf(".href=\"") + 7,
                                        response.lastIndexOf("\";"));
                                Log.e("onRunTask: ", tag);
                                Date d = new Date();
                                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a",
                                        Locale.ENGLISH);
                                String currentDateTimeString = sdf.format(d);
                                showNotification("Refreshed at " +
                                        currentDateTimeString, MainActivity.class,  true);
                                localDatabase.setRefreshURL(refreshURL, Calendar
                                        .getInstance().getTimeInMillis());
                                return GcmNetworkManager.RESULT_SUCCESS;
                            } else {
                                showNotification(getString(R.string.some_error),
                                        MainActivity.class, false);
                                onCancelAllTasks();
                                localDatabase.setBroadcastMessage(getString(R.string.some_error));
                                return GcmNetworkManager.RESULT_FAILURE;
                            }
                        } catch (InterruptedException | ExecutionException |
                                TimeoutException e) {
                            e.printStackTrace();
                            showNotification(getString(R.string.some_error),
                                    MainActivity.class,  false);
                            onCancelAllTasks();
                            localDatabase.setBroadcastMessage(getString(R.string.some_error));
                            return GcmNetworkManager.RESULT_FAILURE;
                        }
                    }
                });
                break;
            default:
                result = GcmNetworkManager.RESULT_FAILURE;
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_DONE);
        intent.putExtra(EXTRA_TAG, tag);
        intent.putExtra(EXTRA_RESULT, result);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);

        return result;
    }

    private void onCancelAllTasks() {
        GcmNetworkManager manager = GcmNetworkManager
                .getInstance(MyTaskService.this);
        manager.cancelAllTasks(MyTaskService.class);
    }

    private int onGetRequest(String url, NetworkCallback networkCallback) {
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(Request.Method.GET, url, future, future)
        {
            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
        request.setShouldCache(false);
        volleyController.addToRequestQueue(request);
        return networkCallback.onResponse(future);
    }

    private void showNotification(String contentText, Class activity, boolean unDestroyable) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.black_logo)
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

    private interface NetworkCallback {
        int onResponse(RequestFuture<String> future);
    }

}
