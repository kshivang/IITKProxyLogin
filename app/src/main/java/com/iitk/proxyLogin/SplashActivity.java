package com.iitk.proxyLogin;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

/**
 * Created by kshivang on 22/01/17.
 *
 */

public class SplashActivity extends AppCompatActivity {

    boolean isAnimationFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Animation bounceAnim = AnimationUtils
                .loadAnimation(getApplicationContext(), R.anim.bounce);

        TextView title = (TextView) findViewById(R.id.primaryText);

        title.setAnimation(bounceAnim);

        bounceAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animationsFinished();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void animationsFinished() {
        isAnimationFinished = true;
        UserLocalDatabase localDatabase = new UserLocalDatabase(SplashActivity.this);
        startActivity(new Intent(SplashActivity.this, localDatabase.isLogin() ?
                MainActivity.class : LoginActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAnimationFinished) {
            finish();
        }
    }
}
