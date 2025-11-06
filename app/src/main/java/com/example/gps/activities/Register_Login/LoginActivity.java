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

        // [추가] 1단계에서 수정한 TokenManager 초기화
        tokenManager = new TokenManager();

        // --- 🔽 [자동 로그인 시도 로직 추가] ---
        // setContentView를 호출하기 *전에* 토큰을 확인합니다.
        String refreshToken = tokenManager.getRefreshToken(); //
        if (refreshToken != null && !refreshToken.isEmpty()) {
            // 유효한 리프레시 토큰이 존재하면, 자동 갱신 시도
            // 갱신 시도 중에는 로그인 폼이 보이지 않도록 합니다.
            tryAutoLogin(refreshToken);
        } else {
            // 리프레시 토큰이 없으면, 평소처럼 로그인 폼을 보여줍니다.
            setupLoginView();
        }

    }

    private void setupLoginView() {
        // 이 코드가 원래 onCreate에 있던 것입니다.
        setContentView(R.layout.activity_login);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("로그인");

        // (tokenManager 초기화는 onCreate에서 이미 수행됨)

        // 뷰 초기화
        editTextUsername = findViewById(R.id.etId);
        editTextPassword = findViewById(R.id.etPw);
        buttonLogin = findViewById(R.id.btnLogin);
        buttonGuestMode = findViewById(R.id.btnGuest);
        textViewSignup = findViewById(R.id.tvSignup);
        textViewFindId = findViewById(R.id.tvFindId);
        textViewFindPassword = findViewById(R.id.tvFindPw);

        // [추가] 2단계-1에서 추가한 "자동 로그인" 체크박스 ID 연결
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

    private void tryAutoLogin(String refreshToken) {
        Log.d("LoginActivity", "저장된 리프레시 토큰 발견. 자동 로그인 시도...");

        // (중요) 갱신 API는 AuthInterceptor가 없는 '갱신 전용' Retrofit을 사용해야 합니다.
        UserApi userApi = ApiClient.getRefreshRetrofitInstance().create(UserApi.class);

        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);

        // 서버의 /api/auth/refresh API 호출
        Call<LoginResponse> call = userApi.refreshToken(refreshRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 1. 자동 갱신 성공!
                    Log.i("LoginActivity", "자동 로그인(토큰 갱신) 성공!");
                    LoginResponse loginResponse = response.body();

                    // 2. 새로 발급받은 AccessToken과 기존 RefreshToken을 다시 저장합니다.
                    tokenManager.saveTokens(loginResponse.getAccessToken(), loginResponse.getRefreshToken());

                    // 3. (중요) 수동 로그인 시 저장했던 사용자 이름을 SharedPreferences에서 다시 가져옵니다.
                    SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                    String username = prefs.getString("username", null);

                    if (username == null) {
                        // 비정상적인 경우 (토큰은 있는데 이름이 없는 경우)
                        Log.e("LoginActivity", "자동 로그인 성공했으나 저장된 username이 없어 실패 처리.");
                        tokenManager.deleteTokens(); // 토큰 삭제
                        setupLoginView(); // 로그인 폼 보여주기
                        return;
                    }

                    // 4. 모든 것이 정상이면 MapsActivity로 이동
                    Toast.makeText(LoginActivity.this, username + "님, 환영합니다!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish(); // 로그인 액티비티 종료

                } else {
                    // 2. 갱신 실패 (예: 토큰 30일 만료, 서버에서 강제 로그아웃 시킴)
                    Log.w("LoginActivity", "자동 로그인(토큰 갱신) 실패. 코드: " + response.code());
                    // 기존의 유효하지 않은 토큰들을 모두 삭제
                    tokenManager.deleteTokens();
                    // 실패했으므로, 사용자에게 수동 로그인을 요청 (로그인 폼 보여주기)
                    setupLoginView();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // 네트워크 오류 등 예외 발생
                Log.e("LoginActivity", "자동 로그인 네트워크 오류", t);
                // 실패 시, 로그인 폼을 보여줌
                setupLoginView();
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
        UserApi userApi = ApiClient.getRetrofitInstance(this).create(UserApi.class);

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