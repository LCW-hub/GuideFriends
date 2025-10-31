package com.example.gps.api;

import com.example.gps.model.User; // User 모델을 사용한다면 import 필요
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

// 사용자 관련 API 호출을 정의하는 인터페이스입니다.
public interface UserApiService {

    // ⭐ [추가] username을 이용해 userId를 조회하는 API
    // 서버의 API 엔드포인트가 예시로 /api/users/id?username={username} 이라 가정합니다.
    @GET("/api/users/id")
    Call<Map<String, Long>> getUserIdByUsername(@Query("username") String username);

    @Multipart
    @POST("/api/users/profile-image")
    Call<Map<String, Object>> uploadProfileImage(
            @Part MultipartBody.Part image // "image"는 @RequestParam("image")와 일치
    );

    // ⭐ [추가] 프로필 이미지 기본값으로 설정
    @DELETE("/api/users/profile-image")
    Call<Map<String, Object>> setDefaultProfileImage();
}