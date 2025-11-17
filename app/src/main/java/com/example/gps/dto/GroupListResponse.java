package com.example.gps.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List; // [필수] List 사용을 위해 import 추가

public class GroupListResponse {

    @SerializedName("groupId")
    private Long groupId;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("destinationName")
    private String destinationName;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("destinationLat")
    private Double destinationLat;

    @SerializedName("destinationLng")
    private Double destinationLng;

    @SerializedName("memberCount")
    private int memberCount;

    @SerializedName("createdByUsername")
    private String createdByUsername;

    // ▼▼▼ [ ★ 여기 추가 ] 참여자 아이디 목록 리스트 ▼▼▼
    // (서버에서 보내주는 JSON 키 값이 "memberIds"라고 가정합니다.
    // 만약 "members"나 "participants"라면 그에 맞춰 수정해야 합니다.)
    @SerializedName("memberIds")
    private List<String> memberIds;
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲


    // --- Getter 메소드들 ---

    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getDestinationName() { return destinationName; }

    public Double getDestinationLat() { return destinationLat; }
    public Double getDestinationLng() { return destinationLng; }
    public int getMemberCount() { return memberCount; }
    public String getCreatedByUsername() { return createdByUsername; }

    // ▼▼▼ [ ★ Getter 추가 ] ▼▼▼
    public List<String> getMemberIds() {
        return memberIds;
    }
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲
}