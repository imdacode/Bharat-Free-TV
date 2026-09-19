package com.imdacode.bharatfreetv;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

@SuppressLint("CustomSplashScreen")
public final class SplashActivity extends ComponentActivity {
    static final long SPLASH_DURATION_MILLIS = 2_500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openMainActivity = () -> {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        enterImmersiveMode();
        handler.postDelayed(openMainActivity, SPLASH_DURATION_MILLIS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openMainActivity);
        super.onDestroy();
    }

    private void enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
