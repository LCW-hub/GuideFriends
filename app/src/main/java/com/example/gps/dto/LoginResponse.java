package com.example.gps.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    // [수정] "token" -> "accessToken"으로 필드명 변경
    @SerializedName("accessToken")
    private String accessToken;

    // [추가] refreshToken 필드
    @SerializedName("refreshToken")
    private String refreshToken;

    // [수정] accessToken의 Getter
    public String getAccessToken() {
        return accessToken;
    }

    // [추가] refreshToken의 Getter
    public String getRefreshToken() {
        return refreshToken;
    }

    // (Setter는 필요 없습니다)
}