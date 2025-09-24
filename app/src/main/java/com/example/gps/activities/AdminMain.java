//관리자모드
package com.example.gps.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gps.R;

public class AdminMain extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        TextView textView = findViewById(R.id.text_admin_welcome);
        textView.setText("관리자 모드에 오신 것을 환영합니다!");
    }
}
