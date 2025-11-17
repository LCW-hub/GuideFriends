package com.example.gps.activities.Register_Login;

import android.content.Intent;
import android.content.SharedPreferences; // ⭐️ [기능 추가] SharedPreferences import
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;
import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;

import java.util.Map;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // 변수 선언 (두 번째 코드 기준)
    private EditText etUsername, etPassword, etEmail, etConfirmPassword, etPhone;
    private Button btnSignup;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("회원가입");
        }

        // 뷰 초기화
        etUsername = findViewById(R.id.et_username);         // 아이디
        etPassword = findViewById(R.id.et_pw);               // 비밀번호
        etEmail = findViewById(R.id.et_email);               // 이메일
        btnSignup = findViewById(R.id.btn_register);         // 회원가입 버튼
        etConfirmPassword = findViewById(R.id.etConfirmPassword); // 비밀번호 확인
        etPhone = findViewById(R.id.et_Phone);               // 전화번호
        tvLoginLink = findViewById(R.id.tv_login_link);      // 로그인 링크

        btnSignup.setOnClickListener(v -> registerUser());
        
        // 로그인 링크 클릭 시 로그인 페이지로 이동
        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // 현재 액티비티 종료
        });
    }

    private void registerUser() {

        // ⭐ [수정] SharedPreferences 저장을 위해 변수들을 final로 선언합니다.
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String phoneNum = etPhone.getText().toString().trim();

        /* 유효성 검사 (두 번째 코드 기준) */
        if (username.isEmpty()) {
            etUsername.setError("아이디를 입력해주세요");
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
        if (phoneNum.isEmpty()) {
            etPhone.setError("전화번호를 입력해주세요");
            etPhone.requestFocus();
            return;
        }

        User user = new User(username, password, email, phoneNum);

        // ApiClient.getClient(this) -> ApiClient.getRetrofit(this)
        UserApi userApi = ApiClient.getClient(this).create(UserApi.class);
        Call<Map<String, Object>> call = userApi.signup(user);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                // 성공 응답 (2xx)
                if (response.isSuccessful()) {
                    String message = (response.body() != null) ? (String) response.body().get("message") : "회원가입 성공!";

                    if (message != null && message.contains("성공")) {

                        // ⭐ [수정] final로 선언된 username과 email 사용
                        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                        prefs.edit()
                                .putString("username", username)
                                .putString("email", email)
                                .apply();

                        // [기능 유지] 두 번째 코드의 로그인 화면으로 아이디 전달 기능 유지
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        finish();
                    }
                }
                // 에러 응답 (4xx, 5xx) - 두 번째 코드의 안정적인 오류 처리 로직 사용
                else {
                    String errorMessage = "회원가입 실패 (오류 코드: " + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();
                            if (errorJson.trim().isEmpty()) {
                                errorMessage = "회원가입 실패: 서버 응답 본문이 비어 있습니다.";
                            } else {
                                JSONObject jsonObject = new JSONObject(errorJson);
                                if (jsonObject.has("message")) {
                                    errorMessage = jsonObject.getString("message");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("RegisterError", "Error parsing error body: " + e.getMessage(), e);
                            errorMessage = "회원가입 실패: 서버 오류 응답 파싱 불가";
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e("RegisterError", "Network error", t);
                Toast.makeText(RegisterActivity.this, "네트워크 에러: 서버에 접속할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        });
    } // ⭐ [수정] registerUser 메소드의 닫는 중괄호가 여기에 추가되어 문법 오류 해결

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}