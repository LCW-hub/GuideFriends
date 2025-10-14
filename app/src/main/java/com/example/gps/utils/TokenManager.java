package com.example.gps.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "AuthPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // 토큰 저장
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    // 토큰 불러오기
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    // 토큰 삭제 (로그아웃 시 사용)
    public void deleteToken() {
        editor.remove(KEY_TOKEN);
        editor.apply();
    }
}