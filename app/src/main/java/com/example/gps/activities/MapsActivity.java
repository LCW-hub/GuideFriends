package com.example.gps.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.activities.Friend.FriendsActivity;
import com.example.gps.adapters.SearchResultAdapter;
// import com.example.gps.api.ApiClient; // Retrofit API Client는 위치 공유에서 사용하지 않으므로 주석 처리
import com.example.gps.dto.LocationResponse;
// import com.example.gps.dto.UpdateLocationRequest; // Retrofit DTO는 사용하지 않으므로 주석 처리
import com.example.gps.fragments.SearchResultDetailFragment;
import com.example.gps.fragments.WeatherBottomSheetFragment;
import com.example.gps.model.SearchResult;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

// ⭐ Firebase Realtime Database 임포트 추가
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Retrofit 관련 임포트는 다른 기능 때문에 남겨둡니다.
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ⭐ [수정] 인터페이스 구현: 장소 선택 이벤트를 받기 위해 인터페이스를 추가
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SearchResultDetailFragment.OnDestinationSelectedListener {

    // 기본 UI 및 지도 관련 변수
    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    // 검색 관련 UI 및 데이터 변수
    private EditText etSearch;
    private ImageView ivSearchIcon;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter searchResultAdapter;
    private Marker searchResultMarker = null;

    // 날씨 관련 UI 변수
    private ImageView ivWeatherIcon;
    private TextView tvTemperature;

    // 백그라운드 작업을 위한 ExecutorService 및 Handler (공용으로 사용)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper()); // 메인 스레드 핸들러

    // 상수
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String OPENWEATHERMAP_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";
    private static final String NAVER_CLIENT_ID = "OAQnuwhbAL34Of8mlxve";
    private static final String NAVER_CLIENT_SECRET = "4roXQDJBpc";

    // 로그인된 사용자 이름을 저장할 변수 (MySQL 회원가입 정보 활용)
    private String loggedInUsername;

    // 실시간 공유를 위한 변수들
    private Long currentGroupId = -1L;
    private final Handler locationUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable locationUpdateRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10초
    private final HashMap<Long, Marker> memberMarkers = new HashMap<>();

    private Marker myLocationMarker = null;

    // ⭐ [추가] Firebase Realtime Database 관련 변수
    private DatabaseReference firebaseDatabase;
    private ValueEventListener memberLocationListener;

    // ⭐ [추가] 모의 위치 이동 관련 변수 (핵심 추가 부분)
    private Handler animationHandler;
    private Runnable animationRunnable;
    // 이동 경로: 서울 시청 (37.5665, 126.9780)에서 부산역 (35.115, 129.04)까지
    private LatLng startLatLng = new LatLng(37.5665, 126.9780);
    private LatLng endLatLng = new LatLng(35.115, 129.04);
    private final long totalDuration = 10000; // 총 이동 시간 (10초)
    private final int updateInterval = 50; // 위치 업데이트 간격 (50ms)
    private long startTime;


    //==============================================================================================
    // 1. 액티비티 생명주기 및 기본 설정
    //==============================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        checkLocationPermission();

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        // 날씨 UI 요소 초기화
        ivWeatherIcon = findViewById(R.id.iv_weather_icon);
        tvTemperature = findViewById(R.id.tv_temperature);

        // 로그인 사용자 이름 가져오기
        loggedInUsername = getIntent().getStringExtra("username");

        // ⭐ [수정] Firebase Database 초기화
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("group_locations"); // 'group_locations' 경로 지정

        initializeMap();
        initializeButtons();
        initializeSearch();
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        // 1. 초기 설정: 상태 변수에 지도 객체 저장
        this.naverMap = map;

        // 2. 위치 및 트래킹 설정: 위치 소스 연결 및 추적 모드 활성화
        naverMap.setLocationSource(locationSource);
        // ⭐ 수정: Follow 대신 NoFollow를 사용하여 카메라 자동 이동 방지 (필요 시 Follow 유지)
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);

        // 3. 카메라 이동: 초기 위치(서울 시청)로 카메라 이동
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(37.5665, 126.9780), 11));

        // 4. 마커 객체 초기화
        if (myLocationMarker == null) {
            myLocationMarker = new Marker();
            myLocationMarker.setCaptionText("내 위치");
            myLocationMarker.setPosition(new LatLng(37.5665, 126.9780)); // 초기 위치 설정
            myLocationMarker.setMap(naverMap); // 지도에 표시
        }

        // 5. 위치 변경 리스너 등록 (마커 및 날씨 업데이트 담당)
        naverMap.addOnLocationChangeListener(location -> {
            // null 체크 및 유효성 검사 (NaN, Infinity 방지)
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                if (Double.isFinite(latitude) && Double.isFinite(longitude)) {

                    LatLng currentLocation = new LatLng(latitude, longitude);

                    // A. 내 마커 위치 업데이트 (애니메이션 중이 아닐 때만 FusedLocationSource의 위치를 따름)
                    if (animationHandler == null) {
                        myLocationMarker.setPosition(currentLocation);
                    }

                    // B. ⭐ [개선] 현재 위치 기반으로 날씨 위젯 업데이트 추가
                    updateWeatherWidget(currentLocation);
                }
            }
        });

        // 6. 부가 기능 로드
        loadWeatherData();
    }

    // ⭐ [추가] 마커를 일정 시간 동안 부드럽게 움직이는 함수 (핵심 추가 함수)
    private void startMockMovement() {
        // 이미 애니메이션 중이면 중복 실행 방지 및 기존 핸들러 제거
        if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationHandler = null; // 핸들러를 null로 만들어 다음 애니메이션 시작 가능하게 함
        }

        // Handler와 Runnable 초기화
        animationHandler = new Handler(Looper.getMainLooper());
        startTime = System.currentTimeMillis();

        // 현재 마커 위치를 시작점으로 설정하고, 목표 위치를 멀리 설정합니다.
        startLatLng = myLocationMarker.getPosition();
        endLatLng = new LatLng(35.115, 129.04); // 부산역 좌표

        Toast.makeText(this, "모의 위치 이동 시작: " + totalDuration / 1000 + "초 동안 부산으로 이동", Toast.LENGTH_LONG).show();

        animationRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float fraction = (float) elapsed / totalDuration; // 진행률 (0.0 ~ 1.0)

                if (fraction < 1.0) {
                    // 1. 진행률에 따른 현재 위치 계산 (선형 보간)
                    double lat = startLatLng.latitude + (endLatLng.latitude - startLatLng.latitude) * fraction;
                    double lon = startLatLng.longitude + (endLatLng.longitude - startLatLng.longitude) * fraction;
                    LatLng currentLatLng = new LatLng(lat, lon);

                    // 2. 내 마커 위치 직접 업데이트 (부드러운 애니메이션 효과)
                    if (myLocationMarker != null && naverMap != null) {
                        myLocationMarker.setPosition(currentLatLng);

                        // 3. (선택) 카메라를 함께 부드럽게 이동
                        // 애니메이션이 부드럽게 보이도록 부드러운 카메라 업데이트 사용
                        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLatLng).animate(CameraAnimation.Linear, updateInterval);
                        naverMap.moveCamera(cameraUpdate);

                        // 4. (선택) Firebase에 모의 위치 공유
                        Location mockLocation = new Location("MockProvider");
                        mockLocation.setLatitude(currentLatLng.latitude);
                        mockLocation.setLongitude(currentLatLng.longitude);
                        mockLocation.setTime(System.currentTimeMillis());
                        mockLocation.setElapsedRealtimeNanos(System.nanoTime());
                        updateMyLocation(mockLocation);
                    }

                    // 다음 업데이트 예약
                    animationHandler.postDelayed(this, updateInterval);
                } else {
                    // 애니메이션 종료
                    if (myLocationMarker != null) {
                        myLocationMarker.setPosition(endLatLng);
                    }
                    Log.d("MapsActivity", "위치 이동 애니메이션 종료");
                    Toast.makeText(MapsActivity.this, "도착지에 도착했습니다: 부산", Toast.LENGTH_SHORT).show();

                    // 핸들러 및 Runnable 객체 제거
                    animationHandler.removeCallbacks(this);
                    animationHandler = null;
                }
            }
        };

        animationHandler.post(animationRunnable); // 애니메이션 시작
    }
    // ----------------------------------------------------------------------


    // ⭐ [추가] SearchResultDetailFragment에서 장소 선택 시 호출되는 콜백 메서드 구현
    @Override
    public void onDestinationSelected(SearchResult selectedResult) {
        // 1. 토스트 메시지로 확인
        Toast.makeText(this, selectedResult.getTitle() + "을 모임 장소로 선택했습니다.", Toast.LENGTH_LONG).show();

        // 2. 검색 결과 화면/마커 숨기기 (선택 사항)
        hideSearchResults();
        if (searchResultMarker != null) searchResultMarker.setMap(null);

        // 3. CreateGroupActivity로 Intent를 통해 데이터 전달 (모임 장소 설정)
        Intent intent = new Intent(MapsActivity.this, CreateGroupActivity.class);

        // SearchResult 객체를 Intent에 담아 전달합니다. (SearchResult 클래스가 Parcelable 또는 Serializable을 구현해야 함)
        intent.putExtra("destination_result", selectedResult);

        // 참고: 이미 로그인 정보를 가지고 있다면 여기서 다시 전달할 수 있습니다.
        intent.putExtra("username", loggedInUsername);

        startActivity(intent);
    }
    // ----------------------------------------------------------------------


    private void initializeMap() {
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        mapView.getMapAsync(this);
    }

    private void initializeButtons() {
        FloatingActionButton btnMapType = findViewById(R.id.btnMapType);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        androidx.cardview.widget.CardView weatherWidget = findViewById(R.id.weather_widget);
        ImageButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);

        // ⭐ [추가] 테스트 버튼 (FloatingActionButton의 ID가 'btnTestMovement'라고 가정)
        // 실제 레이아웃(activity_maps.xml)에 이 ID로 버튼이 추가되어 있어야 합니다.
        FloatingActionButton btnTestMovement = findViewById(R.id.btnTestMovement);


        btnMapType.setOnClickListener(this::showMapTypeMenu);
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        weatherWidget.setOnClickListener(v -> showWeatherBottomSheet());

        btnFriends.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, FriendsActivity.class);
            intent.putExtra("username", loggedInUsername);
            startActivity(intent);
        });

        // ⭐ [수정] 테스트 버튼 클릭 리스너 추가
        if (btnTestMovement != null) {
            btnTestMovement.setOnClickListener(v -> startMockMovement());
        }

        btnCreateGroup.setOnClickListener(v -> {
            // ⭐ [수정] 그룹 생성 버튼 클릭 시 장소 선택 플로우 없이 바로 이동 (기존 로직 유지)
            Intent intent = new Intent(MapsActivity.this, CreateGroupActivity.class);
            startActivity(intent);
        });

        btnMyGroups.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, MyGroupsActivity.class);
            startActivity(intent);
        });
    }

    //==============================================================================================
    // 2. 실시간 위치 공유 관련 (그룹 기능)
    //==============================================================================================

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.hasExtra("groupId")) {
            currentGroupId = intent.getLongExtra("groupId", -1L);
            if (currentGroupId != -1L) {
                Toast.makeText(this, "그룹 ID: " + currentGroupId + " 위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show();
                startLocationSharing();
            }
        }
    }

    private void startLocationSharing() {
        locationUpdateHandler.removeCallbacksAndMessages(null); // 기존 핸들러 중지

        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // 애니메이션이 실행 중이 아닐 때만 실제 위치를 사용하여 Firebase를 업데이트합니다.
                if (locationSource != null && animationHandler == null) {
                    Location lastKnownLocation = locationSource.getLastLocation();
                    if (lastKnownLocation != null) {
                        // ⭐ [수정] 내 위치 업데이트를 Firebase로 실행
                        updateMyLocation(lastKnownLocation);
                    } else {
                        Log.w("MapsActivity", "현재 위치를 가져올 수 없어 내 위치를 업데이트하지 못했습니다.");
                    }
                }
                // ⭐ [수정] 멤버 위치 가져오기 Retrofit 호출은 Firebase 리스너로 대체
                // fetchGroupMembersLocation();
                locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        };

        locationUpdateHandler.post(locationUpdateRunnable); // 즉시 시작 (내 위치 업데이트)

        // ⭐ [추가] Firebase 실시간 위치 수신 시작
        startFirebaseLocationListener();
    }

    // ⭐ [수정] Retrofit 대신 Firebase를 사용하도록 로직 변경
    private void updateMyLocation(Location location) {
        if (currentGroupId == -1L || location == null || loggedInUsername == null) return;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        if (Double.isFinite(latitude) && Double.isFinite(longitude)) {

            // 1. 위치 데이터를 HashMap 형태로 구성
            HashMap<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("timestamp", System.currentTimeMillis());

            // 2. Firebase 경로 설정: /group_locations/{groupId}/{username}
            DatabaseReference groupRef = firebaseDatabase.child(String.valueOf(currentGroupId));

            // 3. Firebase에 위치 데이터 업데이트
            groupRef.child(loggedInUsername).setValue(locationData)
                    .addOnSuccessListener(aVoid -> {
                        // Log.d("MapsActivity", "Firebase 내 위치 업데이트 성공"); // 애니메이션 시 로그가 너무 많이 찍혀 주석 처리
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MapsActivity", "Firebase 내 위치 업데이트 실패", e);
                    });

            // ⚠️ 기존 Retrofit 코드는 제거됨

        } else {
            // 유효하지 않은 좌표는 서버로 보내지 않음
            Log.w("MapsActivity", "Invalid coordinates, skipping location update to server.");
        }
    }

    // ⭐ [추가] Firebase 실시간 위치 리스너 시작
    private void startFirebaseLocationListener() {
        if (currentGroupId == -1L || naverMap == null) return;

        // 이전 리스너가 있다면 제거하여 중복 등록을 방지합니다. (중요)
        if (memberLocationListener != null) {
            firebaseDatabase.child(String.valueOf(currentGroupId)).removeEventListener(memberLocationListener);
        }

        DatabaseReference groupPathRef = firebaseDatabase.child(String.valueOf(currentGroupId));

        memberLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LocationResponse> locations = new ArrayList<>();

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String username = memberSnapshot.getKey();

                    // 내 위치는 이미 myLocationMarker로 처리되므로 건너뜁니다.
                    if (username != null && username.equals(loggedInUsername)) continue;

                    // Firebase에서 위치 데이터 추출
                    Double lat = memberSnapshot.child("latitude").getValue(Double.class);
                    Double lon = memberSnapshot.child("longitude").getValue(Double.class);

                    if (lat != null && lon != null) {
                        LocationResponse lr = new LocationResponse();
                        lr.setUserName(username);
                        lr.setLatitude(lat);
                        lr.setLongitude(lon);

                        // userId는 MySQL 기반이므로 임시로 hashCode 사용
                        // 실제 운영 시에는 MySQL 정보와 연동하여 실제 userId를 사용해야 합니다.
                        lr.setUserId((long) (username != null ? username.hashCode() : 0));
                        locations.add(lr);
                    }
                }

                // 지도 마커 업데이트는 기존 로직 재사용
                updateMemberMarkers(locations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MapsActivity", "Firebase 위치 리스너 오류: " + error.getMessage());
            }
        };

        // 리스너를 그룹 경로에 연결하여 실시간 업데이트를 받습니다.
        groupPathRef.addValueEventListener(memberLocationListener);
    }


    private void updateMemberMarkers(List<LocationResponse> locations) {
        if (naverMap == null) return;

        List<Long> updatedUserIds = new ArrayList<>();
        for (LocationResponse location : locations) {
            updatedUserIds.add(location.getUserId());

            // ⚠️ [안정성] 멤버 위치 좌표 유효성 검사 추가 (서버 문제 대비)
            if (!Double.isFinite(location.getLatitude()) || !Double.isFinite(location.getLongitude())) {
                Log.w("MapsActivity", "수신된 멤버 위치가 유효하지 않습니다: " + location.getUserName());
                continue;
            }

            LatLng memberPosition = new LatLng(location.getLatitude(), location.getLongitude());

            Marker marker = memberMarkers.get(location.getUserId());
            if (marker == null) {
                marker = new Marker();
                marker.setCaptionText(location.getUserName());
                memberMarkers.put(location.getUserId(), marker);
            }
            marker.setPosition(memberPosition);
            marker.setMap(naverMap);
        }

        List<Long> usersToRemove = new ArrayList<>();
        for (Long existingUserId : memberMarkers.keySet()) {
            if (!updatedUserIds.contains(existingUserId)) {
                usersToRemove.add(existingUserId);
            }
        }
        for (Long userId : usersToRemove) {
            Marker markerToRemove = memberMarkers.get(userId);
            if (markerToRemove != null) {
                markerToRemove.setMap(null); // 지도에서 제거
            }
            memberMarkers.remove(userId); // 맵에서 제거
        }
    }

    //==============================================================================================
    // 3. 검색 기능 관련
    //==============================================================================================

    private void initializeSearch() {
        etSearch = findViewById(R.id.et_search);
        ivSearchIcon = findViewById(R.id.iv_search_icon);
        rvSearchResults = findViewById(R.id.rv_search_results);

        searchResultAdapter = new SearchResultAdapter();
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchResultAdapter);

        ivSearchIcon.setOnClickListener(v -> performSearch());
        searchResultAdapter.setOnItemClickListener(searchResult -> {
            moveToSearchResult(searchResult);
            hideSearchResults();
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        searchPlacesWithNaverAPI(query);
    }

    private void searchPlacesWithNaverAPI(String query) {
        executor.execute(() -> {
            try {
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                String urlString = "https://openapi.naver.com/v1/search/local.json?query=" + encodedQuery + "&display=10&start=1";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-Naver-Client-Id", NAVER_CLIENT_ID);
                conn.setRequestProperty("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    List<SearchResult> results = parseNaverSearchResults(new JSONObject(response.toString()));
                    handler.post(() -> {
                        if (results.isEmpty()) {
                            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            showSearchResults(results);
                            results.forEach(this::fetchImageForSearchResult);
                        }
                    });
                } else {
                    Log.e("SearchAPI", "API 응답 코드: " + conn.getResponseCode());
                    handler.post(() -> Toast.makeText(this, "API 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("SearchAPI", "장소 검색 실패", e);
                handler.post(() -> Toast.makeText(this, "검색 중 오류 발생", Toast.LENGTH_LONG).show());
            }
        });
    }

    private List<SearchResult> parseNaverSearchResults(JSONObject json) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        JSONArray items = json.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String title = item.getString("title").replaceAll("<[^>]*>", "");
            String address = item.optString("roadAddress", item.optString("address", ""));
            String category = item.optString("category", "정보 없음");

            long mapx = item.getLong("mapx");
            long mapy = item.getLong("mapy");

            // ⭐ [개선] KATEC 좌표계 변환 처리 (임시 로직)
            // 주의: 이 로직은 정확한 KATEC -> WGS84 변환이 아니며,
            // NaverMap SDK의 'Coord' 클래스를 사용하거나 외부 라이브러리로 대체해야 합니다.
            LatLng convertedLatLng = convertKatecToWGS84_Approximate(mapx, mapy);

            results.add(new SearchResult(title, address, category, convertedLatLng.latitude, convertedLatLng.longitude, "", ""));
        }
        return results;
    }

    // ⭐ [추가] 임시 KATEC -> WGS84 변환 함수 (정확한 변환 코드로 대체 필요)
    private LatLng convertKatecToWGS84_Approximate(long mapx, long mapy) {
        // 실제 WGS84 변환 로직이 없으므로, 현재는 임시로 서울 근처 좌표를 반환하는 것으로 가정합니다.
        // 테스트 시 검색 결과 마커가 서울 근처에 찍히는 것을 볼 수 있습니다.
        return new LatLng(37.5665, 126.9780);
    }

    private void fetchImageForSearchResult(SearchResult result) {
        executor.execute(() -> {
            try {
                String encodedQuery = java.net.URLEncoder.encode(result.getTitle(), "UTF-8");
                String urlString = "https://openapi.naver.com/v1/search/image?query=" + encodedQuery + "&display=1&start=1&sort=sim";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-Naver-Client-Id", NAVER_CLIENT_ID);
                conn.setRequestProperty("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    if (json.has("items") && json.getJSONArray("items").length() > 0) {
                        String imageUrl = json.getJSONArray("items").getJSONObject(0).optString("thumbnail", "");
                        result.setImageUrl(imageUrl);
                        handler.post(searchResultAdapter::notifyDataSetChanged);
                    }
                }
            } catch (Exception e) {
                Log.e("ImageSearchAPI", "이미지 검색 실패: " + result.getTitle(), e);
            }
        });
    }

    private void showSearchResults(List<SearchResult> results) {
        searchResultAdapter.updateResults(results);
        rvSearchResults.setVisibility(View.VISIBLE);
    }

    private void hideSearchResults() {
        rvSearchResults.setVisibility(View.GONE);
        etSearch.clearFocus();
    }

    private void moveToSearchResult(SearchResult result) {
        if (naverMap != null) {
            LatLng location = new LatLng(result.getLatitude(), result.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(location, 16).animate(CameraAnimation.Easing, 1000);
            naverMap.moveCamera(cameraUpdate);

            if (searchResultMarker != null) searchResultMarker.setMap(null);

            searchResultMarker = new Marker();
            searchResultMarker.setPosition(location);
            searchResultMarker.setCaptionText(result.getTitle());
            searchResultMarker.setMap(naverMap);

            // ⭐ [수정] SearchResultDetailFragment를 띄울 때, MapsActivity가 리스너 역할을 합니다.
            SearchResultDetailFragment.newInstance(result).show(getSupportFragmentManager(), "SearchResultDetailFragment");
        }
    }

    //==============================================================================================
    // 4. 날씨 기능 관련
    //==============================================================================================

    private void loadWeatherData() {
        // 초기 로드 시에는 기본 위치(서울)의 날씨를 가져옴
        LatLng defaultLocation = new LatLng(37.5665, 126.9780);
        updateWeatherWidget(defaultLocation);
    }

    private void updateWeatherWidget(LatLng location) {
        executor.execute(() -> {
            try {
                String urlString = String.format(
                        "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric&lang=kr",
                        location.latitude, location.longitude, OPENWEATHERMAP_API_KEY
                );
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                double temperature = json.getJSONObject("main").getDouble("temp");
                String weatherMain = json.getJSONArray("weather").getJSONObject(0).getString("main");

                handler.post(() -> {
                    tvTemperature.setText(String.format("%.0f°", temperature));
                    ivWeatherIcon.setImageResource(getWeatherIconResource(weatherMain));
                });
            } catch (Exception e) {
                Log.e("WeatherAPI", "날씨 정보 로드 실패", e);
                handler.post(() -> Toast.makeText(MapsActivity.this, "날씨 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private int getWeatherIconResource(String weatherMain) {
        switch (weatherMain.toLowerCase()) {
            case "clear": return R.drawable.ic_weather_clear;
            case "clouds": return R.drawable.ic_weather_cloudy;
            case "rain": case "drizzle": case "thunderstorm": return R.drawable.ic_weather_rainy;
            case "snow": return R.drawable.ic_weather_snow;
            case "mist": case "fog": return R.drawable.ic_weather_fog;
            default: return R.drawable.ic_weather_clear;
        }
    }

    private void showWeatherBottomSheet() {
        Location currentLocation = locationSource.getLastLocation();
        double latitude, longitude;
        if (currentLocation != null) {
            latitude = currentLocation.getLatitude();
            longitude = currentLocation.getLongitude();
        } else {
            latitude = 37.5665; // 서울 시청
            longitude = 126.9780;
            Toast.makeText(this, "현재 위치를 가져올 수 없어 기본 위치의 날씨를 표시합니다.", Toast.LENGTH_SHORT).show();
        }
        WeatherBottomSheetFragment.newInstance(latitude, longitude).show(getSupportFragmentManager(), "WeatherBottomSheet");
    }

    //==============================================================================================
    // 5. 지도 및 권한 관련 유틸리티
    //==============================================================================================

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (naverMap != null) {
                // LocationTrackingMode.Tracking 대신 사용 가능한 모드 사용
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }

    private void moveToCurrentLocation() {
        if (naverMap == null || locationSource.getLastLocation() == null) {
            Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Location location = locationSource.getLastLocation();
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(currentLocation, 16).animate(CameraAnimation.Easing, 1200);
        naverMap.moveCamera(cameraUpdate);
        Toast.makeText(this, "📍 내 위치로 이동합니다.", Toast.LENGTH_SHORT).show();
    }

    private void showMapTypeMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.map_type_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.map_type_normal) {
                naverMap.setMapType(NaverMap.MapType.Basic);
                return true;
            } else if (itemId == R.id.map_type_satellite) {
                naverMap.setMapType(NaverMap.MapType.Satellite);
                return true;
            } else if (itemId == R.id.map_type_terrain) {
                naverMap.setMapType(NaverMap.MapType.Terrain);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //==============================================================================================
    // 6. 액티비티 생명주기 콜백
    //==============================================================================================

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // 다른 화면에서 돌아왔을 때, 위치 공유 중이었다면 다시 시작
        if (currentGroupId != -1L) {
            startLocationSharing();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        // 앱이 백그라운드로 갈 때 위치 공유 핸들러 중지
        locationUpdateHandler.removeCallbacksAndMessages(null);

        // 애니메이션 핸들러가 실행 중이면 중지합니다.
        if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationHandler = null; // 핸들러 상태 초기화
        }

        // ⭐ [수정] Firebase 리스너 제거
        if (currentGroupId != -1L && memberLocationListener != null) {
            firebaseDatabase.child(String.valueOf(currentGroupId)).removeEventListener(memberLocationListener);
            Log.d("MapsActivity", "Firebase 위치 리스너 해제됨.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}