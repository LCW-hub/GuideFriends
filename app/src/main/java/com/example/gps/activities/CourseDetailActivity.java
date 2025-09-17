package com.example.gps.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.gps.R;
import com.example.gps.model.CourseDetail;
import com.example.gps.api.CourseApi;
import com.example.gps.api.TransportApi;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseDetailActivity extends AppCompatActivity {
    
    private TextView tvCourseName, tvDescription, tvDistance, tvDuration, tvSteps;
    private TextView tvCalories, tvWaterIntake, tvDifficulty, tvPetFriendly;
    private TextView tvCrowdLevel, tvRating, tvReviewCount;
    private TextView tvTransportInfo, tvFacilities;
    private CardView cardTransport, cardFacilities;
    private Button btnSOS, btnShare, btnFavorite;
    
    private CourseDetail courseDetail;
    private int courseId;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);
        
        // Intent에서 코스 ID 받기
        courseId = getIntent().getIntExtra("course_id", 0);
        if (courseId == 0) {
            Toast.makeText(this, "코스 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        loadCourseDetail();
        setupButtons();
    }
    
    private void initViews() {
        tvCourseName = findViewById(R.id.tv_course_name);
        tvDescription = findViewById(R.id.tv_description);
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);
        tvSteps = findViewById(R.id.tv_steps);
        tvCalories = findViewById(R.id.tv_calories);
        tvWaterIntake = findViewById(R.id.tv_water_intake);
        tvDifficulty = findViewById(R.id.tv_difficulty);
        tvPetFriendly = findViewById(R.id.tv_pet_friendly);
        tvCrowdLevel = findViewById(R.id.tv_crowd_level);
        tvRating = findViewById(R.id.tv_rating);
        tvReviewCount = findViewById(R.id.tv_review_count);
        tvTransportInfo = findViewById(R.id.tv_transport_info);
        tvFacilities = findViewById(R.id.tv_facilities);
        cardTransport = findViewById(R.id.card_transport);
        cardFacilities = findViewById(R.id.card_facilities);
        btnSOS = findViewById(R.id.btn_sos);
        btnShare = findViewById(R.id.btn_share);
        btnFavorite = findViewById(R.id.btn_favorite);
    }
    
    private void loadCourseDetail() {
        executor.execute(() -> {
            courseDetail = CourseApi.getCourseDetail(courseId);
            
            runOnUiThread(() -> {
                if (courseDetail != null) {
                    displayCourseDetail();
                    loadTransportInfo();
                    loadFacilitiesInfo();
                } else {
                    Toast.makeText(CourseDetailActivity.this, "코스 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void displayCourseDetail() {
        tvCourseName.setText(courseDetail.getCourseName());
        tvDescription.setText(courseDetail.getDescription());
        tvDistance.setText(String.format("거리: %.1fkm", courseDetail.getDistance()));
        tvDuration.setText(String.format("소요시간: %d분", courseDetail.getDuration()));
        tvSteps.setText(String.format("걸음수: %,d보", courseDetail.getSteps()));
        tvCalories.setText(String.format("소모 칼로리: %.0fkcal", courseDetail.getCalories()));
        tvWaterIntake.setText(String.format("수분 섭취량: %.0fml", courseDetail.getWaterIntake()));
        tvDifficulty.setText(String.format("난이도: %s", courseDetail.getDifficulty()));
        tvPetFriendly.setText(courseDetail.isPetFriendly() ? "애완동물 동반 가능" : "애완동물 동반 불가");
        tvCrowdLevel.setText(String.format("유동인구: %d/5", courseDetail.getCrowdLevel()));
        tvRating.setText(String.format("평점: %.1f", courseDetail.getRating()));
        tvReviewCount.setText(String.format("리뷰: %d개", courseDetail.getReviewCount()));
    }
    
    private void loadTransportInfo() {
        executor.execute(() -> {
            List<TransportApi.TransportInfo> transports = TransportApi.getTransportForCourse(courseId);
            
            runOnUiThread(() -> {
                if (!transports.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (TransportApi.TransportInfo transport : transports) {
                        sb.append(String.format("• %s: %s (도보 %d분)\n", 
                            transport.getType(), transport.getName(), transport.getWalkingTime()));
                    }
                    tvTransportInfo.setText(sb.toString());
                    cardTransport.setVisibility(View.VISIBLE);
                } else {
                    cardTransport.setVisibility(View.GONE);
                }
            });
        });
    }
    
    private void loadFacilitiesInfo() {
        if (courseDetail.getFacilities() != null && !courseDetail.getFacilities().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String facility : courseDetail.getFacilities()) {
                sb.append("• ").append(facility).append("\n");
            }
            tvFacilities.setText(sb.toString());
            cardFacilities.setVisibility(View.VISIBLE);
        } else {
            cardFacilities.setVisibility(View.GONE);
        }
    }
    
    private void setupButtons() {
        btnSOS.setOnClickListener(v -> {
            // SOS 액티비티로 이동
            // Intent intent = new Intent(this, SOSActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "SOS 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show();
        });
        
        btnShare.setOnClickListener(v -> {
            // 공유 기능
            String shareText = String.format("%s\n거리: %.1fkm, 소요시간: %d분\n평점: %.1f", 
                courseDetail.getCourseName(), courseDetail.getDistance(), 
                courseDetail.getDuration(), courseDetail.getRating());
            
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
            startActivity(android.content.Intent.createChooser(shareIntent, "코스 공유하기"));
        });
        
        btnFavorite.setOnClickListener(v -> {
            // 즐겨찾기 기능
            Toast.makeText(this, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show();
            btnFavorite.setText("즐겨찾기 해제");
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
} 