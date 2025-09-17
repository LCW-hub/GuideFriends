package com.example.gps;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;
import android.util.Log;
import com.google.firebase.FirebaseApp;

public class GuideFriendApplication extends Application {
    private static final String TAG = "GuideFriendApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // UI 성능 개선을 위한 설정
        setupPerformanceOptimizations();
        
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            // Firebase 초기화 실패 시 로그 출력
            Log.e(TAG, "Firebase initialization failed", e);
        }
    }
    
    private void setupPerformanceOptimizations() {
        // 개발 모드에서만 StrictMode 활성화
        boolean isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (isDebug) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());
                
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
        }
        
        // 메모리 최적화 설정
        System.setProperty("rx.ring-buffer.size", "128");
    }
} 