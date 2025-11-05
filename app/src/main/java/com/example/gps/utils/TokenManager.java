package com.example.gps.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.gps.GlobalApplication; // GlobalApplication을 import

public class TokenManager {
    private static final String PREF_NAME = "MyTokenPrefs";

    // [수정] 토큰 키 이름 변경
    private static final String KEY_ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String KEY_REFRESH_TOKEN = "REFRESH_TOKEN";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    // [수정] 생성자가 Context 대신 GlobalApplication을 사용하고 인자가 없습니다.
    // (-> Expected no arguments... 오류 해결)
    public TokenManager() {
        this.context = GlobalApplication.getContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * [수정] 두 개의 토큰을 저장합니다.
     * @param accessToken  1시간짜리 액세스 토큰
     * @param refreshToken 30일짜리 리프레시 토큰
     */
    public void saveTokens(String accessToken, String refreshToken) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    /**
     * [수정] 두 개의 토큰을 모두 삭제합니다. (로그아웃 시 사용)
     * (-> Cannot resolve method 'deleteToken' 오류 해결)
     */
    public void deleteTokens() {
        editor.remove(KEY_ACCESS_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.apply();
    }

    /**
     * [수정] Access Token (일반 열쇠)을 가져옵니다.
     * (AuthInterceptor가 이 메소드를 사용할 것입니다)
     * @return 저장된 Access Token, 없으면 null
     */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * [추가] Refresh Token (만능키)을 가져옵니다.
     * (자동 갱신 로직이 이 메소드를 사용할 것입니다)
     * @return 저장된 Refresh Token, 없으면 null
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * [수정] Access Token이 있는지 확인합니다.
     * @return Access Token이 있으면 true, 없으면 false
     */
    public boolean hasToken() {
        return getAccessToken() != null;
    }
}