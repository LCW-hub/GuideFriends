package com.example.gps.api;

import com.example.gps.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface UserApi {

    @POST("/api/users/signup")
    Call<Map<String, String>> signup(@Body User user);

    // ✅ 이 부분 교체
    @POST("/api/users/login")
    Call<Map<String, String>> login(@Body User user);

    @GET("users/list")
    Call<List<User>> getUserList();

}