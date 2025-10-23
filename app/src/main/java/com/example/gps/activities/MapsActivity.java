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

import com.example.gps.api.GroupApiService; // ⭐️ 추가: 그룹 API
import com.example.gps.api.UserApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SearchResultDetailFragment.OnDestinationSelectedListener {

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

    private final Map<Long, Boolean> incomingSharingRules = new HashMap<>();
    private Marker myLocationMarker = null;

    // --- Mock Movement (for testing) ---
    private Handler animationHandler;
    private Runnable animationRunnable;
    private LatLng startLatLng = new LatLng(37.5665, 126.9780); // Seoul City Hall
    private LatLng endLatLng = new LatLng(35.115, 129.04); // Busan Station
    private final long totalDuration = 10000; // 10 seconds
    private final int updateInterval = 50; // 50ms
    private long startTime;

    private Long loggedInUserId = -1L;

    private boolean rulesNeedReload = false;

    private DatabaseReference rulesVersionRef;
    private ValueEventListener rulesVersionListener;

    private List<LocationResponse> currentMemberLocations = new ArrayList<>();

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

        // 🎯 로그 추가: 앱 시작 시 Firebase 초기화 및 사용자명 확인
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

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
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
        // ⭐ [추가] onNewIntent에서 handleIntent를 호출하기 전에 setIntent(intent)를 추가하여,
        //      액티비티가 현재 Intent를 새 Intent로 갱신하도록 강제합니다.

        if (intent != null && intent.getBooleanExtra("RULES_UPDATED", false)) {
            rulesNeedReload = true;
            Log.d(TAG, "onNewIntent: 공유 설정 변경 감지. 규칙 재로드 플래그 설정.");
        }

        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        // 💡 [추가] CreateGroupActivity에서 돌아올 때 username이 갱신되어야 합니다.
        if (intent.hasExtra("username")) {
            loggedInUsername = intent.getStringExtra("username");
        }

        if ("SELECT_DESTINATION".equals(intent.getStringExtra("PURPOSE"))) {
            isSelectionMode = true;
            Toast.makeText(this, "목적지로 설정할 장소를 검색 후 선택해주세요.", Toast.LENGTH_LONG).show();
        }

        if (intent.hasExtra("groupId")) {
            currentGroupId = intent.getLongExtra("groupId", -1L);

            // 🎯 로그 추가: 인텐트 수신 및 GroupId 확인
            Log.d(TAG, "handleIntent: 인텐트 수신됨. GroupId=" + currentGroupId + ", Username=" + loggedInUsername);

            if (currentGroupId != -1L) {
                Toast.makeText(this, "그룹 ID: " + currentGroupId + " 위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show();
                if (loggedInUserId != -1L) {
                    startLocationSharing();
                } else {
                    // ID 조회는 onCreate에서 이미 시작했거나, 여기서 다시 시작하여
                    // 성공 시 startLocationSharing()을 호출하도록 만듭니다.
                    fetchLoggedInUserId();
                }
            } else {
                Log.w(TAG, "handleIntent: 유효하지 않은 그룹 ID(-1L)를 받았습니다. 위치 공유를 시작하지 않습니다.");
            }
        }
    }

    private void reapplyRulesAndRefreshMarkers() {
        // 로그를 통해 규칙 변경으로 인한 마커 갱신이 시작됨을 확인합니다.
        Log.d(TAG, "reapplyRulesAndRefreshMarkers: 규칙 변경으로 마커 즉시 갱신 시작.");

        // currentMemberLocations에 저장된 최신 위치 데이터를 사용하여
        // updateMemberMarkers를 호출합니다. updateMemberMarkers 내부에서
        // 새로 로드된 incomingSharingRules에 따라 마커 필터링 및 제거가 일어납니다.
        updateMemberMarkers(currentMemberLocations);
    }

    private void fetchLoggedInUserId() {
        UserApiService apiService = ApiClient.getUserApiService(this);

        // ⭐️ [수정]: UserApiService의 정의인 Call<Map<String, Long>>와 일치시킴
        Call<Map<String, Long>> call = apiService.getUserIdByUsername(loggedInUsername);

        // ⭐️ [수정]: Callback 타입도 Map<String, Long>으로 일치시킴
        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Long>> call, @NonNull Response<Map<String, Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 키는 "userId"로, 값은 Long 타입으로 가져옴
                    Long userId = response.body().get("userId");

                    if (userId != null && userId != -1L) {
                        loggedInUserId = userId;
                        Log.d(TAG, "사용자 ID 획득 성공: " + loggedInUserId);
                        return;
                    }
                    reapplyRulesAndRefreshMarkers();
                }
                Log.e(TAG, "❌ 사용자 ID 획득 실패. 응답 코드: " + response.code());
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Long>> call, @NonNull Throwable t) {
                Log.e(TAG, "사용자 ID 네트워크 오류", t);
                finish();
            }
        });
    }

    private void startLocationSharing() {
        locationUpdateHandler.removeCallbacksAndMessages(null);

        if (loggedInUserId != -1L) {
            fetchIncomingSharingRules();
        } else {
            Log.w(TAG, "startLocationSharing: UserID 로드 대기 중. 규칙 로딩 생략.");
        }

        Log.d(TAG, "startLocationSharing: 위치 공유 프로세스 시작. 업데이트 주기=" + LOCATION_UPDATE_INTERVAL + "ms");

        // ... (locationUpdateRunnable 로직은 동일)
        locationUpdateHandler.post(locationUpdateRunnable);
        startFirebaseLocationListener();

        // 🎯 로그 추가: 위치 공유 시작
        Log.d(TAG, "startLocationSharing: 위치 공유 프로세스 시작. 업데이트 주기=" + LOCATION_UPDATE_INTERVAL + "ms");

        locationUpdateRunnable = () -> {
            if (locationSource != null && animationHandler == null) {
                Location lastKnownLocation = locationSource.getLastLocation();
                if (lastKnownLocation != null) {
                    // 🎯 로그 추가: 위치 정보 획득 및 업데이트 요청
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
        startFirebaseLocationListener();
    }

    private void fetchIncomingSharingRules() {
        if (currentGroupId == -1L || loggedInUserId == -1L) {
            Log.e(TAG, "fetchIncomingSharingRules: 로드 중단. GroupID 또는 UserID가 유효하지 않습니다.");
            return;
        }

        GroupApiService apiService = ApiClient.getGroupApiService(this);
        // 💡 API 호출: 내가 Target일 때, 누가 나에게 공유를 허용했는지 규칙을 가져옵니다.
        Call<Map<Long, Boolean>> call = apiService.getSharingRulesForTarget(currentGroupId, loggedInUserId);

        call.enqueue(new Callback<Map<Long, Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<Map<Long, Boolean>> call, @NonNull Response<Map<Long, Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    incomingSharingRules.clear();
                    incomingSharingRules.putAll(response.body());
                    Log.d(TAG, "✅ Incoming 규칙 로드 성공. 로드된 규칙 수: " + incomingSharingRules.size());
                    Log.d(TAG, "Debugging Rules: Incoming rule for ID 17 (xxx) is " + incomingSharingRules.get(17L));
                } else {
                    Log.e(TAG, "❌ Incoming 규칙 로드 실패. 응답 코드: " + response.code());
                    Toast.makeText(MapsActivity.this, "위치 공유 규칙 로드 실패. 모든 멤버 위치 표시 시도.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<Long, Boolean>> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Incoming 규칙 로드 네트워크 오류", t);
                Toast.makeText(MapsActivity.this, "위치 공유 규칙 로드 네트워크 오류.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateMyLocation(Location location) {
        if (currentGroupId == -1L || location == null || loggedInUsername == null || loggedInUserId == -1L) {
            // 🎯 로그 추가: 위치 업데이트 중단 사유
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

            // 🎯 로그 추가: Firebase에 쓰기 시작
            Log.d(TAG, "updateMyLocation: Firebase 쓰기 시도. Path=" + firebasePath + ", Lat=" + latitude);

        }
    }

    private void startFirebaseLocationListener() {
        if (currentGroupId == -1L || naverMap == null) {
            Log.e(TAG, "startFirebaseLocationListener: 리스너 시작 중단. GroupID가 유효하지 않거나 Map이 준비되지 않았습니다.");
            return;
        }

        DatabaseReference groupPathRef = firebaseDatabase.child(String.valueOf(currentGroupId));
        if (memberLocationListener != null) {
            groupPathRef.removeEventListener(memberLocationListener);
            Log.d(TAG, "startFirebaseLocationListener: 기존 리스너 제거 완료.");
        }

        // 🎯 로그 추가: 리스너 등록
        Log.d(TAG, "startFirebaseLocationListener: Firebase 그룹 위치 리스너 등록 시작. GroupPath=" + groupPathRef.toString());

        memberLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 🎯 로그 추가: 데이터 변경 수신
                Log.d(TAG, "onDataChange: 데이터 변경 감지. 총 멤버 위치 개수: " + snapshot.getChildrenCount());

                // ⭐️ [수정] 모든 수신 위치를 저장할 임시 목록 (필터링되지 않음)
                List<LocationResponse> rawLocations = new ArrayList<>();
                // ⭐️ [변경] 규칙을 통과한 데이터만 담는 목록
                List<LocationResponse> filteredLocations = new ArrayList<>();

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String username = memberSnapshot.getKey();

                    // 1. 자기 자신의 위치는 마커 업데이트 목록에서 제외
                    if (username != null && username.equals(loggedInUsername)) continue;

                    LocationResponse locationData = memberSnapshot.getValue(LocationResponse.class);

                    if (locationData != null) {
                        // Firebase Realtime DB는 username을 Key로 사용하므로, DTO에 수동 설정
                        locationData.setUserName(username);

                        // 유효성 검사 (latitude와 longitude는 필수)
                        if (locationData.getLatitude() != null && locationData.getLongitude() != null) {

                            // ⭐️ [추가]: 필터링 여부와 관계없이 일단 모든 유효한 위치 데이터를 rawLocations에 추가합니다.
                            rawLocations.add(locationData);

                            Long sharerId = locationData.getUserId();
                            boolean isAllowed = false;

                            // ⭐ [디버그 및 필터링 로직 시작]
                            if (sharerId != null && sharerId != -1L) {
                                isAllowed = incomingSharingRules.getOrDefault(sharerId, false);

                                // 💡 핵심 디버깅 로그: 필터링 결정 이유를 확인합니다.
                                Log.d(TAG, "DEBUG_FILTER_RULE: Sharer ID " + sharerId + " (" + username + ")" +
                                        " | isAllowed=" + isAllowed +
                                        " | Rule in Map: " + (incomingSharingRules.containsKey(sharerId) ? incomingSharingRules.get(sharerId) : "NOT_IN_MAP(false)") );

                                if (!isAllowed) {
                                    Log.d(TAG, "Filtering: Sharer ID " + sharerId + " (" + username + ")의 위치는 수신 규칙에 따라 표시되지 않습니다. (차단)");
                                    continue; // 마커 업데이트 건너뛰기
                                }
                            } else {
                                Log.w(TAG, "Filtering Skip: Sharer ID가 없어 필터링을 건너뜀 (" + username + ")");
                                // ID가 없는 경우에도 마커를 표시하지 않으려면 여기에도 continue; 를 추가해야 합니다.
                            }
                            // ⭐ [디버그 및 필터링 로직 끝]

                            // ⭐️ [수정]: 필터링 통과 시 filteredLocations에 추가합니다.
                            filteredLocations.add(locationData);

                            // ⭐ 로그 수정: userId 정보가 포함되었는지 확인
                            Log.d(TAG, "onDataChange: 수신된 멤버 위치 -> " + username +
                                    " at (" + locationData.getLatitude() + ", " + locationData.getLongitude() + ")" +
                                    " / ID: " + locationData.getUserId());
                        }
                    }
                }

                // ⭐️ [필수 추가]: rawLocations를 클래스 필드에 저장합니다. (reapplyRulesAndRefreshMarkers에서 사용)
                currentMemberLocations = rawLocations;

                // ⭐️ [수정]: 필터링된 목록을 updateMemberMarkers에 전달합니다.
                updateMemberMarkers(filteredLocations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 🎯 로그 추가: 리스너 취소 오류 (보안 규칙 문제일 가능성 높음)
                Log.e(TAG, "onCancelled: Firebase 리스너 취소 오류 (🚨보안 규칙 확인 요망)", error.toException());
            }
        };
        groupPathRef.addValueEventListener(memberLocationListener);
    }

    private void updateMemberMarkers(List<LocationResponse> locations) {
        if (naverMap == null) return;

        // 🎯 로그 추가: 마커 업데이트 시작
        Log.d(TAG, "updateMemberMarkers: 지도 마커 업데이트 시작. 새 위치 개수: " + locations.size());

        List<String> updatedUsernames = new ArrayList<>();
        for (LocationResponse location : locations) {
            if (!Double.isFinite(location.getLatitude()) || !Double.isFinite(location.getLongitude())) continue;

            String username = location.getUserName();
            Long sharerId = location.getUserId();

            // -----------------------------------------------------------------------------------
            // ⭐️ [핵심 필터링 로직]
            // -----------------------------------------------------------------------------------
            boolean isAllowed = false;
            if (sharerId != null && sharerId != -1L) {
                // isAllowed의 기본값을 'false'(차단)로 설정합니다.
                isAllowed = incomingSharingRules.getOrDefault(sharerId, false);

                if (!isAllowed) {
                    // ❌ [수정]: 마커 제거 로직을 이 위치에서 제거합니다.
                    // 최종 정리 단계에서 모든 제거를 일괄 처리합니다.
                    Log.d(TAG, "Filtering: Sharer ID " + sharerId + " (" + username + ")의 위치는 수신 규칙에 따라 표시되지 않습니다. (차단)");
                    continue; // 마커 업데이트 건너뛰기
                }
            } else {
                // sharerId가 없으면 필터링 불가 (서버 수정이 필요합니다). 임시로 계속 표시합니다.
                Log.w(TAG, "Filtering Skip: Sharer ID가 없으므로 (" + username + ")의 위치는 규칙 확인 없이 표시됩니다. (서버 수정이 완료되면 해결됨)");
            }

            // 필터링 통과 (또는 필터링 생략)
            updatedUsernames.add(username);
            LatLng memberPosition = new LatLng(location.getLatitude(), location.getLongitude());

            // 마커 추가/업데이트 로직 (기존과 동일)
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
        // ⭐️ [최종 정리 로직 수정]: 지도에 남아있는 마커를 확실하게 제거
        // -----------------------------------------------------------------------------------
        // updatedUsernames에 없는 모든 마커(차단된 멤버, 위치가 끊긴 멤버)는 지도에서 제거해야 합니다.
        Iterator<Map.Entry<String, Marker>> iterator = memberMarkers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            String username = entry.getKey();

            // updatedUsernames에 포함되지 않은 마커는 제거 대상입니다.
            if (!updatedUsernames.contains(username)) {
                entry.getValue().setMap(null); // 지도에서 마커 제거
                iterator.remove();             // 맵에서 해당 엔트리 제거
                Log.d(TAG, "updateMemberMarkers: 최종 정리 마커 제거 -> Name: " + username);
            }
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
    // 5. UI Features (Menus, Search, Weather - 수정 없음)
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

                    List<SearchResult> results = parseNaverSearchResults(new JSONObject(response.toString()));
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

    private List<SearchResult> parseNaverSearchResults(JSONObject json) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        JSONArray items = json.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String title = item.getString("title").replaceAll("<[^>]*>", "");
            String address = item.optString("roadAddress", item.optString("address", ""));
            String category = item.optString("category", "정보 없음");

            // Correct coordinate parsing from Code 1
            double longitude = item.getDouble("mapx") / 1e7;
            double latitude = item.getDouble("mapy") / 1e7;

            results.add(new SearchResult(title, address, category, latitude, longitude, "", ""));
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

    private void startRulesVersionListener() {
        if (currentGroupId == -1L) return;

        // 규칙 버전 참조 설정: group_rules_version/{groupId}
        rulesVersionRef = FirebaseDatabase.getInstance()
                .getReference("group_rules_version")
                .child(String.valueOf(currentGroupId));

        // 기존 리스너 제거 (중복 방지)
        if (rulesVersionListener != null) {
            rulesVersionRef.removeEventListener(rulesVersionListener);
        }

        rulesVersionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 규칙 버전이 변경되었을 때 (다른 멤버가 설정을 저장 시)
                Log.d(TAG, "Rules Version Change Detected for Group " + currentGroupId + ". Reloading incoming sharing rules.");
                // 모든 디바이스에서 이 코드가 실행되며 규칙을 서버에서 다시 가져옵니다.
                fetchIncomingSharingRules();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Rules Version Listener Cancelled", error.toException());
            }
        };
        // 리스너 등록
        rulesVersionRef.addValueEventListener(rulesVersionListener);
        Log.d(TAG, "startRulesVersionListener: 규칙 버전 리스너 등록 완료. Group ID: " + currentGroupId);
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

            if (rulesNeedReload) {
                Log.d(TAG, "onResume: 공유 설정 변경 감지. Incoming 규칙 강제 재로드 시작.");
                fetchIncomingSharingRules();
                rulesNeedReload = false;
            }

            startLocationSharing();
            // ⭐ [변경] onResume 시 규칙 버전 리스너 시작 (실시간 동기화)
            startRulesVersionListener();

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

        if (currentGroupId != -1L) {
            if (memberLocationListener != null) {
                firebaseDatabase.child(String.valueOf(currentGroupId)).removeEventListener(memberLocationListener);
                Log.d(TAG, "onPause: Firebase 위치 리스너 제거 완료.");
            }
            // ⭐ [변경] 규칙 버전 리스너 제거
            if (rulesVersionRef != null && rulesVersionListener != null) {
                rulesVersionRef.removeEventListener(rulesVersionListener);
                Log.d(TAG, "onPause: Firebase 규칙 버전 리스너 제거 완료.");
            }
        }
    }

    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        // ⭐ [추가] 규칙 버전 리스너 해제 (중복 호출 방지)
        if (rulesVersionRef != null && rulesVersionListener != null) {
            rulesVersionRef.removeEventListener(rulesVersionListener);
            Log.d(TAG, "onDestroy: Firebase 규칙 버전 리스너 제거 완료.");
        }
    }

    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

}
