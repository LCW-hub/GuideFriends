package com.example.gps.utils;

import android.content.Context;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.example.gps.api.ApiClient; // ApiClient import

import java.io.InputStream;
import okhttp3.OkHttpClient;

@GlideModule
public class AuthGlideModule extends AppGlideModule {

    // 🌟 이 부분을 추가해야 합니다! 🌟
    public AuthGlideModule() {
        // Glide가 클래스를 인스턴스화 할 때 필요합니다.
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // ApiClient에서 인증 헤더가 포함된 OkHttpClient를 가져옵니다.
        OkHttpClient client = ApiClient.getAuthOkHttpClient(context);

        // Glide에게 URL 로드시 OkHttp를 사용하도록 등록합니다.
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }
}