package com.example.gps.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchMapType;
    private TextView tvMapTypeStatus;
    private SharedPreferences prefs;

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

        // UI 요소 초기화
        switchMapType = findViewById(R.id.switch_map_type);
        tvMapTypeStatus = findViewById(R.id.tv_map_type_status);

        // 현재 설정값 로드
        loadSettings();

        // 스위치 리스너 설정
        switchMapType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveMapTypeSetting(isChecked);
                updateMapTypeStatus(isChecked);
            }
        });
    }

    private void loadSettings() {
        // 지도 타입 설정 로드 (기본값: false = 일반 지도)
        boolean isSatelliteMode = prefs.getBoolean("satellite_mode", false);
        switchMapType.setChecked(isSatelliteMode);
        updateMapTypeStatus(isSatelliteMode);
    }

    private void saveMapTypeSetting(boolean isSatelliteMode) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("satellite_mode", isSatelliteMode);
        editor.apply();
        
        Toast.makeText(this, 
            isSatelliteMode ? "위성 지도 모드로 설정되었습니다" : "일반 지도 모드로 설정되었습니다", 
            Toast.LENGTH_SHORT).show();
    }

    private void updateMapTypeStatus(boolean isSatelliteMode) {
        if (isSatelliteMode) {
            tvMapTypeStatus.setText("위성 지도");
            tvMapTypeStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            tvMapTypeStatus.setText("일반 지도");
            tvMapTypeStatus.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
