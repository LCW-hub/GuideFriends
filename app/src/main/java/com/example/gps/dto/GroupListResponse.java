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

    // Getter 메소드
    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getDestinationName() { return destinationName; }
}