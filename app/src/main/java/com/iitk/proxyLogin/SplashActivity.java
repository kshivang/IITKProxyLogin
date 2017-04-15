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
            if ("proxy.app.PROXY_PROGRESS".equals(intent.getAction())) {
                tvProgress.setText(intent.getStringExtra("Progress"));
            } else if ("proxy.app.PROXY_REQUEST_CREDENTIAL".equals(intent.getAction())) {
                String type = intent.getStringExtra("Type");
                tvProgress.setText("You are on IITK " + type + " network");
                onFetched(true);
            } else if ("proxy.app.PROXY_LIVE_SESSION".equals(intent.getAction())) {
                long lastLogin = intent.getLongExtra("Time",
                                        System.currentTimeMillis());
                tvProgress.setText("IITK Fortinet refreshed at " +
                        new SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                                .format(new Date(lastLogin)));
                onFetched(false);
            } else {
                tvProgress.setText("You are not on IITK network");
                onFetched(false);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(proxyReceiver, makeProxyUpdatesIntentFilter());

        fetchType();

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
                isAnimationFinished = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void fetchType() {
        Intent proxyServiceIntent = new Intent(this, ProxyService.class);
        proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
        startService(proxyServiceIntent);
    }

    private void onFetched(boolean isLoginRequired) {
        startActivity(new Intent(SplashActivity.this, isLoginRequired ?
                LoginActivity.class : SessionActivity.class));
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
        intentFilter.addAction("proxy.app.PROXY_PROGRESS");
        intentFilter.addAction("proxy.app.PROXY_REQUEST_CREDENTIAL");
        intentFilter.addAction("proxy.app.PROXY_NOT_IITK");
        intentFilter.addAction("proxy.app.PROXY_LIVE_SESSION");
        return intentFilter;
    }
}
