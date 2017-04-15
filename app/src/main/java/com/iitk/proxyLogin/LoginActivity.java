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
import android.widget.Button;
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
    private Button btLogin;

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
                    break;
                case "proxy.app.PROXY_INCORRECT_PASSWORD":
                    onIncorrectPassword();
                    break;
                case "proxy.app.WIFI_STATE_CHANGE":
                    if (localDatabase.isWifiPresent()){
                        btLogin.setEnabled(true);
                        tvProgress.setText("Wifi connection found");
                    } else {
                        btLogin.setEnabled(false);
                        tvProgress.setText("No wifi connection found");
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                default:
                    onFetched(false);
            }
        }
    };

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
        btLogin = (Button) findViewById(R.id.login);

        EditText etUserName = tilUserName.getEditText();
        EditText etPassword = tilPassword.getEditText();

        if (etUserName != null && etPassword != null) {
            etUserName.setText(localDatabase.getUsername() != null?
                    localDatabase.getUsername(): "");
            etPassword.setText(localDatabase.getPassword() != null?
                    localDatabase.getPassword() : "");
            etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        onLoginClick(btLogin);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {
            this.moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        EditText etUserName = tilUserName.getEditText();
        EditText etPassword = tilPassword.getEditText();
        if (etUserName != null && etPassword != null &&
                !TextUtils.isEmpty(etUserName.getText())
                && !TextUtils.isEmpty(etPassword.getText())) {
            btLogin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            Intent proxyServiceIntent = new Intent(this,
                    ProxyService.class);
            proxyServiceIntent.putExtra("username", etUserName.getText().toString());
            proxyServiceIntent.putExtra("password", etPassword.getText().toString());
            switch (localDatabase.getLastIdentified()) {
                case "fortinet":
                    proxyServiceIntent.setAction("proxy.service.FORTINET_LOGIN");
                    break;
                case "ironport":
                    proxyServiceIntent.setAction("proxy.service.IRONPORT_LOGIN");
                    break;
                default:
                    proxyServiceIntent.setAction("proxy.service.NETWORK_TYPE");
            }
            startService(proxyServiceIntent);
        } else {
            btLogin.setEnabled(true);
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Username and Password are required fields!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onFetched(boolean isLoginRequired) {
        if (isLoginRequired){
            onLoginClick(btLogin);
        }
        else {
            progressBar.setVisibility(View.INVISIBLE);
            startActivity(new Intent(LoginActivity.this, SessionActivity.class));
        }
    }

    private void onIncorrectPassword() {
        progressBar.setVisibility(View.INVISIBLE);
        tvProgress.setText("Incorrect credentials!");
        btLogin.setEnabled(true);
    }

    public void hideKeyBoard(View view) {
        if (view != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static IntentFilter makeProxyUpdatesIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("proxy.app.PROXY_PROGRESS");
        intentFilter.addAction("proxy.app.PROXY_REQUEST_CREDENTIAL");
        intentFilter.addAction("proxy.app.PROXY_NOT_IITK");
        intentFilter.addAction("proxy.app.PROXY_LIVE_SESSION");
        intentFilter.addAction("proxy.app.PROXY_CHECK_SESSION");
        intentFilter.addAction("proxy.app.PROXY_INCORRECT_PASSWORD");
        intentFilter.addAction("proxy.app.WIFI_STATE_CHANGE");
        return intentFilter;
    }
}
