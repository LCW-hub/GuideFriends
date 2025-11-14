// [통합본] 동시접속 제어 + 프로필 사진 기능 + TMap 경로 시뮬레이션 기능이 모두 포함된 MapsActivity
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
import com.example.gps.api.UserApi;
import com.example.gps.dto.FriendResponse;
import com.example.gps.activities.Register_Login.LoginActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.activities.Friend.FriendsActivity;
import com.example.gps.adapters.SearchResultAdapter;
// ⭐️ [추가/수정] FriendAdapter, User 모델 Import
import com.example.gps.adapters.FriendAdapter;
import com.example.gps.model.User;
// ⭐️ [추가/수정 끝]
import com.example.gps.api.ApiClient;
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
// ⭐️ [TMap 추가 Import]
import java.io.OutputStream;
// ⭐️ [TMap 추가 Import]
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// ⭐️ [수정] FriendApiService와 GroupApiService 모두 유지
import com.example.gps.api.GroupApiService;
import com.example.gps.api.UserApiService;
import com.example.gps.api.FriendApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// --- ⭐️ [MERGE] 프로필 사진용 Import 시작 ---
import android.app.AlertDialog;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.naver.maps.map.overlay.OverlayImage;
import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
// --- ⭐️ [MERGE] 프로필 사진용 Import 끝 ---

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.Bitmap.Config;

import androidx.cardview.widget.CardView;
import android.widget.Button;
import com.example.gps.activities.ChatRoomActivity;
import com.example.gps.activities.GroupSharingSettingsActivity;

// ⭐️ [TMap 추가 Import]
import com.naver.maps.map.overlay.PathOverlay;


// ⭐️ [수정] FriendAdapter.OnDeleteClickListener 인터페이스 구현 추가
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SearchResultDetailFragment.OnDestinationSelectedListener, FriendAdapter.OnDeleteClickListener {

    // ⭐️ [TMap 추가] TMap API 키
    private static final String TMAP_API_KEY = "6BXu3W092c8kdbZVOOzDe5YqlALysG305fjlKG10";

    // --- (UI, Map, Search, Weather, Menu 변수들은 변경 없음) ---
    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private DrawerLayout drawerLayout;
    private EditText etSearch;
    private ImageView ivSearchIcon;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter searchResultAdapter;
    private Marker searchResultMarker = null;
    private ImageView ivWeatherIcon;
    private TextView tvTemperature;
    private boolean isSubMenuOpen = false;
    private static final float SUB_MENU_RADIUS_DP = 80f;

    // --- Background Tasks ---
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // --- (Constants, Firebase, Destination 변수들은 변경 없음) ---
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String OPENWEATHERMAP_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";
    private static final String NAVER_CLIENT_ID = "OAQnuwhbAL34Of8mlxve";
    private static final String NAVER_CLIENT_SECRET = "4roXQDJBpc";
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final String TAG = "MapsActivity_FIREBASE";
    private String loggedInUsername;
    private boolean isSelectionMode = false;
    private Long currentGroupId = -1L;
    private DatabaseReference firebaseDatabase;
    private ValueEventListener memberLocationListener;
    private final Handler locationUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable locationUpdateRunnable;
    private final HashMap<String, Marker> memberMarkers = new HashMap<>();
    private final Handler mapRefreshHandler = new Handler(Looper.getMainLooper());
    private Runnable mapRefreshRunnable;
    private static final int MAP_REFRESH_INTERVAL = 2000;
    private final Map<Long, Boolean> incomingSharingRules = new HashMap<>();
    private final Map<Long, Boolean> outgoingSharingStatus = new HashMap<>();
    private Marker myLocationMarker = null;
    private DatabaseReference rulesRef;
    private ValueEventListener rulesListener;

    // ⭐️ [Mock Movement / TMap 변수 수정]
    private Handler animationHandler;
    private Runnable animationRunnable;
    private LatLng startLatLng = new LatLng(37.5665, 126.9780); // 서울시청
    private LatLng endLatLng = new LatLng(37.48723, 126.82056); // 유한대학교
    private final long totalDuration = 10000;
    private final int updateInterval = 50;
    private long startTime;
    // ⭐️ [TMap 시뮬레이션 상태 변수 추가]
    private boolean isSimulationRunning = false;
    private final List<PathOverlay> pathOverlays = new ArrayList<>();
    // ⭐️ [TMap 변수 수정 끝]

    private Long loggedInUserId = -1L;
    private final Map<String, LocationResponse> memberLocationsCache = new HashMap<>();
    private DatabaseReference myMarkerStatusRef;
    private ValueEventListener myMarkerStatusListener;
    private Marker destinationMarker = null;
    private DatabaseReference destinationRef;
    private ValueEventListener destinationListener;

    // --- ⭐️ [MERGE] 프로필 사진용 멤버 변수 ---
    private CircleImageView ivProfile;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private static final int MARKER_BORDER_WIDTH_PX = 6;
    private static final int MARKER_BORDER_COLOR = Color.WHITE;
    // --- ⭐️ [MERGE] 끝 ---

    private TokenManager tokenManager;
    private UserApiService userApiService;

    // --- ⭐️ [MERGE] 동시접속 제어용 변수 ---
    private ValueEventListener activeSessionListener;
    private DatabaseReference activeSessionRef;
    private boolean isSessionListenerInitialized = false;
    // --- ⭐️ [MERGE] 끝 ---
    private boolean isSessionListenerInitialized = false;

    // --- ⭐️ [추가] 마이페이지 친구 목록 관련 변수 ---
    private RecyclerView rvMyPageFriends; // 마이페이지 드로어의 RecyclerView (ID: rv_mypage_friends_list)
    private FriendAdapter friendAdapter; // FriendAdapter 인스턴스
    private FriendApiService friendApiService; // ⭐️ [수정] FriendApiService 사용
    // --- ⭐️ [추가 끝] ---

    //==============================================================================================
    // 1. Activity Lifecycle & Setup
    //==============================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // [수정] TokenManager 및 API Service 초기화
        tokenManager = new TokenManager();
        userApiService = ApiClient.getUserApiService(this);
        friendApiService = ApiClient.getFriendApiService(this); // ⭐️ [수정] FriendApiService 초기화

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

        // --- ⭐️ [MERGE] 갤러리 런처 및 마이페이지 헤더 초기화 ---
        initializeGalleryLauncher();
        bindMyPageHeader();
        fetchUserEmailAndBindHeader(); // ⭐️⭐️⭐️ 추가된 이메일 로드 로직 호출 ⭐️⭐️⭐️
        // --- ⭐️ [MERGE] 끝 ---

        // ⭐️ [추가] 마이페이지 친구 목록 초기화 호출
        initializeMyPageFriendsList();
        // ⭐️ [추가 끝]

        if (loggedInUsername != null) {
            fetchLoggedInUserId();
        }


    }

    private void startMyLocationMarkerListener() {
        if (loggedInUserId == -1L || naverMap == null) return;
        myMarkerStatusRef = FirebaseDatabase.getInstance()
                .getReference("user_status")
                .child(String.valueOf(loggedInUserId))
                .child("is_marker_visible");
        if (myMarkerStatusListener != null) {
            myMarkerStatusRef.removeEventListener(myMarkerStatusListener);
        }
        myMarkerStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isVisible = snapshot.getValue(Boolean.class);
                boolean showMarker = (isVisible != null) ? isVisible : true;
                if (myLocationMarker != null) {
                    if (showMarker) {
                        myLocationMarker.setMap(naverMap);
                        Log.d(TAG, "My Marker Status: 켜짐 (Visible)");
                    } else {
                        myLocationMarker.setMap(null);
                        Log.d(TAG, "My Marker Status: 꺼짐 (Hidden)");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "My Marker Status Listener Cancelled", error.toException());
            }
        };
        // ⭐️ [오타 수정] addValueEventListener로 정정
        myMarkerStatusRef.addValueEventListener(myMarkerStatusListener);
        Log.d(TAG, "startMyLocationMarkerListener: 내 마커 상태 리스너 등록 완료.");
    }
    private void startFirebaseRulesListener() {
        if (loggedInUserId == -1L) {
            Log.e(TAG, "startFirebaseRulesListener: 로드 중단. UserID가 유효하지 않습니다.");
            return;
        }
        rulesRef = FirebaseDatabase.getInstance()
                .getReference("sharing_permissions");
        if (rulesListener != null) {
            rulesRef.removeEventListener(rulesListener);
            Log.d(TAG, "startFirebaseRulesListener: 기존 규칙 리스너 제거 완료.");
        }
        rulesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                incomingSharingRules.clear();
                outgoingSharingStatus.clear();
                for (DataSnapshot sharerSnapshot : snapshot.getChildren()) {
                    String sharerIdStr = sharerSnapshot.getKey();
                    if (sharerIdStr == null) continue;
                    Long sharerId = Long.parseLong(sharerIdStr);
                    for (DataSnapshot targetSnapshot : sharerSnapshot.getChildren()) {
                        String targetIdStr = targetSnapshot.getKey();
                        Boolean isAllowed = targetSnapshot.getValue(Boolean.class);
                        if (targetIdStr == null || isAllowed == null) continue;
                        Long targetId = Long.parseLong(targetIdStr);
                        if (targetId.equals(loggedInUserId)) {
                            incomingSharingRules.put(sharerId, isAllowed);
                        }
                        if (sharerId.equals(loggedInUserId)) {
                            outgoingSharingStatus.put(targetId, isAllowed);
                        }
                    }
                }
                Log.d(TAG, "✅ Firebase Rules Loaded. Incoming Count: " + incomingSharingRules.size() +
                        ", Outgoing Count: " + outgoingSharingStatus.size());
                reapplyRulesAndRefreshMarkers();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Rules Listener Cancelled", error.toException());
                Toast.makeText(MapsActivity.this, "위치 공유 규칙 로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        };
        // ⭐️ [오타 수정] addValueEventListener로 정정
        rulesRef.addValueEventListener(rulesListener);
        Log.d(TAG, "startFirebaseRulesListener: Firebase 규칙 리스너 등록 완료.");
    }
    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        LatLng initialPosition = new LatLng(37.5665, 126.9780);
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(initialPosition, 11));
        if (myLocationMarker == null) {
            myLocationMarker = new Marker();
            myLocationMarker.setCaptionText("내 위치");
        }
        myLocationMarker.setPosition(initialPosition);
        myLocationMarker.setMap(naverMap);
        Log.d(TAG, "onMapReady: NaverMap 위치 변경 리스너 등록 완료");
        naverMap.addOnLocationChangeListener(location -> {
            if (location != null && Double.isFinite(location.getLatitude()) && Double.isFinite(location.getLongitude())) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                // ⭐️ [TMap 수정] TMap 시뮬레이션 중이 아닐 때만 실제 위치에 '내 위치' 마커 업데이트
                if (animationHandler == null && !isSimulationRunning) {
                    myLocationMarker.setPosition(currentLocation);
                }
                updateWeatherWidget(currentLocation);
            }
        });

        // ▼▼▼ [지도 클릭 리스너 - 목적지 선택 모드용] ▼▼▼ (원래 코드에서 복사)
        naverMap.setOnMapClickListener((point, coord) -> {
            if (isSelectionMode) {
                Log.d(TAG, "지도 클릭으로 목적지 선택됨: " + coord.latitude + ", " + coord.longitude);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("PLACE_NAME", "지도에서 선택한 위치");
                resultIntent.putExtra("PLACE_LAT", coord.latitude);
                resultIntent.putExtra("PLACE_LNG", coord.longitude);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
        // ▲▲▲ [지도 클릭 리스너 - 목적지 선택 모드용] ▲▲▲

        applyMapTypeSetting();
        loadWeatherData();
        loadProfileImage(); // ⭐️ [프로필]
    }
    private void startMapRefreshTimer() {
        if (naverMap == null) return;
        if (mapRefreshRunnable != null) {
            mapRefreshHandler.removeCallbacks(mapRefreshRunnable);
        }
        mapRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (naverMap != null && naverMap.getCameraPosition() != null) {
                    CameraUpdate cameraUpdate = CameraUpdate.scrollTo(naverMap.getCameraPosition().target);
                    naverMap.moveCamera(cameraUpdate);
                    Log.d(TAG, "Map Refresh Timer: 지도 뷰 강제 갱신 실행.");
                }
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
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        FloatingActionButton btnTestMovement = findViewById(R.id.btnTestMovement);

        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());

        // ⭐️ [TMap 수정] Mock Movement 버튼 클릭 시 TMap 시뮬레이션 시작/중지
        btnTestMovement.setOnClickListener(v -> startMockMovement());

        findViewById(R.id.weather_widget).setOnClickListener(v -> showWeatherBottomSheet());
        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        FloatingActionButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        FloatingActionButton btnMyPage = findViewById(R.id.btnMyPage);
        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
        btnMainMenu.setOnClickListener(v -> toggleSubMenu());
        btnFriends.setOnClickListener(v -> {
            startActivity(new Intent(this, FriendsActivity.class).putExtra("username", loggedInUsername));
            hideSubMenu();
        });
        btnCreateGroup.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateGroupActivity.class).putExtra("username", loggedInUsername));
            hideSubMenu();
        });
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
                // ⭐️ [추가] 마이페이지 열릴 때 친구 목록 새로고침
                fetchFriendsListForMyPage();
            }
            hideSubMenu();
        });
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            hideSubMenu();
        });
    }
    private void initializeSearch() {
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
    // 3. Real-time Location Sharing (Firebase)
    //==============================================================================================

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);

        currentGroupName = intent.getStringExtra("groupName");

        if (fabGroupMenu == null) {
            fabGroupMenu = findViewById(R.id.fab_group_menu);
            groupMenuContainer = findViewById(R.id.group_menu_container);
        }

        if (fabGroupMenu != null) {
            if (currentGroupId != null && currentGroupId != -1L) {
                fabGroupMenu.setVisibility(View.VISIBLE);
            } else {
                fabGroupMenu.setVisibility(View.GONE);
                if(groupMenuContainer != null) {
                    groupMenuContainer.setVisibility(View.GONE);
                }
            }
        }
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
    private void reapplyRulesAndRefreshMarkers() {
        Log.d(TAG, "reapplyRulesAndRefreshMarkers: 상호 규칙 기반 마커 재적용 시작.");
        if (memberLocationsCache != null) {
            List<LocationResponse> locationsToDisplay = new ArrayList<>();
            for (LocationResponse location : memberLocationsCache.values()) {
                Long sharerId = location.getUserId();
                if (location.getUserName().equals(loggedInUsername)) continue;
                if (sharerId != null && sharerId != -1L) {
                    boolean isAllowedBySharer = incomingSharingRules.getOrDefault(sharerId, false);
                    boolean isAllowedByMe = outgoingSharingStatus.getOrDefault(sharerId, false);
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
            updateMemberMarkers(locationsToDisplay);
        } else {
            Log.w(TAG, "reapplyRulesAndRefreshMarkers: 캐시된 위치 데이터가 없어 강제 갱신을 건너뜐.");
        }
    }

    private void fetchLoggedInUserId() {
        Call<Map<String, Long>> call = userApiService.getUserIdByUsername(loggedInUsername);
        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Long>> call, @NonNull Response<Map<String, Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Long userId = response.body().get("userId");
                    if (userId != null && userId != -1L) {
                        loggedInUserId = userId;
                        Log.d(TAG, "사용자 ID 획득 성공: " + loggedInUserId);

                        // --- ⭐️ [MERGE] 동시접속 제어 리스너 시작 ---
                        startActiveSessionListener();
                        // --- ⭐️ [MERGE] 끝 ---

                        // ⭐️ [추가] ID 로드 성공 후 친구 목록 로드 시작 (최초 로드)
                        fetchFriendsListForMyPage();
                        // ⭐️ [추가 끝]

                        if (currentGroupId != -1L) {
                            startLocationSharing();
                        }
                        return;
                    }
                    reapplyRulesAndRefreshMarkers();
                }
                Log.e(TAG, "❌ 사용자 ID 획득 실패. 응답 코드: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Long>> call, @NonNull Throwable t) {
                Log.e(TAG, "사용자 ID 네트워크 오류", t);
            }
        });
    }


    private void startLocationSharing() {
        locationUpdateHandler.removeCallbacksAndMessages(null);
        if (loggedInUserId == -1L) {
            Log.w(TAG, "startLocationSharing: UserID 로드 대기 중. 위치 공유 시작 중단.");
            return;
        }
        startFirebaseRulesListener();
        startMyLocationMarkerListener();
        startDestinationListener();
        Log.d(TAG, "startLocationSharing: 위치 공유 프로세스 시작. 업데이트 주기=" + LOCATION_UPDATE_INTERVAL + "ms");
        locationUpdateRunnable = () -> {

            // ⭐️ [TMap 수정] TMap 시뮬레이션 중이 아닐 때만 실제 위치 업데이트
            if (locationSource != null && animationHandler == null && !isSimulationRunning) {
                Location lastKnownLocation = locationSource.getLastLocation();
                if (lastKnownLocation != null) {
                    Log.d(TAG, "Location Update: 위치 획득 성공. Latitude=" + lastKnownLocation.getLatitude());
                    updateMyLocation(lastKnownLocation);
                } else {
                    Log.w(TAG, "Location Update: LocationSource에서 마지막 위치 정보를 가져올 수 없습니다. GPS 신호 대기 중.");
                }
            } else if (animationHandler != null) {
                Log.d(TAG, "Location Update: 모의(Mock) 이동 중이므로 실제 위치 업데이트는 건너킵니다.");
            }
            locationUpdateHandler.postDelayed(locationUpdateRunnable, LOCATION_UPDATE_INTERVAL);
        };
        locationUpdateHandler.post(locationUpdateRunnable);
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
                reapplyRulesAndRefreshMarkers();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Firebase 리스너 취소 오류 (🚨보안 규칙 확인 요망)", error.toException());
            }
        };
        // ⭐️ [오타 수정] addValueEventListener로 정정
        groupPathRef.addValueEventListener(memberLocationListener);
    }
    private void updateMyLocation(Location location) {
        if (currentGroupId == -1L || location == null || loggedInUsername == null || loggedInUserId == -1L) {
            Log.d(TAG, "updateMyLocation: 위치 업데이트 중단. GroupID=" + currentGroupId + ", Username=" + loggedInUsername + " (유효하지 않음)");
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
            Long userId = location.getUserId();
            if (userId == null || userId == -1L) {
                Log.w(TAG, "updateMemberMarkers: UserID가 없어 이미지 로드 건너뜐 -> " + username);
                continue;
            }
            updatedUsernames.add(username);
            LatLng memberPosition = new LatLng(location.getLatitude(), location.getLongitude());
            Marker marker = memberMarkers.get(username);
            if (marker == null) {
                marker = new Marker();
                marker.setCaptionText(username);
                memberMarkers.put(username, marker);
                Log.d(TAG, "updateMemberMarkers: 새 멤버 마커 추가 -> " + username);
            }
            marker.setPosition(memberPosition);
            marker.setMap(naverMap);
            fetchAndApplyMemberProfile(userId, marker);
        }
        boolean markerRemoved = false;
        Iterator<Map.Entry<String, Marker>> iterator = memberMarkers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            String username = entry.getKey();
            if (!updatedUsernames.contains(username)) {
                entry.getValue().setMap(null);
                Log.d(TAG, "updateMemberMarkers: 마커 UI 제거 완료 -> Name: " + username);
                iterator.remove();
                Log.d(TAG, "updateMemberMarkers: 최종 정리 맵에서 제거 -> Name: " + username);
                markerRemoved = true;
            }
        }
        if (naverMap != null && markerRemoved) {
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(naverMap.getCameraPosition().target);
            naverMap.moveCamera(cameraUpdate);
            Log.d(TAG, "updateMemberMarkers: 마커 제거 완료 후 지도 뷰 강제 갱신 시도 완료.");
        }
    }

    //==============================================================================================
    // 4. Mock Movement (TMap 시뮬레이션으로 대체) & Destination Selection
    //==============================================================================================

    private void loadWeatherData() {
        LatLng defaultLocation = new LatLng(37.5665, 126.9780);
        updateWeatherWidget(defaultLocation);
    }

    // ⭐️ [TMap 대체] startMockMovement 메서드
    // ⭐️ [TMap 대체] startMockMovement 메서드
    private void startMockMovement() {
        if (isSimulationRunning) {
            // --- 시뮬레이션 중지 ---
            isSimulationRunning = false;
            if (animationHandler != null) {
                animationHandler.removeCallbacks(animationRunnable);
                animationHandler = null;
            }
            for (PathOverlay p : pathOverlays) p.setMap(null);
            pathOverlays.clear();

            Toast.makeText(this, "TMap 시뮬레이션을 중지합니다.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "startMockMovement: TMap 시뮬레이션 중지됨.");
            return;
        }

        if (naverMap == null) {
            Toast.makeText(this, "지도가 준비되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ⭐️ [수정된 로직 시작] 현재 위치를 가져옵니다.
        Location lastKnownLocation = locationSource.getLastLocation();
        if (lastKnownLocation == null) {
            Toast.makeText(this, "현재 위치 정보를 가져올 수 없습니다. GPS 신호를 확인해주세요.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "startMockMovement: FusedLocationSource에서 위치를 가져오지 못했습니다.");
            return;
        }

        // ⭐️ 현재 위치를 출발지로 설정
        startLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        // ⭐️ 도착지는 유한대학교로 유지 (37.48723, 126.82056)

        // --- 시뮬레이션 시작 ---
        isSimulationRunning = true;

        Toast.makeText(this, "TMap API 가상 이동 시작 (현재 위치->유한대학교)", Toast.LENGTH_LONG).show();
        Log.d(TAG, "startMockMovement: TMap API 가상 이동 시작. " + startLatLng.latitude + " -> " + endLatLng.latitude);

        // TMap API로 경로 요청
        double totalDistance = calculateDistance(startLatLng, endLatLng);
        requestTMapWalkSegmentForSimulation(startLatLng, endLatLng, totalDistance);
    }
    // ⭐️ [TMap 대체 끝]

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
    // 5. UI Features (Menus, Search, Weather, Profile, Friends List)
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
            float tx = (float) (Math.cos(rad) * radiusPx * 1.2);
            float ty = (float) (Math.sin(rad) * radiusPx * -1.2);
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

    // --- ⭐️ [MERGE] 프로필 사진 로직 시작 ---

    private void initializeGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(ivProfile);
                            uploadImageToServer(selectedImageUri);
                        }
                    }
                }
        );
    }
    private void bindMyPageHeader() {
        TextView tvUsername = findViewById(R.id.tv_username);
        TextView tvEmail = findViewById(R.id.tv_email); // ⭐️ 이 줄이 살아 있어야 합니다.
        ImageView ivProfile = findViewById(R.id.iv_profile);

        if (tvUsername != null) tvUsername.setText(loggedInUsername != null ? loggedInUsername : "Guest");

        // 이메일 초기 로드 로직 (이전에 수정한 로직)
        // ⭐️ 이메일 로드 전에 tvEmail 변수가 선언되어 있어야 오류가 사라집니다.
        String savedEmail = getSharedPreferences("user_info", MODE_PRIVATE).getString("email", "로딩 중...");
        if (tvEmail != null) tvEmail.setText(savedEmail);

        loadProfileImage();
        ivProfile.setOnClickListener(v -> {
            showProfileImageOptions();
        });
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            logout();
        });
    }
    private void showProfileImageOptions() {
        final CharSequence[] options = {"기본 프로필로 설정", "사진 선택", "취소"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("프로필 사진 변경");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("기본 프로필로 설정")) {
                setProfileToDefault();
            } else if (options[item].equals("사진 선택")) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } else if (options[item].equals("취소")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    private void loadProfileImage() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        String imageUrl = prefs.getString("profileImageUrl", null);
        Log.d(TAG, "loadProfileImage: Loaded URL from Prefs: " + (imageUrl != null ? imageUrl : "null"));
        if (ivProfile == null) {
            ivProfile = findViewById(R.id.iv_profile);
        }
        Object loadTarget = null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("content://")) {
                loadTarget = Uri.parse(imageUrl);
            } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                loadTarget = imageUrl;
            } else {
                String baseUrl = ApiClient.getBaseUrl();
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                loadTarget = baseUrl + imageUrl;
            }
            Log.d(TAG, "loadProfileImage: Final Load Target: " + loadTarget.toString());
            Glide.with(this)
                    .load(loadTarget)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_person);
        }
        updateMyLocationMarkerIcon(imageUrl);
    }

    private void setProfileToDefault() {
        Call<Map<String, Object>> call = userApiService.setDefaultProfileImage();

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.d(TAG, "Default Profile Response Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Default Profile Set SUCCESS. Code: " + response.code());
                    Toast.makeText(MapsActivity.this, "기본 프로필로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("user_info", MODE_PRIVATE).edit()
                            .remove("profileImageUrl").apply();
                    ivProfile.setImageResource(R.drawable.ic_person);
                    updateMyLocationMarkerIcon(null);
                } else {
                    String errorMsg = "기본 프로필 변경 실패. 응답 코드: " + response.code();
                    Log.e(TAG, "Default Profile Set FAILED: " + errorMsg);
                    Toast.makeText(MapsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MapsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadImageToServer(Uri imageUri) {
        File file = createCacheFileFromUri(imageUri);
        if (file == null) {
            Toast.makeText(this, "파일을 변환하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(imageUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        Call<Map<String, Object>> call = userApiService.uploadProfileImage(body);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.d(TAG, "Upload Response Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Upload SUCCESS Response Body: " + response.body().toString());
                    String newImageUrl = (String) response.body().get("profileImageUrl");
                    if (newImageUrl != null && !newImageUrl.trim().isEmpty()) {
                        Toast.makeText(MapsActivity.this, "프로필이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                        prefs.edit().putString("profileImageUrl", newImageUrl).apply();
                        loadProfileImage();
                    } else {
                        Log.e(TAG, "업로드 성공 (HTTP 200) 했으나 'profileImageUrl' 필드 누락.");
                        Toast.makeText(MapsActivity.this, "프로필 변경 성공, URL 처리 오류.", Toast.LENGTH_LONG).show();
                        loadProfileImage();
                    }
                } else {
                    Log.e(TAG, "업로드 실패. HTTP 오류 코드: " + response.code());
                    Toast.makeText(MapsActivity.this, "업로드에 실패했습니다. (HTTP " + response.code() + ")", Toast.LENGTH_LONG).show();
                    loadProfileImage();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류", t);
                Toast.makeText(MapsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                loadProfileImage();
            }
        });
    }

    private File createCacheFileFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            File tempFile = new File(getCacheDir(), "temp_profile_image.jpg");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } catch (Exception e) {
            Log.e("FileUtil", "Failed to create cache file from Uri", e);
            return null;
        }
    }
    // --- ⭐️ [MERGE] 프로필 사진 로직 끝 ---

    // --- ⭐️ [추가] 마이페이지 친구 목록 로직 시작 ---

    private void initializeMyPageFriendsList() {
        // ⭐️ R.id.rv_mypage_friends_list는 my_page_drawer.xml에 추가된 ID입니다.
        rvMyPageFriends = findViewById(R.id.rv_mypage_friends_list);
        rvMyPageFriends.setLayoutManager(new LinearLayoutManager(this));

        // FriendAdapter 초기화 시 MapsActivity 자신을 OnDeleteClickListener로 전달
        friendAdapter = new FriendAdapter(new ArrayList<>(), this);
        rvMyPageFriends.setAdapter(friendAdapter);

        // 친구 목록 불러오기는 ID 로드 후 또는 드로어 열릴 때 호출됩니다.
    }

    @Override
    public void onDeleteClick(User friend) {
        // 🚨 [수정] 이 로그가 찍히는지 확인해야 합니다! 🚨
        Log.d(TAG, "🔴 FRIEND DELETE CLICK HANDLED IN MAPS ACTIVITY! Attempting deletion...");

        // 1. 친구 ID 유효성 검사
        if (friend.getId() == null || friend.getId() == -1L) {
            Toast.makeText(this, "친구 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onDeleteClick: 삭제할 친구의 ID가 유효하지 않습니다.");
            return;
        }

        Log.d(TAG, "친구 삭제 요청 시작: ID=" + friend.getId() + ", Username=" + friend.getUsername());

        // 2. FriendApiService를 사용하여 삭제 API 호출
        friendApiService.deleteFriend(friend.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    // HTTP 200 또는 204 (성공) 응답
                    Toast.makeText(MapsActivity.this, friend.getUsername() + " 친구가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "친구 삭제 성공. 응답 코드: " + response.code());

                    // 3. 삭제 성공 후, 친구 목록을 새로고침하여 화면을 갱신
                    fetchFriendsListForMyPage();
                } else {
                    // 4xx 또는 5xx 오류 처리
                    Toast.makeText(MapsActivity.this, "친구 삭제에 실패했습니다. (코드: " + response.code() + ")", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "친구 삭제 실패. 응답 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // 네트워크 연결 오류 처리
                Toast.makeText(MapsActivity.this, "네트워크 오류로 삭제에 실패했습니다.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "친구 삭제 네트워크 오류", t);
            }
        });
    }

    private void fetchFriendsListForMyPage() {
        if (loggedInUserId == -1L) {
            // User ID 로드 대기
            Log.w(TAG, "fetchFriendsListForMyPage: User ID가 유효하지 않아 친구 목록 로드 중단. ID 로드 대기 중.");
            return;
        }

        // FriendApiService 정의에 따라 인수를 제거했습니다.
        Call<List<FriendResponse>> call = friendApiService.getFriends(); // ⭐️ List<User> -> List<FriendResponse>로 변경

        call.enqueue(new Callback<List<FriendResponse>>() { // ⭐️ Callback<List<User>> -> Callback<List<FriendResponse>>로 변경
            @Override
            public void onResponse(@NonNull Call<List<FriendResponse>> call, @NonNull Response<List<FriendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<FriendResponse> friendResponses = response.body();

                    // ⭐️ 핵심: FriendResponse를 User 모델로 변환하는 로직 추가 ⭐️
                    List<User> friends = new ArrayList<>(); // 변환된 User 리스트
                    for (FriendResponse fr : friendResponses) {
                        User user = new User();

                        // FriendResponse의 필드를 User 모델에 매핑
                        user.setId(fr.getFriendId());

                        // Adapter가 Username 또는 Nickname을 찾으므로, 여기에 친구의 이름을 할당합니다.
                        user.setUsername(fr.getFriendUsername());
                        // user.setNickname(fr.getFriendUsername()); // User 모델에 setNickname이 있다면 이것을 사용하는 것이 가장 좋습니다.
                        user.setProfileImageUrl(fr.getProfileImageUrl());
                        friends.add(user);
                    }
                    // ⭐️ 변환 로직 끝 ⭐️

                    friendAdapter.setFriends(friends); // 변환된 List<User>를 전달
                    Log.d(TAG, "마이페이지 친구 목록 로드 성공. 개수: " + friends.size());
                } else {
                    Log.e(TAG, "마이페이지 친구 목록 로드 실패. 응답 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FriendResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "마이페이지 친구 목록 네트워크 오류", t);
            }
        });
    }

    // ⭐️ [추가] 서버에서 이메일을 가져와 헤더를 업데이트하는 메서드 ⭐️
    /**
     * 서버에서 사용자 이메일을 가져와 마이페이지 헤더를 업데이트합니다.
     */
    private void fetchUserEmailAndBindHeader() {
        if (userApiService == null) {
            Log.e(TAG, "fetchUserEmail: userApiService가 null입니다.");
            return;
        }

        Call<Map<String, String>> call = userApiService.getMyEmail();

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, String> responseBody = response.body();
                    String email = null;

                    // ⭐️ 1. 서버 응답에서 이메일 키를 유연하게 확인 (가장 유력한 문제 해결) ⭐️
                    if (responseBody.containsKey("email")) {
                        email = responseBody.get("email");
                    } else if (responseBody.containsKey("userEmail")) { // 혹시 userEmail로 보낸다면
                        email = responseBody.get("userEmail");
                    } else {
                        Log.w(TAG, "이메일 로드 실패: 응답 본문에 'email' 또는 'userEmail' 키가 없습니다.");
                    }

                    // 2. 이메일 UI 업데이트 및 SharedPreferences 저장
                    if (email != null && !email.isEmpty()) {
                        getSharedPreferences("user_info", MODE_PRIVATE).edit().putString("email", email).apply();

                        TextView tvEmail = findViewById(R.id.tv_email);
                        if (tvEmail != null) {
                            tvEmail.setText(email);
                        }
                        Log.d(TAG, "✅ 사용자 이메일 로드 및 업데이트 성공: " + email);
                    }
                } else {
                    Log.e(TAG, "❌ 사용자 이메일 로드 실패. 응답 코드: " + response.code());

                    try {
                        // 🚨 [수정] 서버가 잘못된 응답을 보낼 때 어떤 데이터를 보냈는지 확인
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No Error Body";
                        Log.e(TAG, "❌ 이메일 로드 실패 Error Body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error Body 로드 중 오류 발생", e);
                    }

                    // ⭐️ 3. 실패 시, 잘못된 값 대신 로그인 이름 기반의 기본값 설정 ⭐️
                    // (기존에 잘못 저장된 "12@example.com" 대신 로그인 이름으로 기본값을 만듭니다.)
                    String defaultEmail = loggedInUsername + "@example.com";

                    TextView tvEmail = findViewById(R.id.tv_email);
                    if (tvEmail != null) {
                        tvEmail.setText(defaultEmail);
                    }
                    getSharedPreferences("user_info", MODE_PRIVATE).edit().putString("email", defaultEmail).apply();

                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Log.e(TAG, "사용자 이메일 네트워크 오류", t);
                // 네트워크 오류 시에도 기본값 설정을 유지합니다.
            }
        });
    }
    // --- ⭐️ [추가] 마이페이지 친구 목록 로직 끝 ---


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
                    reader.close();
                    List<SearchResult> results = parseNaverSearchResults(new JSONObject(response.toString()));
                    for (SearchResult result : results) {
                        String imageUrl = fetchFirstImageUrl(result.getTitle());
                        result.setImageUrl(imageUrl);
                    }
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
    private String fetchFirstImageUrl(String query) {
        try {
            String simpleQuery = query.split(" ")[0].replaceAll("<[^>]*>", "");
            String encodedQuery = java.net.URLEncoder.encode(simpleQuery, "UTF-8");
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
                reader.close();
                JSONObject json = new JSONObject(response.toString());
                JSONArray items = json.getJSONArray("items");
                if (items.length() > 0) {
                    return items.getJSONObject(0).optString("thumbnail", "");
                }
            }
            return "";
        } catch (Exception e) {
            Log.e("ImageSearchAPI", "Failed to fetch image for: " + query, e);
            return "";
        }
    }
    private List<SearchResult> parseNaverSearchResults(JSONObject json) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        JSONArray items = json.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String title = item.getString("title").replaceAll("<[^>]*>", "");
            String address = item.optString("roadAddress", item.optString("address", ""));
            String category = item.optString("category", "정보 없음");
            String description = item.optString("description", "");
            double longitude = item.getDouble("mapx") / 1e7;
            double latitude = item.getDouble("mapy") / 1e7;
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
            default: return R.drawable.ic_weather_clear;
        }
    }


    //==============================================================================================
    // 6. Permissions & Utilities
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
        int mapTypeOrdinal = prefs.getInt("map_type", NaverMap.MapType.Basic.ordinal());
        NaverMap.MapType mapType;
        try {
            mapType = NaverMap.MapType.values()[mapTypeOrdinal];
        } catch (Exception e) {
            mapType = NaverMap.MapType.Basic;
        }
        if (naverMap != null) {
            naverMap.setMapType(mapType);
        }
    }

    //==============================================================================================
    // 7. Activity Lifecycle Callbacks
    //==============================================================================================

    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        applyMapTypeSetting();

        // ⭐️ fetchFriendsListForMyPage() 호출을 제거했습니다. ⭐️

        if (currentGroupId != -1L) {
            Log.d(TAG, "onResume: 유효한 그룹 ID(" + currentGroupId + ")가 있어 위치 공유 재시작.");
            startLocationSharing();
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

        // ⭐️ [TMap 수정] TMap 시뮬레이션 중지 로직 추가
        if (isSimulationRunning) {
            isSimulationRunning = false;
            if (animationHandler != null) {
                animationHandler.removeCallbacks(animationRunnable);
                animationHandler = null;
            }
            for (PathOverlay p : pathOverlays) p.setMap(null);
            pathOverlays.clear();
            Log.d(TAG, "onPause: TMap 시뮬레이션 중지.");
        }
        // ⭐️ [TMap 수정] 기존 부산행 애니메이션 중지 로직 유지
        else if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationHandler = null;
        }

        stopMapRefreshTimer();
        if (currentGroupId != -1L) {
            if (memberLocationListener != null) {
                firebaseDatabase.child(String.valueOf(currentGroupId)).removeEventListener(memberLocationListener);
                Log.d(TAG, "onPause: Firebase 위치 리스너 제거 완료.");
            }
            if (rulesRef != null && rulesListener != null) {
                rulesRef.removeEventListener(rulesListener);
                Log.d(TAG, "onPause: Firebase 규칙 리스너 제거 완료.");
            }
            if (myMarkerStatusRef != null && myMarkerStatusListener != null) {
                myMarkerStatusRef.removeEventListener(myMarkerStatusListener);
                Log.d(TAG, "onPause: 내 마커 상태 리스너 제거 완료.");
            }
            stopDestinationListener();
        }
        stopActiveSessionListener();
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
        stopActiveSessionListener();
    }
    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }


    // 🚀 --- [2.5: 목적지 마커용 새 메서드 4개] ---

    private void startDestinationListener() {
        if (naverMap == null || currentGroupId == -1L) {
            Log.w(TAG, "startDestinationListener: NaverMap이 null이거나 Group ID가 유효하지 않아 중단.");
            return;
        }
        stopDestinationListener();
        destinationRef = FirebaseDatabase.getInstance()
                .getReference("group_destinations")
                .child(String.valueOf(currentGroupId))
                .child("destination");
        Log.d(TAG, "Firebase 목적지 리스너 등록 시도. Path: " + destinationRef.toString());
        destinationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);
                    String name = snapshot.child("name").getValue(String.class);
                    if (latitude != null && longitude != null && name != null &&
                            Double.isFinite(latitude) && Double.isFinite(longitude)) {
                        LatLng destinationLatLng = new LatLng(latitude, longitude);
                        updateDestinationMarker(destinationLatLng, name);
                        Log.d(TAG, "목적지 정보 수신: " + name);
                    } else {
                        removeDestinationMarker();
                        Log.w(TAG, "수신된 목적지 데이터가 유효하지 않습니다.");
                    }
                } else {
                    removeDestinationMarker();
                    Log.d(TAG, "Firebase에 해당 그룹의 목적지 정보가 없습니다.");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase 목적지 리스너 취소됨", error.toException());
                removeDestinationMarker();
            }
        };
        // ⭐️ [오타 수정] addValueEventListener로 정정
        destinationRef.addValueEventListener(destinationListener);
    }
    private void removeDestinationMarker() {
        if (destinationMarker != null) {
            destinationMarker.setMap(null);
            destinationMarker = null;
            Log.d(TAG, "목적지 마커 제거 완료.");
        }
    }
    private void updateDestinationMarker(LatLng position, String caption) {
        if (naverMap == null) {
            Log.w(TAG, "updateDestinationMarker: NaverMap이 null이라 마커를 업데이트할 수 없습니다.");
            return;
        }
        if (destinationMarker == null) {
            destinationMarker = new Marker();
            destinationMarker.setWidth(Marker.SIZE_AUTO);
            destinationMarker.setHeight(Marker.SIZE_AUTO);
            destinationMarker.setZIndex(50);
            Log.d(TAG, "새 목적지 마커 생성.");
        }
        destinationMarker.setPosition(position);
        destinationMarker.setCaptionText("🚩 도착지: " + caption);
        destinationMarker.setMap(naverMap);
    }
    private void stopDestinationListener() {
        if (destinationRef != null && destinationListener != null) {
            destinationRef.removeEventListener(destinationListener);
            Log.d(TAG, "Firebase 목적지 리스너 제거 완료.");
        }
        destinationRef = null;
        destinationListener = null;
    }
    // 🚀 --- [2.5 끝] ---


    // 🚀 --- [MERGE] 프로필 사진 마커 업데이트 메소드 ---

    private void updateMyLocationMarkerIcon(String imageUrl) {
        if (naverMap == null || myLocationMarker == null) return;
        executor.execute(() -> {
            try {
                Object loadTarget = null;
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    if (imageUrl.startsWith("content://")) {
                        loadTarget = Uri.parse(imageUrl);
                    } else {
                        String baseUrl = ApiClient.getBaseUrl();
                        if (baseUrl.endsWith("/")) {
                            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                        }
                        loadTarget = baseUrl + imageUrl;
                    }
                }
                Log.d(TAG, "updateMyLocationMarkerIcon - Load Target: " + (loadTarget != null ? loadTarget.toString() : "BASIC_ICON"));
                final Object finalLoadTarget = loadTarget;
                Bitmap bitmap = null;
                if (finalLoadTarget != null) {
                    bitmap = Glide.with(MapsActivity.this)
                            .asBitmap()
                            .load(finalLoadTarget)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .override(100, 100)
                            .submit()
                            .get();
                }
                final Bitmap finalBitmap = bitmap;
                handler.post(() -> {
                    if (naverMap != null && myLocationMarker != null) {
                        if (finalBitmap != null) {
                            Bitmap borderedBitmap = addBorderToCircularBitmap(finalBitmap, MARKER_BORDER_WIDTH_PX, MARKER_BORDER_COLOR);
                            myLocationMarker.setIcon(OverlayImage.fromBitmap(borderedBitmap));
                            Log.d(TAG, "✅ 내 마커 아이콘이 프로필 사진(테두리 포함)으로 업데이트됨.");
                        } else {
                            myLocationMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_person));
                            Log.d(TAG, "✅ 내 마커 아이콘이 기본 아이콘으로 재설정됨.");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "마커 아이콘 업데이트 실패", e);
                handler.post(() -> {
                    if (myLocationMarker != null) {
                        myLocationMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_person));
                    }
                });
            }
        });
    }

    private void fetchAndApplyMemberProfile(Long userId, final Marker marker) {
        Call<Map<String, String>> call = userApiService.getProfileImageUrl(userId);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().get("profileImageUrl");
                    loadBitmapForMarker(imageUrl, marker);
                } else {
                    Log.e(TAG, "팀원 프로필 URL 획득 실패: ID=" + userId + ", Code=" + response.code());
                    loadBitmapForMarker(null, marker);
                }
            }
            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e(TAG, "팀원 프로필 URL 네트워크 오류: ID=" + userId, t);
                loadBitmapForMarker(null, marker);
            }
        });
    }

    private void loadBitmapForMarker(String imageUrl, final Marker marker) {
        executor.execute(() -> {
            try {
                Object loadTarget = null;
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    if (imageUrl.startsWith("content://")) {
                        loadTarget = Uri.parse(imageUrl);
                    } else {
                        String baseUrl = ApiClient.getBaseUrl();
                        if (baseUrl.endsWith("/")) {
                            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                        }
                        loadTarget = baseUrl + imageUrl;
                    }
                }
                final Object finalLoadTarget = loadTarget;
                Bitmap bitmap = null;
                if (finalLoadTarget != null) {
                    bitmap = Glide.with(MapsActivity.this)
                            .asBitmap()
                            .load(finalLoadTarget)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .override(100, 100)
                            .submit()
                            .get();
                }
                final Bitmap finalBitmap = bitmap;
                handler.post(() -> {
                    if (naverMap != null && marker.getMap() == naverMap) {
                        if (finalBitmap != null) {
                            Bitmap borderedBitmap = addBorderToCircularBitmap(finalBitmap, MARKER_BORDER_WIDTH_PX, MARKER_BORDER_COLOR);
                            marker.setIcon(OverlayImage.fromBitmap(borderedBitmap));
                        } else {
                            marker.setIcon(OverlayImage.fromResource(R.drawable.marker_circle_red));
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "마커 아이콘 업데이트 실패", e);
                handler.post(() -> {
                    if (marker.getMap() == naverMap) {
                        marker.setIcon(OverlayImage.fromResource(R.drawable.marker_circle_red));
                    }
                });
            }
        });
    }

    // 🚀 --- [MERGE] 프로필 마커 로직 끝 ---


    // --- ⭐️ [MERGE] 동시접속 제어 메소드 ---

    private void startActiveSessionListener() {
        if (loggedInUserId == -1L) {
            Log.w(TAG, "startActiveSessionListener: UserID가 없어 세션 감지를 시작할 수 없습니다.");
            return;
        }
        if (tokenManager == null) {
            tokenManager = new TokenManager();
        }
        activeSessionRef = FirebaseDatabase.getInstance()
                .getReference("user_sessions")
                .child(String.valueOf(loggedInUserId))
                .child("activeToken");

        if (activeSessionListener != null) {
            activeSessionRef.removeEventListener(activeSessionListener);
        }
        Log.d(TAG, "startActiveSessionListener: 실시간 세션 감지 리스너 등록 시작.");

        activeSessionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isSessionListenerInitialized) {
                    isSessionListenerInitialized = true; // 플래그를 true로 설정
                    Log.d(TAG, "ActiveSessionListener: 리스너 초기화 완료. 첫 데이터 로드는 건너킵니다.");
                    return; // ◀◀◀ 비교 로직을 실행하지 않고 종료
                }

                String serverActiveToken = snapshot.getValue(String.class);
                String myToken = tokenManager.getAccessToken();

                if (serverActiveToken != null && myToken != null && !serverActiveToken.equals(myToken)) {
                    Log.w(TAG, "ActiveSessionListener: 동시 접속 감지! 강제 로그아웃을 실행합니다.");
                    Toast.makeText(MapsActivity.this, "다른 기기에서 로그인하여 로그아웃됩니다.", Toast.LENGTH_LONG).show();
                    performClientLogout();
                } else if (serverActiveToken == null && myToken != null) {
                    Log.w(TAG, "ActiveSessionListener: 서버에서 로그아웃 신호를 받았습니다. 강제 로그아웃을 실행합니다.");
                    Toast.makeText(MapsActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
                    performClientLogout();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "ActiveSessionListener: 세션 감시 리스너 실패", error.toException());
            }
        };
        // ⭐️ [오타 수정] addValueEventListener로 정정
        activeSessionRef.addValueEventListener(activeSessionListener);
    }
    private void stopActiveSessionListener() {
        if (activeSessionRef != null && activeSessionListener != null) {
            activeSessionRef.removeEventListener(activeSessionListener);
            activeSessionListener = null;
            activeSessionRef = null;
            isSessionListenerInitialized = false;
            Log.d(TAG, "stopActiveSessionListener: 실시간 세션 감지 리스너 제거 완료.");
        }
    }


    private void logout() {
        Call<Map<String, Object>> call = userApiService.logout();

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d("MapsActivity", "서버 로그아웃 성공");
                } else {
                    Log.w("MapsActivity", "서버 로그아웃 응답 실패: " + response.code());
                }
                performClientLogout();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("MapsActivity", "서버 로그아웃 요청 실패", t);
                performClientLogout();
            }
        });
    }

    private void performClientLogout() {
        tokenManager.deleteTokens();
        Toast.makeText(MapsActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private Bitmap addBorderToCircularBitmap(Bitmap srcBitmap, int borderWidthPx, int borderColor) {
        if (srcBitmap == null) return null;

        int srcDiameter = srcBitmap.getWidth();
        int newDiameter = srcDiameter + (borderWidthPx * 2);
        int radius = srcDiameter / 2;
        int newRadius = newDiameter / 2;
        int center = newDiameter / 2;

        Bitmap outputBitmap = Bitmap.createBitmap(newDiameter, newDiameter, Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);

        // 1. 테두리(바깥쪽 원)를 그립니다.
        Paint borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.FILL);
        borderPaint.setAntiAlias(true);
        canvas.drawCircle(center, center, newRadius, borderPaint);

        // 2. 원본 비트맵(안쪽 원)을 그립니다.
        Paint imagePaint = new Paint();
        imagePaint.setShader(new BitmapShader(srcBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        imagePaint.setAntiAlias(true);
        canvas.drawCircle(center, center, radius, imagePaint);

        return outputBitmap;
    }

    // ----------------------------------------------------------------
    // ⭐️ [TMap 추가] TMap 시뮬레이션 관련 메서드 4개
    // ----------------------------------------------------------------

    /**
     * TMap API를 호출하여 시뮬레이션을 위한 실제 보행 경로를 요청합니다.
     */
    private void requestTMapWalkSegmentForSimulation(LatLng start, LatLng end, double totalDistance) {
        new Thread(() -> {
            try {
                URL url = new URL("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("appKey", TMAP_API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                JSONObject body = new JSONObject();
                body.put("startX", start.longitude);
                body.put("startY", start.latitude);
                body.put("endX", end.longitude);
                body.put("endY", end.latitude);
                body.put("reqCoordType", "WGS84GEO");
                body.put("resCoordType", "WGS84GEO");
                body.put("startName", "출발지");
                body.put("endName", "도착지");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStreamReader isr = new InputStreamReader(conn.getInputStream());
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray features = json.getJSONArray("features");
                    List<LatLng> path = new ArrayList<>();

                    for (int i = 0; i < features.length(); i++) {
                        JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");
                        if ("LineString".equals(geometry.getString("type"))) {
                            JSONArray coords = geometry.getJSONArray("coordinates");
                            for (int j = 0; j < coords.length(); j++) {
                                JSONArray point = coords.getJSONArray(j);
                                path.add(new LatLng(point.getDouble(1), point.getDouble(0)));
                            }
                        }
                    }

                    if (path.size() > 2) {
                        runOnUiThread(() -> startPathSimulation(path, totalDistance));
                    } else {
                        Log.w(TAG, "TMap API 경로가 유효하지 않아 직선 경로로 대체합니다.");
                        runOnUiThread(() -> {
                            List<LatLng> straightPath = new ArrayList<>();
                            straightPath.add(start);
                            straightPath.add(end);
                            startPathSimulation(straightPath, totalDistance);
                        });
                    }
                } else {
                    Log.e(TAG, "TMap API 오류 (HTTP " + code + "). 직선 경로로 대체합니다.");
                    runOnUiThread(() -> {
                        List<LatLng> straightPath = new ArrayList<>();
                        straightPath.add(start);
                        straightPath.add(end);
                        startPathSimulation(straightPath, totalDistance);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "TMapSimulation 경로 요청 실패", e);
                runOnUiThread(() -> {
                    List<LatLng> straightPath = new ArrayList<>();
                    straightPath.add(start);
                    straightPath.add(end);
                    startPathSimulation(straightPath, totalDistance);
                });
            }
        }).start();
    }

    /**
     * TMap API로 받은 경로(path)를 따라 마커를 이동시키는 시뮬레이션을 시작합니다.
     */
    private void startPathSimulation(List<LatLng> path, double totalDistance) {
        if (path.size() < 2) {
            isSimulationRunning = false;
            return;
        }

        for (PathOverlay p : pathOverlays) p.setMap(null);
        pathOverlays.clear();

        PathOverlay pathOverlay = new PathOverlay();
        pathOverlay.setCoords(path);
        pathOverlay.setColor(0xFF00FF00); // TMap 시뮬레이션 경로는 초록색
        pathOverlay.setWidth(12);
        pathOverlay.setMap(naverMap);
        pathOverlays.add(pathOverlay);

        final int[] currentPathIndex = {0};
        final LatLng[] currentPosition = {new LatLng(path.get(0).latitude, path.get(0).longitude)};

        if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
        animationHandler = new Handler(Looper.getMainLooper());

        // 내 마커를 시작 위치로 이동시킵니다.
        if (myLocationMarker != null) {
            myLocationMarker.setPosition(currentPosition[0]);
            myLocationMarker.setMap(naverMap);
        }
        naverMap.moveCamera(CameraUpdate.scrollTo(currentPosition[0]).animate(CameraAnimation.Easing));

        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSimulationRunning) {
                    animationHandler = null;
                    return;
                }

                if (currentPathIndex[0] >= path.size() - 1) {
                    isSimulationRunning = false;
                    Toast.makeText(MapsActivity.this, "TMap 시뮬레이션이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                    animationHandler = null;

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        for (PathOverlay p : pathOverlays) p.setMap(null);
                        pathOverlays.clear();
                    }, 2000);
                    return;
                }

                LatLng nextPoint = path.get(currentPathIndex[0] + 1);
                double distanceToNext = calculateDistance(currentPosition[0], nextPoint);

                if (distanceToNext <= 2.0) {
                    currentPathIndex[0]++;
                    currentPosition[0] = new LatLng(nextPoint.latitude, nextPoint.longitude);
                } else {

                    double realWalkSpeedPerFrame = 1.0;

                    double moveDistance = Math.min(distanceToNext * 0.1, realWalkSpeedPerFrame);
                    double bearing = calculateBearing(currentPosition[0], nextPoint);
                    LatLng newPosition = calculateNewPosition(currentPosition[0], bearing, moveDistance);
                    currentPosition[0] = newPosition;
                }

                if (myLocationMarker != null) {
                    myLocationMarker.setPosition(currentPosition[0]);
                }

                Location mockLocation = new Location("TMapMockProvider");
                mockLocation.setLatitude(currentPosition[0].latitude);
                mockLocation.setLongitude(currentPosition[0].longitude);
                updateMyLocation(mockLocation);

                if (animationHandler != null) {
                    animationHandler.postDelayed(this, 10);
                }
            }
        };

        animationHandler.post(animationRunnable);
    }

    /**
     * 두 지점 간 거리 계산 (미터)
     */
    private double calculateDistance(LatLng point1, LatLng point2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double deltaLat = Math.toRadians(point2.latitude - point1.latitude);
        double deltaLng = Math.toRadians(point2.longitude - point1.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371000 * c; // 지구 반지름 (미터)
    }

    /**
     * 두 지점 간 방향 계산 (도)
     */
    private double calculateBearing(LatLng point1, LatLng point2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double dLon = Math.toRadians(point2.longitude - point1.longitude);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.atan2(y, x);

        // 라디안을 도로로 변환 (0~360)
        bearing = Math.toDegrees(bearing);
        if (bearing < 0) {
            bearing += 360;
        }
        return bearing;
    }

    /**
     * 새로운 위치 계산 (현재 위치, 방향, 거리)
     */
    private LatLng calculateNewPosition(LatLng current, double bearing, double distance) {
        double R = 6371000; // 지구 반지름 (미터)
        double lat1 = Math.toRadians(current.latitude);
        double lon1 = Math.toRadians(current.longitude);
        double brng = Math.toRadians(bearing);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / R) +
                Math.cos(lat1) * Math.sin(distance / R) * Math.cos(brng));

        double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(distance / R) * Math.cos(lat1),
                Math.cos(distance / R) - Math.sin(lat1) * Math.sin(lat2));

        // 경도 범위 조정 (-180 ~ 180)
        lon2 = (lon2 + 540) % 360 - 180;

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

}