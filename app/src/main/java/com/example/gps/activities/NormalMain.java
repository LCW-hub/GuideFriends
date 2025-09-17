//회원 모드
package com.example.gps.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gps.R;

public class NormalMain extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_normal_main);  // 레이아웃 파일명은 그대로 둘 수 있어
    }
}