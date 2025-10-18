package com.example.gps.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioGroupMapType;
    private RadioButton radioNormal;
    private RadioButton radioSatellite;
    private RadioButton radioTerrain;
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
        radioGroupMapType = findViewById(R.id.radio_group_map_type);
        radioNormal = findViewById(R.id.radio_normal);
        radioSatellite = findViewById(R.id.radio_satellite);
        radioTerrain = findViewById(R.id.radio_terrain);

        // 현재 설정값 로드
        loadSettings();

        // 라디오 그룹 리스너 설정
        radioGroupMapType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                saveMapTypeSetting(checkedId);
            }
        });
    }

    private void loadSettings() {
        // 지도 타입 설정 로드 (기본값: 0 = 일반 지도)
        int mapType = 0; // 기본값
        
        try {
            // 기존에 저장된 값이 String인 경우를 대비해 try-catch 사용
            mapType = prefs.getInt("map_type", 0);
        } catch (ClassCastException e) {
            // 기존에 String으로 저장된 값이 있다면 제거하고 기본값 사용
            android.util.Log.w("SettingsActivity", "기존 map_type 설정이 잘못된 형식입니다. 기본값으로 재설정합니다.", e);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("map_type");
            editor.putInt("map_type", 0);
            editor.apply();
        }
        
        switch (mapType) {
            case 0:
                radioNormal.setChecked(true);
                break;
            case 1:
                radioSatellite.setChecked(true);
                break;
            case 2:
                radioTerrain.setChecked(true);
                break;
            default:
                radioNormal.setChecked(true);
                break;
        }
    }

    private void saveMapTypeSetting(int checkedId) {
        SharedPreferences.Editor editor = prefs.edit();
        String mapTypeName = "";
        
        if (checkedId == R.id.radio_normal) {
            editor.putInt("map_type", 0);
            mapTypeName = "일반 지도";
        } else if (checkedId == R.id.radio_satellite) {
            editor.putInt("map_type", 1);
            mapTypeName = "위성 지도";
        } else if (checkedId == R.id.radio_terrain) {
            editor.putInt("map_type", 2);
            mapTypeName = "지형 지도";
        }
        
        editor.apply();
        
        Toast.makeText(this, mapTypeName + "로 설정되었습니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
