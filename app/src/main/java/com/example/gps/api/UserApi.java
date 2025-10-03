package com.example.gps.api;

import com.example.gps.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserApi {

    // --- 회원 관리 API (UserController) ---
    @POST("/api/users/signup")
    Call<Map<String, Object>> signup(@Body User user);

    @POST("/api/users/login")
    Call<Map<String, Object>> login(@Body User user);

    @POST("/api/users/find-id")
    Call<Map<String, Object>> findIdByEmail(@Body Map<String, String> emailMap);

    @GET("/api/users/{userId}")
    Call<Map<String, Object>> getUser(@Path("userId") int userId);


    @POST("/api/users/{userId}/step-reward")
    Call<Map<String, Object>> requestStepReward(@Path("userId") int userId, @Body Map<String, Object> stepData);

    @GET("/api/users")
    Call<Map<String, Object>> getUserList();

    @POST("/api/users/request-password-reset")
    Call<Map<String, Object>> requestPasswordReset(@Body Map<String, String> userInfo);

    @POST("/api/users/reset-password")
    Call<Map<String, Object>> resetPassword(@Body Map<String, String> data);

    // --- 친구 관리 API (FriendController) ---
    @POST("api/friends/request")
    Call<Map<String, Object>> requestFriend(@Body Map<String, String> body);

    @PUT("api/friends/accept")
    Call<Map<String, Object>> acceptFriend(@Body Map<String, String> body);

    @GET("api/friends/{username}")
    Call<List<User>> getFriends(@Path("username") String username);

    @GET("api/friends/pending/{username}")
    Call<List<User>> getPendingFriendRequests(@Path("username") String username);

    // 내가 보낸 친구 요청 목록 가져오기
    @GET("api/friends/sent/{username}")
    Call<List<User>> getSentFriendRequests(@Path("username") String username);

    // 친구 요청 취소하기
    @HTTP(method = "DELETE", path = "api/friends/cancel", hasBody = true)
    Call<Map<String, Object>> cancelFriendRequest(@Body Map<String, String> body);

    // 친구 요청 거절하기
    @HTTP(method = "DELETE", path = "api/friends/decline", hasBody = true)
    Call<Map<String, Object>> declineFriendRequest(@Body Map<String, String> body);

    @HTTP(method = "DELETE", path = "api/friends/delete", hasBody = true)
    Call<Map<String, Object>> deleteFriend(@Body Map<String, String> body);

}