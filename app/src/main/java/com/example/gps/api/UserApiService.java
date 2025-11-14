package com.example.gps.api;

import com.example.gps.model.User;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import java.util.Map;
import retrofit2.http.Path;

// 사용자 관련 API 호출을 정의하는 인터페이스입니다.
public interface UserApiService {

    // (이 API는 현재 사용 중 - MapsActivity Line 508)
    @GET("/api/users/id")
    Call<Map<String, Long>> getUserIdByUsername(@Query("username") String username);

    @Multipart
    @POST("/api/users/profile-image")
    Call<Map<String, Object>> uploadProfileImage(
            @Part MultipartBody.Part image
    );

    @DELETE("/api/users/profile-image")
    Call<Map<String, Object>> setDefaultProfileImage();

    // ⭐️ [수정] "팀원 마커 403" 오류 해결
    // 주소 맨 앞에 슬래시(/)가 누락되어 있었습니다.
    @GET("/api/users/{id}/profile-image")
    Call<Map<String, String>> getProfileImageUrl(@Path("id") Long userId);

    @POST("/api/users/logout")
    Call<Map<String, Object>> logout();

    // ⭐️ [삭제]
    // MapsActivity가 더 이상 사용하지 않는 옛날 API이므로 삭제합니다.
    /*
    @GET("/api/users/profile")
    Call<Map<String, String>> getUserProfile();
    */

    // ⭐️ [추가] "이메일 로드 403" 오류 해결
    // MapsActivity(Line 1056)가 호출하는 userApiService.getMyEmail()과
    // UserController의 @GetMapping("/api/users/me/email")을 연결합니다.
    @GET("/api/users/me/email")
    Call<Map<String, String>> getMyEmail();
}