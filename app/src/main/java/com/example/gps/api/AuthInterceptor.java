package com.example.gps.api;

import android.content.Context;
import com.example.gps.utils.TokenManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    // [수정] TokenManager를 멤버 변수로 선언
    private TokenManager tokenManager;

    // [수정] 생성자 (Context는 TokenManager 생성에만 사용)
    public AuthInterceptor(Context context) {
        // [수정] 1단계에서 수정한 새 TokenManager 생성자 사용
        this.tokenManager = new TokenManager();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // [수정] 1단계에서 수정한 getAccessToken() 메소드 호출
        String token = tokenManager.getAccessToken();

        Request.Builder requestBuilder = chain.request().newBuilder();

        // 토큰이 있는 경우에만 헤더에 추가
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        return chain.proceed(requestBuilder.build());
    }
}