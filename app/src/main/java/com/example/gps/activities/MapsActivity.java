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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // --- User & State ---
    private String loggedInUsername;
    private boolean isSelectionMode = false;

    // --- Firebase & Real-time Location Sharing ---
    private Long currentGroupId = -1L;
    private DatabaseReference firebaseDatabase;
    private ValueEventListener memberLocationListener;
    private final Handler locationUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable locationUpdateRunnable;
    private final HashMap<Long, Marker> memberMarkers = new HashMap<>();
    private Marker myLocationMarker = null;

    // --- Mock Movement (for testing) ---
    private Handler animationHandler;
    private Runnable animationRunnable;
    private LatLng startLatLng = new LatLng(37.5665, 126.9780); // Seoul City Hall
    private LatLng endLatLng = new LatLng(35.115, 129.04); // Busan Station
    private final long totalDuration = 10000; // 10 seconds
    private final int updateInterval = 50; // 50ms
    private long startTime;

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
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("group_locations");

        initializeMap();
        initializeButtons();
        initializeSearch();
        initializeSubMenu(); // From Code 1
        bindMyPageHeader();  // From Code 1
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(37.5665, 126.9780), 11));

        if (myLocationMarker == null) {
            myLocationMarker = new Marker();
            myLocationMarker.setCaptionText("내 위치");
            myLocationMarker.setMap(naverMap);
        }

        naverMap.addOnLocationChangeListener(location -> {
            if (location != null && Double.isFinite(location.getLatitude()) && Double.isFinite(location.getLongitude())) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (animationHandler == null) { // Update marker only if not in mock movement
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
        // --- Main UI Buttons ---
        FloatingActionButton btnMapType = findViewById(R.id.btnMapType);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        FloatingActionButton btnTestMovement = findViewById(R.id.btnTestMovement); // From Code 2
        findViewById(R.id.weather_widget).setOnClickListener(v -> showWeatherBottomSheet());

        // --- Speed Dial Menu Buttons (from Code 1) ---
        FloatingActionButton btnMainMenu = findViewById(R.id.btnMainMenu);
        FloatingActionButton btnFriends = findViewById(R.id.btnFriends);
        FloatingActionButton btnCreateGroup = findViewById(R.id.btnCreateGroup);
        FloatingActionButton btnMyGroups = findViewById(R.id.btnMyGroups);
        FloatingActionButton btnMyPage = findViewById(R.id.btnMyPage);
        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);

        // --- Click Listeners ---
        btnMapType.setOnClickListener(this::showMapTypeMenu);
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        if (btnTestMovement != null) {
            btnTestMovement.setOnClickListener(v -> startMockMovement());
        }

        // Speed Dial Listeners
        btnMainMenu.setOnClickListener(v -> toggleSubMenu());
        btnFriends.setOnClickListener(v -> {
            startActivity(new Intent(this, FriendsActivity.class).putExtra("username", loggedInUsername));
            hideSubMenu();
        });
        btnCreateGroup.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateGroupActivity.class));
            hideSubMenu();
        });
        btnMyGroups.setOnClickListener(v -> {
            startActivity(new Intent(this, MyGroupsActivity.class));
            hideSubMenu();
        });
        btnMyPage.setOnClickListener(v -> {
            drawerLayout.openDrawer(findViewById(R.id.my_page_drawer)); // Corrected to use ID
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
    // 3. Real-time Location Sharing (Firebase - from Code 2)
    //==============================================================================================

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        if ("SELECT_DESTINATION".equals(intent.getStringExtra("PURPOSE"))) {
            isSelectionMode = true;
            Toast.makeText(this, "목적지로 설정할 장소를 검색 후 선택해주세요.", Toast.LENGTH_LONG).show();
        }

        if (intent.hasExtra("groupId")) {
            currentGroupId = intent.getLongExtra("groupId", -1L);
            if (currentGroupId != -1L) {
                Toast.makeText(this, "그룹 ID: " + currentGroupId + " 위치 공유를 시작합니다.", Toast.LENGTH_SHORT).show();
                startLocationSharing();
            }
        }
    }

    private void startLocationSharing() {
        locationUpdateHandler.removeCallbacksAndMessages(null);
        locationUpdateRunnable = () -> {
            if (locationSource != null && animationHandler == null) { // Only update real location if not mocking
                Location lastKnownLocation = locationSource.getLastLocation();
                if (lastKnownLocation != null) {
                    updateMyLocation(lastKnownLocation);
                }
            }
            locationUpdateHandler.postDelayed(locationUpdateRunnable, LOCATION_UPDATE_INTERVAL);
        };
        locationUpdateHandler.post(locationUpdateRunnable);
        startFirebaseLocationListener();
    }

    private void updateMyLocation(Location location) {
        if (currentGroupId == -1L || location == null || loggedInUsername == null) return;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        if (Double.isFinite(latitude) && Double.isFinite(longitude)) {
            HashMap<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("timestamp", System.currentTimeMillis());

            firebaseDatabase.child(String.valueOf(currentGroupId))
                    .child(loggedInUsername)
                    .setValue(locationData)
                    .addOnFailureListener(e -> Log.e("MapsActivity", "Firebase update failed", e));
        }
    }

    private void startFirebaseLocationListener() {
        if (currentGroupId == -1L || naverMap == null) return;

        DatabaseReference groupPathRef = firebaseDatabase.child(String.valueOf(currentGroupId));
        if (memberLocationListener != null) {
            groupPathRef.removeEventListener(memberLocationListener);
        }

        memberLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LocationResponse> locations = new ArrayList<>();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String username = memberSnapshot.getKey();
                    if (username != null && username.equals(loggedInUsername)) continue;

                    Double lat = memberSnapshot.child("latitude").getValue(Double.class);
                    Double lon = memberSnapshot.child("longitude").getValue(Double.class);

                    if (lat != null && lon != null) {
                        LocationResponse lr = new LocationResponse();
                        lr.setUserName(username);
                        lr.setLatitude(lat);
                        lr.setLongitude(lon);
                        lr.setUserId((long) (username != null ? username.hashCode() : 0)); // Using hashcode as temp ID
                        locations.add(lr);
                    }
                }
                updateMemberMarkers(locations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MapsActivity", "Firebase listener error: " + error.getMessage());
            }
        };
        groupPathRef.addValueEventListener(memberLocationListener);
    }

    private void updateMemberMarkers(List<LocationResponse> locations) {
        if (naverMap == null) return;

        List<Long> updatedUserIds = new ArrayList<>();
        for (LocationResponse location : locations) {
            if (!Double.isFinite(location.getLatitude()) || !Double.isFinite(location.getLongitude())) continue;

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

        // Remove markers for users who left
        new HashMap<>(memberMarkers).forEach((userId, marker) -> {
            if (!updatedUserIds.contains(userId)) {
                marker.setMap(null);
                memberMarkers.remove(userId);
            }
        });
    }

    //==============================================================================================
    // 4. Mock Movement & Destination Selection (from Code 2)
    //==============================================================================================

    private void startMockMovement() {
        if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
        animationHandler = new Handler(Looper.getMainLooper());
        startTime = System.currentTimeMillis();
        startLatLng = myLocationMarker.getPosition();

        Toast.makeText(this, "Mock movement to Busan started.", Toast.LENGTH_LONG).show();

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
                updateMyLocation(mockLocation);

                if (fraction < 1.0) {
                    animationHandler.postDelayed(this, updateInterval);
                } else {
                    Toast.makeText(MapsActivity.this, "Arrived in Busan.", Toast.LENGTH_SHORT).show();
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
    // 5. UI Features (Menus, Search, Weather - Mostly from Code 1)
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

    // ... searchPlacesWithNaverAPI, parseNaverSearchResults, fetchImageForSearchResult ...
    // These methods remain largely the same, but ensure parseNaverSearchResults uses correct coordinate parsing.
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

    // --- Other UI methods (show/hide search, weather, map type menu) remain the same ---
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
    // 7. Activity Lifecycle Callbacks
    //==============================================================================================

    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        applyMapTypeSetting();
        if (currentGroupId != -1L) {
            startLocationSharing();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        locationUpdateHandler.removeCallbacksAndMessages(null);
        if (animationHandler != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationHandler = null;
        }
        if (currentGroupId != -1L && memberLocationListener != null) {
            firebaseDatabase.child(String.valueOf(currentGroupId)).removeEventListener(memberLocationListener);
        }
    }

    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }

    @Override
    protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }

    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

}