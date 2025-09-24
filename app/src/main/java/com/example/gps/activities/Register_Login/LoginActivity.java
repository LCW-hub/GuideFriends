//로그인 화면
package com.example.gps.activities.Register_Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gps.R;
import com.example.gps.activities.GuestMain;
import com.example.gps.activities.NormalMain;
import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin, buttonGuestMode;
    private TextView textViewSignup, textViewFindId, textViewFindPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("로그인");

        // 뷰 초기화
        editTextUsername = findViewById(R.id.etId);
        editTextPassword = findViewById(R.id.etPw);
        buttonLogin = findViewById(R.id.btnLogin);
        buttonGuestMode = findViewById(R.id.btnGuest);
        textViewSignup = findViewById(R.id.tvSignup);
        textViewFindId = findViewById(R.id.tvFindId);
        textViewFindPassword = findViewById(R.id.tvFindPw);

        // 기본값 자동 입력
        editTextUsername.setText("testuser");
        editTextPassword.setText("1234");

        // 로그인 버튼 클릭 리스너
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        // 게스트 모드 버튼 클릭 리스너
        buttonGuestMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, GuestMain.class));
                finish();
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
        String username = editTextUsername.getText().toString();
        String password = editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "사용자 이름과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // API 호출
        UserApi userApi = ApiClient.getClient().create(UserApi.class);
        User user = new User(username, password, "", "");

        Call<Map<String, Object>> call = userApi.login(user);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    if ("success".equals(result.get("status"))) {
                        // SharedPreferences에 로그인 정보 저장
                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("is_logged_in", true);
                        editor.putString("username", username);
                        editor.putInt("user_coins", 1000); // 기본 코인 1000개
                        
                        // 서버에서 받은 사용자 정보가 있다면 저장
                        if (result.get("user") != null) {
                            Map<String, Object> userData = (Map<String, Object>) result.get("user");
                            editor.putString("email", (String) userData.get("email"));
                            editor.putString("name", (String) userData.get("name"));
                            if (userData.get("coins") != null) {
                                editor.putInt("user_coins", (Integer) userData.get("coins"));
                            }
                        }
                        editor.apply();
                        
                        Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, NormalMain.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "로그인 실패: " + result.get("message"), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "서버 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
