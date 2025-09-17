//회원가입 화면
package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gps.R;

import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etEmail, etName;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etUsername = findViewById(R.id.et_id);
        etPassword = findViewById(R.id.et_pw);
        etEmail = findViewById(R.id.et_email);
        etName = findViewById(R.id.et_name);
        btnSignup = findViewById(R.id.btn_signup);

        //btnSignup.setOnClickListener(v -> signupUser());
    }

    private void signupUser() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();
        String email = etEmail.getText().toString();
        String name = etName.getText().toString();

        User user = new User(username, password, email, name);

        UserApi userApi = ApiClient.getClient().create(UserApi.class);
        Call<Map<String, String>> call = userApi.signup(user);

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();

                    // ✅ 회원가입이 성공했을 때 이동
                    if (message != null && message.contains("성공")) {
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish(); // 회원가입 화면을 닫아서 뒤로가기 방지
                    }

                } else {
                    Toast.makeText(SignupActivity.this, "회원가입 실패", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(SignupActivity.this, "에러 발생: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}