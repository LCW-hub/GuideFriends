package com.example.gps.api;

import com.example.gps.api.AuthInterceptor;
import com.example.gps.api.GroupApiService;
import com.example.gps.utils.TokenManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor; // 로깅 인터셉터 import
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Context;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;
    private static GroupApiService groupApiService;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    // ⭐ [핵심 추가] 인증 OkHttpClient 인스턴스를 직접 생성하여 반환하는 헬퍼 메서드
    private static OkHttpClient createAuthOkHttpClient(Context context) {
        TokenManager tokenManager = new TokenManager(context.getApplicationContext());

        // 1. HttpLoggingInterceptor 생성 및 레벨 설정
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 2. AuthInterceptor와 Logging Interceptor를 사용하는 OkHttpClient 생성
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenManager))
                .addInterceptor(logging)
                .build();
    }

    // ⭐ [수정] Retrofit 인스턴스를 생성할 때 위 헬퍼 메서드를 사용
    public static synchronized Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            OkHttpClient okHttpClient = createAuthOkHttpClient(context); // 👈 수정: 헬퍼 메서드 사용

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // ✅ 직접 만든 OkHttpClient를 사용하도록 설정
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // ⭐ [핵심 추가]: Glide 모듈이 사용할, 인증 헤더가 포함된 OkHttpClient를 반환하는 정적 메서드
    //               Glide가 이미지 로드시 이 클라이언트를 사용하게 됩니다.
    public static OkHttpClient getAuthOkHttpClient(Context context) {
        return createAuthOkHttpClient(context);
    }

    // GroupApiService를 싱글톤으로 제공 (기존 코드 유지)
    public static GroupApiService getGroupApiService(Context context) {
        if (groupApiService == null) {
            groupApiService = getRetrofit(context).create(GroupApiService.class);
        }
        return groupApiService;
    }
}