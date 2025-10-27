package com.example.gps.api;

import com.example.gps.model.WeatherInfo;
import com.example.gps.model.WeatherForecast;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("weather")
    Call<WeatherInfo> getWeather(
        @Query("q") String city,
        @Query("appid") String apiKey
    );

    @GET("forecast")
    Call<WeatherForecast> getWeatherForecast(
        @Query("q") String city,
        @Query("appid") String apiKey
    );

    // 위도/경도 기반 현재 날씨 정보
    @GET("weather")
    Call<WeatherInfo> getWeatherByCoordinates(
        @Query("lat") double latitude,
        @Query("lon") double longitude,
        @Query("appid") String apiKey
    );

    // 위도/경도 기반 5일 예보
    @GET("forecast")
    Call<WeatherForecast> getWeatherForecastByCoordinates(
        @Query("lat") double latitude,
        @Query("lon") double longitude,
        @Query("appid") String apiKey
    );
}