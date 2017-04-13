package com.iitk.proxyLogin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by kshivang on 13/04/17.
 *
 */

public class SessionActivity extends AppCompatActivity{

    private TextView tvProgress;
    private Button btRefresh;
    private Button btLogout;
    private LocalDatabase localDatabase;
    private LocalBroadcastManager localBroadcastManager;

    private final BroadcastReceiver proxyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "proxy.app.PROXY_PROGRESS":
                    tvProgress.setText(intent.getStringExtra("Progress"));
                    break;
                case "proxy.app.PROXY_REQUEST_CREDENTIAL":
                    onFetched(true);
                    break;
                case "proxy.app.PROXY_LIVE_SESSION":
                    onFetched(false);
                    break;
                case "proxy.app.PROXY_CHECK_SESSION":
                    tvProgress.setText("retrying..");
                    Intent proxyServiceIntent = new Intent(SessionActivity.this,
                            ProxyService.class);
                    proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
                    startService(proxyServiceIntent);
                    break;
                default:
                    onFetched(false);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localDatabase = new LocalDatabase(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(proxyReceiver, makeProxyUpdatesIntentFilter());

        setContentView(R.layout.activity_session);
        tvProgress = (TextView) findViewById(R.id.progress_text);
        btLogout = (Button) findViewById(R.id.logout);
        btRefresh = (Button) findViewById(R.id.refresh);

        onRefreshUI(localDatabase.getLastIdentified());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.localBroadcastManager != null) {
            localBroadcastManager.unregisterReceiver(proxyReceiver);
        }
    }

    void onRefreshUI(String lastIdentified) {
        enableButtons(true);
        switch (lastIdentified) {
            case "fortinet":
                btLogout.setVisibility(View.INVISIBLE);
                tvProgress.setText("IITK fortinet network: will refresh at " +
                        new SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                                .format(localDatabase.getRefreshTime() + 240000));
                break;
            case "ironport":
                btLogout.setVisibility(View.INVISIBLE);
                tvProgress.setText("IITK ironport network: will refresh at " +
                        new SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                                .format(localDatabase.getRefreshTime() + 600000));
                break;
            default:
                tvProgress.setText("Your are not on IITK network");
                btLogout.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void onFetched(boolean isLoginRequired) {
        if (isLoginRequired){
            startActivity(new Intent(SessionActivity.this,
                    LoginActivity.class));
        }
        else {
            onRefreshUI(localDatabase.getLastIdentified());
        }
    }

    private void enableButtons(boolean isEnable) {
        btRefresh.setEnabled(isEnable);
        btLogout.setEnabled(isEnable);
    }

    public void onRefreshClick(View view) {
        Intent proxyServiceIntent = new Intent(this, ProxyService.class);
        switch (localDatabase.getLastIdentified()) {
            case "fortinet":
                proxyServiceIntent.setAction("proxy.service.FORTINET_REFRESH");
                break;
            case "ironport":
                proxyServiceIntent.setAction("proxy.service.IRONPORT_REFRESH");
                break;
            default:
                proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
        }
        startService(proxyServiceIntent);
        enableButtons(false);
    }

    public void onLogoutClick(View view) {
        Intent proxyServiceIntent = new Intent(this, ProxyService.class);
        switch (localDatabase.getLastIdentified()) {
            case "fortinet":
                proxyServiceIntent.setAction("proxy.service.FORTINET_LOGOUT");
                break;
            case "ironport":
                proxyServiceIntent.setAction("proxy.service.IRONPORT_LOGOUT");
                break;
            default:
                view.setVisibility(View.INVISIBLE);
        }
        startService(proxyServiceIntent);
        enableButtons(false);
    }

    public static IntentFilter makeProxyUpdatesIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("proxy.app.PROXY_PROGRESS");
        intentFilter.addAction("proxy.app.PROXY_REQUEST_CREDENTIAL");
        intentFilter.addAction("proxy.app.PROXY_NOT_IITK");
        intentFilter.addAction("proxy.app.PROXY_LIVE_SESSION");
        intentFilter.addAction("proxy.app.PROXY_CHECK_SESSION");
        return intentFilter;
    }
}
