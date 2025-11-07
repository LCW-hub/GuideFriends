package com.example.gps.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
// import android.widget.CompoundButton; // 삭제
// import android.widget.Switch; // 삭제
// import android.widget.TextView; // 삭제
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