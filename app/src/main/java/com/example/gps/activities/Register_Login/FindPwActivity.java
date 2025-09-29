package com.example.gps.activities.Register_Login;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class FindPwActivity extends AppCompatActivity {

    private EditText etUsername, etEmail;
    private Button btnRequestReset;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pw);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        btnRequestReset = findViewById(R.id.btnRequestReset);
        tvResult = findViewById(R.id.tvResult);

        btnRequestReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String email = etEmail.getText().toString().trim();

                if (username.isEmpty() || email.isEmpty()) {
                    Toast.makeText(FindPwActivity.this, "아이디와 이메일을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 서버로 비밀번호 재설정 요청
                requestPasswordReset(username, email);
            }
        });
    }

    private void requestPasswordReset(String username, String email) {
        UserApi userApi = ApiClient.getClient().create(UserApi.class);

        // 1. 서버에 보낼 데이터 (아이디, 이메일)
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("email", email);

        Call<Map<String, Object>> call = userApi.requestPasswordReset(userInfo);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // 2. 서버 응답이 오면 (성공/실패 무관)
                if (response.body() != null) {
                    String message = (String) response.body().get("message");
                    tvResult.setText(message);
                    tvResult.setVisibility(View.VISIBLE);
                } else {
                    // 서버가 메시지를 보내지 않은 경우 (일반적으로는 발생하지 않음)
                    Toast.makeText(FindPwActivity.this, "요청을 처리했습니다. 이메일을 확인해주세요.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // 3. 네트워크 통신 실패
                Log.e("FindPwActivity", "API call failed: ", t);
                Toast.makeText(FindPwActivity.this, "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}