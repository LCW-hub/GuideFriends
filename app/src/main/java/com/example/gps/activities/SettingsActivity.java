package com.example.gps.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import com.naver.maps.map.NaverMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    // --- 이 변수들은 이미 있습니다 ---
    private RadioGroup rgMapType;
    private RadioButton rbNormal, rbSatellite, rbTerrain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("설정");
        }

        // SharedPreferences 초기화
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        // RadioGroup 및 RadioButton 초기화 (기존 코드 유지)
        rgMapType = findViewById(R.id.rgMapType);
        rbNormal = findViewById(R.id.rbMapTypeNormal);
        rbSatellite = findViewById(R.id.rbMapTypeSatellite);
        rbTerrain = findViewById(R.id.rbMapTypeTerrain);

        // 현재 설정값 로드 (메서드 이름 변경/수정)
        loadMapTypeSetting(); // loadSettings() -> loadMapTypeSetting() 호출


        // RadioGroup 리스너 설정
        rgMapType.setOnCheckedChangeListener((group, checkedId) -> {
            int mapType = NaverMap.MapType.Basic.ordinal(); // 기본값: 일반 지도
            if (checkedId == R.id.rbMapTypeSatellite) {
                mapType = NaverMap.MapType.Satellite.ordinal();
            } else if (checkedId == R.id.rbMapTypeTerrain) {
                mapType = NaverMap.MapType.Terrain.ordinal();
            }
            saveMapTypeSetting(mapType); // int 값을 저장하는 새 메서드 호출
        });

        // 약관 및 정책 항목 클릭 리스너 설정
        setupTermsClickListeners();
    }

    private void setupTermsClickListeners() {
        // 이용약관
        LinearLayout layoutTermsOfService = findViewById(R.id.layout_terms_of_service);
        layoutTermsOfService.setOnClickListener(v -> {
            Intent intent = new Intent(this, TermsViewerActivity.class);
            intent.putExtra(TermsViewerActivity.EXTRA_TERMS_TYPE, TermsViewerActivity.TYPE_TERMS_OF_SERVICE);
            startActivity(intent);
        });

        // 개인정보처리방침
        LinearLayout layoutPrivacyPolicy = findViewById(R.id.layout_privacy_policy);
        layoutPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(this, TermsViewerActivity.class);
            intent.putExtra(TermsViewerActivity.EXTRA_TERMS_TYPE, TermsViewerActivity.TYPE_PRIVACY_POLICY);
            startActivity(intent);
        });

        // 위치정보 이용약관
        LinearLayout layoutLocationTerms = findViewById(R.id.layout_location_terms);
        layoutLocationTerms.setOnClickListener(v -> {
            Intent intent = new Intent(this, TermsViewerActivity.class);
            intent.putExtra(TermsViewerActivity.EXTRA_TERMS_TYPE, TermsViewerActivity.TYPE_LOCATION_TERMS);
            startActivity(intent);
        });

        // 오픈소스 라이선스
        LinearLayout layoutOpenSourceLicense = findViewById(R.id.layout_open_source_license);
        layoutOpenSourceLicense.setOnClickListener(v -> {
            Intent intent = new Intent(this, TermsViewerActivity.class);
            intent.putExtra(TermsViewerActivity.EXTRA_TERMS_TYPE, TermsViewerActivity.TYPE_OPEN_SOURCE);
            startActivity(intent);
        });
    }


    private void loadMapTypeSetting() {
        // 저장된 지도 타입 값 (int) 로드, 기본값은 Basic (일반 지도)의 ordinal 값
        int savedMapTypeOrdinal = prefs.getInt("map_type", NaverMap.MapType.Basic.ordinal());

        if (savedMapTypeOrdinal == NaverMap.MapType.Satellite.ordinal()) {
            rbSatellite.setChecked(true);
        } else if (savedMapTypeOrdinal == NaverMap.MapType.Terrain.ordinal()) {
            rbTerrain.setChecked(true);
        } else {
            rbNormal.setChecked(true); // 기본값 또는 Basic
        }
    }

    private void saveMapTypeSetting(int mapTypeOrdinal) {
        SharedPreferences.Editor editor = prefs.edit();
        // boolean 대신 int 값(ordinal) 저장
        editor.putInt("map_type", mapTypeOrdinal);
        editor.apply();

        String typeName = "일반";
        if (mapTypeOrdinal == NaverMap.MapType.Satellite.ordinal()) {
            typeName = "위성";
        } else if (mapTypeOrdinal == NaverMap.MapType.Terrain.ordinal()) {
            typeName = "지형";
        }
        Toast.makeText(this, typeName + " 지도로 설정되었습니다", Toast.LENGTH_SHORT).show();
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}