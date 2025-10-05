package com.example.gps.dto;

import com.google.gson.annotations.SerializedName;

public class LocationResponse {

    @SerializedName("userId")
    private Long userId;

    @SerializedName("userName")
    private String userName;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("lastUpdatedAt")
    private String lastUpdatedAt; // 시간 정보는 문자열로 받습니다.

    // Getter
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getLastUpdatedAt() { return lastUpdatedAt; }
}