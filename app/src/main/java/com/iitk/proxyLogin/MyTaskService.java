package com.iitk.proxyLogin;

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
                                showMessage(getString(R.string.some_error) + ": after response");
                                onCancelAllTasks();
                                return GcmNetworkManager.RESULT_FAILURE;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            showMessage(getString(R.string.some_error) + ": in");
                            onCancelAllTasks();
                            return GcmNetworkManager.RESULT_FAILURE;
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                            showMessage(getString(R.string.some_error) + ": ex");
                            onLogout();
                            onCancelAllTasks();
                            return GcmNetworkManager.RESULT_FAILURE;
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                            showMessage(getString(R.string.some_error) + ": ti");
                            onCancelAllTasks();
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

    private void onLogout() {
        onGetRequest(getString(R.string.logout_url), new NetworkCallback() {
            @Override
            public int onResponse(RequestFuture<String> future) {
                try {
                    String response = future.get(TIME_OUT, TimeUnit.SECONDS);
                    onPing();
                }catch (ExecutionException e) {
                    e.printStackTrace();
                    showMessage("Some error while logout ex");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    showMessage("Some error while logout in");
                } catch (TimeoutException e) {
                    e.printStackTrace();
                    showMessage("Some error while logout ti");
                }
                return 0;
            }
        });
    }

    private void onPing() {

        onGetRequest(getString(R.string.default_ping_url), new NetworkCallback() {
            @Override
            public int onResponse(RequestFuture<String> future) {
                try {
                    String response = future.get(TIME_OUT, TimeUnit.SECONDS);
                    showMessage("You are not on iitk fortinet network");
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    Log.e("My Task:onResponse: ", e.getMessage());
                } catch (InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }

    private void showMessage(String message) {
        showNotification(message, MainActivity.class, false);
        localDatabase.setBroadcastMessage(message);
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

        Intent resultIntent = new Intent(this, activity);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(activity);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager.notify(mId, mBuilder.build());
    }

    private interface NetworkCallback {
        int onResponse(RequestFuture<String> future);
    }

}
