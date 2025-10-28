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

    // 1. 그룹 생성 API: POST /api/groups
    @POST("/api/groups")
    Call<Map<String, String>> createGroup(@Body CreateGroupRequest request);

    // 2. 내 그룹 목록 조회 API: GET /api/groups
    @GET("/api/groups")
    Call<List<GroupListResponse>> getMyGroups();

    // 3. 위치 업데이트 API: POST /api/groups/{groupId}/location
    @POST("/api/groups/{groupId}/location")
    Call<Void> updateLocation(@Path("groupId") Long groupId, @Body UpdateLocationRequest request);

    // 4. 그룹 멤버 위치 조회 API: GET /api/groups/{groupId}/locations
    @GET("/api/groups/{groupId}/locations")
    Call<List<LocationResponse>> getGroupMemberLocations(@Path("groupId") Long groupId);

    // 5. 위치 공유 규칙 변경 API (설정 저장): POST /api/groups/{groupId}/sharing-rule
    // Sharer(나)가 Target(다른 멤버)에게 내 위치를 공유할지 말지 설정합니다.
    @POST("/api/groups/{groupId}/sharing-rule")
    Call<Void> updateSharingRule(
            @Path("groupId") Long groupId,
            @Query("targetUserId") Long targetUserId,
            @Query("allow") boolean allow
    );

    // 6. 모든 그룹 멤버 조회 API (설정 화면용): GET /api/groups/{groupId}/all-members
    @GET("/api/groups/{groupId}/all-members")
    Call<List<User>> getAllGroupMembers(@Path("groupId") Long groupId);

    // ⭐️ 7. [추가] 내 위치 공유 규칙 조회 API (체크박스 초기화용): GET /api/groups/{groupId}/sharing-rules
    // 로그인된 사용자(Sharer)가 그룹 내 다른 멤버(Target)들에게 설정한 공유 규칙 상태를 모두 조회합니다.
    // 서버 응답: Map<TargetUserId, IsAllowed(Boolean)>
    @GET("/api/groups/{groupId}/sharing-rules")
    Call<Map<Long, Boolean>> getSharingRulesForSharer(
            @Path("groupId") Long groupId,
            // 💡 Retrofit은 @GET에서 Path를 사용하지 않는 Body 정보를 URL Query로 자동 변환하지 않으므로,
            //    서버에서 SharerId를 Path나 Header, 또는 Query로 받도록 가정합니다.
            //    여기서는 GET 요청이지만, 로그인 상태라면 서버가 SharerId를 Header(Token)에서 추출하는 것이 일반적입니다.
            //    클라이언트에서 명시적으로 보내야 한다면 아래와 같이 Query로 추가합니다.
            @Query("sharerId") Long sharerId
    );

    @GET("/api/groups/{groupId}/incoming-sharing-rules")
    Call<Map<Long, Boolean>> getSharingRulesForTarget(
            @Path("groupId") Long groupId,
            @Query("targetId") Long targetId // 내가 Target
    );
    @GET("/api/group/{groupId}/sharing/rules/source/{sourceId}")
    Call<Map<Long, Boolean>> getSharingRulesForSource(@Path("groupId") Long groupId, @Path("sourceId") Long sourceId);
}