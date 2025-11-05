package com.example.gps.api;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gps.GlobalApplication;
import com.example.gps.activities.Register_Login.LoginActivity;
import com.example.gps.dto.LoginResponse;
import com.example.gps.utils.TokenManager;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

/**
 * 401 Unauthorized 응답을 받았을 때 (Access Token 만료 시)
 * 자동으로 Access Token 갱신을 시도하는 클래스입니다.
 */
public class TokenAuthenticator implements Authenticator {

    private static final String TAG = "TokenAuthenticator";
    private final TokenManager tokenManager;

    public TokenAuthenticator() {
        this.tokenManager = new TokenManager();
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {

        // 1. 저장된 Refresh Token(만능키)을 가져옵니다.
        final String refreshToken = tokenManager.getRefreshToken();

        // 2. 만능키가 없으면(자동 로그인 안 함) 갱신을 포기합니다.
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "authenticate: Refresh Token이 없습니다. 갱신 포기.");
            forceLogout();
            return null; // 갱신 포기
        }

        // 3. 401 오류가 발생한 요청의 Access Token이 현재 저장된 Access Token과 동일한지 확인합니다.
        //    (여러 요청이 동시에 401을 받았을 때, 갱신을 한 번만 시도하기 위한 잠금(synchronized) 처리)
        synchronized (this) {
            final String currentAccessToken = tokenManager.getAccessToken();
            final String failedAccessToken = response.request().header("Authorization").replace("Bearer ", "");

            // 현재 토큰과 실패한 토큰이 다르면, 다른 스레드에서 이미 갱신에 성공한 것입니다.
            // (실패했던 원래 요청을 새 토큰으로 재시도합니다.)
            if (!currentAccessToken.equals(failedAccessToken)) {
                Log.d(TAG, "authenticate: 토큰이 이미 갱신되었습니다. 새 토큰으로 재시도합니다.");
                return newRequestWithToken(response.request(), currentAccessToken);
            }

            // 4. "만능키"로 새 "일반 열쇠"(Access Token)를 받아오는 동기식 API 호출
            Log.d(TAG, "authenticate: Access Token 만료. Refresh Token으로 동기 갱신 시도...");
            UserApi userApi = ApiClient.getRefreshRetrofitInstance().create(UserApi.class); // (중요: 새 Retrofit 인스턴스 사용)

            Map<String, String> refreshRequest = new HashMap<>();
            refreshRequest.put("refreshToken", refreshToken);

            Call<LoginResponse> call = userApi.refreshToken(refreshRequest);

            try {
                // (주의: Authenticator 내에서는 비동기(enqueue)가 아닌 동기(execute)로 호출해야 함)
                retrofit2.Response<LoginResponse> refreshResponse = call.execute();

                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    // 5. 갱신 성공!
                    LoginResponse loginResponse = refreshResponse.body();
                    String newAccessToken = loginResponse.getAccessToken();
                    String newRefreshToken = loginResponse.getRefreshToken(); // (Refresh Token도 갱신될 수 있음)

                    // 새 토큰들을 저장합니다.
                    tokenManager.saveTokens(newAccessToken, newRefreshToken);
                    Log.i(TAG, "authenticate: Access Token 갱신 성공!");

                    // 6. 실패했던 원래 요청에 '새 Access Token'을 붙여서 재시도합니다.
                    return newRequestWithToken(response.request(), newAccessToken);

                } else {
                    // 7. 갱신 실패 (예: Refresh Token 마저 만료됨, 서버가 강제 로그아웃 시킴)
                    Log.e(TAG, "authenticate: Refresh Token 갱신 실패. 코드: " + refreshResponse.code());
                    forceLogout(); // 강제 로그아웃 처리
                    return null; // 갱신 포기
                }
            } catch (Exception e) {
                Log.e(TAG, "authenticate: 갱신 중 예외 발생", e);
                forceLogout();
                return null; // 갱신 포기
            }
        }
    }

    /**
     * 강제로 로그아웃 처리하고 로그인 화면으로 보냅니다.
     */
    private void forceLogout() {
        tokenManager.deleteTokens(); // 모든 토큰 삭제
        Context context = GlobalApplication.getContext();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * 기존 요청(Request)에 새로운 토큰(newAccessToken)을 헤더에 담아 반환합니다.
     */
    @NonNull
    private Request newRequestWithToken(@NonNull Request request, @NonNull String newAccessToken) {
        return request.newBuilder()
                .header("Authorization", "Bearer " + newAccessToken)
                .build();
    }
}