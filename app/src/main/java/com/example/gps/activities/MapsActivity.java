package com.example.gps.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gps.R;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    // OpenWeatherMap API 키는 실제 유효한 키로 교체해야 합니다.
    private static final String OPENWEATHERMAP_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // 위치 권한 확인 및 요청
        checkLocationPermission();

        // 지도 초기화
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // FusedLocationSource 초기화
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // 버튼 초기화 및 이벤트 설정
        com.google.android.material.floatingactionbutton.FloatingActionButton btnMapType = findViewById(R.id.btnMapType);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        androidx.cardview.widget.CardView weatherWidget = findViewById(R.id.weather_widget);

        // 지도 타입 변경 버튼 이벤트
        btnMapType.setOnClickListener(v -> showMapTypeMenu(v));
        // 내 위치 버튼 이벤트
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        // 날씨 위젯 클릭 이벤트
        weatherWidget.setOnClickListener(v -> showWeatherBottomSheet());
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;

        // 위치 소스 설정
        naverMap.setLocationSource(locationSource);
        // 위치 추적 모드 활성화
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // 초기 카메라 위치를 서울 중심부로 설정
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(37.5665, 126.9780), 11));

        // 날씨 정보 로드
        loadWeatherData();
    }

    /**
     * 위치 권한이 있는지 확인하고, 없으면 요청합니다.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (naverMap != null) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 현재 위치로 지도를 이동시킵니다.
     */
    private void moveToCurrentLocation() {
        if (naverMap == null || locationSource.getLastLocation() == null) {
            Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Location location = locationSource.getLastLocation();
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(currentLocation, 16)
                .animate(CameraAnimation.Easing, 1200);
        naverMap.moveCamera(cameraUpdate);
        Toast.makeText(this, "📍 내 위치로 이동합니다.", Toast.LENGTH_SHORT).show();
    }

    /**
     * 지도 타입(일반, 위성, 지형도)을 변경하는 메뉴를 보여줍니다.
     */
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

    /**
     * 날씨 정보를 불러와 UI에 업데이트합니다.
     */
    private void loadWeatherData() {
        // 앱 시작 시에는 기본 위치(서울)의 날씨를 보여줍니다.
        LatLng defaultLocation = new LatLng(37.5665, 126.9780);
        updateWeatherWidget(defaultLocation);
    }

    /**
     * 특정 위치의 날씨 정보를 API로 요청하고 위젯을 업데이트합니다.
     * @param location 날씨를 조회할 위치
     */
    private void updateWeatherWidget(LatLng location) {
        new Thread(() -> {
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
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                double temperature = json.getJSONObject("main").getDouble("temp");
                String weatherMain = json.getJSONArray("weather").getJSONObject(0).getString("main");

                runOnUiThread(() -> {
                    ImageView ivWeatherIcon = findViewById(R.id.iv_weather_icon);
                    TextView tvTemperature = findViewById(R.id.tv_temperature);
                    tvTemperature.setText(String.format("%.0f°", temperature));
                    ivWeatherIcon.setImageResource(getWeatherIconResource(weatherMain));
                });

            } catch (Exception e) {
                Log.e("WeatherAPI", "날씨 정보 로드 실패", e);
            }
        }).start();
    }

    /**
     * 날씨 상태(영문)에 맞는 아이콘 리소스를 반환합니다.
     * @param weatherMain 날씨 상태 (예: "Clear", "Clouds")
     * @return drawable 리소스 ID
     */
    private int getWeatherIconResource(String weatherMain) {
        switch (weatherMain.toLowerCase()) {
            case "clear":
                return R.drawable.ic_weather_clear;
            case "clouds":
                return R.drawable.ic_weather_cloudy;
            case "rain":
            case "drizzle":
            case "thunderstorm":
                return R.drawable.ic_weather_rainy;
            case "snow":
                return R.drawable.ic_weather_snow;
            case "mist":
            case "fog":
                return R.drawable.ic_weather_fog;
            default:
                return R.drawable.ic_weather_clear;
        }
    }

    /**
     * 날씨 상세 정보를 보여주는 Bottom Sheet를 띄웁니다.
     */
    private void showWeatherBottomSheet() {
        Location currentLocation = locationSource.getLastLocation();
        double latitude, longitude;

        if (currentLocation != null) {
            latitude = currentLocation.getLatitude();
            longitude = currentLocation.getLongitude();
        } else {
            // 위치를 가져올 수 없는 경우 기본값(서울) 사용
            latitude = 37.5665;
            longitude = 126.9780;
            Toast.makeText(this, "현재 위치를 가져올 수 없어 기본 위치의 날씨를 표시합니다.", Toast.LENGTH_SHORT).show();
        }


    }

    // Android Activity Lifecycle Callbacks
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
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