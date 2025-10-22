package com.example.gps.api;

import com.example.gps.model.User; // User 모델을 사용한다면 import 필요
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.Map;

// 사용자 관련 API 호출을 정의하는 인터페이스입니다.
public interface UserApiService {

    // ⭐ [추가] username을 이용해 userId를 조회하는 API
    // 서버의 API 엔드포인트가 예시로 /api/users/id?username={username} 이라 가정합니다.
    @GET("/api/users/id")
    Call<Map<String, Long>> getUserIdByUsername(@Query("username") String username);
}