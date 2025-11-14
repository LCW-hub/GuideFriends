package com.example.gps.api;

import com.example.gps.dto.FriendResponse;
import com.example.gps.model.User;

import java.util.List;
import java.util.Map; // ⭐️ [추가] Map 임포트
import retrofit2.Call;
import retrofit2.http.Body; // ⭐️ [추가] @Body 임포트
import retrofit2.http.GET;
import retrofit2.http.DELETE;
import retrofit2.http.POST; // ⭐️ [추가] @POST 임포트
import retrofit2.http.PUT;  // ⭐️ [추가] @PUT 임포트
import retrofit2.http.Path;

public interface FriendApiService {

    @GET("/api/friends/group-members")
    Call<List<User>> getGroupSelectableMembers();

    // ⭐️ 친구 목록 (MapsActivity, FriendsActivity 공용)
    @GET("/api/friends")
    Call<List<FriendResponse>> getFriends();

    // ⭐️ 친구 삭제 (MapsActivity, FriendsActivity 공용)
    @DELETE("/api/friends/{friendId}")
    Call<Void> deleteFriend(@Path("friendId") Long friendId);

    // --- ⭐️ [FriendsActivity용 API 6개] ⭐️ ---

    @POST("/api/friends/request")
    Call<Map<String, Object>> requestFriend(@Body Map<String, String> body);

    @PUT("/api/friends/accept")
    Call<Map<String, Object>> acceptFriend(@Body Map<String, String> body);

    @GET("/api/friends/pending")
    Call<List<User>> getPendingFriendRequests();

    @GET("/api/friends/sent")
    Call<List<User>> getSentFriendRequests();

    @retrofit2.http.HTTP(method = "DELETE", path = "/api/friends/cancel", hasBody = true)
    Call<Map<String, Object>> cancelFriendRequest(@Body Map<String, String> body);

    @retrofit2.http.HTTP(method = "DELETE", path = "/api/friends/decline", hasBody = true)
    Call<Map<String, Object>> declineFriendRequest(@Body Map<String, String> body);
}