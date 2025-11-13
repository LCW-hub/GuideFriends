package com.example.gps.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    // ⭐️ [이 필드를 추가하세요] ⭐️
    @SerializedName("profileImageUrl")
    private String profileImageUrl;

    // --- Getters ---

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    // ⭐️ [이 Getter를 추가하세요] ⭐️
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}