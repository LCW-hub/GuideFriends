package com.example.gps;

import android.app.Application;
import com.naver.maps.map.NaverMapSdk;

public class GlobalApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 앱이 시작될 때 단 한 번, 가장 먼저 실행됩니다.
        NaverMapSdk.getInstance(this).setClient(
                new NaverMapSdk.NaverCloudPlatformClient("v0la9en3t0")
        );
    }
}