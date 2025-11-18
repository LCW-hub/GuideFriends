package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.activities.Register_Login.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500; // 2.5초

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 애니메이션 적용
        ImageView logo = findViewById(R.id.ivLogo);
        TextView appName = findViewById(R.id.tvAppName);
        TextView slogan = findViewById(R.id.tvSlogan);

        // 로고 애니메이션 (스케일 + 페이드)
        Animation scaleAnim = AnimationUtils.loadAnimation(this, R.anim.splash_scale);
        if (logo != null) logo.startAnimation(scaleAnim);
        
        // 앱 이름 애니메이션
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000);
        fadeIn.setStartOffset(400);
        if (appName != null) appName.startAnimation(fadeIn);
        
        // 슬로건 애니메이션
        Animation fadeInSlow = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeInSlow.setDuration(1000);
        fadeInSlow.setStartOffset(700);
        if (slogan != null) slogan.startAnimation(fadeInSlow);

        // 일정 시간 후 로그인 화면으로 이동
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                
                // 부드러운 전환 효과
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, SPLASH_DURATION);
    }
}

