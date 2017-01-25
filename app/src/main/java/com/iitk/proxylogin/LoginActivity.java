package com.iitk.proxylogin;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
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
    private boolean isClicked = false;
    private LogHandler logHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        tilUserName = (TextInputLayout) findViewById(R.id.userNameInput);
        tilPassword = (TextInputLayout) findViewById(R.id.passwordInput);
        progressBar = (ProgressBar) findViewById(android.R.id.progress);

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

        logHandler = LogHandler.newInstance(this, new LogHandler.OnProgressListener() {
            @Override
            public void onProgress(String message) {
                setProgress();
                logHandler.showNotification(message, LoginActivity.class, false);
            }

            @Override
            public void onFinish(String message) {
                finishProgress();
                logHandler.showNotification(message, LoginActivity.class, false);
            }
        });

        logHandler.showNotification("Require login", LoginActivity.class, false);
    }

    public void onLoginClick(View view) {
        hideKeyBoard(view);
        if (!isClicked) {
            EditText etUserName = tilUserName.getEditText();
            EditText etPassword = tilPassword.getEditText();
            if (etUserName != null && etPassword != null &&
                    !TextUtils.isEmpty(etUserName.getText())
                    && !TextUtils.isEmpty(etPassword.getText())) {

                logHandler.onLog(etUserName.getText().toString(),
                        etPassword.getText().toString());
            } else {
                Toast.makeText(this, "Username and Password are required fields!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onLogoutClick(View view) {
        hideKeyBoard(view);
        if (!isClicked) {
            logHandler.onLog(null, null);
        }
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
        isClicked = true;
    }

    public void finishProgress() {
        progressBar.setVisibility(View.GONE);
        isClicked = false;
    }
}
