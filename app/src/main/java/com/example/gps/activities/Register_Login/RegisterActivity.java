//회원가입 화면
package com.example.gps.activities.Register_Login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;
import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText  etUsername, etPassword, etEmail,  etConfirmPassword, etPhone;
    private Button  btnSignup;

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

        etUsername = findViewById(R.id.et_username);         // 이름
        etPassword = findViewById(R.id.et_pw);       // 비밀번호
        etEmail = findViewById(R.id.et_email);       // 이메일
        btnSignup = findViewById(R.id.btn_register); // 버튼 ID가 바뀌었으면 여기도 바꿔야 함
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.et_Phone);


        btnSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        /* 간단한 유효성 검사 */

        if (username.isEmpty()) {
            etUsername.setError("이름을 입력해주세요");
            etUsername.requestFocus();
            return;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("올바른 이메일 형식을 입력해주세요");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("6자 이상의 비밀번호를 입력해주세요");
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

        // 서버 전송용 객체 생성
        User user = new User(username, password, email, phone);
        UserApi userApi = ApiClient.getClient().create(UserApi.class);
        Call<Map<String, Object>> call = userApi.signup(user);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = (String) response.body().get("message");
                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                    if (message != null && message.contains("성공")) {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "회원가입 실패", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "에러 발생: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}