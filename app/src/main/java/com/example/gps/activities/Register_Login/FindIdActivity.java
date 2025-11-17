//id찾기
package com.example.gps.activities.Register_Login;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;
import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindIdActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnFindId;
    private TextView tvResult; // 결과를 보여줄 TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("아이디 찾기");
        }

        etEmail = findViewById(R.id.etEmail);
        btnFindId = findViewById(R.id.btnFindId);
        tvResult = findViewById(R.id.tvResult); // XML 레이아웃에 해당 ID의 TextView가 있어야 합니다.

        // '아이디 찾기' 버튼 클릭 이벤트 설정
        btnFindId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();

                // 이메일 유효성 검사
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(FindIdActivity.this, "올바른 이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 서버로 아이디 찾기 요청
                findId(email);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // ✅ 기존 findId 메서드를 아래 코드로 전체 교체해주세요.
    private void findId(String email) {
        // ⭐ [수정] ApiClient.getClient(this) -> ApiClient.getRetrofit(this)
        UserApi userApi = ApiClient.getClient(this).create(UserApi.class);
        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("email", email);

        Call<Map<String, Object>> call = userApi.findIdByEmail(emailMap);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // ======================== 🔍 디버깅 로그 추가 🔍 ========================
                Log.d("FindIdDebug", "서버 응답 코드: " + response.code());
                Log.d("FindIdDebug", "응답 성공 여부: " + response.isSuccessful());

                if (response.body() != null) {
                    Log.d("FindIdDebug", "응답 본문(Body): " + response.body().toString());
                } else {
                    Log.d("FindIdDebug", "응답 본문(Body)이 null입니다.");
                }

                if (response.errorBody() != null) {
                    try {
                        Log.d("FindIdDebug", "에러 본문(ErrorBody): " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // =======================================================================

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();
                    String status = (String) responseBody.get("status");
                    Log.d("FindIdDebug", "파싱된 상태(status): " + status); // status 값 확인

                    if ("success".equals(status)) {
                        String username = (String) responseBody.get("username");
                        tvResult.setText("회원님의 아이디는 [ " + username + " ] 입니다.");
                        tvResult.setVisibility(View.VISIBLE);
                    } else {
                        String message = (String) responseBody.get("message");
                        tvResult.setText(message != null ? message : "알 수 없는 오류가 발생했습니다.");
                        tvResult.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvResult.setText("해당 이메일로 가입된 아이디가 없습니다.");
                    tvResult.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("FindIdDebug", "API 통신 완전 실패: ", t);
                Toast.makeText(FindIdActivity.this, "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}