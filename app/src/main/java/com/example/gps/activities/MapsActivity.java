package com.example.gps.activities;

import android.Manifest;
import android.content.Intent;
import android.content.Context; // PHJ:
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.gps.R;
import com.example.gps.activities.Friend.FriendsActivity;
import com.example.gps.adapters.SearchResultAdapter; // PHJ:
import com.example.gps.fragments.SearchResultDetailFragment; // PHJ:
import com.example.gps.model.SearchResult; // PHJ:
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.gps.api.ApiClient;
import com.example.gps.dto.LocationResponse;
import com.example.gps.dto.UpdateLocationRequest;
import java.util.HashMap;
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
    private SearchResultAdapter searchResultAdapter;
    private Marker searchResultMarker = null;

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

    // ✅ 1. 로그인된 사용자 이름을 저장할 변수를 여기에 선언합니다.
    private String loggedInUsername;

    // --- ✅✅✅ 실시간 공유를 위한 변수들 추가 ✅✅✅ ---
    private Long currentGroupId = -1L;
    private Handler locationUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable locationUpdateRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10초
    private HashMap<Long, Marker> memberMarkers = new HashMap<>();

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

        com.google.android.material.floatingactionbutton.FloatingActionButton btnMapType = findViewById(R.id.btnMapType);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        androidx.cardview.widget.CardView weatherWidget = findViewById(R.id.weather_widget);

        btnMapType.setOnClickListener(v -> showMapTypeMenu(v));
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        weatherWidget.setOnClickListener(v -> showWeatherBottomSheet());

        initializeSearch(); // PHJ: 검색 기능 초기화 메서드 호출



        // ✅ 2. LoginActivity로부터 받은 사용자 이름을 변수에 저장합니다.
        // 이 코드는 이미 있을 수 있습니다.
        loggedInUsername = getIntent().getStringExtra("username");

        // ✅ 3. 친구 버튼 클릭 리스너를 수정합니다.
        // 이전에 추가했던 btnFriends 부분을 아래 코드로 교체해주세요.
        ImageButton btnFriends = findViewById(R.id.btnFriends);
        btnFriends.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, FriendsActivity.class);
            // FriendsActivity로 현재 로그인된 사용자 이름을 전달합니다.
            intent.putExtra("username", loggedInUsername);
            startActivity(intent);
        });

        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CreateGroupActivity를 시작하는 인텐트 생성
                Intent intent = new Intent(MapsActivity.this, CreateGroupActivity.class);
                startActivity(intent);
            }
        });

        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        btnMyGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, MyGroupsActivity.class);
                startActivity(intent);
            }
        });
    }
    // MyGroupsActivity에서 그룹을 선택하고 돌아왔을 때 호출됩니다.
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

    // --- ✅✅✅ 실시간 위치 공유 로직 전체 추가 ✅✅✅ ---
    private void startLocationSharing() {
        locationUpdateHandler.removeCallbacksAndMessages(null); // 기존 핸들러 중지

        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                Location lastKnownLocation = locationSource.getLastLocation();
                if (lastKnownLocation != null) {
                    updateMyLocation(lastKnownLocation);
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
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) Log.d("MapsActivity", "내 위치 업데이트 성공");
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("MapsActivity", "내 위치 업데이트 실패", t);
            }
        });
    }

    private void fetchGroupMembersLocation() {
        if (currentGroupId == -1L) return;
        Call<List<LocationResponse>> call = ApiClient.getGroupApiService(this).getGroupMemberLocations(currentGroupId);
        call.enqueue(new Callback<List<LocationResponse>>() {
            @Override
            public void onResponse(Call<List<LocationResponse>> call, Response<List<LocationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMemberMarkers(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<LocationResponse>> call, Throwable t) {
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

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(37.5665, 126.9780), 11));

        loadWeatherData(); // 지도가 준비된 후 날씨 정보 로드
    }

    private void initializeMap() {
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        mapView.getMapAsync(this);
    }

    private void initializeButtons() {
        com.google.android.material.floatingactionbutton.FloatingActionButton btnMapType = findViewById(R.id.btnMapType);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        androidx.cardview.widget.CardView weatherWidget = findViewById(R.id.weather_widget);

        btnMapType.setOnClickListener(v -> showMapTypeMenu(v));
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        weatherWidget.setOnClickListener(v -> showWeatherBottomSheet());
    }

    //==============================================================================================
    // 2. 검색 기능 관련 메서드
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

            // FIXME: ⚠️ 매우 중요! 네이버 지역검색 API는 KATEC(TM128) 좌표계를 반환합니다.
            // NaverMap SDK는 WGS84 위경도 좌표계를 사용하므로, 반드시 좌표 변환이 필요합니다.
            // 아래 코드는 잘못된 위치를 가리키게 되므로, 실제 서비스에서는 좌표 변환 라이브러리나 API를 사용해야 합니다.
            // 예: new LatLng(y, x) -> new LatLng(위도, 경도)
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
    // 3. 날씨 기능 관련 메서드
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
                    // onCreate에서 미리 찾아둔 UI 변수를 사용합니다.
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
    // 4. 지도 및 권한 관련 유틸리티 메서드
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

    private void showWeatherBottomSheet() {
        Location currentLocation = locationSource.getLastLocation();
        double latitude, longitude;

        if (currentLocation != null) {
            latitude = currentLocation.getLatitude();
            longitude = currentLocation.getLongitude();
        } else {
            latitude = 37.5665;
            longitude = 126.9780;
            Toast.makeText(this, "현재 위치를 가져올 수 없어 기본 위치의 날씨를 표시합니다.", Toast.LENGTH_SHORT).show();
        }
        // TODO: WeatherBottomSheetFragment 띄우는 로직 필요
    }

    @Override protected void onStop() {
        if (locationUpdateHandler != null) {
            locationUpdateHandler.removeCallbacksAndMessages(null);
        }
        super.onStop();
        mapView.onStop();
    }

    // Android Activity Lifecycle Callbacks
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }

    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}