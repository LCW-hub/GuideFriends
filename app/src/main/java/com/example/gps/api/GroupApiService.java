package com.example.gps.api;

import com.example.gps.dto.CreateGroupRequest;
import com.example.gps.dto.GroupListResponse;
import com.example.gps.dto.LocationResponse;
import com.example.gps.dto.UpdateLocationRequest;
import com.example.gps.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GroupApiService {

    // ⭐ 1. 그룹 생성 API: POST /api/groups
    @POST("/api/groups")
    Call<Map<String, String>> createGroup(@Body CreateGroupRequest request);

    // ⭐ 2. 내 그룹 목록 조회 API: GET /api/groups
    @GET("/api/groups")
    Call<List<GroupListResponse>> getMyGroups();

    // ⭐ 3. 위치 업데이트 API: POST /api/groups/{groupId}/location
    // [수정] 서버가 본문 없이 200 OK를 반환할 때 파싱 오류를 피하기 위해 Call<Void> 사용
    @POST("/api/groups/{groupId}/location")
    Call<Void> updateLocation(@Path("groupId") Long groupId, @Body UpdateLocationRequest request);

    // ⭐ 4. 그룹 멤버 위치 조회 API: GET /api/groups/{groupId}/locations
    @GET("/api/groups/{groupId}/locations")
    Call<List<LocationResponse>> getGroupMemberLocations(@Path("groupId") Long groupId);

    // ⭐ 5. 위치 공유 규칙 변경 API: POST /api/groups/{groupId}/sharing-rule
    // [수정] 서버가 메시지만 반환하므로 파싱 오류를 피하기 위해 Call<Void> 사용
    @POST("/api/groups/{groupId}/sharing-rule")
    Call<Void> updateSharingRule(
            @Path("groupId") Long groupId,
            @Query("targetUserId") Long targetUserId,
            @Query("allow") boolean allow
    );

    // ⭐ 6. 모든 그룹 멤버 조회 API (설정 화면용): GET /api/groups/{groupId}/all-members
    @GET("/api/groups/{groupId}/all-members")
    Call<List<User>> getAllGroupMembers(@Path("groupId") Long groupId);
}
