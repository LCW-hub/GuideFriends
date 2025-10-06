package com.example.gps.api;

import com.example.gps.api.AuthInterceptor;
import com.example.gps.api.GroupApiService;
import com.example.gps.utils.TokenManager; // TokenManager import
import okhttp3.OkHttpClient; // OkHttpClient import
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Context; // Context import

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;
    private static GroupApiService groupApiService;

    public static synchronized Retrofit getClient(Context context) {
        if (retrofit == null) {
            // TokenManager 인스턴스 생성
            TokenManager tokenManager = new TokenManager(context.getApplicationContext());

            // AuthInterceptor를 사용하는 OkHttpClient 생성
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(tokenManager))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // ✅ 직접 만든 OkHttpClient를 사용하도록 설정
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // GroupApiService를 싱글톤으로 제공
    public static GroupApiService getGroupApiService(Context context) {
        if (groupApiService == null) {
            groupApiService = getClient(context).create(GroupApiService.class);
        }
        return groupApiService;
    }
}