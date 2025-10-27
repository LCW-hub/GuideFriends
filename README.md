   # 길잡이 친구 (GuideFriend)

## 📱 프로젝트 개요
자연과 함께하는 안전한 산책을 위한 GPS 기반 산책로 탐색 및 정보 제공 Android 애플리케이션

> 🚶‍♀️ **"자연과 함께하는 안전한 산책"** - 산책로 도우미가 당신의 산책을 더욱 즐겁고 안전하게 만들어드립니다.

## 🏗️ 프로젝트 구조
```
GPS/
├── app/                    # Android 앱 소스코드
│   ├── src/main/
│   │   ├── java/com/example/gps/
│   │   │   ├── api/       # API 클라이언트
│   │   │   ├── model/     # 데이터 모델
│   │   │   └── *.java     # 액티비티 및 프래그먼트
│   │   ├── res/           # 리소스 파일
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── server/                 # Node.js 백엔드 서버
│   ├── routes/            # API 라우트
│   ├── server.js          # 메인 서버 파일
│   └── package.json
└── README.md
```

## ✨ 주요 기능

### 🗺️ 지도 및 경로
- **네이버 지도 API** 연동으로 정확한 지도 표시
- **실시간 GPS 위치 추적** 및 현재 위치 표시
- **산책로 경로 표시** 및 경로 안내
- **위험 구역 및 편의시설 마커** 표시
- **검색 기능**: 장소 검색 및 해당 위치로 자동 이동

### 📊 코스 정보
- **경로별 상세 정보**: 소요시간, 걸음수, 거리
- **애완동물 동반 가능 여부** 표시
- **소모 칼로리 및 수분 섭취량** 자동 계산
- **유동인구 정보** 및 혼잡도 표시
- **만보기 기능**: 실시간 걸음수, 거리, 칼로리 추적

### 🚨 안전 기능
- **SOS 긴급 상황 알림** 시스템
- **주변 사용자에게 도움 요청** 기능
- **실시간 응답 시스템** 및 위치 공유

### 🌤️ 날씨 및 추천
- **OpenWeatherMap API** 연동
- **실시간 날씨 정보** 및 하단 시트 표시
- **8시간 날씨 예보** 제공
- **날씨 기반 경로 추천**
- **계절별 코스 추천**

### 🚌 교통 정보
- **대중교통 정보** 제공
- **주변 정류장 및 지하철역** 표시
- **실시간 도착 정보** 및 경로 안내

### 👥 커뮤니티
- **사용자 간 소통** 및 경험 공유
- **포토스팟 추천** 및 리뷰
- **경로 리뷰 및 평가** 시스템
- **즐겨찾기** 기능

### 🔍 검색 기능
- **실시간 장소 검색**: 공원, 카페, 병원, 학교, 지하철역 등
- **네이버 지도 API** 연동 (API 키 설정 시)
- **샘플 데이터** 제공 (API 미설정 시)
- **검색 결과 클릭 시 자동 지도 이동**

## 🛠️ 기술 스택

### Android 앱
- **언어**: Java
- **지도**: 네이버 지도 SDK, T-Map API
- **위치**: Google Play Services Location

### 백엔드 서버
- **언어**: Node.js
- **데이터베이스**: Firebase Firestore, sql developer


### 외부 API
- **날씨**: OpenWeatherMap API
- **지도**: 네이버 지도 API, T-Map API

## 📋 API 키 설정

### 필수 API 키
1. **네이버 지도 API**
   - [네이버 클라우드 플랫폼](https://www.ncloud.com/)에서 발급
   - 현재 설정된 키: `iaxrf6bmtc` (지도 SDK용)
   - 장소 검색용 API Gateway 키 별도 필요

2. **OpenWeatherMap API**
   - [OpenWeatherMap](https://openweathermap.org/api)에서 발급
   - 현재 설정된 키: `7a4aa78797771aa887fe9b14a9be94e5`

### API 키 설정 방법

#### 1. 네이버 지도 API (장소 검색용)
```java
// MapsActivity.java에서 설정
private static final String NAVER_CLIENT_ID = "YOUR_NCP_CLIENT_ID";
private static final String NAVER_CLIENT_SECRET = "YOUR_NCP_CLIENT_SECRET";
```

#### 2. AndroidManifest.xml (지도 SDK용)
```xml
<meta-data
    android:name="com.naver.maps.map.NCP_KEY_ID"
    android:value="iaxrf6bmtc" />
```

#### 3. 날씨 API
```java
// WeatherBottomSheetFragment.java에서 설정
private static final String WEATHER_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";
```

### API 키 없이 사용하기
- **검색 기능**: API 키가 없어도 샘플 데이터로 정상 작동
- **날씨 기능**: 현재 API 키로 정상 작동
- **지도 기능**: 현재 API 키로 정상 작동

## 🚀 설치 및 실행

### 필요 조건
- **Android Studio** Arctic Fox 이상
- **JDK 11** 이상
- **Android SDK** API Level 21 이상
- **네이버 지도 API 키** (선택사항)

### 설치 방법
1. **프로젝트 클론**
   ```bash
   git clone [repository-url]
   cd GuideFreind-main
   ```

2. **Android Studio에서 프로젝트 열기**
   - Android Studio 실행
   - "Open an existing project" 선택
   - 프로젝트 폴더 선택

3. **Gradle 동기화**
   - Android Studio에서 자동으로 Gradle 동기화 실행
   - 또는 `./gradlew build` 명령어 실행

4. **에뮬레이터 또는 실제 기기에서 실행**
   - 에뮬레이터: AVD Manager에서 가상 기기 생성 후 실행
   - 실제 기기: USB 디버깅 활성화 후 연결

### 빌드 명령어
```bash
# Debug 빌드
./gradlew assembleDebug

# Release 빌드
./gradlew assembleRelease

# 클린 빌드
./gradlew clean build
```

## 📱 사용법

### 기본 사용법
1. **앱 실행**: 스플래시 화면 후 메인 지도 화면으로 이동
2. **위치 권한 허용**: GPS 위치 추적을 위한 권한 허용
3. **지도 탐색**: 네이버 지도에서 원하는 지역 탐색

### 주요 기능 사용법

#### 🔍 장소 검색
1. 상단 검색바에 원하는 장소명 입력
2. 검색 아이콘 클릭 또는 엔터키 누르기
3. 검색 결과에서 원하는 장소 클릭
4. 자동으로 해당 위치로 지도 이동

#### 🌤️ 날씨 정보 확인
1. 우측 상단 날씨 위젯 클릭
2. 하단에서 날씨 정보 시트 확인
3. 현재 날씨 및 8시간 예보 확인

#### 🚶‍♀️ 만보기 기능
1. 하단 만보기 아이콘 클릭
2. 만보기 시작/중지
3. 실시간 걸음수, 거리, 칼로리 확인

#### 📍 코스 정보 확인
1. 지도에서 코스 마커 클릭
2. 코스 상세 정보 패널 확인
3. 소요시간, 거리, 난이도 등 정보 확인

### 검색 키워드 예시
- **공원**: "공원", "park" → 한강공원, 올림픽공원 등
- **카페**: "카페", "cafe" → 스타벅스, 투썸플레이스 등
- **병원**: "병원", "hospital" → 서울대병원, 삼성서울병원 등
- **학교**: "학교", "school" → 서울대학교, 연세대학교 등
- **지하철**: "지하철", "station" → 강남역, 홍대입구역 등

## 🐛 문제 해결

### 자주 발생하는 문제들

#### 1. 에뮬레이터에서 한국어 입력 안됨
- **해결방법**: Settings → System → Languages & input → Virtual keyboard → Korean 추가
- **대안**: 영어로 검색 ("park", "cafe", "hospital" 등)

#### 2. 위치 권한 오류
- **해결방법**: 앱 설정에서 위치 권한 허용
- **에뮬레이터**: Extended Controls → Location에서 위치 설정

#### 3. 네이버 API 인증 오류
- **현재 상태**: 샘플 데이터로 정상 작동
- **실제 API 사용**: 네이버 클라우드 플랫폼에서 API Gateway 키 발급

#### 4. 빌드 오류
- **해결방법**: `./gradlew clean` 후 다시 빌드
- **Android Studio**: File → Invalidate Caches and Restart

## 👥 팀 정보
- **팀명**: 길잡이 친구들(GuideFriends)
- **팀장**: 이채운
- **팀원**: 박희재, 옥진서, 이강호

## 📞 문의
프로젝트 관련 문의사항이 있으시면 이슈를 생성해주세요.

## 🤝 기여하기
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스
이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 🎯 향후 계획
- [ ] 실시간 사용자 위치 공유 기능
- [ ] AI 기반 개인화된 코스 추천
- [ ] 소셜 로그인 연동
- [ ] 오프라인 지도 지원
- [ ] 다국어 지원 (영어, 일본어, 중국어)
- [ ] Apple Watch 연동
- [ ] 음성 안내 기능

## 📊 프로젝트 통계
- **개발 기간**: 2024년 1월 ~ 현재
- **주요 언어**: Java (Android)
- **API 연동**: 네이버 지도, OpenWeatherMap
- **지원 OS**: Android 5.0 (API 21) 이상

---

<div align="center">

**🚶‍♀️ 자연과 함께하는 안전한 산책을 위한 길잡이 친구 🚶‍♂️**

Made with ❤️ by GuideFriends Team

</div> 