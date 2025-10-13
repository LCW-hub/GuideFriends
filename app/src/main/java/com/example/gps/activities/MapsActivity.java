package com.example.gps.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.res.ColorStateList;
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
import com.example.gps.activities.Register_Login.LoginActivity;
import com.example.gps.adapters.SearchResultAdapter;
import com.example.gps.utils.TokenManager;
import com.example.gps.api.ApiClient;
import com.example.gps.dto.LocationResponse;
import com.example.gps.dto.UpdateLocationRequest;
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
import com.naver.maps.geometry.Tm128;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    // 기본 UI 및 지도 관련 변수
    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    // 검색 관련 UI 및 데이터 변수
    private EditText etSearch;
    private ImageView ivSearchIcon;
    private RecyclerView rvSearchResults;
    private Marker searchResultMarker = null;

    // 선택 모드인지 확인하는 플래그 추가
    private boolean isSelectionMode = false;
    private SearchResultAdapter searchResultAdapter;


    // 날씨 관련 UI 변수
    private ImageView ivWeatherIcon;
    private TextView tvTemperature;

    // 백그라운드 작업을 위한 ExecutorService 및 Handler (공용으로 사용)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 상수
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String OPENWEATHERMAP_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";
    private static final String NAVER_CLIENT_ID = "OAQnuwhbAL34Of8mlxve";
    private static final String NAVER_CLIENT_SECRET = "4roXQDJBpc";

    // 로그인된 사용자 이름을 저장할 변수
    private String loggedInUsername;
    
    // 서브 메뉴 상태 관리
    private boolean isSubMenuOpen = false;
    private static final float SUB_MENU_RADIUS_DP = 80f;

    // 실시간 공유를 위한 변수들
    private Long currentGroupId = -1L;
    private final Handler locationUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable locationUpdateRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10초
    private final HashMap<Long, Marker> memberMarkers = new HashMap<>();

    //==============================================================================================
    // 1. 액티비티 생명주기 및 기본 설정
    //==============================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        checkLocationPermission();

        if ("SELECT_DESTINATION".equals(getIntent().getStringExtra("PURPOSE"))) {
            isSelectionMode = true;
            // 선택 모드일 때 사용자에게 안내 메시지 표시
            Toast.makeText(this, "목적지로 설정할 장소를 검색 후 선택해주세요.", Toast.LENGTH_LONG).show();
        }



        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        ivWeatherIcon = findViewById(R.id.iv_weather_icon);
        tvTemperature = findViewById(R.id.tv_temperature);

        // 날씨 UI 요소 초기화
        ivWeatherIcon = findViewById(R.id.iv_weather_icon);
        tvTemperature = findViewById(R.id.tv_temperature);

        // 로그인 사용자 이름 가져오기
        loggedInUsername = getIntent().getStringExtra("username");

        initializeMap();
        initializeButtons();
        bindMyPageHeader();
        initializeSearch();
        initializeSubMenu();
    }

    private void bindMyPageHeader() {
        TextView tvUsername = findViewById(R.id.tv_username);
        TextView tvEmail = findViewById(R.id.tv_email);
        if (tvUsername != null) {
            tvUsername.setText(loggedInUsername != null ? loggedInUsername : "Guest");
        }
        if (tvEmail != null) {
            String email = getSharedPreferences("user_info", MODE_PRIVATE).getString("email", "user@example.com");
            tvEmail.setText(email);
        }
        View btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                TokenManager tokenManager = new TokenManager(this);
                tokenManager.deleteToken();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    // 서브 메뉴 토글 메서드
    private void toggleSubMenu() {
        if (isSubMenuOpen) {
            hideSubMenu();
        } else {
            showSubMenu();
        }
    }

    // 서브 메뉴 표시
    private void showSubMenu() {
        isSubMenuOpen = true;

        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        FloatingActionButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        FloatingActionButton btnMyPage = findViewById(R.id.btnMyPage);
        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);

        // 메인 버튼 스타일 변경
        btnMainMenu.setImageResource(R.drawable.ic_close);
        btnMainMenu.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red)));

        FloatingActionButton[] targets = new FloatingActionButton[]{btnFriends, btnCreateGroup, btnMyGroups, btnMyPage, btnSettings};

        // 버튼 초기화 및 보이기
        for (FloatingActionButton b : targets) {
            b.setVisibility(View.VISIBLE);
            b.setAlpha(0f);
            b.setTranslationX(0f);
            b.setTranslationY(0f);
        }

        // 펼쳐질 반경
        float radiusPx = dpToPx(SUB_MENU_RADIUS_DP);

        // ★★★ 핵심 수정 부분: 각 버튼의 각도를 직접 지정 ★★★
        // targets 배열 순서대로 각도를 설정합니다. (btnFriends, btnCreateGroup, btnMyGroups, btnMyPage, btnSettings)
        float[] angles = new float[]{180f, 135f, 90f, 45f, 0f}; // 부채꼴 모양으로 배치 (왼쪽에서 오른쪽으로)
        float[] distancesDp = new float[]{100f, 100f, 100f, 100f, 100f}; // 모든 버튼이 같은 거리로 배치

        long delay = 0L;
        for (int i = 0; i < targets.length; i++) {
            // 지정된 각도를 라디안으로 변환
            double rad = Math.toRadians(angles[i]);
            radiusPx = dpToPx(distancesDp[i]);

            // x, y 좌표 계산
            float tx = (float) (Math.cos(rad) * radiusPx);
            float ty = (float) (Math.sin(rad) * radiusPx);

            targets[i].animate()
                    .translationX(tx)
                    .translationY(ty * -1f) // Y축 반전
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(delay)
                    .start();
            delay += 50L;
        }
    }

    // 서브 메뉴 숨기기
    private void hideSubMenu() {
        isSubMenuOpen = false;

        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        FloatingActionButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        FloatingActionButton btnMyPage = findViewById(R.id.btnMyPage);
        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);

        // 메인 버튼 스타일 원복 (빨강 -> 초록, X -> 메뉴)
        btnMainMenu.setImageResource(R.drawable.ic_menu);
        btnMainMenu.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));

        FloatingActionButton[] targets = new FloatingActionButton[]{btnFriends, btnCreateGroup, btnMyGroups, btnMyPage, btnSettings};

        long delay = 0L;
        // 역순으로 애니메이션을 적용하면 더 자연스럽습니다.
        for (int i = targets.length - 1; i >= 0; i--) {
            int finalI = i;
            targets[i].animate()
                    .translationX(0f)
                    .translationY(0f)
                    .alpha(0f)
                    .setDuration(250)
                    .setStartDelay(delay)
                    .withEndAction(() -> targets[finalI].setVisibility(View.GONE)) // 애니메이션 종료 후 숨김
                    .start();
            delay += 40L;
        }
    }

    // 서브 메뉴 초기화
    private void initializeSubMenu() {
        FloatingActionButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        FloatingActionButton btnMyPage = findViewById(R.id.btnMyPage);
        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
        
        // 초기 상태: 숨김, 투명도 0
        btnFriends.setVisibility(View.GONE);
        btnCreateGroup.setVisibility(View.GONE);
        btnMyGroups.setVisibility(View.GONE);
        btnMyPage.setVisibility(View.GONE);
        btnSettings.setVisibility(View.GONE);
        
        btnFriends.setAlpha(0f);
        btnCreateGroup.setAlpha(0f);
        btnMyGroups.setAlpha(0f);
        btnMyPage.setAlpha(0f);
        btnSettings.setAlpha(0f);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    // 설정된 지도 타입 적용
    private void applyMapTypeSetting() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isSatelliteMode = prefs.getBoolean("satellite_mode", false);
        
        if (naverMap != null) {
            if (isSatelliteMode) {
                naverMap.setMapType(NaverMap.MapType.Satellite);
            } else {
                naverMap.setMapType(NaverMap.MapType.Basic);
            }
        }
    }

    private void initializeSearch() {
        etSearch = findViewById(R.id.et_search);
        ivSearchIcon = findViewById(R.id.iv_search_icon);
        rvSearchResults = findViewById(R.id.rv_search_results);

        searchResultAdapter = new SearchResultAdapter();
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchResultAdapter);

        ivSearchIcon.setOnClickListener(v -> performSearch());

        // ## 4. 검색 결과 아이템 클릭 리스너 수정 ##
        // isSelectionMode 값에 따라 다르게 동작하도록 이 부분을 수정합니다.
        searchResultAdapter.setOnItemClickListener(searchResult -> {
            if (isSelectionMode) {
                // [선택 모드일 경우]
                // 1. 결과 데이터를 담을 Intent를 생성합니다.
                Intent resultIntent = new Intent();
                resultIntent.putExtra("PLACE_NAME", searchResult.getTitle());
                resultIntent.putExtra("PLACE_LAT", searchResult.getLatitude());
                resultIntent.putExtra("PLACE_LNG", searchResult.getLongitude());

                // 2. setResult()를 호출하여 CreateGroupActivity로 데이터를 돌려보낼 준비를 합니다.
                setResult(Activity.RESULT_OK, resultIntent);

                // 3. 현재 MapsActivity를 종료하여 이전 화면(CreateGroupActivity)으로 돌아갑니다.
                finish();

            } else {
                // [일반 모드일 경우] (기존 로직과 동일)
                // 선택한 위치로 이동하고 상세 정보창을 띄웁니다.
                moveToSearchResult(searchResult);
                hideSearchResults();
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(37.5665, 126.9780), 11));

        // 설정된 지도 타입 적용
        applyMapTypeSetting();

        loadWeatherData(); // 지도가 준비된 후 날씨 정보 로드
    }

    private void initializeMap() {
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        mapView.getMapAsync(this);
    }

    private void initializeButtons() {
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        FloatingActionButton btnMapType = findViewById(R.id.btnMapType);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        androidx.cardview.widget.CardView weatherWidget = findViewById(R.id.weather_widget);
        
        // 메인 메뉴 버튼과 서브 메뉴 버튼들
        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        FloatingActionButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        FloatingActionButton btnMyPage = findViewById(R.id.btnMyPage);
        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);

        btnMapType.setOnClickListener(v -> showMapTypeMenu(v));
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        weatherWidget.setOnClickListener(v -> showWeatherBottomSheet());

        // 메인 메뉴 버튼 클릭 시 서브 메뉴 토글
        btnMainMenu.setOnClickListener(v -> toggleSubMenu());

        // 서브 메뉴 버튼들 클릭 리스너
        btnFriends.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, FriendsActivity.class);
            intent.putExtra("username", loggedInUsername);
            startActivity(intent);
            hideSubMenu();
        });

        btnCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, CreateGroupActivity.class);
            startActivity(intent);
            hideSubMenu();
        });

        btnMyGroups.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, MyGroupsActivity.class);
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
            Intent intent = new Intent(MapsActivity.this, SettingsActivity.class);
            startActivity(intent);
            hideSubMenu();
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
                if (locationSource != null) {
                    Location lastKnownLocation = locationSource.getLastLocation();
                    if (lastKnownLocation != null) {
                        updateMyLocation(lastKnownLocation);
                    } else {
                        Log.w("MapsActivity", "현재 위치를 가져올 수 없어 내 위치를 업데이트하지 못했습니다.");
                    }
                }
                fetchGroupMembersLocation();
                locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        };
        locationUpdateHandler.post(locationUpdateRunnable); // 즉시 시작
    }

    private void updateMyLocation(Location location) {
        if (currentGroupId == -1L) return;
        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setLatitude(location.getLatitude());
        request.setLongitude(location.getLongitude());

        Call<String> call = ApiClient.getGroupApiService(this).updateLocation(currentGroupId, request);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) Log.d("MapsActivity", "내 위치 업데이트 성공");
            }
            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("MapsActivity", "내 위치 업데이트 실패", t);
            }
        });
    }

    private void fetchGroupMembersLocation() {
        if (currentGroupId == -1L) return;
        Call<List<LocationResponse>> call = ApiClient.getGroupApiService(this).getGroupMemberLocations(currentGroupId);
        call.enqueue(new Callback<List<LocationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<LocationResponse>> call, @NonNull Response<List<LocationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMemberMarkers(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<LocationResponse>> call, @NonNull Throwable t) {
                Log.e("MapsActivity", "멤버 위치 가져오기 실패", t);
            }
        });
    }

    private void updateMemberMarkers(List<LocationResponse> locations) {
        if (naverMap == null) return;

        List<Long> updatedUserIds = new ArrayList<>();
        for (LocationResponse location : locations) {
            updatedUserIds.add(location.getUserId());
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
                markerToRemove.setMap(null);
            }
            memberMarkers.remove(userId);
        }
    }

    //==============================================================================================
    // 3. 검색 기능 관련
    //==============================================================================================




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

            // 1. API가 주는 비정상적인 값을 double로 읽어옵니다.
            double rawMapX = Double.parseDouble(item.getString("mapx"));
            double rawMapY = Double.parseDouble(item.getString("mapy"));

            // 2. 이 값들은 사실 위도/경도에 10,000,000이 곱해진 값이므로, 다시 나눠서 원래 값으로 복원합니다.
            //    (주의: mapy가 위도(latitude), mapx가 경도(longitude)에 해당합니다.)
            double latitude = rawMapY / 10000000.0;
            double longitude = rawMapX / 10000000.0;

            // 로그로 복원된 실제 위경도 값을 확인합니다.
            Log.d("MAPS_ACTIVITY_DEBUG", "[RESTORED] title: " + title + ", Lat: " + latitude + ", Lng: " + longitude);

            // 3. Tm128 변환 없이, 복원된 위경도 값으로 SearchResult를 생성합니다.
            results.add(new SearchResult(title, address, category, latitude, longitude, "", ""));
        }
        return results;
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
            // FIXME: parseNaverSearchResults에서 좌표 변환이 올바르게 되면, 이 부분도 정확한 위치로 이동합니다.
            Log.d("MAPS_ACTIVITY_DEBUG", "[MOVING TO] title: " + result.getTitle() + ", Lat: " + result.getLatitude() + ", Lng: " + result.getLongitude());
            LatLng location = new LatLng(result.getLatitude(), result.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(location, 16).animate(CameraAnimation.Easing, 1000);
            naverMap.moveCamera(cameraUpdate);

            if (searchResultMarker != null) searchResultMarker.setMap(null);

            searchResultMarker = new Marker();
            searchResultMarker.setPosition(location);
            searchResultMarker.setCaptionText(result.getTitle());
            searchResultMarker.setMap(naverMap);

            SearchResultDetailFragment.newInstance(result).show(getSupportFragmentManager(), "SearchResultDetailFragment");
        }
    }

    //==============================================================================================
    // 4. 날씨 기능 관련
    //==============================================================================================

    private void loadWeatherData() {
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
        
        // 설정 변경사항 적용
        applyMapTypeSetting();
        
        // 다른 화면에서 돌아왔을 때, 위치 공유 중이었다면 다시 시작
        if (currentGroupId != -1L) {
            startLocationSharing();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        // 앱이 백그라운드로 갈 때 위치 공유 중지
        locationUpdateHandler.removeCallbacksAndMessages(null);
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