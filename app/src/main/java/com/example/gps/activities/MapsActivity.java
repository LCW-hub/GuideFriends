package com.example.gps.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.activities.Friend.FriendsActivity;
import com.example.gps.activities.Register_Login.LoginActivity;
import com.example.gps.adapters.SearchResultAdapter;
import com.example.gps.api.ApiClient; // Still needed for other API calls if any
import com.example.gps.dto.LocationResponse;
import com.example.gps.fragments.SearchResultDetailFragment;
import com.example.gps.fragments.WeatherBottomSheetFragment;
import com.example.gps.model.SearchResult;
import com.example.gps.utils.TokenManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.gps.api.GroupApiService;
import com.example.gps.api.UserApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SearchResultDetailFragment.OnDestinationSelectedListener {

    // --- UI & Map Variables ---
    // --- UI & Map Variables ---
    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private DrawerLayout drawerLayout;

    // --- Search UI & Data ---
    private EditText etSearch;
    private ImageView ivSearchIcon;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter searchResultAdapter;
    private Marker searchResultMarker = null;

    // --- Weather UI ---
    private ImageView ivWeatherIcon;
    private TextView tvTemperature;

    // --- Menu UI & State ---
    private boolean isSubMenuOpen = false;
    private static final float SUB_MENU_RADIUS_DP = 80f;

    // --- Background Tasks ---
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // --- Constants ---
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String OPENWEATHERMAP_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";
    private static final String NAVER_CLIENT_ID = "OAQnuwhbAL34Of8mlxve";
    private static final String NAVER_CLIENT_SECRET = "4roXQDJBpc";
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final String TAG = "MapsActivity_FIREBASE"; // 🎯 Firebase 로그 TAG 추가

    // --- User & State ---
    private String loggedInUsername;
    private boolean isSelectionMode = false;

    // --- Firebase & Real-time Location Sharing ---
    private Long currentGroupId = -1L;
    private DatabaseReference firebaseDatabase;
    private ValueEventListener memberLocationListener;
    private final Handler locationUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable locationUpdateRunnable;
    private final HashMap<String, Marker> memberMarkers = new HashMap<>();

    private final Handler mapRefreshHandler = new Handler(Looper.getMainLooper());
    private Runnable mapRefreshRunnable;
    private static final int MAP_REFRESH_INTERVAL = 2000; // 2초마다 강제 갱신 (원하는 간격으로 설정 가능)

    // ⭐️ [Firebase 규칙] 내가 상대방 위치를 볼 수 있는지 (Sharer -> Me)
    private final Map<Long, Boolean> incomingSharingRules = new HashMap<>();
    // ⭐️ [Firebase 규칙] 상대방이 내 위치를 볼 수 있는지 (Me -> Target)
    private final Map<Long, Boolean> outgoingSharingStatus = new HashMap<>();
    private Marker myLocationMarker = null;

    // ⭐️ [Firebase 규칙 리스너]
    private DatabaseReference rulesRef;
    private ValueEventListener rulesListener;

    // --- Mock Movement (for testing) ---
    private Handler animationHandler;
    private Runnable animationRunnable;
    private LatLng startLatLng = new LatLng(37.5665, 126.9780); // Seoul City Hall
    private LatLng endLatLng = new LatLng(35.115, 129.04); // Busan Station
    private final long totalDuration = 10000; // 10 seconds
    private final int updateInterval = 50; // 50ms
    private long startTime;

    private Long loggedInUserId = -1L;
    private final Map<String, LocationResponse> memberLocationsCache = new HashMap<>(); // ⭐️ 위치 데이터 캐시

    // ⭐️ [추가] 내 마커 상태 리스너를 위한 필드
    private DatabaseReference myMarkerStatusRef;
    private ValueEventListener myMarkerStatusListener;

    // 🚀 --- [2.1: 목적지용 변수 3개 추가] ---
    private Marker destinationMarker = null; // 목적지 마커 객체
    private DatabaseReference destinationRef; // 목적지 데이터베이스 참조
    private ValueEventListener destinationListener; // 목적지 리스너
    // 🚀 --- [2.1 끝] ---

    //==============================================================================================
    // 1. Activity Lifecycle & Setup
    //==============================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        checkLocationPermission();
        handleIntent(getIntent());

        // --- Initialize Components ---
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        ivWeatherIcon = findViewById(R.id.iv_weather_icon);
        tvTemperature = findViewById(R.id.tv_temperature);
        drawerLayout = findViewById(R.id.drawer_layout);

        loggedInUsername = getIntent().getStringExtra("username");

        Log.d(TAG, "onCreate: FirebaseDatabase 인스턴스 획득 시도");
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("group_locations");
        Log.d(TAG, "onCreate: 사용자명 확인 (loggedInUsername)=" + loggedInUsername);

        initializeMap();
        initializeButtons();
        initializeSearch();
        initializeSubMenu();
        bindMyPageHeader();

        if (loggedInUsername != null) {
            fetchLoggedInUserId();
        }
    }

    private void startMyLocationMarkerListener() {
        if (loggedInUserId == -1L || naverMap == null) return;

        // 경로: user_status/{userId}/is_marker_visible
        myMarkerStatusRef = FirebaseDatabase.getInstance()
                .getReference("user_status")
                .child(String.valueOf(loggedInUserId))
                .child("is_marker_visible");

        // 기존 리스너 제거 (중복 방지)
        if (myMarkerStatusListener != null) {
            myMarkerStatusRef.removeEventListener(myMarkerStatusListener);
        }

        myMarkerStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 값이 없을 경우 기본값(true)을 사용하여 마커가 보이도록 합니다.
                Boolean isVisible = snapshot.getValue(Boolean.class);
                boolean showMarker = (isVisible != null) ? isVisible : true;

                if (myLocationMarker != null) {
                    if (showMarker) {
                        myLocationMarker.setMap(naverMap); // 지도에 표시
                        Log.d(TAG, "My Marker Status: 켜짐 (Visible)");
                    } else {
                        myLocationMarker.setMap(null); // 지도에서 제거
                        Log.d(TAG, "My Marker Status: 꺼짐 (Hidden)");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "My Marker Status Listener Cancelled", error.toException());
            }
        };

        myMarkerStatusRef.addValueEventListener(myMarkerStatusListener);
        Log.d(TAG, "startMyLocationMarkerListener: 내 마커 상태 리스너 등록 완료.");
    }

    // ⭐️ [새로 추가] Firebase 규칙 리스너 (상호 허용 상태를 실시간으로 가져옴)
    private void startFirebaseRulesListener() {
        if (loggedInUserId == -1L) {
            Log.e(TAG, "startFirebaseRulesListener: 로드 중단. UserID가 유효하지 않습니다.");
            return;
        }

        // 2. 규칙 경로 설정: 'sharing_permissions' 노드 전체를 감시합니다.
        // 구조: sharing_permissions/{sharerId}/{targetId} : boolean
        rulesRef = FirebaseDatabase.getInstance()
                .getReference("sharing_permissions");

        // 3. 기존 리스너 제거 (중복 방지)
        if (rulesListener != null) {
            rulesRef.removeEventListener(rulesListener);
            Log.d(TAG, "startFirebaseRulesListener: 기존 규칙 리스너 제거 완료.");
        }

        // 4. 새로운 리스너 정의 및 등록
        rulesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 규칙 맵 초기화
                incomingSharingRules.clear();
                outgoingSharingStatus.clear();

                // Firebase의 모든 공유 규칙을 순회하며 Incoming/Outgoing으로 분리
                for (DataSnapshot sharerSnapshot : snapshot.getChildren()) {

                    // Outer Key: Sharer ID (위치를 공유하는 사람)
                    String sharerIdStr = sharerSnapshot.getKey();
                    if (sharerIdStr == null) continue;
                    // String 대신 Long을 사용해야 하므로 형변환
                    Long sharerId = Long.parseLong(sharerIdStr);

                    // Inner Key: Target ID
                    for (DataSnapshot targetSnapshot : sharerSnapshot.getChildren()) {
                        String targetIdStr = targetSnapshot.getKey();
                        Boolean isAllowed = targetSnapshot.getValue(Boolean.class);

                        if (targetIdStr == null || isAllowed == null) continue;
                        Long targetId = Long.parseLong(targetIdStr);

                        // ----------------------------------------------------
                        // A. Incoming Rules (수신 규칙): 상대방이 나에게 허용했는지? (Sharer -> Me)
                        if (targetId.equals(loggedInUserId)) {
                            incomingSharingRules.put(sharerId, isAllowed);
                        }

                        // B. Outgoing Status (송신 상태): 내가 상대방에게 허용했는지? (Me -> Target)
                        if (sharerId.equals(loggedInUserId)) {
                            outgoingSharingStatus.put(targetId, isAllowed);
                        }
                        // ----------------------------------------------------
                    }
                }

                Log.d(TAG, "✅ Firebase Rules Loaded. Incoming Count: " + incomingSharingRules.size() +
                        ", Outgoing Count: " + outgoingSharingStatus.size());

                // 5. 규칙이 갱신되었으므로 마커를 재적용하여 상호 허용 상태를 반영합니다.
                reapplyRulesAndRefreshMarkers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Rules Listener Cancelled", error.toException());
                Toast.makeText(MapsActivity.this, "위치 공유 규칙 로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        };
        // 리스너 등록
        rulesRef.addValueEventListener(rulesListener);
        Log.d(TAG, "startFirebaseRulesListener: Firebase 규칙 리스너 등록 완료.");
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        // ... (나머지 onMapReady 로직은 동일) ...
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        LatLng initialPosition = new LatLng(37.5665, 126.9780); // 서울 시청 좌표
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(initialPosition, 11));

        if (myLocationMarker == null) {
            myLocationMarker = new Marker();
            myLocationMarker.setCaptionText("내 위치");
        }
        myLocationMarker.setPosition(initialPosition);
        myLocationMarker.setMap(naverMap);

        // 🎯 로그 추가: 위치 변경 리스너 등록 확인
        Log.d(TAG, "onMapReady: NaverMap 위치 변경 리스너 등록 완료");

        naverMap.addOnLocationChangeListener(location -> {
            if (location != null && Double.isFinite(location.getLatitude()) && Double.isFinite(location.getLongitude())) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (animationHandler == null) {
                    myLocationMarker.setPosition(currentLocation);
                }
                updateWeatherWidget(currentLocation);
            }
        });

        applyMapTypeSetting();
        loadWeatherData();
    }

    private void startMapRefreshTimer() {
        if (naverMap == null) return;

        // 기존 리스너가 있다면 제거
        if (mapRefreshRunnable != null) {
            mapRefreshHandler.removeCallbacks(mapRefreshRunnable);
        }

        mapRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (naverMap != null && naverMap.getCameraPosition() != null) {
                    // 현재 카메라 위치로 다시 이동하여 뷰 갱신을 강제로 유도합니다.
                    CameraUpdate cameraUpdate = CameraUpdate.scrollTo(naverMap.getCameraPosition().target);
                    naverMap.moveCamera(cameraUpdate);
                    Log.d(TAG, "Map Refresh Timer: 지도 뷰 강제 갱신 실행.");
                }
                // 설정된 간격마다 반복 실행
                mapRefreshHandler.postDelayed(this, MAP_REFRESH_INTERVAL);
            }
        };

        mapRefreshHandler.post(mapRefreshRunnable);
        Log.d(TAG, "startMapRefreshTimer: 지도 강제 갱신 타이머 시작. 간격: " + MAP_REFRESH_INTERVAL + "ms");
    }

    private void stopMapRefreshTimer() {
        if (mapRefreshRunnable != null) {
            mapRefreshHandler.removeCallbacks(mapRefreshRunnable);
            Log.d(TAG, "stopMapRefreshTimer: 지도 강제 갱신 타이머 중단.");
        }
    }

    //==============================================================================================
    // 2. Initializers
    //==============================================================================================

    private void initializeMap() {
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        mapView.getMapAsync(this);
    }

    private void initializeButtons() {

        FloatingActionButton btnMapType = findViewById(R.id.btnMapType);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        FloatingActionButton btnTestMovement = findViewById(R.id.btnTestMovement);
        findViewById(R.id.weather_widget).setOnClickListener(v -> showWeatherBottomSheet());

        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        FloatingActionButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        FloatingActionButton btnMyPage = findViewById(R.id.btnMyPage);
        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);

        btnMapType.setOnClickListener(this::showMapTypeMenu);
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        if (btnTestMovement != null) {
            btnTestMovement.setOnClickListener(v -> startMockMovement());
        }

        btnMainMenu.setOnClickListener(v -> toggleSubMenu());

        // ⭐ [수정] FriendsActivity로 username 전달
        btnFriends.setOnClickListener(v -> {
            startActivity(new Intent(this, FriendsActivity.class).putExtra("username", loggedInUsername));
            hideSubMenu();
        });

        // ⭐ [수정] CreateGroupActivity로 username 전달
        btnCreateGroup.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateGroupActivity.class).putExtra("username", loggedInUsername));
            hideSubMenu();
        });

        // ⭐ [핵심 수정] MyGroupsActivity로 username 전달
        btnMyGroups.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyGroupsActivity.class);
            intent.putExtra("username", loggedInUsername);
            startActivity(intent);
            hideSubMenu();
        });

        btnMyPage.setOnClickListener(v -> {
            View sidebar = findViewById(R.id.sidebar);
            if (drawerLayout.isDrawerOpen(sidebar)) {
                drawerLayout.closeDrawer(sidebar);
            } else {
                drawerLayout.openDrawer(sidebar);
            }
            hideSubMenu();
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            hideSubMenu();
        });
    }

    private void initializeSearch() {
        // ... (검색 UI 초기화 로직은 동일)
        etSearch = findViewById(R.id.et_search);
        ivSearchIcon = findViewById(R.id.iv_search_icon);
        rvSearchResults = findViewById(R.id.rv_search_results);

        searchResultAdapter = new SearchResultAdapter();
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchResultAdapter);

        ivSearchIcon.setOnClickListener(v -> performSearch());

        searchResultAdapter.setOnItemClickListener(searchResult -> {
            if (isSelectionMode) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("PLACE_NAME", searchResult.getTitle());
                resultIntent.putExtra("PLACE_LAT", searchResult.getLatitude());
                resultIntent.putExtra("PLACE_LNG", searchResult.getLongitude());
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                moveToSearchResult(searchResult);
                hideSearchResults();
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    //==============================================================================================
    // 3. Real-time Location Sharing (Firebase - 로그 추가)
    //==============================================================================================

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        if (intent.hasExtra("username")) {
            loggedInUsername = intent.getStringExtra("username");
        }

        if ("SELECT_DESTINATION".equals(intent.getStringExtra("PURPOSE"))) {
            isSelectionMode = true;
            Toast.makeText(this, "목적지로 설정할 장소를 검색 후 선택해주세요.", Toast.LENGTH_LONG).show();
        }

        if (intent.hasExtra("groupId")) {
            currentGroupId = intent.getLongExtra("groupId", -1L);

            Log.d(TAG, "handleIntent: 인텐트 수신됨. GroupId=" + currentGroupId + ", Username=" + loggedInUsername);

            if (currentGroupId != -1L) {
                Toast.makeText(this, "그룹 ID: " + currentGroupId + " 위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show();
                if (loggedInUserId != -1L) {
                    startLocationSharing();
                } else {
                    fetchLoggedInUserId();
                }
            } else {
                Log.w(TAG, "handleIntent: 유효하지 않은 그룹 ID(-1L)를 받았습니다. 위치 공유를 시작하지 않습니다.");
            }
        }
    }

    // MapsActivity.java (가정)
    private void reapplyRulesAndRefreshMarkers() {
        Log.d(TAG, "reapplyRulesAndRefreshMarkers: 상호 규칙 기반 마커 재적용 시작.");

        // 1. 캐시된 위치 데이터가 있는지 확인합니다.
        if (memberLocationsCache != null) {
            List<LocationResponse> locationsToDisplay = new ArrayList<>();

            // 2. 캐시된 모든 멤버의 위치 데이터를 순회합니다.
            for (LocationResponse location : memberLocationsCache.values()) {
                Long sharerId = location.getUserId(); // 위치를 공유한 상대방 (예: xxx)의 ID

                // 본인의 위치는 마커 업데이트 목록에서 제외합니다.
                if (location.getUserName().equals(loggedInUsername)) continue;

                if (sharerId != null && sharerId != -1L) {

                    // 3. 조건 1 확인 (Incoming Rule): 상대방이 나에게 위치를 공유했는가?
                    boolean isAllowedBySharer = incomingSharingRules.getOrDefault(sharerId, false);

                    // 4. 조건 2 확인 (Outgoing Status): 내가 상대방에게 위치를 공유했는가?
                    boolean isAllowedByMe = outgoingSharingStatus.getOrDefault(sharerId, false);

                    // 5. 최종 필터링: 상호 허용 조건을 만족하는지 확인 (AND 조건)
                    if (isAllowedBySharer && isAllowedByMe) {
                        locationsToDisplay.add(location);
                        Log.d(TAG, "reapplyRulesAndRefreshMarkers: ✅ 상호 허용으로 마커 표시 -> " + location.getUserName());
                    } else {
                        Log.d(TAG, "reapplyRulesAndRefreshMarkers: ❌ 상호 미허용으로 마커 미표시 -> " + location.getUserName() +
                                " (상대방 허용: " + isAllowedBySharer + ", 나의 허용: " + isAllowedByMe + ")");
                    }
                } else {
                    Log.w(TAG, "reapplyRulesAndRefreshMarkers: Sharer ID가 없어 필터링 건너뜐 -> " + location.getUserName());
                }
            }

            // 6. 필터링된 목록으로 마커 업데이트 요청
            updateMemberMarkers(locationsToDisplay);

        } else {
            Log.w(TAG, "reapplyRulesAndRefreshMarkers: 캐시된 위치 데이터가 없어 강제 갱신을 건너뜐.");
        }
    }

    private void fetchLoggedInUserId() {
        UserApiService apiService = ApiClient.getUserApiService(this);
        Call<Map<String, Long>> call = apiService.getUserIdByUsername(loggedInUsername);

        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Long>> call, @NonNull Response<Map<String, Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Long userId = response.body().get("userId");

                    if (userId != null && userId != -1L) {
                        loggedInUserId = userId;
                        Log.d(TAG, "사용자 ID 획득 성공: " + loggedInUserId);

                        // ID 획득 후 위치 공유 시작
                        if (currentGroupId != -1L) {
                            startLocationSharing();
                        }
                        return;
                    }
                    reapplyRulesAndRefreshMarkers();
                }
                Log.e(TAG, "❌ 사용자 ID 획득 실패. 응답 코드: " + response.code());
                // finish(); // ID 획득 실패 시 앱 종료 대신 오류 처리만
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Long>> call, @NonNull Throwable t) {
                Log.e(TAG, "사용자 ID 네트워크 오류", t);
                // finish();
            }
        });
    }
    // MapsActivity.java


    private void startLocationSharing() {
        // ⭐️ [수정]: 중복 호출 문제 해결을 위해 Handler 제거
        locationUpdateHandler.removeCallbacksAndMessages(null);

        if (loggedInUserId == -1L) {
            Log.w(TAG, "startLocationSharing: UserID 로드 대기 중. 위치 공유 시작 중단.");
            return;
        }

        // 1. Firebase 규칙 리스너 시작 (상호 허용 상태 모니터링)
        startFirebaseRulesListener();

        startMyLocationMarkerListener();

        // 🚀 --- [2.2: 목적지 리스너 시작 호출 추가] ---
        startDestinationListener();
        // 🚀 --- [2.2 끝] ---

        // 2. 주기적 위치 업데이트 시작
        Log.d(TAG, "startLocationSharing: 위치 공유 프로세스 시작. 업데이트 주기=" + LOCATION_UPDATE_INTERVAL + "ms");

        // Runnable 정의 (중복 정의 제거)
        locationUpdateRunnable = () -> {
            if (locationSource != null && animationHandler == null) {
                Location lastKnownLocation = locationSource.getLastLocation();
                if (lastKnownLocation != null) {
                    Log.d(TAG, "Location Update: 위치 획득 성공. Latitude=" + lastKnownLocation.getLatitude());
                    updateMyLocation(lastKnownLocation);
                } else {
                    Log.w(TAG, "Location Update: LocationSource에서 마지막 위치 정보를 가져올 수 없습니다. GPS 신호 대기 중.");
                }
            } else if (animationHandler != null) {
                Log.d(TAG, "Location Update: 모의(Mock) 이동 중이므로 실제 위치 업데이트는 건너뜁니다.");
            }
            locationUpdateHandler.postDelayed(locationUpdateRunnable, LOCATION_UPDATE_INTERVAL);
        };
        locationUpdateHandler.post(locationUpdateRunnable);

        // 3. Firebase 위치 데이터 리스너 시작
        startFirebaseLocationListener();
    }

    private void startFirebaseLocationListener() {
        if (currentGroupId == -1L || naverMap == null) {
            Log.e(TAG, "startFirebaseLocationListener: 리스너 시작 중단. GroupID가 유효하지 않거나 Map이 준비되지 않았습니다.");
            return;
        }

        DatabaseReference groupPathRef = firebaseDatabase.child(String.valueOf(currentGroupId));
        if (memberLocationListener != null) {
            groupPathRef.removeEventListener(memberLocationListener);
            Log.d(TAG, "startFirebaseLocationListener: 기존 위치 리스너 제거 완료.");
        }

        Log.d(TAG, "startFirebaseLocationListener: Firebase 그룹 위치 리스너 등록 시작. GroupPath=" + groupPathRef.toString());

        memberLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: 위치 데이터 변경 감지. 총 멤버 위치 개수: " + snapshot.getChildrenCount());

                // ⭐️ [수정]: 여기서 필터링은 Incoming Rule만 확인합니다. 최종 필터링은 reapplyRulesAndRefreshMarkers에서 처리합니다.
                //        캐시 저장 후 reapplyRulesAndRefreshMarkers를 호출하여 상호 허용을 확인합니다.
                Map<String, LocationResponse> tempCache = new HashMap<>();

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String username = memberSnapshot.getKey();
                    if (username != null && username.equals(loggedInUsername)) continue;

                    LocationResponse locationData = memberSnapshot.getValue(LocationResponse.class);

                    if (locationData != null && locationData.getLatitude() != null && locationData.getLongitude() != null) {
                        locationData.setUserName(username);
                        tempCache.put(username, locationData);
                    }
                }

                memberLocationsCache.clear();
                memberLocationsCache.putAll(tempCache);

                // ⭐️ 위치 데이터 갱신 시, 규칙 필터링을 다시 적용
                reapplyRulesAndRefreshMarkers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Firebase 리스너 취소 오류 (🚨보안 규칙 확인 요망)", error.toException());
            }
        };
        groupPathRef.addValueEventListener(memberLocationListener);
    }

    private void updateMyLocation(Location location) {
        if (currentGroupId == -1L || location == null || loggedInUsername == null || loggedInUserId == -1L) {
            Log.e(TAG, "updateMyLocation: 위치 업데이트 중단. GroupID=" + currentGroupId + ", Username=" + loggedInUsername + " (유효하지 않음)");
            return;
        }

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String firebasePath = String.valueOf(currentGroupId) + "/" + loggedInUsername;

        if (Double.isFinite(latitude) && Double.isFinite(longitude)) {
            HashMap<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("timestamp", System.currentTimeMillis());
            locationData.put("userId", loggedInUserId);

            // 🎯 [수정]: Firebase에 위치 데이터 쓰기
            firebaseDatabase.child(firebasePath).setValue(locationData);

            Log.d(TAG, "updateMyLocation: Firebase 쓰기 완료. Path=" + firebasePath + ", Lat=" + latitude);
        }
    }

    private void updateMemberMarkers(List<LocationResponse> locations) {
        if (naverMap == null) return;

        Log.d(TAG, "updateMemberMarkers: 지도 마커 업데이트 시작. 새 위치 개수: " + locations.size());

        List<String> updatedUsernames = new ArrayList<>();
        for (LocationResponse location : locations) {
            if (!Double.isFinite(location.getLatitude()) || !Double.isFinite(location.getLongitude())) continue;

            String username = location.getUserName();

            // 필터링 통과
            updatedUsernames.add(username);
            LatLng memberPosition = new LatLng(location.getLatitude(), location.getLongitude());

            // 마커 추가/업데이트 로직
            Marker marker = memberMarkers.get(username);
            if (marker == null) {
                marker = new Marker();
                marker.setCaptionText(username);
                memberMarkers.put(username, marker);
                Log.d(TAG, "updateMemberMarkers: 새 멤버 마커 추가 -> " + username);
            }
            marker.setPosition(memberPosition);
            marker.setMap(naverMap);
        }

        // -----------------------------------------------------------------------------------
        // ⭐️ [최종 정리 로직 수정]: Handler 제거 및 즉시 제거 실행
        // -----------------------------------------------------------------------------------
        boolean markerRemoved = false;

        Iterator<Map.Entry<String, Marker>> iterator = memberMarkers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            String username = entry.getKey();

            // updatedUsernames에 포함되지 않은 마커는 제거 대상입니다.
            if (!updatedUsernames.contains(username)) {

                // ⭐️ [수정]: Handler 제거 및 setMap(null) 즉시 호출
                entry.getValue().setMap(null); // 지도에서 마커 즉시 제거
                Log.d(TAG, "updateMemberMarkers: 마커 UI 제거 완료 -> Name: " + username);

                iterator.remove();             // 내부 맵(memberMarkers)에서 해당 엔트리 제거
                Log.d(TAG, "updateMemberMarkers: 최종 정리 맵에서 제거 -> Name: " + username);

                markerRemoved = true; // 마커가 제거되었음을 표시
            }
        }

        // -----------------------------------------------------------------------------------
        // ⭐️ [강제 갱신 로직]: 제거 루프 완료 후 단 한 번만 실행 (기존 코드 유지)
        // -----------------------------------------------------------------------------------
        if (naverMap != null && markerRemoved) {
            // 마커 제거 후 지도 UI의 강제 갱신을 유도합니다.
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(naverMap.getCameraPosition().target);
            naverMap.moveCamera(cameraUpdate);
            Log.d(TAG, "updateMemberMarkers: 마커 제거 완료 후 지도 뷰 강제 갱신 시도 완료.");
        }
    }

    //==============================================================================================
    // 4. Mock Movement & Destination Selection (로그 추가)
    //==============================================================================================

    private void loadWeatherData() {
        LatLng defaultLocation = new LatLng(37.5665, 126.9780);
        updateWeatherWidget(defaultLocation);
    }

    private void startMockMovement() {
        if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
        animationHandler = new Handler(Looper.getMainLooper());
        startTime = System.currentTimeMillis();
        startLatLng = myLocationMarker.getPosition();

        Toast.makeText(this, "Mock movement to Busan started.", Toast.LENGTH_LONG).show();
        Log.d(TAG, "startMockMovement: 가상 이동 시작. 시작 위치: " + startLatLng.latitude);

        animationRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float fraction = Math.min((float) elapsed / totalDuration, 1.0f);

                double lat = startLatLng.latitude + (endLatLng.latitude - startLatLng.latitude) * fraction;
                double lon = startLatLng.longitude + (endLatLng.longitude - startLatLng.longitude) * fraction;
                LatLng currentLatLng = new LatLng(lat, lon);

                myLocationMarker.setPosition(currentLatLng);
                naverMap.moveCamera(CameraUpdate.scrollTo(currentLatLng));

                Location mockLocation = new Location("MockProvider");
                mockLocation.setLatitude(lat);
                mockLocation.setLongitude(lon);
                updateMyLocation(mockLocation); // 🎯 가상 위치도 Firebase에 업데이트

                if (fraction < 1.0) {
                    animationHandler.postDelayed(this, updateInterval);
                } else {
                    Toast.makeText(MapsActivity.this, "Arrived in Busan.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "startMockMovement: 가상 이동 완료.");
                    animationHandler = null;
                }
            }
        };
        animationHandler.post(animationRunnable);
    }

    @Override
    public void onDestinationSelected(SearchResult selectedResult) {
        Toast.makeText(this, selectedResult.getTitle() + " selected as destination.", Toast.LENGTH_LONG).show();
        hideSearchResults();
        if (searchResultMarker != null) searchResultMarker.setMap(null);

        Intent intent = new Intent(this, CreateGroupActivity.class);
        intent.putExtra("destination_result", selectedResult);
        intent.putExtra("username", loggedInUsername);
        startActivity(intent);
    }

    //==============================================================================================
    // 5. UI Features (Menus, Search, Weather)
    //==============================================================================================

    private void toggleSubMenu() {
        if (isSubMenuOpen) hideSubMenu();
        else showSubMenu();
    }

    private void showSubMenu() {
        isSubMenuOpen = true;
        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        btnMainMenu.setImageResource(R.drawable.ic_close);
        btnMainMenu.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red)));

        FloatingActionButton[] targets = {
                findViewById(R.id.btnFriends), findViewById(R.id.btnCreateGroup),
                findViewById(R.id.btnMyGroups), findViewById(R.id.btnMyPage), findViewById(R.id.btnSettings)
        };
        float[] angles = {180f, 135f, 90f, 45f, 0f};
        float radiusPx = dpToPx(SUB_MENU_RADIUS_DP);

        for (int i = 0; i < targets.length; i++) {
            targets[i].setVisibility(View.VISIBLE);
            targets[i].setAlpha(0f);
            double rad = Math.toRadians(angles[i]);
            float tx = (float) (Math.cos(rad) * radiusPx * 1.2); // Adjust distance
            float ty = (float) (Math.sin(rad) * radiusPx * -1.2); // Adjust distance & invert Y
            targets[i].animate().translationX(tx).translationY(ty).alpha(1f).setDuration(300).setStartDelay(i * 40L).start();
        }
    }

    private void hideSubMenu() {
        isSubMenuOpen = false;
        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        btnMainMenu.setImageResource(R.drawable.ic_menu);
        btnMainMenu.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));

        FloatingActionButton[] targets = {
                findViewById(R.id.btnFriends), findViewById(R.id.btnCreateGroup),
                findViewById(R.id.btnMyGroups), findViewById(R.id.btnMyPage), findViewById(R.id.btnSettings)
        };

        for (int i = 0; i < targets.length; i++) {
            int finalI = i;
            targets[i].animate().translationX(0f).translationY(0f).alpha(0f).setDuration(250).setStartDelay((targets.length - 1 - i) * 30L)
                    .withEndAction(() -> targets[finalI].setVisibility(View.GONE)).start();
        }
    }

    private void initializeSubMenu() {
        FloatingActionButton[] targets = {
                findViewById(R.id.btnFriends), findViewById(R.id.btnCreateGroup),
                findViewById(R.id.btnMyGroups), findViewById(R.id.btnMyPage), findViewById(R.id.btnSettings)
        };
        for(FloatingActionButton fab : targets) {
            fab.setVisibility(View.GONE);
            fab.setAlpha(0f);
        }
    }

    private void bindMyPageHeader() {
        TextView tvUsername = findViewById(R.id.tv_username);
        TextView tvEmail = findViewById(R.id.tv_email);
        if (tvUsername != null) tvUsername.setText(loggedInUsername != null ? loggedInUsername : "Guest");
        if (tvEmail != null) tvEmail.setText(getSharedPreferences("user_info", MODE_PRIVATE).getString("email", ""));

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            new TokenManager(this).deleteToken();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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

    // ⭐️ [1. 수정] searchPlacesWithNaverAPI 메서드를 아래 코드로 덮어쓰기
    // (이미지 검색 API 호출 로직 추가됨)
    private void searchPlacesWithNaverAPI(String query) {
        executor.execute(() -> {
            try {
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                URL url = new URL("https://openapi.naver.com/v1/search/local.json?query=" + encodedQuery + "&display=10");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-Naver-Client-Id", NAVER_CLIENT_ID);
                conn.setRequestProperty("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close(); // 리소스 닫기

                    // 1. 지역 검색 결과를 파싱합니다. (이때 imageUrl 필드는 비어있음)
                    //    (description은 parseNaverSearchResults에서 파싱됨)
                    List<SearchResult> results = parseNaverSearchResults(new JSONObject(response.toString()));

                    // ✨✨✨ [핵심 수정] ✨✨✨
                    // 지역 검색 결과(results)를 순회하며 각각의 이미지 URL을 가져옵니다.
                    for (SearchResult result : results) {

                        // 2. 장소 이름(title)으로 이미지 검색 API(헬퍼 메서드)를 호출합니다.
                        String imageUrl = fetchFirstImageUrl(result.getTitle());

                        // 3. SearchResult 객체에 이미지 URL을 설정합니다. (Setter가 있으므로 OK)
                        result.setImageUrl(imageUrl);
                    }
                    // ✨✨✨ [수정 완료] ✨✨✨


                    // 4. 이미지 URL까지 모두 채워진 results를 UI 스레드로 전달합니다.
                    handler.post(() -> {
                        if (results.isEmpty()) Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                        else showSearchResults(results);
                    });
                } else {
                    handler.post(() -> Toast.makeText(this, "API 오류 발생", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("SearchAPI", "Search failed", e);
                handler.post(() -> Toast.makeText(this, "검색 중 오류 발생", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ⭐️ [2. 새로 추가] 장소 이름으로 첫 번째 이미지 썸네일 URL을 가져오는 헬퍼 메서드
    // (이 메서드는 반드시 백그라운드 스레드에서 호출되어야 합니다)
    private String fetchFirstImageUrl(String query) {
        try {
            // 이미지 검색 API는 검색어가 너무 길면(주소 포함 등) 검색이 안될 수 있으므로,
            // 간단한 이름만 사용하도록 앞의 일부만 잘라낼 수 있습니다. (선택 사항)
            String simpleQuery = query.split(" ")[0].replaceAll("<[^>]*>", ""); // HTML 태그도 제거

            String encodedQuery = java.net.URLEncoder.encode(simpleQuery, "UTF-8");

            // ⭐️ 이미지 검색 API (image.json) 호출, display=1 (1개만)
            URL url = new URL("https://openapi.naver.com/v1/search/image?query=" + encodedQuery + "&display=1&sort=sim");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Naver-Client-Id", NAVER_CLIENT_ID);
            conn.setRequestProperty("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close(); // 리소스 닫기

                JSONObject json = new JSONObject(response.toString());
                JSONArray items = json.getJSONArray("items");
                if (items.length() > 0) {
                    // ⭐️ 썸네일(thumbnail) URL 반환
                    return items.getJSONObject(0).optString("thumbnail", "");
                }
            }
            return ""; // API 오류 또는 검색 결과 없음
        } catch (Exception e) {
            Log.e("ImageSearchAPI", "Failed to fetch image for: " + query, e);
            return ""; // 예외 발생 시 빈 문자열 반환
        }
    }

    // ⭐️ [3. 수정] parseNaverSearchResults 메서드를 생성자에 맞게 수정
    private List<SearchResult> parseNaverSearchResults(JSONObject json) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        JSONArray items = json.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String title = item.getString("title").replaceAll("<[^>]*>", "");
            String address = item.optString("roadAddress", item.optString("address", ""));
            String category = item.optString("category", "정보 없음");

            // ✨ [수정] description을 파싱합니다.
            String description = item.optString("description", "");
            // (link는 SearchResult 모델에 없으므로 파싱하지 않습니다.)

            // Correct coordinate parsing from Code 1
            double longitude = item.getDouble("mapx") / 1e7;
            double latitude = item.getDouble("mapy") / 1e7;

            // ✨ [수정] 생성자에 (description, "")을 전달합니다. (imageUrl은 나중에 채워짐)
            results.add(new SearchResult(title, address, category, latitude, longitude, description, ""));
        }
        return results;
    }

    private void moveToSearchResult(SearchResult result) {
        if (naverMap != null) {
            LatLng location = new LatLng(result.getLatitude(), result.getLongitude());
            naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(location, 16).animate(CameraAnimation.Easing));

            if (searchResultMarker != null) searchResultMarker.setMap(null);
            searchResultMarker = new Marker(location);
            searchResultMarker.setCaptionText(result.getTitle());
            searchResultMarker.setMap(naverMap);

            SearchResultDetailFragment.newInstance(result).show(getSupportFragmentManager(), "SearchResultDetail");
        }
    }

    private void showSearchResults(List<SearchResult> results) {
        searchResultAdapter.updateResults(results);
        rvSearchResults.setVisibility(View.VISIBLE);
    }

    private void hideSearchResults() {
        rvSearchResults.setVisibility(View.GONE);
    }

    private void showWeatherBottomSheet() {
        Location location = locationSource.getLastLocation();
        double lat = (location != null) ? location.getLatitude() : 37.5665;
        double lon = (location != null) ? location.getLongitude() : 126.9780;
        WeatherBottomSheetFragment.newInstance(lat, lon).show(getSupportFragmentManager(), "WeatherBottomSheet");
    }

    private void updateWeatherWidget(LatLng location) {
        executor.execute(() -> {
            try {
                URL url = new URL(String.format(Locale.US,
                        "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric&lang=kr",
                        location.latitude, location.longitude, OPENWEATHERMAP_API_KEY));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) response.append(line);

                    JSONObject json = new JSONObject(response.toString());
                    double temp = json.getJSONObject("main").getDouble("temp");
                    String weatherMain = json.getJSONArray("weather").getJSONObject(0).getString("main");

                    handler.post(() -> {
                        tvTemperature.setText(String.format(Locale.getDefault(), "%.0f°", temp));
                        ivWeatherIcon.setImageResource(getWeatherIconResource(weatherMain));
                    });
                }
            } catch (Exception e) {
                Log.e("WeatherAPI", "Failed to load weather", e);
            }
        });
    }

    private int getWeatherIconResource(String weatherMain) {
        switch (weatherMain.toLowerCase()) {
            case "clear": return R.drawable.ic_weather_clear;
            case "clouds": return R.drawable.ic_weather_cloudy;
            case "rain": case "drizzle": return R.drawable.ic_weather_rainy;
            case "snow": return R.drawable.ic_weather_snow;
            default: return R.drawable.ic_weather_clear;
        }
    }


    //==============================================================================================
    // 6. Permissions & Utilities (수정 없음)
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
            if (naverMap != null) naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        }
    }

    private void moveToCurrentLocation() {
        if (naverMap != null && locationSource.getLastLocation() != null) {
            Location loc = locationSource.getLastLocation();
            naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(loc.getLatitude(), loc.getLongitude()), 16)
                    .animate(CameraAnimation.Easing));
        }
    }

    private void showMapTypeMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.map_type_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.map_type_normal) naverMap.setMapType(NaverMap.MapType.Basic);
            else if (id == R.id.map_type_satellite) naverMap.setMapType(NaverMap.MapType.Satellite);
            else if (id == R.id.map_type_terrain) naverMap.setMapType(NaverMap.MapType.Terrain);
            return true;
        });
        popup.show();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void applyMapTypeSetting() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isSatellite = prefs.getBoolean("satellite_mode", false);
        if (naverMap != null) naverMap.setMapType(isSatellite ? NaverMap.MapType.Satellite : NaverMap.MapType.Basic);
    }

    //==============================================================================================
    // 7. Activity Lifecycle Callbacks (로그 추가)
    //==============================================================================================

    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        applyMapTypeSetting();

        if (currentGroupId != -1L) {
            Log.d(TAG, "onResume: 유효한 그룹 ID(" + currentGroupId + ")가 있어 위치 공유 재시작.");

            // ❌ [제거] 규칙 강제 재로드 및 rulesNeedReload 로직 제거 (Firebase 실시간 리스너 사용)
            // if (rulesNeedReload) { ... }

            // ⭐️ [수정] startLocationSharing에 모든 초기화 로직이 포함됨
            startLocationSharing();

            // ❌ [제거] 규칙 버전 리스너 제거
            // startRulesVersionListener();

            startMapRefreshTimer();

        } else {
            Log.d(TAG, "onResume: 그룹 ID가 없어 위치 공유를 시작하지 않음.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        locationUpdateHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "onPause: 주기적인 위치 업데이트 (Handler) 중단.");

        if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationHandler = null;
        }

        stopMapRefreshTimer();
        if (currentGroupId != -1L) {
            if (memberLocationListener != null) {
                firebaseDatabase.child(String.valueOf(currentGroupId)).removeEventListener(memberLocationListener);
                Log.d(TAG, "onPause: Firebase 위치 리스너 제거 완료.");
            }
            // ⭐️ [수정] Firebase 규칙 리스너 제거
            if (rulesRef != null && rulesListener != null) {
                rulesRef.removeEventListener(rulesListener);
                Log.d(TAG, "onPause: Firebase 규칙 리스너 제거 완료.");
            }
            if (myMarkerStatusRef != null && myMarkerStatusListener != null) {
                myMarkerStatusRef.removeEventListener(myMarkerStatusListener);
                Log.d(TAG, "onPause: 내 마커 상태 리스너 제거 완료.");
            }

            // 🚀 --- [이 부분이 추가되었습니다] ---
            stopDestinationListener();
            // 🚀 --- [추가 완료] ---
        }
    }

    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        if (myMarkerStatusRef != null && myMarkerStatusListener != null) {
            myMarkerStatusRef.removeEventListener(myMarkerStatusListener);
            Log.d(TAG, "onDestroy: 내 마커 상태 리스너 제거 완료.");
        }
    }

    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }


    // 🚀 --- [2.5: 목적지 마커용 새 메서드 4개 추가] ---

    /**
     * Firebase에서 목적지 정보를 구독하는 리스너를 시작합니다.
     */
    private void startDestinationListener() {
        // 맵이 준비되지 않았거나 그룹 ID가 없으면 실행 중단
        if (naverMap == null || currentGroupId == -1L) {
            Log.w(TAG, "startDestinationListener: NaverMap이 null이거나 Group ID가 유효하지 않아 중단.");
            return;
        }

        // 중복 실행 방지를 위해 기존 리스너가 있다면 먼저 중지
        stopDestinationListener();

        // 🚩 경로 설정: 'group_destinations/{그룹ID}/destination'
        // CreateGroupActivity에서 저장한 경로와 반드시 동일해야 합니다.
        destinationRef = FirebaseDatabase.getInstance()
                .getReference("group_destinations")
                .child(String.valueOf(currentGroupId))
                .child("destination");

        Log.d(TAG, "Firebase 목적지 리스너 등록 시도. Path: " + destinationRef.toString());

        destinationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Firebase에서 위도, 경도, 이름 데이터 추출
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);
                    String name = snapshot.child("name").getValue(String.class);

                    // 모든 데이터가 유효한지 확인
                    if (latitude != null && longitude != null && name != null &&
                            Double.isFinite(latitude) && Double.isFinite(longitude)) {

                        LatLng destinationLatLng = new LatLng(latitude, longitude);
                        // 마커 업데이트
                        updateDestinationMarker(destinationLatLng, name);
                        Log.d(TAG, "목적지 정보 수신: " + name);
                    } else {
                        // 데이터가 일부 누락되었거나 유효하지 않으면 마커 제거
                        removeDestinationMarker();
                        Log.w(TAG, "수신된 목적지 데이터가 유효하지 않습니다.");
                    }
                } else {
                    // 'destination' 노드가 Firebase에 존재하지 않으면 마커 제거
                    removeDestinationMarker();
                    Log.d(TAG, "Firebase에 해당 그룹의 목적지 정보가 없습니다.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase 목적지 리스너 취소됨", error.toException());
                removeDestinationMarker(); // 오류 발생 시에도 마커 제거
            }
        };

        // 리스너 등록
        destinationRef.addValueEventListener(destinationListener);
    }

    /**
     * 지도에서 목적지 마커를 제거합니다.
     */
    private void removeDestinationMarker() {
        if (destinationMarker != null) {
            destinationMarker.setMap(null); // 지도에서 제거
            destinationMarker = null; // 참조 해제
            Log.d(TAG, "목적지 마커 제거 완료.");
        }
    }

    /**
     * 목적지 마커를 지도에 생성하거나 위치를 업데이트합니다.
     */
    private void updateDestinationMarker(LatLng position, String caption) {
        if (naverMap == null) {
            Log.w(TAG, "updateDestinationMarker: NaverMap이 null이라 마커를 업데이트할 수 없습니다.");
            return;
        }

        if (destinationMarker == null) {
            // 마커가 없으면 새로 생성
            destinationMarker = new Marker();
            destinationMarker.setWidth(Marker.SIZE_AUTO);
            destinationMarker.setHeight(Marker.SIZE_AUTO);

            // 🚩 (선택 사항) 목적지 마커 아이콘을 다르게 설정할 수 있습니다.
            // 예: drawable에 'ic_flag_pin.png' 같은 아이콘 추가 후
            // destinationMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_flag_pin));

            // 멤버 마커(Z-index 기본값 100)보다 낮은 Z-index를 주어 멤버 마커가 위로 오게 함
            destinationMarker.setZIndex(50);
            Log.d(TAG, "새 목적지 마커 생성.");
        }

        // 위치 및 캡션 설정
        destinationMarker.setPosition(position);
        // 🚀 --- [ "도착지: " 문구 추가 ] ---
        destinationMarker.setCaptionText("🚩 도착지: " + caption);
        // 🚀 --- [ 수정 완료 ] ---
        destinationMarker.setMap(naverMap); // 지도에 표시
    }

    /**
     * 목적지 정보 구독 리스너를 중지하고 참조를 해제합니다.
     */
    private void stopDestinationListener() {
        if (destinationRef != null && destinationListener != null) {
            destinationRef.removeEventListener(destinationListener);
            Log.d(TAG, "Firebase 목적지 리스너 제거 완료.");
        }
        destinationRef = null;
        destinationListener = null;
    }

    // 🚀 --- [2.5 끝] ---

}
