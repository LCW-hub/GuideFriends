//회원가입 화면
package com.example.gps.activities.Register_Login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

    // ⭐ [주석 수정] etUsername은 아이디를 받을 것으로 추정하여 주석을 정리했습니다.
    private EditText etUsername, etPassword, etEmail, etConfirmPassword, etPhone;
    private Button btnSignup;

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


        btnSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String phoneNum = etPhone.getText().toString().trim();

        /* 유효성 검사 */
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

        // 서버 전송용 객체 생성 (User 모델에 맞게 데이터 전달)
        // User 모델의 생성자 인수가 (username, password, email, phoneNum) 순서라고 가정
        User user = new User(username, password, email, phoneNum);
        UserApi userApi = ApiClient.getClient(this).create(UserApi.class);
        Call<Map<String, Object>> call = userApi.signup(user);


        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                // 성공 응답 (2xx)
                if (response.isSuccessful()) {
                    // 서버에서 "message" 키를 사용한다고 가정
                    String message = (response.body() != null) ? (String) response.body().get("message") : "회원가입 성공!";
                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                    if (message != null && message.contains("성공")) {
                        // ⭐ [개선] 성공 시 로그인 화면으로 아이디를 전달
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        finish();
                    }
                }
                // 에러 응답 (4xx, 5xx)
                else {
                    String errorMessage = "회원가입 실패 (오류 코드: " + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();

                            // ⭐ [수정] JSON 파싱 전, 응답 본문이 비어 있는지 확인하여 JSONException 방지
                            if (errorJson.trim().isEmpty()) {
                                errorMessage = "회원가입 실패: 서버 응답 본문이 비어 있습니다.";
                            } else {
                                JSONObject jsonObject = new JSONObject(errorJson);
                                // 서버가 "message" 키를 사용한다고 가정
                                if (jsonObject.has("message")) {
                                    errorMessage = jsonObject.getString("message");
                                } else if (jsonObject.has("status") && "fail".equals(jsonObject.get("status"))) {
                                    // 서버가 status: fail 형식으로 응답할 경우
                                    errorMessage = jsonObject.optString("message", errorMessage);
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
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("RegisterError", "Network error", t);
                Toast.makeText(RegisterActivity.this, "네트워크 에러: 서버에 접속할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}