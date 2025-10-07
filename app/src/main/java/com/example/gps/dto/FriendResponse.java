package com.example.gps.dto;

import com.google.gson.annotations.SerializedName;

public class FriendResponse {
    @SerializedName("friendId")
    private Long friendId;

    @SerializedName("friendUsername")
    private String friendUsername;

    // Getter
    public Long getFriendId() { return friendId; }
    public String getFriendUsername() { return friendUsername; }
}