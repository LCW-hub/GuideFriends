package com.example.gps.dto;

import com.google.gson.annotations.SerializedName;

public class GroupListResponse {

    @SerializedName("groupId")
    private Long groupId;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("destinationName")
    private String destinationName;

    // startTime, endTime은 지금 당장 화면에 표시할 필요는 없지만,
    // 나중에 필요할 수 있으니 일단 만들어 둡니다.
    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    // ▼▼▼ [ 1. 이 4개의 필드를 추가합니다 ] ▼▼▼
    @SerializedName("destinationLat")
    private Double destinationLat;

    @SerializedName("destinationLng")
    private Double destinationLng;

    @SerializedName("memberCount")
    private int memberCount;

    @SerializedName("createdByUsername")
    private String createdByUsername;
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲

    // Getter 메소드
    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getDestinationName() { return destinationName; }
    // ▼▼▼ [ 2. 새로 추가한 필드의 Getter 4개를 추가합니다 ] ▼▼▼
    public Double getDestinationLat() { return destinationLat; }
    public Double getDestinationLng() { return destinationLng; }
    public int getMemberCount() { return memberCount; }
    public String getCreatedByUsername() { return createdByUsername; }
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲
}