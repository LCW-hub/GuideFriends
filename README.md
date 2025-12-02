# 📍 WICHIN - 그룹 실시간 위치 공유 앱

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=for-the-badge&logo=Firebase&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-43853D?style=for-the-badge&logo=node.js&logoColor=white)

**친구들과 함께하는 안전한 실시간 위치 공유 서비스**

[기능 소개](#-주요-기능) • [설치 방법](#-설치-및-실행) • [사용법](#-사용법) • [기술 스택](#-기술-스택)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

---

## 📱 프로젝트 소개

**WICHIN**은 친구, 가족, 동료들과 함께 그룹을 만들어 실시간으로 위치를 공유할 수 있는 Android 애플리케이션입니다. WICHIN은 "위치를 공유하는 친구들"의 약자입니다.

### 📦 프로젝트 구성
- **Android 앱**: 이 저장소 (현재 저장소)
- **백엔드 서버**: [GPS_SpringBoot-MySQL](https://github.com/johnock/GPS_SpringBoot-MySQL-.git) (Spring Boot + MySQL) 

### 🎯 핵심 가치
- 🎯 **목적지 기반 모임**: 그룹 생성 시 목적지를 설정하고 함께 도착할 수 있습니다
- 👥 **그룹 위치 공유**: 그룹 멤버들의 실시간 위치를 지도에서 확인할 수 있습니다
- 🔒 **개인화된 공유 설정**: 각 멤버별로 위치 공유 허용/거부를 세밀하게 설정할 수 있습니다
- 💬 **그룹 채팅**: 그룹 내에서 실시간으로 소통할 수 있습니다
- 🗺️ **네이버 지도 연동**: 정확한 지도 표시와 경로 안내를 제공합니다
- 🌤️ **날씨 정보**: 현재 위치의 실시간 날씨 및 예보를 확인할 수 있습니다

---

## ✨ 주요 기능

### 👥 그룹 관리

#### 그룹 생성
- **그룹 이름 설정**: 원하는 그룹 이름 지정
- **목적지 선택**: 네이버 지도에서 목적지 검색 및 선택
- **시간 설정**: 시작 시간 및 종료 시간 설정 (KST 기준)
- **멤버 초대**: 친구 목록에서 그룹 멤버 선택
- **Firebase 연동**: 목적지 정보를 Firebase에 저장하여 모든 멤버가 확인 가능

#### 그룹 목록
- **내 그룹 조회**: 참여 중인 모든 그룹을 한눈에 확인
- **그룹 선택**: 그룹 클릭 시 위치 공유 설정 화면으로 이동
- **그룹 정보 표시**: 그룹 이름, 멤버 수 등 정보 표시

### 📍 실시간 위치 공유

#### 위치 추적 시스템
- **Firebase Realtime Database** 기반 실시간 위치 동기화
- **자동 위치 업데이트**: 설정된 주기로 자동으로 위치 정보 전송
- **그룹 멤버 위치 표시**: 지도에서 모든 멤버의 현재 위치를 마커로 표시
- **프로필 이미지 마커**: 각 멤버의 프로필 이미지를 마커로 표시
- **위치 공유 제어**: 위치 공유 시작/중지 기능
- **그룹 유효성 검사**: 그룹이 삭제되면 자동으로 위치 공유 중단

#### 위치 공유 규칙 설정
- **개인별 공유 설정**: 각 그룹 멤버에게 내 위치를 공유할지 말지 개별 설정
- **동적 공유 규칙**: 그룹 설정 화면에서 실시간으로 공유 규칙 변경 가능
- **프라이버시 보호**: 원하지 않는 사용자에게는 위치를 숨길 수 있습니다
- **설정 저장**: 변경된 공유 규칙을 서버에 저장하여 영구 적용

### 👫 친구 관리

#### 친구 요청 시스템
- **친구 요청 보내기**: 사용자명으로 친구 요청 전송
- **받은 요청 관리**: 받은 친구 요청 목록 확인 및 수락/거절
- **보낸 요청 관리**: 보낸 친구 요청 목록 확인 및 취소
- **친구 목록**: 현재 친구 목록 조회 및 관리
- **친구 삭제**: 더 이상 친구가 아닌 사용자 삭제

#### 온라인 상태 표시
- **Firebase Presence**: Firebase를 통한 실시간 온라인 상태 감지
- **상태 표시**: 친구 목록에서 온라인/오프라인 상태 표시
- **실시간 업데이트**: 상태 변경 시 즉시 반영

### 💬 그룹 채팅

#### 실시간 채팅
- **Firebase Firestore** 기반 실시간 메시지 교환
- **그룹별 채팅방**: 각 그룹마다 독립적인 채팅 공간
- **메시지 표시**: 발신자 이름, 메시지 내용, 시간 표시
- **자동 스크롤**: 새 메시지 수신 시 자동으로 스크롤

### 🗺️ 지도 및 네비게이션

#### 네이버 지도 기능
- **네이버 지도 SDK** 연동으로 정확한 지도 표시
- **지도 타입 변경**: 일반 지도, 위성 지도, 지형 지도 선택 가능
- **현재 위치 표시**: 실시간 GPS 위치 추적 및 표시
- **목적지 마커**: 그룹 목적지를 지도에 표시
- **경로 안내**: 목적지까지의 경로 확인

#### 장소 검색
- **네이버 검색 API** 연동
- **실시간 검색**: 검색어 입력 시 즉시 결과 표시
- **검색 결과 표시**: 장소명, 주소, 카테고리 정보 표시
- **이미지 표시**: 검색 결과에 장소 이미지 표시
- **지도 이동**: 검색 결과 클릭 시 해당 위치로 자동 이동
- **상세 정보**: 장소 상세 정보 Bottom Sheet 표시

### 🌤️ 날씨 정보

#### 날씨 위젯
- **OpenWeatherMap API** 연동
- **현재 날씨**: 현재 위치의 실시간 날씨 정보 표시
- **날씨 아이콘**: 날씨 상태에 따른 아이콘 표시
- **온도 표시**: 현재 온도 표시

#### 날씨 상세 정보
- **Bottom Sheet**: 날씨 위젯 클릭 시 상세 정보 표시
- **시간별 예보**: 24시간 날씨 예보 (시간, 아이콘, 온도)
- **일별 예보**: 일주일 날씨 예보 (요일, 최저/최고 온도)
- **업데이트 시간**: 마지막 업데이트 시간 표시

### 👤 사용자 관리

#### 계정 관리
- **회원가입**: 이메일, 사용자명, 비밀번호로 계정 생성
- **로그인**: 사용자명/비밀번호 기반 로그인
- **아이디 찾기**: 이메일로 아이디 찾기
- **비밀번호 찾기**: 이메일로 비밀번호 재설정
- **로그아웃**: 계정 로그아웃

#### 프로필 관리
- **프로필 이미지 업로드**: 갤러리에서 프로필 이미지 선택 및 업로드
- **기본 이미지 설정**: 프로필 이미지를 기본값으로 변경
- **프로필 이미지 표시**: 사이드바 및 마커에 프로필 이미지 표시

#### 설정
- **지도 타입 설정**: 기본 지도 타입 설정 및 저장
- **약관 보기**: 이용약관, 개인정보처리방침, 위치정보 이용약관 확인
- **오픈소스 라이선스**: 사용된 오픈소스 라이선스 정보 확인

---

## 🏗️ 프로젝트 구조

```
GuideFriends_main/
├── app/                          # Android 앱 소스코드
│   ├── src/main/
│   │   ├── java/com/example/gps/
│   │   │   ├── activities/       # 액티비티 클래스
│   │   │   │   ├── MapsActivity.java              # 메인 지도 화면
│   │   │   │   ├── MyGroupsActivity.java          # 그룹 목록
│   │   │   │   ├── CreateGroupActivity.java       # 그룹 생성
│   │   │   │   ├── GroupSharingSettingsActivity.java  # 위치 공유 설정
│   │   │   │   ├── ChatRoomActivity.java         # 채팅방
│   │   │   │   ├── FriendsActivity.java          # 친구 관리
│   │   │   │   ├── SettingsActivity.java         # 설정
│   │   │   │   ├── SplashActivity.java           # 스플래시 화면
│   │   │   │   └── Register_Login/                # 로그인/회원가입
│   │   │   │       ├── LoginActivity.java
│   │   │   │       ├── RegisterActivity.java
│   │   │   │       ├── FindIdActivity.java
│   │   │   │       ├── FindPwActivity.java
│   │   │   │       └── ResetPasswordActivity.java
│   │   │   ├── fragments/        # 프래그먼트
│   │   │   │   ├── WeatherBottomSheetFragment.java    # 날씨 상세 정보
│   │   │   │   └── SearchResultDetailFragment.java     # 검색 결과 상세
│   │   │   ├── api/              # API 클라이언트
│   │   │   │   ├── ApiClient.java                # Retrofit 클라이언트
│   │   │   │   ├── GroupApiService.java           # 그룹 API
│   │   │   │   ├── FriendApiService.java          # 친구 API
│   │   │   │   ├── UserApiService.java            # 사용자 API
│   │   │   │   ├── UserApi.java                  # 사용자 API (확장)
│   │   │   │   ├── AuthInterceptor.java           # 인증 인터셉터
│   │   │   │   └── TokenAuthenticator.java        # 토큰 인증
│   │   │   ├── model/            # 데이터 모델
│   │   │   │   ├── User.java
│   │   │   │   ├── Message.java
│   │   │   │   ├── SearchResult.java
│   │   │   │   └── WeatherInfo.java
│   │   │   ├── dto/              # DTO 클래스
│   │   │   │   ├── CreateGroupRequest.java
│   │   │   │   ├── GroupListResponse.java
│   │   │   │   ├── LocationResponse.java
│   │   │   │   └── FriendResponse.java
│   │   │   ├── adapters/         # RecyclerView 어댑터
│   │   │   │   ├── GroupListAdapter.java
│   │   │   │   ├── FriendAdapter.java
│   │   │   │   ├── FriendSelectAdapter.java
│   │   │   │   ├── MemberSharingAdapter.java
│   │   │   │   ├── MessageAdapter.java
│   │   │   │   ├── SearchResultAdapter.java
│   │   │   │   ├── WeatherForecastAdapter.java
│   │   │   │   └── DailyForecastAdapter.java
│   │   │   └── utils/            # 유틸리티 클래스
│   │   │       ├── TokenManager.java              # 토큰 관리
│   │   │       └── AuthGlideModule.java           # Glide 인증 모듈
│   │   ├── res/                  # 리소스 파일
│   │   │   ├── layout/           # 레이아웃 XML
│   │   │   ├── drawable/         # 드로어블 리소스
│   │   │   ├── values/           # 문자열, 색상 등
│   │   │   └── mipmap/          # 앱 아이콘
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── GPS_SpringBoot-MySQL--main/   # Spring Boot 백엔드 서버
│   # GitHub: https://github.com/johnock/GPS_SpringBoot-MySQL-.git
│   ├── src/main/java/com/example/findpathserver/
│   │   ├── controller/           # REST API 컨트롤러
│   │   │   ├── UserController.java      # 사용자 API
│   │   │   ├── GroupController.java     # 그룹 API
│   │   │   └── FriendController.java    # 친구 API
│   │   ├── service/              # 비즈니스 로직
│   │   │   ├── UserService.java
│   │   │   ├── GroupService.java
│   │   │   ├── FriendService.java
│   │   │   ├── EmailService.java        # 이메일 서비스
│   │   │   ├── FileStorageService.java  # 파일 저장 서비스
│   │   │   └── FirebaseService.java     # Firebase 연동
│   │   ├── repository/           # JPA 리포지토리
│   │   │   ├── UserRepository.java
│   │   │   ├── GroupRepository.java
│   │   │   ├── FriendRepository.java
│   │   │   └── SharingRuleRepository.java
│   │   ├── model/                # 엔티티 모델
│   │   │   ├── User.java
│   │   │   ├── Group.java
│   │   │   ├── Friend.java
│   │   │   └── SharingRule.java
│   │   ├── dto/                  # 데이터 전송 객체
│   │   │   ├── CreateGroupRequest.java
│   │   │   ├── GroupListResponse.java
│   │   │   └── LocationResponse.java
│   │   └── config/               # 설정 클래스
│   │       ├── SecurityConfig.java       # Spring Security 설정
│   │       ├── JwtUtil.java              # JWT 유틸리티
│   │       └── JwtRequestFilter.java     # JWT 필터
│   ├── resources/
│   │   ├── application.properties # 애플리케이션 설정
│   │   └── serviceAccountKey.json # Firebase 서비스 계정 키
│   ├── pom.xml                   # Maven 의존성 관리
│   └── uploads/                  # 업로드된 파일 저장 디렉토리
├── build.gradle.kts              # 프로젝트 빌드 설정
└── README.md
```

---

## 🛠️ 기술 스택

### Android 앱
- **언어**: Java
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 34 (Android 14)
- **아키텍처**: MVC 패턴
- **UI**: ViewBinding, Material Design

### 백엔드 서버
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.3.11
- **데이터베이스**: MySQL 8.0
- **ORM**: Spring Data JPA (Hibernate)
- **보안**: Spring Security + JWT
- **인증**: JWT (Access Token + Refresh Token)
- **파일 저장**: 로컬 파일 시스템 (`./uploads`)
- **이메일**: Spring Mail (Gmail SMTP)
- **Firebase**: Firebase Admin SDK

### 데이터베이스 및 실시간 통신
- **Firebase Realtime Database**: 실시간 위치 공유
- **Firebase Firestore**: 그룹 채팅 메시지 저장
- **Firebase Presence**: 사용자 온라인 상태 관리
- **Firebase Analytics**: 사용자 분석

### 외부 API 및 SDK
- **네이버 지도 SDK**: 지도 표시 및 위치 서비스
- **네이버 검색 API**: 장소 검색
- **Google Play Services Location**: GPS 위치 추적
- **OpenWeatherMap API**: 날씨 정보 제공
- **Retrofit2**: REST API 통신
- **OkHttp**: HTTP 클라이언트

### 주요 라이브러리

#### Android 앱
```gradle
- 네이버 지도 SDK: 3.21.0
- Firebase BOM: 33.0.0
- Retrofit2: 2.9.0
- Glide: 4.16.0
- Google Play Services Location: 21.0.1
- Material Design Components
```

#### Spring Boot 백엔드
```xml
- Spring Boot: 3.3.11
- Spring Security: 3.3.11
- Spring Data JPA: 3.3.11
- MySQL Connector: 8.0.33
- JWT (jjwt): 0.11.5
- Firebase Admin SDK: 9.3.0
- Lombok: (최신 버전)
- Spring Mail: 3.3.11
```

---

## 📋 사전 요구사항

### 개발 환경
- **Android Studio**: Arctic Fox 이상
- **JDK**: 11 이상 (Android), 17 이상 (백엔드)
- **Android SDK**: API Level 24 이상
- **Maven**: 3.6 이상 (백엔드 빌드)
- **MySQL**: 8.0 이상 (백엔드 데이터베이스)
- **IDE**: Eclipse (백엔드 개발 시 권장)

### 필수 API 키

#### 1. 네이버 지도 API
- [네이버 클라우드 플랫폼](https://www.ncloud.com/)에서 발급
- 지도 SDK용 키: `AndroidManifest.xml`에 설정
- 장소 검색용 API Gateway 키 (선택사항)

#### 2. Firebase 프로젝트
- [Firebase Console](https://console.firebase.google.com/)에서 프로젝트 생성
- `google-services.json` 파일을 `app/` 디렉토리에 배치
- Realtime Database 및 Firestore 활성화
- Firebase 보안 규칙 설정

#### 3. OpenWeatherMap API
- [OpenWeatherMap](https://openweathermap.org/api)에서 발급

---

## 🚀 설치 및 실행

### 네이버 지도 API 키 설정

#### AndroidManifest.xml 설정
```xml
<meta-data
    android:name="com.naver.maps.map.NCP_KEY_ID"
    android:value="YOUR_NAVER_MAP_API_KEY" />
```

#### 네이버 검색 API (선택사항)
`MapsActivity.java`에서 설정:
```java
private static final String NAVER_CLIENT_ID = "YOUR_NCP_CLIENT_ID";
private static final String NAVER_CLIENT_SECRET = "YOUR_NCP_CLIENT_SECRET";
```

#### 사용자 생성 및 권한 부여
```sql
CREATE USER 'your_username'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON guidefriends.* TO 'your_username'@'localhost';
FLUSH PRIVILEGES;
```

### 백엔드 서버

백엔드 서버는 별도 프로젝트입니다.

**GitHub 저장소**: [GPS_SpringBoot-MySQL](https://github.com/johnock/GPS_SpringBoot-MySQL-.git)

```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

### 7. Android 앱에서 백엔드 서버 주소 설정

#### ApiClient.java 수정
`app/src/main/java/com/example/gps/api/ApiClient.java`에서 서버 주소 확인:
```java
private static final String BASE_URL = "http://your-server-ip:8080/";
```

**참고**: 에뮬레이터에서 실행 시 `http://your-server-ip:8080/` 사용

### 앱 빌드 및 실행

#### 에뮬레이터 사용
1. Android Studio → Tools → AVD Manager
2. 가상 기기 생성 (API 24 이상)
3. 실행

#### 실제 기기 사용
1. 개발자 옵션 활성화
2. USB 디버깅 활성화
3. USB로 연결
4. Android Studio에서 실행

---

## 📱 사용법

### 기본 사용 흐름

#### 1️⃣ 회원가입 및 로그인
1. 앱 실행 후 회원가입 또는 로그인
2. 위치 권한 허용 (필수)
3. 인터넷 권한 확인

#### 2️⃣ 친구 추가
1. 사이드바에서 "친구" 메뉴 선택
2. 사용자명 입력 후 친구 요청 전송
3. 받은 요청에서 수락 또는 거절
4. 친구 목록에서 온라인 상태 확인

#### 3️⃣ 그룹 생성
1. 메인 화면에서 "그룹 만들기" 선택
2. 그룹 정보 입력:
   - **그룹 이름**: 원하는 그룹 이름 입력
   - **목적지**: "목적지 선택" 버튼 클릭 → 지도에서 검색 및 선택
   - **시작 시간**: 날짜 및 시간 선택
   - **종료 시간**: 날짜 및 시간 선택
   - **멤버**: 친구 목록에서 멤버 선택
3. "그룹 만들기" 버튼 클릭
4. 자동으로 지도 화면으로 이동하여 위치 공유 시작

#### 4️⃣ 위치 공유 시작
1. 그룹 생성 후 자동으로 지도 화면으로 이동
2. 위치 공유가 자동으로 시작됩니다
3. 지도에서 그룹 멤버들의 위치를 확인할 수 있습니다
4. 각 멤버의 프로필 이미지가 마커로 표시됩니다

#### 5️⃣ 위치 공유 설정
1. 하단중앙바에서 "내 그룹" 메뉴 선택
2. 그룹 목록에서 원하는 그룹 선택
3. 위치 공유 설정 화면에서 각 멤버별로 위치 공유 허용/거부 설정
4. "저장" 버튼 클릭하여 설정 저장

#### 6️⃣ 그룹 채팅
1. 그룹 화면에서 채팅 아이콘 클릭
2. 그룹 멤버들과 실시간으로 메시지 교환
3. 메시지는 Firebase Firestore에 저장됩니다

#### 7️⃣ 날씨 정보 확인
1. 지도 화면 우측 상단의 날씨 위젯 클릭
2. Bottom Sheet에서 상세 날씨 정보 확인:
   - 현재 날씨 및 온도
   - 시간별 예보 (24시간)
   - 일별 예보 (일주일)

#### 8️⃣ 장소 검색
1. 지도 화면 상단의 검색바에 장소명 입력
2. 검색 아이콘 클릭 또는 엔터키 누르기
3. 검색 결과에서 원하는 장소 클릭
4. 자동으로 해당 위치로 지도 이동
5. 장소 상세 정보 Bottom Sheet 표시

---

## 🔧 주요 기능 상세

### 실시간 위치 공유 시스템

#### 위치 업데이트 프로세스
1. **위치 획득**: Google Play Services Location을 통해 GPS 위치 획득
2. **유효성 검사**: 그룹 유효성 및 마커 표시 상태 확인
3. **Firebase 전송**: 유효한 위치 정보를 Firebase Realtime Database에 전송
4. **동기화**: 다른 멤버들의 위치 정보를 실시간으로 수신
5. **마커 업데이트**: 지도에 멤버 위치 마커 표시

#### 위치 공유 규칙 시스템
- **규칙 저장**: 서버에 위치 공유 규칙 저장
- **동적 적용**: 설정 변경 시 즉시 반영
- **프라이버시 보호**: 거부된 사용자에게는 위치 정보 전송 안 함

### 친구 관리 시스템

#### 친구 요청 프로세스
1. **요청 전송**: 사용자명으로 친구 요청 전송
2. **요청 수신**: 상대방이 요청 수신
3. **수락/거절**: 상대방이 요청 수락 또는 거절
4. **친구 관계 생성**: 수락 시 양방향 친구 관계 생성

#### 온라인 상태 시스템
- **Firebase Presence**: Firebase의 Presence 기능 활용
- **실시간 감지**: 사용자 접속/해제 시 실시간 감지
- **상태 표시**: 친구 목록에서 온라인/오프라인 상태 표시

### 그룹 채팅 시스템

#### 메시지 전송 프로세스
1. **메시지 입력**: 채팅 입력창에 메시지 입력
2. **Firestore 저장**: Firebase Firestore에 메시지 저장
3. **실시간 동기화**: 다른 멤버들에게 실시간으로 메시지 전달
4. **UI 업데이트**: RecyclerView에 새 메시지 추가

---

## 🐛 문제 해결

### 자주 발생하는 문제들

#### 1. 위치 권한 오류
**증상**: 위치 정보를 가져올 수 없음

**해결방법**:
- 앱 설정 → 권한 → 위치 권한 허용
- 에뮬레이터: Extended Controls → Location에서 위치 설정
- Android 10 이상: 백그라운드 위치 권한도 허용 필요

#### 2. Firebase 연결 오류
**증상**: 위치 공유가 작동하지 않음

**해결방법**:
- `google-services.json` 파일이 올바른 위치에 있는지 확인
- Firebase Console에서 Realtime Database가 활성화되어 있는지 확인
- Firebase 보안 규칙 확인
- 인터넷 연결 확인

#### 3. 네이버 지도가 표시되지 않음
**증상**: 지도가 비어있거나 오류 발생

**해결방법**:
- `AndroidManifest.xml`에서 네이버 지도 API 키 확인
- 네이버 클라우드 플랫폼에서 API 키 유효성 확인
- API 키의 패키지명이 `com.example.gps`와 일치하는지 확인

#### 4. 그룹 생성 실패
**증상**: 그룹 생성 시 오류 발생

**해결방법**:
- Spring Boot 서버가 실행 중인지 확인 (`http://localhost:8080`)
- MySQL 데이터베이스가 실행 중인지 확인
- `application.properties`의 데이터베이스 연결 설정 확인
- 네트워크 연결 확인
- 필수 정보(그룹명, 목적지, 멤버)가 모두 입력되었는지 확인
- 서버 로그 확인 (콘솔 또는 로그 파일)

#### 5. 친구 요청이 작동하지 않음
**증상**: 친구 요청 전송/수락 실패

**해결방법**:
- Spring Boot 서버가 실행 중인지 확인
- JWT 토큰이 유효한지 확인 (로그인 상태 확인)
- 사용자명이 정확한지 확인
- 이미 친구인 사용자에게는 요청 불가
- MySQL 데이터베이스 연결 확인
- 서버 로그 확인

#### 6. 빌드 오류
**해결방법**:
```bash
./gradlew clean
```
Android Studio: File → Invalidate Caches and Restart

#### 7. 프로필 이미지가 표시되지 않음
**증상**: 프로필 이미지가 로드되지 않음

**해결방법**:
- 인터넷 연결 확인
- 서버에서 이미지 URL이 올바르게 반환되는지 확인
- Glide 캐시 초기화

---

## 🔐 보안 및 프라이버시

### 보안 기능
- **JWT 인증**: Access Token + Refresh Token 기반 인증
- **Spring Security**: Spring Security를 통한 API 보안
- **비밀번호 암호화**: BCrypt를 사용한 비밀번호 해싱
- **위치 데이터 암호화**: Firebase Realtime Database의 보안 규칙 적용
- **개인화된 공유 설정**: 사용자가 직접 위치 공유 대상을 선택
- **그룹 유효성 검사**: 삭제된 그룹에 대한 자동 위치 공유 중단
- **CORS 설정**: 허용된 도메인에서만 API 접근 가능

### 프라이버시 보호
- **선택적 공유**: 각 멤버별로 위치 공유 허용/거부 설정 가능
- **위치 공유 제어**: 언제든지 위치 공유 시작/중지 가능
- **데이터 보관**: Firebase 보안 규칙을 통한 데이터 접근 제어

### 권장 사항
- **프로덕션 환경**: 
  - Firebase 보안 규칙을 엄격하게 설정
  - JWT Secret Key를 강력하게 설정 (32자 이상)
  - `application.properties`의 민감한 정보를 환경 변수로 관리
- **API 키 보호**: API 키를 코드에 하드코딩하지 않고 환경 변수 사용
- **HTTPS 사용**: 모든 통신은 HTTPS를 통해 암호화
- **데이터베이스 보안**: MySQL 사용자 권한을 최소화하여 설정
- **파일 업로드**: 업로드된 파일의 크기 및 타입 검증

---

## 📊 API 엔드포인트

### 그룹 API
```
POST   /api/groups                         # 그룹 생성
GET    /api/groups                         # 내 그룹 목록 조회
GET    /api/groups/{groupId}/all-members   # 그룹 멤버 조회
POST   /api/groups/{groupId}/location      # 위치 업데이트
GET    /api/groups/{groupId}/locations     # 그룹 멤버 위치 조회
POST   /api/groups/{groupId}/sharing-rule  # 위치 공유 규칙 변경
GET    /api/groups/{groupId}/sharing-rules # 위치 공유 규칙 조회
```

### 친구 API
```
GET    /api/friends                      # 친구 목록 조회
GET    /api/friends/group-members        # 그룹 멤버 선택 가능한 친구 목록
POST   /api/friends/request              # 친구 요청 전송
PUT    /api/friends/accept                # 친구 요청 수락
GET    /api/friends/pending              # 받은 친구 요청 목록
GET    /api/friends/sent                 # 보낸 친구 요청 목록
DELETE /api/friends/cancel               # 친구 요청 취소
DELETE /api/friends/decline              # 친구 요청 거절
DELETE /api/friends/{friendId}            # 친구 삭제
```

### 사용자 API
```
POST   /api/users/signup                    # 회원가입
POST   /login                                # 로그인
GET    /api/users/{userId}                   # 사용자 정보 조회
GET    /api/users/id                         # 사용자명으로 ID 조회
GET    /api/users/username/{username}        # 사용자명으로 사용자 조회
GET    /api/users/me/email                   # 내 이메일 조회
GET    /api/users/profile-image             # 프로필 이미지 URL 조회
POST   /api/users/profile-image             # 프로필 이미지 업로드 (Multipart)
DELETE /api/users/profile-image/default      # 기본 프로필 이미지 설정
POST   /api/users/logout                     # 로그아웃
POST   /api/users/find-username              # 아이디 찾기 (이메일)
POST   /api/users/reset-password             # 비밀번호 재설정 요청
POST   /api/users/reset-password/confirm     # 비밀번호 재설정 확인
```

---

## 👥 팀 정보

- **팀명**: WICHIN 팀 (위치를 공유하는 친구들)
- **팀장**: 이채운
- **팀원**: 박희재, 옥진서, 이강호

---


## 🎯 향후 계획

- [ ] 그룹 초대 링크 기능
- [ ] 위치 히스토리 저장 및 재생
- [ ] 푸시 알림 기능
- [ ] 위치 기반 알림 (목적지 도착 알림 등)
- [ ] 오프라인 모드 지원
- [ ] 그룹 통계 및 분석 기능
- [ ] 소셜 로그인 연동 (Google, Kakao)
- [ ] iOS 버전 개발
- [ ] AI 기반 경로 추천
- [ ] 음성 안내 기능

---

## 📞 문의

프로젝트 관련 문의사항이 있으시면 이슈를 생성해주세요.

- **문의사항**: chaewoonlove2@naver.com
- **Android 앱 저장소**: 현재 저장소
- **백엔드 서버 저장소**: [GPS_SpringBoot-MySQL](https://github.com/johnock/GPS_SpringBoot-MySQL-.git)

---

<div align="center">

**📍 WICHIN - 위치를 공유하는 친구들**

Made with ❤️ by WICHIN Team

[⬆ 맨 위로 이동](#-wichin---그룹-실시간-위치-공유-앱)

</div>
