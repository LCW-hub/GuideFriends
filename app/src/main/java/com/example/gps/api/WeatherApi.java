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
}