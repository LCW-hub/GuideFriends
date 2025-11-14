package com.example.gps.dto;

import com.google.gson.annotations.SerializedName;

public class FriendResponse {
    @SerializedName("friendId")
    private Long friendId;

    @SerializedName("friendUsername")
    private String friendUsername;

    // ⭐️ [이 필드를 추가하세요] ⭐️
    @SerializedName("profileImageUrl")
    private String profileImageUrl;

    // --- Getters ---

    public Long getFriendId() { return friendId; }
    public String getFriendUsername() { return friendUsername; }

    // ⭐️ [이 Getter를 추가하세요] ⭐️
    public String getProfileImageUrl() { return profileImageUrl; }
}