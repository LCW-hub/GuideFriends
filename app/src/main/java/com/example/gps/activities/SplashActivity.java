package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.gps.R;
import androidx.core.content.ContextCompat;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.FirebaseApp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 2000; // 2초로 단축
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 투명하게 설정
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        window.setNavigationBarColor(ContextCompat.getColor(this, android.R.color.transparent));

        setContentView(R.layout.activity_splash);

        // 백그라운드 작업을 위한 ExecutorService 초기화
        executorService = Executors.newSingleThreadExecutor();

        // UI 요소들 찾기
        CardView cardLogo = findViewById(R.id.cardLogo);
        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvSubTitle = findViewById(R.id.tvSubTitle);
        CircularProgressIndicator progressIndicator = findViewById(R.id.progressIndicator);

        // 애니메이션 로드
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);

        // 로고 카드 애니메이션 (페이드인 + 슬라이드업)
        cardLogo.startAnimation(fadeIn);
        cardLogo.startAnimation(slideUp);

        // 로고 회전 애니메이션
        ivLogo.startAnimation(rotate);

        // 텍스트 애니메이션 (지연 후 페이드인)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            tvAppName.startAnimation(fadeIn);
        }, 300);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            tvSubTitle.startAnimation(fadeIn);
        }, 600);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressIndicator.startAnimation(fadeIn);
        }, 900);

        // 백그라운드에서 무거운 초기화 작업 수행
        executorService.execute(() -> {
            try {
                // Firebase 초기화
                FirebaseApp.initializeApp(this);
                
                // 기타 초기화 작업들...
                Thread.sleep(500); // 최소 대기 시간
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 2초 후 메인 화면으로 전환
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MapsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
} 