package com.example.gps.dto;

import java.util.List;

public class CreateGroupRequest {
    private String name;
    private String destinationName;
    private Double destinationLat;
    private Double destinationLng;
    private String startTime; // 안드로이드에서는 문자열로 보내는 것이 더 간단합니다.
    private String endTime;
    private List<Long> memberIds;

    // Setter
    public void setName(String name) { this.name = name; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
}