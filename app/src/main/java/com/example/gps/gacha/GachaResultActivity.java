package com.example.gps.gacha;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.example.gps.R;

public class GachaResultActivity extends AppCompatActivity {

    public static final String EXTRA_IS_MULTI = "is_multi";
    public static final String EXTRA_RESULTS = "results"; // String[]

    private LinearLayout container;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gacha_result);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("뽑기 결과");
        }

        boolean isMulti = getIntent().getBooleanExtra(EXTRA_IS_MULTI, false);
        String[] results = getIntent().getStringArrayExtra(EXTRA_RESULTS);

        TextView title = findViewById(R.id.tv_title);
        container = findViewById(R.id.container_results);
        Button btnBackToShop = findViewById(R.id.btn_back_to_shop);

        title.setText(isMulti ? "🎁 10연속 결과" : "🎉 획득 아이템");

        // 결과 아이템들을 카드 형태로 표시
        if (results != null && results.length > 0) {
            for (int i = 0; i < results.length; i++) {
                final int index = i;
                handler.postDelayed(() -> {
                    createItemCard(results[index], index);
                }, i * 200); // 순차적으로 나타나게 함
            }
        }

        // 상점으로 돌아가기 버튼
        btnBackToShop.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShopActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void createItemCard(String item, int index) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 8, 0, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(12f);
        cardView.setCardElevation(4f);
        cardView.setCardBackgroundColor(getColor(R.color.colorCard));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(20, 16, 20, 16);

        // 아이템 아이콘
        TextView icon = new TextView(this);
        icon.setText("🎁");
        icon.setTextSize(24f);
        icon.setPadding(0, 0, 16, 0);

        // 아이템 텍스트
        TextView itemText = new TextView(this);
        itemText.setText(item);
        itemText.setTextSize(16f);
        itemText.setTextColor(getColor(R.color.textColorPrimary));
        itemText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // 등급에 따른 색상 설정
        if (item.contains("꽝")) {
            // 꽝
            itemText.setTextColor(getColor(R.color.textColorSecondary));
            icon.setText("😢");
        } else if (item.contains("코인 500개")) {
            // 최고 등급 코인
            itemText.setTextColor(getColor(R.color.colorAccent));
            icon.setText("💰");
        } else if (item.contains("코인 200개") || item.contains("케이크")) {
            // 고급 아이템
            itemText.setTextColor(getColor(R.color.colorPrimary));
            icon.setText("⭐");
        } else if (item.contains("코인 100개")) {
            // 일반 코인
            itemText.setTextColor(getColor(R.color.colorPrimary));
            icon.setText("💰");
        } else if (item.contains("기프티콘")) {
            // 기프티콘 아이템들
            if (item.contains("치킨")) {
                icon.setText("🍗");
            } else if (item.contains("피자")) {
                icon.setText("🍕");
            } else if (item.contains("스타벅스")) {
                icon.setText("☕");
            } else if (item.contains("햄버거")) {
                icon.setText("🍔");
            } else if (item.contains("케이크")) {
                icon.setText("🍰");
            }
            itemText.setTextColor(getColor(R.color.colorPrimary));
        }

        cardContent.addView(icon);
        cardContent.addView(itemText);
        cardView.addView(cardContent);

        // 애니메이션 효과
        cardView.setAlpha(0f);
        cardView.setTranslationY(50f);
        container.addView(cardView);

        // 카드가 나타나는 애니메이션
        cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}


