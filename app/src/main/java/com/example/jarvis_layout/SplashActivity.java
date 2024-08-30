package com.example.jarvis_layout;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_splash);

        getWindow().getDecorView().post(() -> hideSystemBars());
        //获得时间handle
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startSplashFadeOut();  // 开始淡出动画
            }
        }, 1500); // 1.5秒后执行

        View mainLayout = findViewById(R.id.main);

    }
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30 及以上：使用 WindowInsetsController
            getWindow().setDecorFitsSystemWindows(false);
            View decorView = getWindow().getDecorView();
            WindowInsetsController insetsController = decorView.getWindowInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // API 30 以下：使用 SYSTEM_UI_FLAG
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
    private void startSplashFadeOut() {
        View splashLayout = findViewById(R.id.activity_splash);
        splashLayout.animate()
                .alpha(0f)
                .setDuration(1000)  // 淡出动画持续时间1秒
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // 动画结束后启动 MainActivity
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                });
    }

}
