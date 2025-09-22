//회원가입 화면
package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gps.R;

import com.example.gps.manager.UserManager;

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

        btnSignup.setOnClickListener(v -> signupUser());
    }

    private void signupUser() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();
        String email = etEmail.getText().toString();
        String name = etName.getText().toString();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // UserManager를 통한 회원가입
        UserManager userManager = UserManager.getInstance(this);
        userManager.signup(username, password, email, name, new UserManager.SignupCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(SignupActivity.this, message, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SignupActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}