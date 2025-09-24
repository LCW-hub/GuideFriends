package com.example.gps.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gps.R;
import com.example.gps.activities.Register_Login.LoginActivity;
import com.example.gps.activities.Register_Login.RegisterActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProfileBottomSheetFragment extends BottomSheetDialogFragment {

    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;
    private LinearLayout guestModeLayout;
    private LinearLayout loggedInLayout;
    private Button loginButton;
    private Button signupButton;
    private Button logoutButton;
    private Button settingsButton;
    private Button userInfoButton;

    @Nullable
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialog);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_bottom_sheet, container, false);
        
        initViews(view);
        setupClickListeners();
        updateUI();
        
        return view;
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        guestModeLayout = view.findViewById(R.id.guest_mode_layout);
        loggedInLayout = view.findViewById(R.id.logged_in_layout);
        loginButton = view.findViewById(R.id.btn_login);
        signupButton = view.findViewById(R.id.btn_signup);
        logoutButton = view.findViewById(R.id.btn_logout);
        settingsButton = view.findViewById(R.id.btn_settings);
        userInfoButton = view.findViewById(R.id.btn_user_info);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
            dismiss();
        });

        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), RegisterActivity.class);
            startActivity(intent);
            dismiss();
        });

        logoutButton.setOnClickListener(v -> {
            logout();
        });

        settingsButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "설정 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
        });

        userInfoButton.setOnClickListener(v -> {
            showUserInfo();
        });
    }

    private void updateUI() {
        SharedPreferences prefs = getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        if (isLoggedIn) {
            // 로그인된 상태
            guestModeLayout.setVisibility(View.GONE);
            loggedInLayout.setVisibility(View.VISIBLE);
            
            String username = prefs.getString("username", "사용자");
            String email = prefs.getString("email", "");
            
            profileName.setText(username);
            profileEmail.setText(email.isEmpty() ? "이메일 정보 없음" : email);
            
            // 기본 프로필 이미지 설정 (로그인된 사용자)
            profileImage.setImageResource(R.drawable.ic_person);
            
        } else {
            // 게스트 모드
            guestModeLayout.setVisibility(View.VISIBLE);
            loggedInLayout.setVisibility(View.GONE);
            
            profileName.setText("게스트 사용자");
            profileEmail.setText("로그인하여 더 많은 기능을 이용하세요");
            
            // 기본 프로필 이미지 설정 (게스트)
            profileImage.setImageResource(R.drawable.ic_person);
        }
    }

    private void logout() {
        SharedPreferences prefs = getContext().getSharedPreferences("user_prefs", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("username");
        editor.remove("email");
        editor.apply();
        
        Toast.makeText(getContext(), "로그아웃되었습니다", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void showUserInfo() {
        // SharedPreferences를 통해 로그인 상태 확인
        SharedPreferences prefs = getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        
        if (isLoggedIn) {
            // 사용자 정보 표시 로직
            String username = prefs.getString("username", "");
            String email = prefs.getString("email", "");
            Toast.makeText(getContext(), "사용자: " + username + "\n이메일: " + email, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 다른 화면에서 돌아왔을 때 UI 업데이트
        updateUI();
    }
}
