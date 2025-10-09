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
import com.naver.maps.map.NaverMapSdk; // [수정] import 문 추가
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
    private Location latestLocation = null;

    // 검색 관련 UI 및 데이터 변수
    private EditText etSearch;
    private ImageView ivSearchIcon;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter searchResultAdapter;
    private Marker searchResultMarker = null;

    // 날씨 관련 UI 변수
    private ImageView ivWeatherIcon;
    private TextView tvTemperature;

    // 날씨 업데이트 시간 간격을 위한 변수
    private long lastWeatherUpdateTime = 0;
    private static final long WEATHER_UPDATE_INTERVAL = 5000; // 5초 (5000ms)

    // 백그라운드 작업을 위한 ExecutorService 및 Handler (공용으로 사용)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 상수
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String OPENWEATHERMAP_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";
    // 네이버 API키는 Manifest와 아래 onCreate의 초기화 코드에서 관리하므로 클래스 상수로 둘 필요가 없습니다.

    // 로그인된 사용자 이름을 저장할 변수
    private String loggedInUsername;

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

        // ★★★★★ 최종 해결을 위한 가장 중요한 코드 ★★★★★
        // setContentView() 보다 먼저 NaverMapSdk를 초기화해야 합니다.
        NaverMapSdk.getInstance(this).setClient(
                new NaverMapSdk.NaverCloudPlatformClient("v0la9en3t0")
        );

        setContentView(R.layout.activity_maps);

        checkLocationPermission();

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        // 날씨 UI 요소 초기화
        ivWeatherIcon = findViewById(R.id.iv_weather_icon);
        tvTemperature = findViewById(R.id.tv_temperature);

        // 로그인 사용자 이름 가져오기
        loggedInUsername = getIntent().getStringExtra("username");

        initializeMap();
        initializeButtons();
        initializeSearch();
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.getUiSettings().setLocationButtonEnabled(true);
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(37.5665, 126.9780), 11));

        naverMap.addOnLocationChangeListener(location -> {
            this.latestLocation = location;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWeatherUpdateTime > WEATHER_UPDATE_INTERVAL) {
                lastWeatherUpdateTime = currentTime;
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                updateWeatherWidget(currentLatLng);
                Log.d("MapsActivity", "날씨 정보를 실시간으로 업데이트합니다.");
            }
        });

        loadWeatherData();
    }

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

        btnMapType.setOnClickListener(v -> showMapTypeMenu(v));
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        weatherWidget.setOnClickListener(v -> showWeatherBottomSheet());

        btnFriends.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, FriendsActivity.class);
            intent.putExtra("username", loggedInUsername);
            startActivity(intent);
        });

        btnCreateGroup.setOnClickListener(v -> {
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
                if (latestLocation != null) { // [수정] 안정성을 위해 latestLocation 사용
                    updateMyLocation(latestLocation);
                } else {
                    Log.w("MapsActivity", "현재 위치를 가져올 수 없어 내 위치를 업데이트하지 못했습니다.");
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
                // FIXME: 보안을 위해 여기에 키를 직접 쓰는 것은 좋지 않습니다. 실제 앱에서는 build.gradle 등을 통해 관리하세요.
                conn.setRequestProperty("X-Naver-Client-Id", "OAQnuwhbAL34Of8mlxve");
                conn.setRequestProperty("X-Naver-Client-Secret", "4roXQDJBpc");

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
            // FIXME: 네이버 지역검색 API는 KATEC 좌표계를 반환하므로, 실제 서비스에서는 WGS84 위경도 좌표계로 변환해야 합니다.
            LatLng latLng = new LatLng(mapy, mapx); // 이 부분은 실제 위경도로 변환해야 합니다.
            results.add(new SearchResult(title, address, category, latLng.latitude, latLng.longitude, "", ""));
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
                // FIXME: 보안을 위해 여기에 키를 직접 쓰는 것은 좋지 않습니다. 실제 앱에서는 build.gradle 등을 통해 관리하세요.
                conn.setRequestProperty("X-Naver-Client-Id", "OAQnuwhbAL34Of8mlxve");
                conn.setRequestProperty("X-Naver-Client-Secret", "4roXQDJBpc");

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
        if (latestLocation != null) {
            double latitude = latestLocation.getLatitude();
            double longitude = latestLocation.getLongitude();
            WeatherBottomSheetFragment.newInstance(latitude, longitude).show(getSupportFragmentManager(), "WeatherBottomSheet");
        } else {
            Toast.makeText(this, "현재 위치를 파악 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
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
            if (naverMap != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }

    private void moveToCurrentLocation() {
        if (naverMap == null || latestLocation == null) { // [수정] 안정성을 위해 latestLocation 사용
            Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        LatLng currentLocation = new LatLng(latestLocation.getLatitude(), latestLocation.getLongitude());
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
        if (currentGroupId != -1L) {
            startLocationSharing();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
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