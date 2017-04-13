package com.iitk.proxyLogin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by kshivang on 22/01/17.
 *
 */

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUserName, tilPassword;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private LocalDatabase localDatabase;

    private LocalBroadcastManager localBroadcastManager;

    private final BroadcastReceiver proxyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "proxy.app.PROXY_PROGRESS":
                    tvProgress.setText(intent.getStringExtra("Progress"));
                    setProgress();
                    break;
            }
        }
    };


//    private boolean isClicked = false;
//    private LogHandler logHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localDatabase = new LocalDatabase(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(proxyReceiver, makeProxyUpdatesIntentFilter());

        setContentView(R.layout.activity_login);

        tilUserName = (TextInputLayout) findViewById(R.id.userNameInput);
        tilPassword = (TextInputLayout) findViewById(R.id.passwordInput);
        progressBar = (ProgressBar) findViewById(android.R.id.progress);
        tvProgress = (TextView) findViewById(R.id.primaryText);

        tilUserName.getEditText().setText(localDatabase.getUsername() != null?
                localDatabase.getUsername(): "");
        tilPassword.getEditText().setText(localDatabase.getPassword() != null?
                localDatabase.getPassword() : "");

        if (tilPassword.getEditText() != null) {
            tilPassword.getEditText()
                    .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        onLoginClick(findViewById(R.id.login));
                        return true;
                    }
                    return false;
                }
            });
        }

//        logHandler = LogHandler.newInstance(this, new LogHandler.OnProgressListener() {
//            @Override
//            public void onProgress(String message) {
//                setProgress();
//                logHandler.showNotification(message, LoginActivity.class, false);
//            }
//
//            @Override
//            public void onFinish(String message) {
//                finishProgress();
//                logHandler.showNotification(message, LoginActivity.class, false);
//            }
//        });
//
//        logHandler.showNotification("Require login", LoginActivity.class, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.localBroadcastManager != null) {
            localBroadcastManager.unregisterReceiver(proxyReceiver);
        }
    }

    public void onLoginClick(View view) {
        hideKeyBoard(view);
//        if (!isClicked) {
            EditText etUserName = tilUserName.getEditText();
            EditText etPassword = tilPassword.getEditText();
            if (etUserName != null && etPassword != null &&
                    !TextUtils.isEmpty(etUserName.getText())
                    && !TextUtils.isEmpty(etPassword.getText())) {

//                logHandler.onLog(etUserName.getText().toString(),
//                        etPassword.getText().toString());
            } else {
                Toast.makeText(this, "Username and Password are required fields!",
                        Toast.LENGTH_SHORT).show();
            }
//        }
    }

    public void onLogoutClick(View view) {
        hideKeyBoard(view);
//        if (!isClicked) {
//            logHandler.onLog(null, null);
//        }
    }

    public void hideKeyBoard(View view) {
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void setProgress() {
        progressBar.setVisibility(View.VISIBLE);
//        isClicked = true;
    }

    public void finishProgress() {
        progressBar.setVisibility(View.GONE);
//        isClicked = false;
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
