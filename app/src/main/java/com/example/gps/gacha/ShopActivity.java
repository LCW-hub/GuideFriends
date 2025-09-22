package com.example.gps.gacha;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;
import com.example.gps.manager.UserManager;

import java.util.Random;

public class ShopActivity extends AppCompatActivity {
    
    private TextView tvCoins, tvGachaResult, tvProbability;
    private Button btnSingleGacha, btnMultiGacha;
    private ImageView ivGachaResult;
    
    private int coins = 1000; // 기본 코인
    private Random random = new Random();
    
    // 뽑기 아이템과 확률
    private String[] items = {
        "🍗 치킨 기프티콘 (2만원)", 
        "🍕 피자 기프티콘 (2만원)", 
        "🍔 햄버거 세트 기프티콘 (1만원)",
        "🍰 케이크 기프티콘 (1만원)",
        "☕ 스타벅스 기프티콘 (5천원)",
        "💰 코인 500개 추가", 
        "💰 코인 200개 추가", 
        "💰 코인 100개 추가",
    };
    private int[] probabilities = {1, 1, 5, 5, 10, 10, 20, 40}; // 확률 (%)
    private int[] itemValues = {20000, 20000, 10000, 10000, 5000, 500, 200, 100}; // 아이템 가치
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        
        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("상점");
        }
        
        initViews();
        loadUserCoins();
        updateUI();
        setupClickListeners();
    }
    
    private void initViews() {
        tvCoins = findViewById(R.id.tv_coins);
        tvGachaResult = findViewById(R.id.tv_gacha_result);
        tvProbability = findViewById(R.id.tv_probability);
        btnSingleGacha = findViewById(R.id.btn_single_gacha);
        btnMultiGacha = findViewById(R.id.btn_multi_gacha);
        ivGachaResult = findViewById(R.id.iv_gacha_result);
    }
    
    private void loadUserCoins() {
        UserManager userManager = UserManager.getInstance(this);
        if (userManager.isLoggedIn()) {
            coins = userManager.getCoins();
        } else {
            // 비회원인 경우 기본 코인 0개
            coins = 1000;
            Toast.makeText(this, "회원만 상점을 이용할 수 있습니다", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateUI() {
        tvCoins.setText("💰 " + coins + " 코인");
        updateProbabilityInfo();
    }
    
    private void updateProbabilityInfo() {
        StringBuilder probText = new StringBuilder("📊 뽑기 확률 정보\n\n");
        probText.append("🍗 치킨 기프티콘: 1%\n");
        probText.append("🍕 피자 기프티콘: 1%\n");
        probText.append("🍔 햄버거 세트 기프티콘: 5%\n");
        probText.append("🍰 케이크 기프티콘: 5%\n");
        probText.append("☕ 스타벅스 기프티콘: 10%\n");
        probText.append("💰 코인 500개: 10%\n");
        probText.append("💰 코인 200개: 20%\n");
        probText.append("💰 코인 100개: 40%\n");
        probText.append("💡 코인 보상은 즉시 지급됩니다!");
        tvProbability.setText(probText.toString());
    }
    
    private void setupClickListeners() {
        btnSingleGacha.setOnClickListener(v -> performGacha(1));
        btnMultiGacha.setOnClickListener(v -> performGacha(10));
    }
    
    private void performGacha(int count) {
        UserManager userManager = UserManager.getInstance(this);
        if (!userManager.isLoggedIn()) {
            Toast.makeText(this, "회원만 상점을 이용할 수 있습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int cost = count * 100; // 1회당 100코인
        
        if (coins < cost) {
            Toast.makeText(this, "코인이 부족합니다!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 코인 차감
        int newCoins = coins - cost;
        userManager.updateCoins(newCoins, new UserManager.CoinUpdateCallback() {
            @Override
            public void onSuccess(int updatedCoins) {
                runOnUiThread(() -> {
                    coins = updatedCoins;
                    updateUI();
                    
                    if (count == 1) {
                        String result = getRandomItem();
                        processItemReward(result);
                        startCapsuleFlow(false, new String[]{ result });
                    } else {
                        String[] results = new String[10];
                        for (int i = 0; i < 10; i++) {
                            results[i] = getRandomItem();
                            processItemReward(results[i]);
                        }
                        startCapsuleFlow(true, results);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ShopActivity.this, "코인 업데이트 실패: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private String getRandomItem() {
        int randomNum = random.nextInt(100) + 1; // 1-100
        int cumulative = 0;
        
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (randomNum <= cumulative) {
                return items[i];
            }
        }
        return items[0]; // 기본값
    }
    
    private void processItemReward(String item) {
        // 코인 추가 아이템 처리
        int rewardCoins = 0;
        if (item.contains("코인 100개 추가")) {
            rewardCoins = 100;
        } else if (item.contains("코인 200개 추가")) {
            rewardCoins = 200;
        } else if (item.contains("코인 500개 추가")) {
            rewardCoins = 500;
        }
        
        if (rewardCoins > 0) {
            UserManager userManager = UserManager.getInstance(this);
            int newCoins = coins + rewardCoins;
            userManager.updateCoins(newCoins, new UserManager.CoinUpdateCallback() {
                @Override
                public void onSuccess(int updatedCoins) {
                    runOnUiThread(() -> {
                        coins = updatedCoins;
                        updateUI();
                    });
                }

                @Override
                public void onError(String error) {
                    // 코인 보상 실패는 조용히 처리 (이미 뽑기는 완료됨)
                }
            });
        }
        // 꽝은 아무것도 하지 않음
        // 기프티콘은 실제로는 사용자가 직접 사용해야 함
    }
    
    private void startCapsuleFlow(boolean isMulti, String[] results) {
        Intent intent = new Intent(this, CapsuleAnimationActivity.class);
        intent.putExtra(CapsuleAnimationActivity.EXTRA_IS_MULTI, isMulti);
        intent.putExtra(CapsuleAnimationActivity.EXTRA_RESULTS, results);
        startActivity(intent);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
