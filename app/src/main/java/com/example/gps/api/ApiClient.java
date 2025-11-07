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

    private static Retrofit retrofit = null; // (인증 API 호출용)
    private static Retrofit refreshRetrofit = null; // (토큰 갱신 API 호출용)

    // --- 🔽 [추가] 공용 Retrofit 및 인증 OkHttpClient 싱글톤 인스턴스 ---
    private static Retrofit publicRetrofit = null;
    private static OkHttpClient authOkHttpClient = null;
    // --- 🔼 [추가 완료] ---


    /**
     * [신규] 두 번째 코드의 장점을 가져온 메소드
     * 인증이 필요한 OkHttpClient를 싱글톤으로 생성/제공합니다.
     * (Glide 라이브러리 등에서 사용 가능)
     */
    public static synchronized OkHttpClient getAuthOkHttpClient(Context context) {
        if (authOkHttpClient == null) {
            // 로그 인터셉터 (디버깅용)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttpClient 설정
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
     * [수정] 인증이 필요한 API 호출 시 사용하는 Retrofit 인스턴스
     * (위에서 만든 공용 OkHttpClient를 사용하도록 수정)
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            Gson gson = new GsonBuilder().setLenient().create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getAuthOkHttpClient(context)) // [수정] 공용 인증 OkHttpClient 사용
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    /**
     * [유지] 토큰 갱신 API(/api/auth/refresh) 전용 Retrofit 인스턴스
     * (기존 코드와 동일하며, 절대 수정하면 안 됨)
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
     * (기능은 동일, publicRetrofit 변수를 사용하도록 싱글톤 방식 강화)
     */
    public static Retrofit getRetrofitInstance(Context context) {
        if (publicRetrofit == null) { // [수정] publicRetrofit 변수 확인
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            httpClientBuilder.addInterceptor(loggingInterceptor);
            // (AuthInterceptor X, Authenticator X)

            Gson gson = new GsonBuilder().setLenient().create();

            publicRetrofit = new Retrofit.Builder() // [수정] publicRetrofit에 할당
                    .baseUrl(BASE_URL)
                    .client(httpClientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return publicRetrofit; // [수정] publicRetrofit 반환
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