//로그인 화면
package com.example.gps.activities.Register_Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox; // [추가]
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gps.R;
import com.example.gps.activities.MapsActivity;
import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
// import com.example.gps.model.User;

import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.gps.dto.LoginResponse; // (1단계)
import com.example.gps.utils.TokenManager; // (1단계)


public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin, buttonGuestMode;
    private TextView textViewSignup, textViewFindId, textViewFindPassword;

    private CheckBox checkBoxRememberMe; // [추가]
    private TokenManager tokenManager; // [추가]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("로그인");

        // [추가] 1단계에서 수정한 TokenManager 초기화
        tokenManager = new TokenManager();

        // 뷰 초기화
        editTextUsername = findViewById(R.id.etId);
        editTextPassword = findViewById(R.id.etPw);
        buttonLogin = findViewById(R.id.btnLogin);
        buttonGuestMode = findViewById(R.id.btnGuest);
        textViewSignup = findViewById(R.id.tvSignup);
        textViewFindId = findViewById(R.id.tvFindId);
        textViewFindPassword = findViewById(R.id.tvFindPw);

        // [추가] 2단계-1에서 추가한 "자동 로그인" 체크박스 ID 연결
        // (ID가 cb_remember_me가 맞는지 activity_login.xml에서 확인 필요)
        checkBoxRememberMe = findViewById(R.id.cb_remember_me);

        // 기본값 자동 입력
        editTextUsername.setText("ock123");
        editTextPassword.setText("ock123123");

        // 로그인 버튼 클릭 리스너
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });



        // 회원가입 텍스트 클릭 리스너
        textViewSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        // 아이디 찾기 텍스트 클릭 리스너
        textViewFindId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, FindIdActivity.class));
            }
        });

        // 비밀번호 찾기 텍스트 클릭 리스너
        textViewFindPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, FindPwActivity.class));
            }
        });
    }

    private void login() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // [추가] 자동 로그인 체크박스 상태 가져오기
        boolean rememberMe = checkBoxRememberMe.isChecked();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- [오류 수정] ---
        // 'getRetrofitInstance(this)' -> 'getClient(this)'로 원복
        UserApi userApi = ApiClient.getClient(this).create(UserApi.class);

        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);

        // [추가] "rememberMe" 상태를 서버에 전송
        loginData.put("rememberMe", String.valueOf(rememberMe));

        // 서버에 로그인 요청 (응답 타입은 LoginResponse)
        Call<LoginResponse> call = userApi.login(loginData);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // --- ✨ [수정] 로그인 성공 및 토큰 처리 로직 ✨ ---

                    // 1. [수정] 1단계에서 수정한 LoginResponse에서 토큰 2개 가져오기
                    String accessToken = response.body().getAccessToken();
                    String refreshToken = response.body().getRefreshToken();

                    // 2. [수정] 1단계에서 수정한 TokenManager를 사용해 토큰 2개 저장
                    tokenManager.saveTokens(accessToken, refreshToken);

                    // 3. 사용자 정보를 SharedPreferences에 저장 (기존 코드)
                    SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                    prefs.edit()
                            .putString("username", username)
                            .putString("email", username + "@example.com") // 실제로는 서버에서 가져와야 함
                            .apply();

                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();

                } else {
                    // 로그인 실패 (서버에서 401 Unauthorized 등 응답)
                    Toast.makeText(LoginActivity.this, "로그인 실패: 아이디 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LoginActivity", "Login failed", t);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}