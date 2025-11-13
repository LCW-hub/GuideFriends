//로그인 화면
package com.example.gps.activities.Register_Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

import com.example.gps.dto.LoginResponse;
import com.example.gps.utils.TokenManager;


public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin, buttonGuestMode;
    private TextView textViewSignup, textViewFindId, textViewFindPassword;

    private CheckBox checkBoxRememberMe;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenManager = new TokenManager();

        // --- 🔽 [자동 로그인 시도 로직] ---
        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken != null && !refreshToken.isEmpty()) {
            tryAutoLogin(refreshToken);
        } else {
            setupLoginView();
        }
    }

    private void setupLoginView() {
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("로그인");

        editTextUsername = findViewById(R.id.etId);
        editTextPassword = findViewById(R.id.etPw);
        buttonLogin = findViewById(R.id.btnLogin);
        buttonGuestMode = findViewById(R.id.btnGuest);
        textViewSignup = findViewById(R.id.tvSignup);
        textViewFindId = findViewById(R.id.tvFindId);
        textViewFindPassword = findViewById(R.id.tvFindPw);
        checkBoxRememberMe = findViewById(R.id.cb_remember_me);

        editTextUsername.setText("ock123");
        editTextPassword.setText("ock123123");

        buttonLogin.setOnClickListener(v -> login());
        textViewSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        textViewFindId.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, FindIdActivity.class)));
        textViewFindPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, FindPwActivity.class)));
    }

    private void tryAutoLogin(String refreshToken) {
        Log.d("LoginActivity", "저장된 리프레시 토큰 발견. 자동 로그인 시도...");
        UserApi userApi = ApiClient.getRefreshRetrofitInstance().create(UserApi.class);

        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);

        Call<LoginResponse> call = userApi.refreshToken(refreshRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i("LoginActivity", "자동 로그인(토큰 갱신) 성공!");
                    LoginResponse loginResponse = response.body();

                    // 1. 토큰 저장
                    tokenManager.saveTokens(loginResponse.getAccessToken(), loginResponse.getRefreshToken());

                    // 2. ⭐️ [핵심 수정] ⭐️
                    // 자동 로그인 시에도 프로필 이미지 URL을 갱신합니다.
                    String profileImageUrl = loginResponse.getProfileImageUrl();

                    SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                    prefs.edit()
                            .putString("profileImageUrl", profileImageUrl) // ⭐️ [추가]
                            .apply();

                    // 3. (기존 로직) 저장된 사용자 이름 읽기
                    String username = prefs.getString("username", null);
                    if (username == null) {
                        Log.e("LoginActivity", "자동 로그인 성공했으나 저장된 username이 없어 실패 처리.");
                        tokenManager.deleteTokens();
                        setupLoginView();
                        return;
                    }

                    // 4. MapsActivity로 이동
                    Toast.makeText(LoginActivity.this, username + "님, 환영합니다!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();

                } else {
                    Log.w("LoginActivity", "자동 로그인(토큰 갱신) 실패. 코드: " + response.code());
                    tokenManager.deleteTokens();
                    setupLoginView();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("LoginActivity", "자동 로그인 네트워크 오류", t);
                setupLoginView();
            }
        });
    }

    private void login() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        boolean rememberMe = checkBoxRememberMe.isChecked();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        UserApi userApi = ApiClient.getRetrofitInstance(this).create(UserApi.class);

        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        loginData.put("rememberMe", String.valueOf(rememberMe));

        Call<LoginResponse> call = userApi.login(loginData);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    LoginResponse loginResponse = response.body();

                    // 1. 토큰 2개 가져오기
                    String accessToken = loginResponse.getAccessToken();
                    String refreshToken = loginResponse.getRefreshToken();

                    // ⭐️ 2. [핵심 수정] ⭐️
                    // 서버에서 보낸 프로필 이미지 URL을 가져옵니다.
                    String profileImageUrl = loginResponse.getProfileImageUrl();

                    // 3. 토큰 2개 저장
                    tokenManager.saveTokens(accessToken, refreshToken);

                    // 4. ⭐️ [핵심 수정] ⭐️
                    // 사용자 정보를 SharedPreferences에 저장할 때, profileImageUrl도 함께 저장합니다.
                    SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                    prefs.edit()
                            .putString("username", username)
                            // .putString("email", username + "@example.com") // (이메일은 MapsActivity에서 가져오므로 삭제)
                            .putString("profileImageUrl", profileImageUrl) // ⭐️ [추가]
                            .apply();

                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();

                } else {
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