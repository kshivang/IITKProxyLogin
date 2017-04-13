package com.iitk.proxyLogin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by kshivang on 22/01/17.
 *
 */

public class SplashActivity extends AppCompatActivity {

    private boolean isAnimationFinished = false;
    private TextView tvProgress;
    private LocalBroadcastManager localBroadcastManager;
    private final BroadcastReceiver proxyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("proxy.app.PROXY_NEW_LOGIN_PROGRESS".equals(intent.getAction())) {
                tvProgress.setText(intent.getStringExtra("Progress"));
            } else if ("proxy.app.PROXY_REQUEST_CREDENTIAL".equals(intent.getAction())) {
                tvProgress.setText("You are on IITK " + intent.getStringExtra("Type") +
                        " network");
                onFetched();
            } else if ("proxy.app.PROXY_LIVE_SESSION".equals(intent.getAction())) {
                tvProgress.setText("IITK Fortinet refreshed at " +
                        new SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                                .format(new Date(intent.getLongExtra("Time",
                                        System.currentTimeMillis()))));
                onFetched();
            } else {
                tvProgress.setText("You are not on IITK network");
                onFetched();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(proxyReceiver, makeProxyUpdatesIntentFilter());
        getType();

        Animation bounceAnim = AnimationUtils
                .loadAnimation(getApplicationContext(), R.anim.bounce);

        tvProgress = (TextView) findViewById(R.id.progress_text);
        TextView tvTitle = (TextView) findViewById(R.id.appName);

        tvTitle.setAnimation(bounceAnim);

        bounceAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                animationsFinished();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void getType() {

        Intent proxyServiceIntent = new Intent(this, ProxyService.class);
        proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
        proxyServiceIntent.putExtra("Finish", false);
        startService(proxyServiceIntent);
    }

    private void onFetched() {
        Intent proxyServiceIntent = new Intent(this, ProxyService.class);
        proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
        proxyServiceIntent.putExtra("Finish", true);
        startService(proxyServiceIntent);
    }

    private void animationsFinished() {
        isAnimationFinished = true;
        UserLocalDatabase localDatabase = new UserLocalDatabase(SplashActivity.this);
        startActivity(new Intent(SplashActivity.this, localDatabase.isLogin() ?
                MainActivity.class : LoginActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.localBroadcastManager != null) {
            localBroadcastManager.unregisterReceiver(proxyReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAnimationFinished) {
            finish();
        }
    }

    public static IntentFilter makeProxyUpdatesIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("proxy.app.PROXY_NEW_LOGIN_PROGRESS");
        intentFilter.addAction("proxy.app.PROXY_REQUEST_CREDENTIAL");
        intentFilter.addAction("proxy.app.PROXY_NOT_IITK");
        intentFilter.addAction("proxy.app.PROXY_LIVE_SESSION");
        return intentFilter;
    }
}
