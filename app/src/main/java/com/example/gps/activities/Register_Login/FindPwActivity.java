//비번찾기
package com.example.gps.activities.Register_Login;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gps.R;

public class FindPwActivity extends AppCompatActivity {

    private EditText etId, etEmail;
    private TextView tvFoundPw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pw);

        etId = findViewById(R.id.et_id);
        etEmail = findViewById(R.id.et_email);
        tvFoundPw = findViewById(R.id.tv_found_pw);
        Button btnFindPw = findViewById(R.id.btn_find_pw);

        btnFindPw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputId = etId.getText().toString();
                String inputEmail = etEmail.getText().toString();

                // TODO: DB에서 해당 ID와 EMAIL이 일치하면 비밀번호 찾기
                String fakePassword = "1234"; // 테스트용
                tvFoundPw.setText("비밀번호는: " + fakePassword);
            }
        });
    }
}