package com.example.gps.api;

import com.example.gps.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserApi {

    @POST("/api/users/signup")
    Call<Map<String, Object>> signup(@Body User user);

    @POST("/api/users/login")
    Call<Map<String, Object>> login(@Body User user);

    @GET("/api/users/{userId}")
    Call<Map<String, Object>> getUser(@Path("userId") int userId);

    @PUT("/api/users/{userId}/coins")
    Call<Map<String, Object>> updateCoins(@Path("userId") int userId, @Body Map<String, Integer> coinData);

    @POST("/api/users/{userId}/step-reward")
    Call<Map<String, Object>> requestStepReward(@Path("userId") int userId, @Body Map<String, Object> stepData);

    @GET("/api/users")
    Call<Map<String, Object>> getUserList();

}