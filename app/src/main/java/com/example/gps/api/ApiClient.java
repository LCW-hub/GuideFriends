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
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    // --- 🔽 [이 메소드 추가] ---
    /**
     * MapsActivity 등에서 프로필 이미지 URL을 완성하기 위해
     * 서버의 기본 URL을 반환합니다.
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }
    // --- 🔼 [추가 완료] ---

    private static Retrofit retrofit = null; // (인증 API 호출용)
    private static Retrofit refreshRetrofit = null; // (토큰 갱신 API 호출용)
    private static Retrofit publicRetrofit = null; // (공개 API 호출용)

    // [MERGE] 인증용 OkHttpClient를 싱글톤으로 관리하기 위한 변수
    private static OkHttpClient authOkHttpClient = null;

    /**
     * [MERGE] 인증이 필요한 OkHttpClient를 생성하는 private 헬퍼 메소드
     * (TokenAuthenticator가 포함된 버전)
     */
    private static synchronized OkHttpClient createAuthOkHttpClient(Context context) {
        if (authOkHttpClient == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

            // 1. AuthInterceptor 추가 (Access Token 삽입)
            httpClientBuilder.addInterceptor(new AuthInterceptor(context));

            // 2. TokenAuthenticator 추가 (401 감지 시 자동 갱신)
            httpClientBuilder.authenticator(new TokenAuthenticator());

            httpClientBuilder.addInterceptor(loggingInterceptor);
            authOkHttpClient = httpClientBuilder.build();
        }
        return authOkHttpClient;
    }

    /**
     * [MERGE] 인증이 필요한 API 호출 시 사용하는 Retrofit 인스턴스
     * (createAuthOkHttpClient 헬퍼 사용)
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            Gson gson = new GsonBuilder().setLenient().create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(createAuthOkHttpClient(context)) // [MERGE] 헬퍼 메소드 사용
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    /**
     * [MERGE] 토큰 갱신 API(/api/auth/refresh) 전용 Retrofit 인스턴스
     * (V1 코드를 그대로 유지 - 절대 수정하면 안 됨)
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
     * [MERGE] 인증이 *필요 없는* API 호출 시 사용하는 Retrofit 인스턴스
     * (V1 코드를 유지하되, 싱글톤으로 변경)
     */
    public static Retrofit getRetrofitInstance(Context context) {
        if (publicRetrofit == null) { // [MERGE] publicRetrofit 변수 사용
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            httpClientBuilder.addInterceptor(loggingInterceptor);
            // (AuthInterceptor X, Authenticator X)

            Gson gson = new GsonBuilder().setLenient().create();

            publicRetrofit = new Retrofit.Builder() // [MERGE] publicRetrofit에 할당
                    .baseUrl(BASE_URL)
                    .client(httpClientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return publicRetrofit; // [MERGE] publicRetrofit 반환
    }

    // --- [MERGE] V2의 Glide 연동용 OkHttpClient Getter 추가 ---
    /**
     * [MERGE] Glide 모듈 등이 사용할, 인증 헤더가 포함된 OkHttpClient를 반환하는 정적 메서드
     */
    public static OkHttpClient getAuthOkHttpClient(Context context) {
        return createAuthOkHttpClient(context);
    }
    // --- [MERGE] 추가 완료 ---

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