package com.example.gps.activities.Register_Login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnResetPassword;

    private String resetToken; // 이메일 링크로부터 받은 토큰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        // 딥링크를 통해 전달된 데이터 받기
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            resetToken = data.getQueryParameter("token");

            if (resetToken == null || resetToken.isEmpty()) {
                Toast.makeText(this, "유효하지 않은 접근입니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            } else {
                Log.d("ResetPasswordActivity", "Received token: " + resetToken);
            }
        } else {
            Toast.makeText(this, "유효하지 않은 접근입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ '비밀번호 변경' 버튼 클릭 이벤트 리스너 추가
        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                // 1. 유효성 검사
                if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(ResetPasswordActivity.this, "새 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(ResetPasswordActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPassword.length() < 6) { // 예: 최소 6자리 이상
                    Toast.makeText(ResetPasswordActivity.this, "비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. 서버로 비밀번호 변경 요청
                performPasswordReset(resetToken, newPassword);
            }
        });
    }

    // ✅ 서버에 비밀번호 변경을 요청하는 메서드
    private void performPasswordReset(String token, String password) {
        UserApi userApi = ApiClient.getClient(this).create(UserApi.class);

        // 서버에 보낼 데이터 (토큰, 새 비밀번호)
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("password", password);

        Call<Map<String, Object>> call = userApi.resetPassword(data);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // 성공 응답 (2xx)
                if (response.isSuccessful() && response.body() != null) {
                    String message = (String) response.body().get("message");
                    Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_LONG).show();

                    // 성공 시 로그인 화면으로 이동
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                // 에러 응답 (4xx, 5xx)
                else {
                    // ✅ 실패 원인을 자세히 로그로 출력합니다.
                    try {
                        Log.e("ResetPasswordError", "Code: " + response.code() + ", Message: " + response.message());
                        if (response.errorBody() != null) {
                            Log.e("ResetPasswordError", "Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ResetPasswordActivity.this, "요청에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("ResetPasswordActivity", "API call failed: ", t);
                Toast.makeText(ResetPasswordActivity.this, "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}