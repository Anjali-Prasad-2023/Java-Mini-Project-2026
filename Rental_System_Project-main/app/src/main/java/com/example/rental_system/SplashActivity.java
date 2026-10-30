package com.example.rental_system;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable navigateToLogin = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler.postDelayed(navigateToLogin, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigateToLogin);
        super.onDestroy();
    }
}