package com.example.gps.api;

import com.example.gps.utils.TokenManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor; // ✅ 로깅 인터셉터 import 추가
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Context;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.219.102:8080/";
    private static Retrofit retrofit = null;
    private static GroupApiService groupApiService;

    public static synchronized Retrofit getClient(Context context) {
        if (retrofit == null) {
            // TokenManager 인스턴스 생성
            TokenManager tokenManager = new TokenManager(context.getApplicationContext());

            // ✅ HttpLoggingInterceptor 생성
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // 요청/응답의 Body까지 모두 로그로 출력

            // AuthInterceptor와 LoggingInterceptor를 사용하는 OkHttpClient 생성
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(tokenManager))
                    .addInterceptor(loggingInterceptor) // ✅ 로깅 인터셉터 추가
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // 직접 만든 OkHttpClient를 사용하도록 설정
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