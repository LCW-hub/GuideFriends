package com.example.gps.api;

import com.example.gps.dto.CreateGroupRequest;
import com.example.gps.dto.GroupListResponse;
import com.example.gps.dto.LocationResponse;
import com.example.gps.dto.UpdateLocationRequest;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GroupApiService {

    // 1. 그룹 생성 API
    @POST("api/groups")
    Call<Map<String, String>> createGroup(@Body CreateGroupRequest request);

    // 2. 내 그룹 목록 조회 API
    @GET("api/groups")
    Call<List<GroupListResponse>> getMyGroups();

    // 3. 위치 업데이트 API
    @POST("api/groups/{groupId}/location")
    Call<String> updateLocation(@Path("groupId") Long groupId, @Body UpdateLocationRequest request);

    // 4. 그룹 멤버 위치 조회 API
    @GET("api/groups/{groupId}/locations")
    Call<List<LocationResponse>> getGroupMemberLocations(@Path("groupId") Long groupId);
}