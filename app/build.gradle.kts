plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gps"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gps"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // UI 성능 개선을 위한 설정
        vectorDrawables.useSupportLibrary = true
    }

    // 리소스 최적화 설정
    androidResources {
        noCompress += listOf("png", "jpg", "jpeg")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    // UI 성능 개선을 위한 설정
    buildFeatures {
        viewBinding = true
    }

    configurations {
        all {
            exclude(group = "androidx.annotation", module = "annotation-experimental")
        }
    }

    // 리소스 충돌 해결
    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.naver.maps:map-sdk:3.21.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // implementation("com.github.bumptech.glide:glide:4.12.0") // ❌ 제거
    // annotationProcessor("com.github.bumptech.glide:compiler:4.12.0") // ❌ 제거 (4.16.0으로 통일)

    // 💡 Firebase BOM 버전 업데이트 (33.0.0 이상으로 권장)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // 💡 위치 공유를 위해 Realtime Database 추가
    implementation("com.google.firebase:firebase-database")

    implementation("com.google.firebase:firebase-analytics")

    // Cloud Firestore 라이브러리 추가
    implementation("com.google.firebase:firebase-firestore")

    // ✅ Glide 기본 및 컴파일러 추가 (중복 제거)
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // 🌟 에러 해결을 위한 핵심 추가: Glide OkHttp3 통합 라이브러리
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")

    // OkHttp 라이브러리 및 인터셉터
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Desugaring 라이브러리 (중복 제거 후 하나만 남김)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // CircleImageView for profile image in MyPage drawer
    implementation("de.hdodenhof:circleimageview:3.1.0")
}