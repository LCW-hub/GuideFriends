package com.example.gps.api;

import com.example.gps.dto.LoginResponse;
import com.example.gps.model.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart; // ⭐ [핵심 추가]
import retrofit2.http.Part;      // ⭐ [핵심 추가]
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserApi {

    // --- 회원 관리 API (UserController) ---
    @POST("/api/users/signup")
    Call<Map<String, Object>> signup(@Body User user);

    @POST("login")
    Call<LoginResponse> login(@Body Map<String, String> loginData);

    // [추가] 실제 로그아웃 API
    @POST("/api/users/logout")
    Call<Map<String, Object>> logout();

    // [추가] 토큰 재발급 API
    @POST("/api/auth/refresh")
    Call<LoginResponse> refreshToken(@Body Map<String, String> request);

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

    // --- 프로필 이미지 관리 API (MapsActivity에서 사용) ---

    // ⭐ 1. 프로필 이미지 URL 조회 (팀원 마커 이미지 로딩용)
    @GET("/api/users/profile-image")
    Call<Map<String, String>> getProfileImageUrl(@Query("userId") Long userId);

    // ⭐ 2. 프로필 이미지 업로드 (MapsActivity.uploadImageToServer에서 사용)
    @Multipart // 👈 ⭐ 파일 업로드를 위해 추가된 핵심 어노테이션
    @POST("/api/users/profile-image")
    Call<Map<String, Object>> uploadProfileImage(@Part MultipartBody.Part image); // 👈 ⭐ @Body에서 @Part로 변경

    // ⭐ 3. 프로필 이미지를 기본값으로 설정 (MapsActivity.setProfileToDefault에서 사용)
    @DELETE("/api/users/profile-image/default")
    Call<Map<String, Object>> setDefaultProfileImage();


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

    @GET("/api/users/username/{username}") // 사용자 이름으로 ID를 가져오는 API
    Call<Map<String, Long>> getUserIdByUsername(@Path("username") String username);


}