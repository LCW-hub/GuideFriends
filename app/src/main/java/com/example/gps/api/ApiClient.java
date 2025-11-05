package com.example.gps.api;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // [수정] IP 주소 확인 (백엔드 서버 IP)
    private static final String BASE_URL = "http://172.30.1.55:8080";

    private static Retrofit retrofit = null; // (일반 API 호출용)
    private static Retrofit refreshRetrofit = null; // (토큰 갱신 API 호출용)

    /**
     * [수정] 인증이 필요한 API 호출 시 사용하는 Retrofit 인스턴스
     * (AuthInterceptor + TokenAuthenticator 포함)
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // 로그 인터셉터 (디버깅용)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // [수정] OkHttpClient 설정
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

            // 1. [수정] 3단계에서 수정한 AuthInterceptor 추가 (Access Token 삽입)
            httpClientBuilder.addInterceptor(new AuthInterceptor(context));

            // 2. [추가] 4단계에서 생성한 TokenAuthenticator 추가 (401 감지 시 자동 갱신)
            httpClientBuilder.authenticator(new TokenAuthenticator());

            httpClientBuilder.addInterceptor(loggingInterceptor);
            OkHttpClient okHttpClient = httpClientBuilder.build();

            Gson gson = new GsonBuilder().setLenient().create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // [수정] 새로 만든 okHttpClient 사용
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    /**
     * [추가] 토큰 갱신 API(/api/auth/refresh) 전용 Retrofit 인스턴스
     * (중요: 이 인스턴스는 AuthInterceptor나 Authenticator를 *포함하지 않습니다.*)
     * (이것을 따로 만들지 않으면 갱신 요청이 401 날 경우 무한 루프에 빠짐)
     */
    public static Retrofit getRefreshRetrofitInstance() {
        if (refreshRetrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            httpClientBuilder.addInterceptor(loggingInterceptor);
            // (AuthInterceptor X, Authenticator X)

            refreshRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return refreshRetrofit;
    }

    /**
     * [수정] 인증이 *필요 없는* API 호출 시 사용하는 Retrofit 인스턴스
     * (예: 회원가입, 아이디/비밀번호 찾기 등)
     */
    public static Retrofit getRetrofitInstance(Context context) {
        // (기존 getClient와 동일한 로직이지만, AuthInterceptor와 Authenticator가 없음)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(loggingInterceptor);
        // (AuthInterceptor X, Authenticator X)

        Gson gson = new GsonBuilder().setLenient().create();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    // --- (기존 UserApiService, GroupApiService, FriendApiService Getter는 그대로 유지) ---

    public static UserApiService getUserApiService(Context context) {
        return getClient(context).create(UserApiService.class);
    }

    public static GroupApiService getGroupApiService(Context context) {
        return getClient(context).create(GroupApiService.class);
    }

    public static FriendApiService getFriendApiService(Context context) {
        return getClient(context).create(FriendApiService.class);
    }
}