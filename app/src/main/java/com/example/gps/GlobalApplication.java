package com.example.gps;

import android.app.Application;
import android.content.Context; // [추가] Context import

import com.naver.maps.map.NaverMapSdk;

public class GlobalApplication extends Application {

    // [추가] 앱의 Context를 저장할 static 변수
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        // [추가] 앱이 시작될 때 context를 저장합니다.
        context = getApplicationContext();


    }

    // [추가] 앱의 어느 곳에서나 Context를 참조할 수 있게 해주는 static 메소드
    public static Context getContext() {
        return context;
    }
}