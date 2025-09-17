package com.example.gps.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gps.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword, etPhone;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("회원가입");

        // 뷰 초기화
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);

        // 회원가입 버튼 클릭 이벤트
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // 입력 유효성 검사
        if (name.isEmpty()) {
            etName.setError("이름을 입력해주세요");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("이메일을 입력해주세요");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("올바른 이메일 형식이 아닙니다");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("비밀번호를 입력해주세요");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("비밀번호는 6자 이상이어야 합니다");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("비밀번호가 일치하지 않습니다");
            etConfirmPassword.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("전화번호를 입력해주세요");
            etPhone.requestFocus();
            return;
        }

        // 회원가입 성공 처리 (실제로는 서버 통신)
        Toast.makeText(this, "🎉 회원가입이 완료되었습니다! 환영합니다 " + name + "님!", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 