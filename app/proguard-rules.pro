# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# UI 성능 개선을 위한 규칙들
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# 네이버 지도 SDK 보호
-keep class com.naver.maps.** { *; }
-keep class com.naver.maps.map.** { *; }

# Firebase 관련 클래스 보호
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Retrofit 관련 클래스 보호
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keep class * {
    @retrofit2.http.* <methods>;
}

# Glide 관련 클래스 보호
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# OkHttp 관련 클래스 보호
-keepnames class okhttp3.internal.http2.Http2Connection
-keepnames class okhttp3.internal.http2.Http2Connection$Listener
-keepnames class okhttp3.internal.http2.Http2Stream
-keepnames class okhttp3.internal.http2.Http2Stream$FramingSource
-keepnames class okhttp3.internal.http2.Http2Stream$FramingSink

# UI 관련 클래스 보호
-keep class androidx.** { *; }
-keep class android.support.** { *; }
-keep class com.google.android.material.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# UI 성능 최적화
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification