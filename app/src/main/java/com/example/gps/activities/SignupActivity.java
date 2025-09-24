//회원가입 화면
package com.example.gps.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gps.R;
import com.example.gps.manager.UserManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword, etPhone;
    private TextInputLayout nameLayout, emailLayout, passwordLayout, confirmPasswordLayout, phoneLayout;
    private Button btnSignup;
    private ProgressBar progressBar;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initViews() {
        // TextInputEditText 초기화
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etPhone = findViewById(R.id.et_phone);

        // TextInputLayout 초기화
        nameLayout = findViewById(R.id.name_layout);
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        phoneLayout = findViewById(R.id.phone_layout);

        // 버튼 및 기타 뷰 초기화
        btnSignup = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
        tvLoginLink = findViewById(R.id.tv_login_link);

        // 애니메이션 효과 적용
        animateViews();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("회원가입");
        }
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> {
            animateButtonClick(v);
            signupUser();
        });
        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void signupUser() {
        if (!validateInputs()) {
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // 로딩 상태 표시
        setLoadingState(true);

        // UserManager를 통한 회원가입
        UserManager userManager = UserManager.getInstance(this);
        userManager.signup(email, password, email, name, new UserManager.SignupCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(SignupActivity.this, "✨ " + message, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(SignupActivity.this, "❌ " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // 이름 검증
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("이름을 입력해주세요");
            isValid = false;
        } else if (name.length() < 2) {
            nameLayout.setError("이름은 2자 이상 입력해주세요");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        // 이메일 검증
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("이메일을 입력해주세요");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("올바른 이메일 형식을 입력해주세요");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        // 비밀번호 검증
        String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("비밀번호를 입력해주세요");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("비밀번호는 6자 이상 입력해주세요");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        // 비밀번호 확인 검증
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("비밀번호 확인을 입력해주세요");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("비밀번호가 일치하지 않습니다");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        // 전화번호 검증 (선택사항)
        String phone = etPhone.getText().toString().trim();
        if (!TextUtils.isEmpty(phone) && phone.length() < 10) {
            phoneLayout.setError("올바른 전화번호를 입력해주세요");
            isValid = false;
        } else {
            phoneLayout.setError(null);
        }

        return isValid;
    }

    private void setLoadingState(boolean isLoading) {
        btnSignup.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSignup.setText(isLoading ? "가입 중..." : "회원가입");
    }

    private void animateViews() {
        // 초기 상태 설정
        nameLayout.setAlpha(0f);
        nameLayout.setTranslationY(50f);
        emailLayout.setAlpha(0f);
        emailLayout.setTranslationY(50f);
        passwordLayout.setAlpha(0f);
        passwordLayout.setTranslationY(50f);
        confirmPasswordLayout.setAlpha(0f);
        confirmPasswordLayout.setTranslationY(50f);
        phoneLayout.setAlpha(0f);
        phoneLayout.setTranslationY(50f);
        btnSignup.setAlpha(0f);
        btnSignup.setTranslationY(50f);

        // 애니메이션 실행
        animateView(nameLayout, 0);
        animateView(emailLayout, 100);
        animateView(passwordLayout, 200);
        animateView(confirmPasswordLayout, 300);
        animateView(phoneLayout, 400);
        animateView(btnSignup, 500);
    }

    private void animateView(View view, long delay) {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimator, translationAnimator);
        animatorSet.setDuration(600);
        animatorSet.setStartDelay(delay);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(150);
        animatorSet.start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}