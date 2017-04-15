package com.iitk.proxyLogin;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by kshivang on 15/04/17.
 *
 */

public class AlarmReceiver extends WakefulBroadcastReceiver {
    private static final String TAG;

    static {
        TAG = AlarmReceiver.class.getSimpleName();
    }

    public void onReceive(Context context, Intent intent) {
        if ("proxy.service.ALARM_BROADCAST".equals(intent.getAction())) {
            Intent proxyServiceIntent = new Intent(context, ProxyService.class);
            proxyServiceIntent.setAction("proxy.service.ALARM_BROADCAST");
            Log.i(TAG, "#### Launching ProxyService for " +
                    intent.getAction() + " @ " + SystemClock.elapsedRealtime());
            WakefulBroadcastReceiver.startWakefulService(context, proxyServiceIntent);
        }
    }
}
