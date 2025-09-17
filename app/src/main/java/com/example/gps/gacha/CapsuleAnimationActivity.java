package com.example.gps.gacha;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;

public class CapsuleAnimationActivity extends AppCompatActivity {

    public static final String EXTRA_IS_MULTI = "is_multi";
    public static final String EXTRA_RESULTS = "results"; // String[]

    private ImageView capsule, capsuleDrop, sparkle1, sparkle2, sparkle3;
    private TextView tvLoading;
    private ProgressBar progressBar;
    private Button btnOpenCapsule;
    private Handler handler = new Handler();
    
    private boolean isCapsuleReady = false;
    private String[] results;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capsule_animation);

        initViews();
        results = getIntent().getStringArrayExtra(EXTRA_RESULTS);
        startGachaAnimation();
    }

    private void initViews() {
        capsule = findViewById(R.id.iv_capsule);
        capsuleDrop = findViewById(R.id.iv_capsule_drop);
        sparkle1 = findViewById(R.id.iv_sparkle1);
        sparkle2 = findViewById(R.id.iv_sparkle2);
        sparkle3 = findViewById(R.id.iv_sparkle3);
        tvLoading = findViewById(R.id.tv_loading);
        progressBar = findViewById(R.id.progress_bar);
        btnOpenCapsule = findViewById(R.id.btn_open_capsule);
        
        btnOpenCapsule.setOnClickListener(v -> openCapsule());
    }

    private void startGachaAnimation() {
        // 1단계: 로딩 텍스트 애니메이션
        animateLoadingText();
        
        // 2단계: 진행바 애니메이션
        animateProgressBar();
        
        // 3단계: 캡슐이 머신에서 떨어지는 애니메이션
        handler.postDelayed(this::animateCapsuleFromMachine, 1500);
    }

    private void animateLoadingText() {
        String[] loadingTexts = {"가챠머신이 작동 중...", "캡슐을 준비하는 중...", "캡슐이 떨어집니다!"};
        
        for (int i = 0; i < loadingTexts.length; i++) {
            final int index = i;
            handler.postDelayed(() -> {
                tvLoading.setText(loadingTexts[index]);
                // 텍스트 페이드 효과
                tvLoading.setAlpha(0f);
                tvLoading.animate().alpha(1f).setDuration(300).start();
            }, i * 500);
        }
    }

    private void animateProgressBar() {
        ValueAnimator progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(1500);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            progressBar.setProgress(progress);
        });
        progressAnimator.start();
    }

    private void animateCapsuleFromMachine() {
        // 캡슐이 머신 상단에서 수집구로 떨어지는 애니메이션
        capsuleDrop.setVisibility(View.VISIBLE);
        capsuleDrop.setAlpha(0f);
        capsuleDrop.setScaleX(0.5f);
        capsuleDrop.setScaleY(0.5f);
        
        // 캡슐이 나타나는 애니메이션
        capsuleDrop.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start();
        
        // 캡슐이 떨어지는 애니메이션
        handler.postDelayed(() -> {
            capsuleDrop.animate()
                    .translationY(200f)
                    .setDuration(800)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        // 캡슐이 수집구에 도착
                        capsuleDrop.setVisibility(View.GONE);
                        capsule.setVisibility(View.VISIBLE);
                        capsule.setAlpha(0f);
                        capsule.setScaleX(0.5f);
                        capsule.setScaleY(0.5f);
                        
                        // 수집구의 캡슐이 나타나는 애니메이션
                        capsule.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(500)
                                .setInterpolator(new BounceInterpolator())
                                .withEndAction(() -> {
                                    // 캡슐 준비 완료
                                    isCapsuleReady = true;
                                    showCapsuleOpenButton();
                                    animateSparkles();
                                })
                                .start();
                    })
                    .start();
        }, 500);
    }

    private void showCapsuleOpenButton() {
        tvLoading.setText("캡슐을 까보세요! 🎁");
        btnOpenCapsule.setVisibility(View.VISIBLE);
        btnOpenCapsule.setAlpha(0f);
        btnOpenCapsule.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
    }

    private void animateSparkles() {
        // 반짝이는 효과들을 순차적으로 나타나게 함
        sparkle1.setVisibility(View.VISIBLE);
        sparkle1.setAlpha(0f);
        sparkle1.animate().alpha(1f).setDuration(500).start();

        handler.postDelayed(() -> {
            sparkle2.setVisibility(View.VISIBLE);
            sparkle2.setAlpha(0f);
            sparkle2.animate().alpha(1f).setDuration(500).start();
        }, 200);

        handler.postDelayed(() -> {
            sparkle3.setVisibility(View.VISIBLE);
            sparkle3.setAlpha(0f);
            sparkle3.animate().alpha(1f).setDuration(500).start();
        }, 400);

        // 반짝이는 효과 반복
        animateSparkleRotation();
    }

    private void animateSparkleRotation() {
        ObjectAnimator rotation1 = ObjectAnimator.ofFloat(sparkle1, "rotation", 0f, 360f);
        rotation1.setDuration(1000);
        rotation1.setRepeatCount(ValueAnimator.INFINITE);
        rotation1.setInterpolator(new LinearInterpolator());
        rotation1.start();

        ObjectAnimator rotation2 = ObjectAnimator.ofFloat(sparkle2, "rotation", 0f, -360f);
        rotation2.setDuration(1200);
        rotation2.setRepeatCount(ValueAnimator.INFINITE);
        rotation2.setInterpolator(new LinearInterpolator());
        rotation2.start();

        ObjectAnimator rotation3 = ObjectAnimator.ofFloat(sparkle3, "rotation", 0f, 360f);
        rotation3.setDuration(800);
        rotation3.setRepeatCount(ValueAnimator.INFINITE);
        rotation3.setInterpolator(new LinearInterpolator());
        rotation3.start();
    }

    private void openCapsule() {
        if (!isCapsuleReady) return;
        
        btnOpenCapsule.setEnabled(false);
        tvLoading.setText("캡슐을 여는 중...");
        
        // 캡슐이 열리는 애니메이션
        capsule.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() -> {
                    capsule.animate()
                            .scaleX(0f)
                            .scaleY(0f)
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                // 결과 화면으로 이동
                                boolean isMulti = getIntent().getBooleanExtra(EXTRA_IS_MULTI, false);
                                Intent resultIntent = new Intent(CapsuleAnimationActivity.this, GachaResultActivity.class);
                                resultIntent.putExtra(GachaResultActivity.EXTRA_IS_MULTI, isMulti);
                                resultIntent.putExtra(GachaResultActivity.EXTRA_RESULTS, results);
                                startActivity(resultIntent);
                                finish();
                            })
                            .start();
                })
                .start();
    }
}


