package com.example.gps.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.activities.Register_Login.LoginActivity;
import com.example.gps.activities.Register_Login.RegisterActivity;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.PopupMenu;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.example.gps.fragments.WeatherBottomSheetFragment;
import com.example.gps.adapters.SearchResultAdapter;
import com.example.gps.model.SearchResult;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.overlay.OverlayImage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.view.animation.AccelerateDecelerateInterpolator;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private final List<PathOverlay> pathOverlays = new ArrayList<>();
    // private DangerZoneManager dangerZoneManager;
    // private FacilityManager facilityManager;

    private androidx.cardview.widget.CardView courseInfoPanel;
    
    // 메뉴 관련 - 설정 메뉴만 유지
    private Menu optionsMenu;
    
    // 코스 정보 패널 뷰들
    private TextView courseTitle;
    private TextView courseRecommendation;
    private TextView courseDistance;
    private TextView courseDuration;
    private TextView courseDifficulty;
    private TextView courseDescription;
    private Button btnCloseInfo;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String TMAP_API_KEY = "6BXu3W092c8kdbZVOOzDe5YqlALysG305fjlKG10";

    private JSONArray coursesJSON;
    private final List<Marker> activeMarkers = new ArrayList<>();
    
    // API 캐싱 및 제어를 위한 변수들
    private final Map<String, List<LatLng>> routeCache = new HashMap<>();
    private final AtomicInteger pendingApiCalls = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_API_CALLS = 2; // 동시 API 호출 제한 더 엄격하게
    private static final long API_DELAY_MS = 500; // API 호출 간격 증가 (500ms)
    private static final int MAX_RETRY_COUNT = 3; // 최대 재시도 횟수
    
    // 드래그 관련 변수들
    private float initialTouchY = 0f;
    private float initialPanelY = 0f;
    private boolean isDragging = false;
    
    // 바텀시트 관련 변수들 (일반 View 애니메이션 사용)
    private View bottomSheetView = null;
    private boolean isBottomSheetVisible = false;
    private static final float PANEL_VISIBLE_Y = 0f;
    private static final float DRAG_THRESHOLD = 80f; // 드래그 임계값 조정
    
    // 코스별 패널 숨김 위치 (각 코스의 패널 높이에 맞춰 조정)
    private static final float[] PANEL_HIDDEN_Y_VALUES = {
        745f,  // 1코스 - 핸들 부분만 보이도록 큰 값으로 조정
        680f,  // 2코스 - 중간 길이 설명에 맞춰 조정
        800f   // 3코스 - 긴 설명에 맞춰 가장 크게 조정
    };
    
    private int currentCourseIndex = -1; // 현재 선택된 코스 인덱스
    
    // 검색 관련 변수들
    private EditText etSearch;
    private ImageView ivSearchIcon;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter searchResultAdapter;
    // 네이버 클라우드 플랫폼 API Gateway 키 (실제 키로 교체 필요)
    private static final String NAVER_CLIENT_ID = "YOUR_NCP_CLIENT_ID";
    private static final String NAVER_CLIENT_SECRET = "YOUR_NCP_CLIENT_SECRET";
    
    // 코스별 정보 데이터
    private static class CourseInfo {
        String title;
        String recommendation;
        String distance;
        String duration;
        String difficulty;
        String description;
        int titleColor;
        
        CourseInfo(String title, String recommendation, String distance, String duration, 
                  String difficulty, String description, int titleColor) {
            this.title = title;
            this.recommendation = recommendation;
            this.distance = distance;
            this.duration = duration;
            this.difficulty = difficulty;
            this.description = description;
            this.titleColor = titleColor;
        }
    }
    
    private CourseInfo[] courseInfos = {
        new CourseInfo(
            "둘레길 1코스 - 역사 탐방길",
            "👨‍👩‍👧‍👦 가족 단위, 초보자 추천",
            "📏 거리: 3.2km",
            "⏱️ 소요시간: 1시간30분",
            "⭐ 난이도: 쉬움",
            "남한산성의 대표적인 입문 코스로, 주요 성문과 역사적 건물들을 둘러볼 수 있습니다. 경사가 완만하여 어린이나 어르신도 부담 없이 걸을 수 있으며, 조선시대 역사와 문화를 체험할 수 있는 최적의 코스입니다.",
            0xFFFF5722
        ),
        new CourseInfo(
            "둘레길 2코스 - 문화 체험길",
            "🎭 중급자, 문화 애호가 추천",
            "📏 거리: 2.8km",
            "⏱️ 소요시간: 2시간",
            "⭐⭐ 난이도: 보통",
            "남한산성의 문화유산을 집중적으로 탐방할 수 있는 코스입니다. 영월정, 수어장대 등 조선시대 건축물과 국청사, 숭렬전 등 역사적 의미가 깊은 장소들을 만날 수 있어 역사 공부에 최적입니다.",
            0xFF2196F3
        ),
        new CourseInfo(
            "둘레길 3코스 - 자연 힐링길",
            "🏔️ 고급자, 자연 애호가 추천",
            "📏 거리: 4.1km",
            "⏱️ 소요시간: 2시간30분",
            "⭐⭐⭐ 난이도: 어려움",
            "남한산성의 자연경관을 만끽할 수 있는 코스입니다. 벌봉 정상에서의 탁 트인 전망과 깊은 숲길, 계곡을 따라 걸으며 사계절 아름다운 자연을 느낄 수 있습니다. 체력적으로 도전적이지만 그만큼 큰 만족감을 줍니다.",
            0xFF4CAF50
        )
    };

    private boolean isDangerZonesVisible = true;
    private boolean isFacilitiesVisible = true;

    private CoordinatorLayout coordinatorLayout;

    private LatLng startPoint = null;
    private LatLng endPoint = null;

    private boolean isSimulationRunning = false; // 시뮬레이션 실행 상태 추적

    private double accumulatedDistance = 0.0;
    private LatLng lastMovedPosition = null;
    
    // 만보기 관련 변수들
    private boolean isStepCounterRunning = false;
    private int currentSteps = 0;
    private double currentDistance = 0.0;
    private double currentCalories = 0.0;
    private long stepCounterStartTime = 0;
    private Handler stepCounterHandler = new Handler(Looper.getMainLooper());
    private Runnable stepCounterRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        // DrawerLayout 초기화
        coordinatorLayout = findViewById(R.id.coordinator_layout);

        // 네비게이션 메뉴는 아이콘 버튼으로 대체됨

        // 지도 초기화
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // 코스 정보 패널 초기화
        courseInfoPanel = findViewById(R.id.course_info_panel);
        courseTitle = findViewById(R.id.course_title);
        courseRecommendation = findViewById(R.id.course_recommendation);
        courseDistance = findViewById(R.id.course_distance);
        courseDuration = findViewById(R.id.course_duration);
        courseDifficulty = findViewById(R.id.course_difficulty);
        courseDescription = findViewById(R.id.course_description);
        btnCloseInfo = findViewById(R.id.btn_close_info);

        // 패널 드래그 설정
        setupPanelDrag();

        // 위험 구역 매니저 초기화
        // dangerZoneManager = new DangerZoneManager();

        // 시설물 매니저 초기화
        // facilityManager = new FacilityManager();

        // 코스 데이터 로드
        loadCoursesFromJSON();
        
        // 날씨 정보 로드
        loadWeatherData();

        // 검색 기능 초기화
        initializeSearch();

        // 버튼 초기화 및 이벤트 설정
        Button btnAll = findViewById(R.id.btnAll);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        androidx.cardview.widget.CardView weatherWidget = findViewById(R.id.weather_widget);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnSelectCourse = findViewById(R.id.btnSelectCourse);
        
        // 날씨 위젯 뷰들
        ImageView ivWeatherIcon = findViewById(R.id.iv_weather_icon);
        TextView tvTemperature = findViewById(R.id.tv_temperature);

        btnAll.setOnClickListener(v -> {
            // 대한민국 전체가 보이도록 카메라 이동
            showMainMarkers();
            LatLngBounds koreaBounds = new LatLngBounds(
                new LatLng(33.0, 124.0), // 남서쪽
                new LatLng(43.0, 132.0)  // 북동쪽
            );
            CameraUpdate cameraUpdate = CameraUpdate.fitBounds(koreaBounds, 0)
                .animate(CameraAnimation.Easing, 1200);
            naverMap.moveCamera(cameraUpdate);
            Toast.makeText(this, "📍 대한민국 전체 지도로 돌아갑니다.", Toast.LENGTH_SHORT).show();
        });
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        weatherWidget.setOnClickListener(v -> {
            // 날씨 하단 패널 표시
            showWeatherBottomSheet();
        });

        // 아이콘 버튼들 초기화 및 이벤트 설정
        setupIconButtons();
        
        // 바텀시트 초기화
        setupBottomSheet();
        btnSelectCourse.setOnClickListener(v -> {
            // 코스 선택 팝업 메뉴 표시
            PopupMenu popupMenu = new PopupMenu(this, btnSelectCourse);
            popupMenu.getMenuInflater().inflate(R.menu.course_popup_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.popup_course1) {
                    selectCourse(0);
                    return true;
                } else if (itemId == R.id.popup_course2) {
                    selectCourse(1);
                    return true;
                } else if (itemId == R.id.popup_course3) {
                    selectCourse(2);
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });

        // 닫기 버튼 클릭 이벤트 설정
        btnCloseInfo.setOnClickListener(v -> hideCourseInfoPanel());

        // 만보기 패널 버튼 이벤트 설정
        // 기존 만보기 패널 버튼들은 바텀시트로 이동됨

        // 만보기 관련 버튼 이벤트는 바텀시트에서 처리됨
    }

    // 시뮬레이션 중지 메서드
    private void stopSimulation() {
        isSimulationRunning = false;
        
        // 현재위치 마커 제거
        for (int i = activeMarkers.size() - 1; i >= 0; i--) {
            Marker marker = activeMarkers.get(i);
            if ("현재위치".equals(marker.getCaptionText())) {
                marker.setMap(null);
                activeMarkers.remove(i);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        
        // loadDangerZones();
        // loadFacilities();

        loadCoursesFromJSON(); // JSON 먼저 로드

        // 초기 카메라 위치를 서울 중심부로 설정
        naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(new LatLng(37.5665, 126.9780), 11));

        // 초기에 남한산성과 북한산 둘레길 마커만 표시
        showMainMarkers();

        naverMap.setOnMapClickListener((pointF, latLng) -> {
            // 시작점과 도착점 설정
            if (startPoint == null) {
                startPoint = latLng;
                Marker startMarker = new Marker();
                startMarker.setPosition(latLng);
                startMarker.setCaptionText("🚶‍♀️ 시작점");
                startMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_cute_start));
                startMarker.setWidth(50);
                startMarker.setHeight(50);
                startMarker.setMap(naverMap);
                activeMarkers.add(startMarker);
                Toast.makeText(this, "시작점이 설정되었습니다. 도착점을 클릭하세요.", Toast.LENGTH_SHORT).show();
            } else if (endPoint == null) {
                endPoint = latLng;
                Marker endMarker = new Marker();
                endMarker.setPosition(latLng);
                endMarker.setCaptionText("🏁 도착점");
                endMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_cute_end));
                endMarker.setWidth(50);
                endMarker.setHeight(50);
                endMarker.setMap(naverMap);
                activeMarkers.add(endMarker);
                Toast.makeText(this, "도착점이 설정되었습니다. 시뮬레이션을 시작하려면 메뉴를 사용하세요.", Toast.LENGTH_SHORT).show();
            } else {
                // 리셋
                for (Marker marker : activeMarkers) {
                    if ("시작점".equals(marker.getCaptionText()) || "도착점".equals(marker.getCaptionText())) {
                        marker.setMap(null);
                    }
                }
                startPoint = latLng;
                endPoint = null;
                Marker startMarker = new Marker();
                startMarker.setPosition(latLng);
                startMarker.setCaptionText("🚶‍♀️ 시작점");
                startMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_cute_start));
                startMarker.setWidth(50);
                startMarker.setHeight(50);
                startMarker.setMap(naverMap);
                activeMarkers.add(startMarker);
                Toast.makeText(this, "시작점이 재설정되었습니다. 도착점을 클릭하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCoursesFromJSON() {
        try {
            InputStream is = getAssets().open("course_data.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");

            JSONObject root = new JSONObject(jsonStr);
            coursesJSON = root.getJSONArray("courses");

        } catch (Exception e) {
            Log.e("JSON", "코스 불러오기 실패", e);
        }
    }

    /*
    private void loadDangerZones() {
        try {
            // 위험 구역 데이터 로드 (assets/danger_zones.json에서)
            InputStream is = getAssets().open("danger_zones.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            JSONArray dangerZones = new JSONArray(sb.toString());
            // dangerZoneManager.displayDangerZones(this, naverMap, dangerZones);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "위험 구역 로드 실패", Toast.LENGTH_SHORT).show();
        }
    }
    */

    /*
    private void loadFacilities() {
        try {
            InputStream is = getAssets().open("facilities.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            JSONArray facilities = new JSONArray(sb.toString());
            // facilityManager.displayFacilities(this, naverMap, facilities);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "시설 정보 로드 실패", Toast.LENGTH_SHORT).show();
        }
    }
    */

    private void displayCourse(int index) {
        // 기존 경로만 지우고 마커는 유지
        for (PathOverlay p : pathOverlays) p.setMap(null);
        pathOverlays.clear();

        try {
            JSONObject course = coursesJSON.getJSONObject(index);
            int color = getColorForIndex(index);

            // 마커는 표시하지 않고 경로만 표시
            JSONArray route = course.getJSONArray("route");
            for (int j = 0; j < route.length() - 1; j++) {
                JSONObject p1 = route.getJSONObject(j);
                JSONObject p2 = route.getJSONObject(j + 1);
                LatLng start = new LatLng(p1.getDouble("lat"), p1.getDouble("lng"));
                LatLng end = new LatLng(p2.getDouble("lat"), p2.getDouble("lng"));
                requestTMapWalkSegment(start, end, color);
            }
        } catch (Exception e) {
            Log.e("DisplayCourse", "코스 표시 실패", e);
        }
    }

    private void clearMap() {
        // 기존 마커들 제거
        for (Marker marker : activeMarkers) {
            marker.setMap(null);
        }
        activeMarkers.clear();

        // 기존 경로들 제거
        for (PathOverlay overlay : pathOverlays) {
            overlay.setMap(null);
        }
        pathOverlays.clear();
        
        // 캐시 크기 관리 (100개 이상이면 절반 정리)
        if (routeCache.size() > 100) {
            clearOldCache();
        }
    }
    
    // 오래된 캐시 정리 (메모리 관리)
    private void clearOldCache() {
        Log.d("TMapAPI", "캐시 정리 시작 - 현재 캐시 수: " + routeCache.size());
        int targetSize = routeCache.size() / 2;
        List<String> keysToRemove = new ArrayList<>();
        
        int count = 0;
        for (String key : routeCache.keySet()) {
            if (count >= targetSize) break;
            keysToRemove.add(key);
            count++;
        }
        
        for (String key : keysToRemove) {
            routeCache.remove(key);
        }
        
        Log.d("TMapAPI", "캐시 정리 완료 - 정리 후 캐시 수: " + routeCache.size());
    }
    
    // 전체 캐시 지우기 (필요시 사용)
    private void clearAllCache() {
        routeCache.clear();
        Log.d("TMapAPI", "모든 캐시 삭제됨");
    }

    private int getColorForIndex(int i) {
        switch (i) {
            case 0: return 0xFFFF0000;
            case 1: return 0xFF0077FF;
            case 2: return 0xFF00AA00;
            default: return 0xFF888888;
        }
    }

    private void requestTMapWalkSegment(LatLng start, LatLng end, int color) {
        requestTMapWalkSegmentWithRetry(start, end, color, 0);
    }
    
    private void requestTMapWalkSegmentWithRetry(LatLng start, LatLng end, int color, int retryCount) {
        // 캐시 키 생성
        String cacheKey = String.format("%.6f,%.6f_%.6f,%.6f", 
            start.latitude, start.longitude, end.latitude, end.longitude);
        
        // 캐시에서 확인 (단, 직선 경로가 아닌 경우만)
        if (routeCache.containsKey(cacheKey)) {
            List<LatLng> cachedPath = routeCache.get(cacheKey);
            if (cachedPath != null && cachedPath.size() > 2) { // 2개 이상이면 실제 경로
                Log.d("TMapAPI", "캐시에서 실제 경로 로드: " + cacheKey + " (포인트: " + cachedPath.size() + ")");
                new Handler(Looper.getMainLooper()).post(() -> drawSegment(cachedPath, color));
                return;
            }
        }
        
        // 최대 재시도 횟수 초과 시 경로 표시하지 않음
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.w("TMapAPI", "최대 재시도 횟수 초과 - 경로 표시 안함: " + cacheKey);
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(MapsActivity.this, "⚠️ 일부 경로를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            );
            return;
        }
        
        // 동시 API 호출 수 확인
        int currentCalls = pendingApiCalls.get();
        if (currentCalls >= MAX_CONCURRENT_API_CALLS) {
            Log.w("TMapAPI", "API 호출 대기 중... 현재 대기: " + currentCalls + ", 재시도: " + retryCount);
            // 잠시 후 재시도
            new Handler(Looper.getMainLooper()).postDelayed(() -> 
                requestTMapWalkSegmentWithRetry(start, end, color, retryCount), API_DELAY_MS * 2);
            return;
        }
        
        // API 호출 수 증가
        pendingApiCalls.incrementAndGet();
        
        new Thread(() -> {
            try {
                // API 호출 간 딜레이 (재시도일 때는 더 길게)
                long delay = API_DELAY_MS + (retryCount * 300);
                Thread.sleep(delay);
                
                Log.d("TMapAPI", "경로 요청: " + cacheKey + " (재시도: " + retryCount + ", 대기중: " + pendingApiCalls.get() + ")");
                
                URL url = new URL("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("appKey", TMAP_API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000); // 15초 타임아웃
                conn.setReadTimeout(20000); // 20초 읽기 타임아웃

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
                InputStreamReader isr = new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream());
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                if (code == 429) {
                    Log.w("TMapAPI", "API 할당량 초과 (재시도 " + retryCount + ") - " + delay + "ms 후 재시도");
                    // 429 오류 시 더 긴 딜레이 후 재시도
                    new Handler(Looper.getMainLooper()).postDelayed(() -> 
                        requestTMapWalkSegmentWithRetry(start, end, color, retryCount + 1), delay * 2);
                    return;
                } else if (code != 200) {
                    Log.e("TMapAPI", "HTTP 오류 " + code + " (재시도 " + retryCount + "): " + sb);
                    // 다른 HTTP 오류도 재시도
                    new Handler(Looper.getMainLooper()).postDelayed(() -> 
                        requestTMapWalkSegmentWithRetry(start, end, color, retryCount + 1), delay);
                    return;
                }

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
                
                // 실제 경로만 표시 (3개 이상 포인트인 경우만)
                if (path.size() > 2) {
                    routeCache.put(cacheKey, new ArrayList<>(path));
                    Log.d("TMapAPI", "✅ 실제 경로 성공: " + cacheKey + " (포인트 수: " + path.size() + ", 재시도: " + retryCount + ")");
                    new Handler(Looper.getMainLooper()).post(() -> drawSegment(path, color));
                } else {
                    Log.w("TMapAPI", "유효하지 않은 경로 응답 (포인트: " + path.size() + ") - 재시도");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> 
                        requestTMapWalkSegmentWithRetry(start, end, color, retryCount + 1), delay);
                }

            } catch (Exception e) {
                Log.e("TMapAPI", "경로 요청 실패 (재시도 " + retryCount + "): " + e.getMessage(), e);
                // 예외 발생 시에도 재시도
                new Handler(Looper.getMainLooper()).postDelayed(() -> 
                    requestTMapWalkSegmentWithRetry(start, end, color, retryCount + 1), API_DELAY_MS * (retryCount + 1));
            } finally {
                // API 호출 수 감소
                pendingApiCalls.decrementAndGet();
            }
        }).start();
    }

    private void drawSegment(List<LatLng> path, int color) {
        PathOverlay overlay = new PathOverlay();
        overlay.setCoords(path);
        overlay.setColor(color);
        overlay.setWidth(12);
        overlay.setMap(naverMap);
        pathOverlays.add(overlay);
        
        // 실제 경로 표시 완료 로그
        Log.d("TMapAPI", "✅ 실제 보행 경로 표시 완료 (포인트: " + path.size() + ")");
        
        // 첫 번째 경로가 표시될 때만 성공 토스트 (중복 방지)
        if (pathOverlays.size() == 1) {
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(MapsActivity.this, "✅ 실제 보행 경로가 표시되었습니다!", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void showMainMarkers() {
        try {
            clearMap();
            
            // 원형 버튼들과 정보 패널 완전히 숨기기
            courseInfoPanel.setVisibility(View.GONE);
            currentCourseIndex = -1; // 코스 인덱스 초기화
            
            // 남한산성 둘레길 마커 (산성로터리 위치) - 귀여운 스타일
            Marker namhansanMarker = new Marker();
            namhansanMarker.setPosition(new LatLng(37.478046, 127.184021));
            namhansanMarker.setCaptionText("🏔️ 남한산성 둘레길");
            namhansanMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_cute_poi));
            namhansanMarker.setWidth(60);
            namhansanMarker.setHeight(60);
            namhansanMarker.setMap(naverMap);
            
            // 남한산성 마커 클릭 이벤트
            namhansanMarker.setOnClickListener(overlay -> {
                // 남한산성으로 부드럽게 이동하면서 확대
                CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(
                    new LatLng(37.478046, 127.184021), 14)
                    .animate(CameraAnimation.Easing, 1500);
                naverMap.moveCamera(cameraUpdate);
                
                // 코스 선택 버튼 보이기
                btnCloseInfo.setVisibility(View.VISIBLE);
                btnCloseInfo.setText("닫기");
                
                // 코스 선택 애니메이션
                courseInfoPanel.animate()
                    .translationY(PANEL_VISIBLE_Y)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
                
                // 코스 선택 안내 메시지
                Toast.makeText(MapsActivity.this, "우측 상단 버튼에서 원하는 코스를 선택하세요.", Toast.LENGTH_LONG).show();
                return true;
            });
            
            // 북한산 둘레길 마커 (북한산 위치) - 귀여운 스타일
            Marker bukhansanMarker = new Marker();
            bukhansanMarker.setPosition(new LatLng(37.6586, 126.9770));
            bukhansanMarker.setCaptionText("⛰️ 북한산 둘레길");
            bukhansanMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_cute_poi));
            bukhansanMarker.setWidth(60);
            bukhansanMarker.setHeight(60);
            bukhansanMarker.setMap(naverMap);
            
            // 북한산 마커 클릭 이벤트
            bukhansanMarker.setOnClickListener(overlay -> {
                // 북한산으로 부드럽게 이동하면서 확대
                CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(
                    new LatLng(37.6586, 126.9770), 14)
                    .animate(CameraAnimation.Easing, 1500);
                naverMap.moveCamera(cameraUpdate);
                
                // 코스 선택 버튼 보이기
                btnCloseInfo.setVisibility(View.VISIBLE);
                btnCloseInfo.setText("닫기");
                
                // 코스 선택 애니메이션
                courseInfoPanel.animate()
                    .translationY(PANEL_VISIBLE_Y)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
                
                // 코스 선택 안내 메시지
                Toast.makeText(MapsActivity.this, "우측 상단 버튼에서 원하는 코스를 선택하세요.", Toast.LENGTH_LONG).show();
                return true;
            });
            
            activeMarkers.add(namhansanMarker);
            activeMarkers.add(bukhansanMarker);
            
        } catch (Exception e) {
            Log.e("ShowMainMarkers", "메인 마커 표시 실패", e);
        }
    }

    private void displayAllCourses() {
        // 기존 경로만 지우고 마커는 유지
        for (PathOverlay p : pathOverlays) p.setMap(null);
        pathOverlays.clear();

        try {
            // 1코스, 2코스, 3코스 모두 표시
            for (int courseIndex = 0; courseIndex < coursesJSON.length(); courseIndex++) {
                JSONObject course = coursesJSON.getJSONObject(courseIndex);
                int color = getColorForIndex(courseIndex);

                // 각 코스의 마커들 표시
                JSONArray markers = course.getJSONArray("markers");
                for (int j = 0; j < markers.length(); j++) {
                    JSONObject m = markers.getJSONObject(j);
                    Marker marker = new Marker();
                    marker.setPosition(new LatLng(m.getDouble("lat"), m.getDouble("lng")));
                    marker.setCaptionText(m.getString("name"));
                    marker.setMap(naverMap);
                    activeMarkers.add(marker);
                }

                // 각 코스의 경로 표시
                JSONArray route = course.getJSONArray("route");
                for (int j = 0; j < route.length() - 1; j++) {
                    JSONObject p1 = route.getJSONObject(j);
                    JSONObject p2 = route.getJSONObject(j + 1);
                    LatLng start = new LatLng(p1.getDouble("lat"), p1.getDouble("lng"));
                    LatLng end = new LatLng(p2.getDouble("lat"), p2.getDouble("lng"));
                    requestTMapWalkSegment(start, end, color);
                }
            }
        } catch (Exception e) {
            Log.e("DisplayAllCourses", "전체 코스 표시 실패", e);
        }
    }

    private void displaySingleCourse(int courseIndex) {
        // 기존 모든 마커와 경로 지우기
        clearMap();

        try {
            JSONObject course = coursesJSON.getJSONObject(courseIndex);
            int color = getColorForIndex(courseIndex);
            String courseName = course.getString("name");

            // 선택된 코스의 마커들만 표시
            JSONArray markers = course.getJSONArray("markers");
            for (int j = 0; j < markers.length(); j++) {
                JSONObject m = markers.getJSONObject(j);
                Marker marker = new Marker();
                marker.setPosition(new LatLng(m.getDouble("lat"), m.getDouble("lng")));
                marker.setCaptionText(m.getString("name"));
                
                // 위치별 커스텀 마커 아이콘 설정
                String locationName = m.getString("name");
                int markerIcon = getMarkerIconForLocation(locationName);
                
                if (isPhotoMarker(locationName)) {
                    // 실제 사진인 경우 원형으로 변환해서 적용
                    OverlayImage circularImage = getCircularMarkerImageFromAssets(getPhotoFileName(locationName), color);
                    marker.setIcon(circularImage);
                    marker.setWidth(72);
                    marker.setHeight(72);
                } else {
                    // XML 마커인 경우 그대로 사용
                    marker.setIcon(OverlayImage.fromResource(markerIcon));
                    marker.setWidth(60);
                    marker.setHeight(60);
                }
                
                // 마커 클릭 이벤트 설정
                final String finalLocationName = locationName;
                marker.setOnClickListener(overlay -> {
                    // MarkerInfo info = MarkerInfo.getMarkerInfo(finalLocationName);
                    // showMarkerDetail(info.getName(), info.getDescription(), info.getImageUrl(), info.getType());
                    Toast.makeText(MapsActivity.this, finalLocationName + " 클릭됨", Toast.LENGTH_SHORT).show();
                    return true;
                });
                
                marker.setMap(naverMap);
                activeMarkers.add(marker);
            }

            // 선택된 코스의 경로 표시
            JSONArray route = course.getJSONArray("route");
            for (int j = 0; j < route.length() - 1; j++) {
                JSONObject p1 = route.getJSONObject(j);
                JSONObject p2 = route.getJSONObject(j + 1);
                LatLng start = new LatLng(p1.getDouble("lat"), p1.getDouble("lng"));
                LatLng end = new LatLng(p2.getDouble("lat"), p2.getDouble("lng"));
                requestTMapWalkSegment(start, end, color);
            }
            
            Log.d("DisplaySingleCourse", courseName + " 표시 완료");
        } catch (Exception e) {
            Log.e("DisplaySingleCourse", "단일 코스 표시 실패", e);
        }
    }

    // 위치별 마커 아이콘을 선택하는 메서드 - assets에서 로드
    private String getPhotoFileName(String locationName) {
        switch (locationName) {
            case "산성로터리":
                return "photo_sansungrotary.png";
            case "서문":
                return "photo_seomun.png";
            case "북문":
                return "photo_bukmun.png";
            case "남문":
                return "photo_nammun.png";
            case "동문":
                return "photo_dongmun.png";
            case "천주사터":
                return "photo_cheonjusateo.png";
            case "현절사":
                return "photo_hyeonjeolsa.png";
            case "장경사":
                return "photo_janggyeongsa.png";
            case "망월사":
                return "photo_mangwolsa.png";
            case "영월정":
                return "photo_yeongwoljeong.png";
            case "수어장대":
                return "photo_sueojangdae.png";
            case "남한산성세계유산센터":
                return "photo_heritage_center.png";
            case "국청사":
                return "photo_gukcheonsa.png";
            case "숭렬전":
                return "photo_sungryeoljeon.png";
            case "벌봉":
                return "photo_beolbong.png";
            default:
                return null;
        }
    }

    // assets에서 이미지를 로드하여 원형 마커 이미지 생성
    private OverlayImage getCircularMarkerImageFromAssets(String fileName, int borderColor) {
        try {
            InputStream inputStream = getAssets().open("images/" + fileName);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (originalBitmap == null) {
                return OverlayImage.fromResource(R.drawable.marker_circle_red);
            }

            // 원형 비트맵 생성
            Bitmap circularBitmap = getCircularBitmap(originalBitmap);
            
            // 테두리 추가
            Bitmap finalBitmap = addBorderToCircularBitmap(circularBitmap, borderColor);
            
            return OverlayImage.fromBitmap(finalBitmap);
        } catch (Exception e) {
            Log.e("AssetLoader", "이미지 로드 실패: " + fileName, e);
            return OverlayImage.fromResource(R.drawable.marker_circle_red);
        }
    }

    // 위치별 마커 아이콘을 선택하는 메서드 - 백업용 XML 마커
    private int getMarkerIconForLocation(String locationName) {
        switch (locationName) {
            case "산성로터리":
                return R.drawable.marker_circle_red;
            case "서문":
            case "북문":
            case "남문":
            case "동문":
                return R.drawable.marker_gate;
            case "천주사터":
            case "현절사":
            case "장경사":
            case "망월사":
                return R.drawable.marker_temple;
            case "영월정":
            case "수어장대":
                return R.drawable.marker_pavilion;
            case "남한산성세계유산센터":
            case "국청사":
            case "숭렬전":
                return R.drawable.marker_center;
            case "벌봉":
                return R.drawable.marker_mountain;
            default:
                return R.drawable.marker_circle_red;
        }
    }

    // 실제 사진인지 확인하는 메서드 - 원형 처리 적용
    private boolean isPhotoMarker(String locationName) {
        switch (locationName) {
            case "산성로터리":
            case "서문":
            case "북문":
            case "남문":
            case "동문":
            case "천주사터":
            case "현절사":
            case "장경사":
            case "망월사":
            case "영월정":
            case "수어장대":
            case "남한산성세계유산센터":
            case "국청사":
            case "숭렬전":
            case "벌봉":
                return true;
            default:
                return false;
        }
    }

    // 비트맵을 원형으로 변환하는 메서드
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFFFFFFFF);
        
        // 원형 마스크 생성
        canvas.drawOval(rectF, paint);
        
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        
        // 원본 이미지를 정사각형으로 크롭
        int x = (bitmap.getWidth() - size) / 2;
        int y = (bitmap.getHeight() - size) / 2;
        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);
        
        canvas.drawBitmap(squareBitmap, rect, rect, paint);

        return output;
    }

    // 원형 비트맵에 테두리 추가
    private Bitmap addBorderToCircularBitmap(Bitmap src, int borderColor) {
        int borderWidth = 6;
        int size = src.getWidth() + borderWidth * 2;
        
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        Paint borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.FILL);
        borderPaint.setAntiAlias(true);
        
        // 테두리 원 그리기
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint);
        
        // 내부 흰색 원 그리기
        Paint whitePaint = new Paint();
        whitePaint.setColor(0xFFFFFFFF);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, (size - borderWidth * 2) / 2f, whitePaint);
        
        // 원형 이미지 그리기
        canvas.drawBitmap(src, borderWidth, borderWidth, null);
        
        return output;
    }

    // 코스 정보 표시
    private void showCourseInfo(int courseIndex) {
        if (courseIndex < 0 || courseIndex >= courseInfos.length) return;
        
        // 현재 코스 인덱스 저장
        currentCourseIndex = courseIndex;
        
        CourseInfo info = courseInfos[courseIndex];
        
        // 코스 정보 업데이트
        courseTitle.setText(info.title);
        courseTitle.setTextColor(info.titleColor);
        courseRecommendation.setText(info.recommendation);
        courseRecommendation.setTextColor(info.titleColor);
        courseDistance.setText(info.distance);
        courseDuration.setText(info.duration);
        courseDifficulty.setText(info.difficulty);
        courseDescription.setText(info.description);
        
        // 패널 보이기
        courseInfoPanel.setVisibility(View.VISIBLE);
        
        // 슬라이드 업 애니메이션
        courseInfoPanel.animate()
            .translationY(PANEL_VISIBLE_Y)
            .setDuration(500)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    // 코스 정보 패널 숨기기
    private void hideCourseInfoPanel() {
        if (currentCourseIndex < 0 || currentCourseIndex >= PANEL_HIDDEN_Y_VALUES.length) {
            return; // 유효하지 않은 인덱스인 경우 아무것도 하지 않음
        }
        // 슬라이드 다운 애니메이션 (핸들은 보이도록)
        courseInfoPanel.animate()
            .translationY(PANEL_HIDDEN_Y_VALUES[currentCourseIndex])
            .setDuration(300)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                // 패널이 완전히 내려간 후 초기화면(메인 마커만)으로 전환
                showMainMarkers();
            })
            .start();
        // visibility는 VISIBLE로 유지하여 드래그 가능하도록 함
    }

    // 패널 드래그 기능 설정
    private void setupPanelDrag() {
        courseInfoPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 유효한 코스가 선택되지 않은 경우 드래그 비활성화
                if (currentCourseIndex < 0 || currentCourseIndex >= PANEL_HIDDEN_Y_VALUES.length) {
                    return false;
                }
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 터치 시작
                        initialTouchY = event.getRawY();
                        initialPanelY = courseInfoPanel.getTranslationY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // 드래그 중
                        if (!isDragging) {
                            float deltaY = Math.abs(event.getRawY() - initialTouchY);
                            if (deltaY > 10) { // 최소 드래그 감지 거리
                                isDragging = true;
                            }
                        }

                        if (isDragging) {
                            float currentY = event.getRawY();
                            float deltaY = currentY - initialTouchY;
                            float newTranslationY = initialPanelY + deltaY;

                            // 패널이 너무 위로 올라가거나 아래로 내려가지 않도록 제한
                            newTranslationY = Math.max(PANEL_VISIBLE_Y, newTranslationY);
                            newTranslationY = Math.min(PANEL_HIDDEN_Y_VALUES[currentCourseIndex], newTranslationY);

                            courseInfoPanel.setTranslationY(newTranslationY);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // 터치 종료
                        if (isDragging) {
                            handlePanelDragEnd();
                        } else {
                            // 단순 터치인 경우 아무것도 하지 않음 (버튼 클릭 등을 방해하지 않음)
                            return false;
                        }
                        isDragging = false;
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    // 드래그 종료 시 패널 위치 결정
    private void handlePanelDragEnd() {
        // 유효한 코스가 선택되지 않은 경우 처리하지 않음
        if (currentCourseIndex < 0 || currentCourseIndex >= PANEL_HIDDEN_Y_VALUES.length) {
            return;
        }
        
        float currentY = courseInfoPanel.getTranslationY();
        float targetY;
        long duration;

        // 현재 위치에 따라 완전히 보이거나 숨길지 결정
        if (currentY > DRAG_THRESHOLD) {
            // 임계값 이상 아래로 드래그한 경우 숨기기 (핸들만 보이도록)
            targetY = PANEL_HIDDEN_Y_VALUES[currentCourseIndex];
            duration = 250;
            courseInfoPanel.animate()
                .translationY(targetY)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        } else {
            // 위쪽에서 놓은 경우 다시 완전히 보이기
            targetY = PANEL_VISIBLE_Y;
            duration = 200;
            courseInfoPanel.animate()
                .translationY(targetY)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }

    // 시뮬레이션 시작 메서드
    private void startSimulation() {
        if (startPoint == null || endPoint == null) {
            Toast.makeText(this, "시작점과 도착점을 먼저 설정하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "가짜 GPS 시뮬레이션을 시작합니다!", Toast.LENGTH_SHORT).show();
        
        // 시작점에서 도착점까지의 경로 표시
        requestTMapWalkSegment(startPoint, endPoint, 0xFF00FF00); // 초록색 경로
        
        // 시작점에서 도착점까지의 총 거리 계산
        double totalDistance = calculateDistance(startPoint, endPoint);
        
        // 거리 기반 걸음 수 추정 (평균 보폭 0.7m 가정)
        int estimatedSteps = (int) (totalDistance / 0.7);
        
        // 칼로리 계산 (걸음당 0.04kcal)
        float calories = estimatedSteps * 0.04f;
        
        // 만보기 패널 업데이트
        runOnUiThread(() -> {
            TextView tvStepsMain = findViewById(R.id.tv_steps_bottom);
            TextView tvDistanceMain = findViewById(R.id.tv_distance_bottom);
            TextView tvCaloriesMain = findViewById(R.id.tv_calories_bottom);

            if (tvStepsMain != null && tvDistanceMain != null && tvCaloriesMain != null) {
                tvStepsMain.setText(String.valueOf(estimatedSteps));
                tvDistanceMain.setText(String.format("%.2f", totalDistance / 1000.0)); // 미터를 km로 변환
                tvCaloriesMain.setText(String.format("%.1f", calories));
            }
        });
        
        // 시뮬레이션 완료 메시지
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(this, "시뮬레이션이 완료되었습니다! 총 " + estimatedSteps + "걸음, " + 
                String.format("%.2f", totalDistance / 1000.0) + "km", Toast.LENGTH_LONG).show();
        }, 2000);
        
        isSimulationRunning = true; // 시뮬레이션 실행 상태 업데이트
        
        // 실시간 시뮬레이션 시작
        startRealTimeSimulation(startPoint, endPoint, totalDistance);
    }

    // 실시간 시뮬레이션 시작
    private void startRealTimeSimulation(LatLng start, LatLng end, double totalDistance) {
        // TMap API로 실제 보행 경로를 가져와서 시뮬레이션
        requestTMapWalkSegmentForSimulation(start, end, totalDistance);
    }

    // 시뮬레이션용 TMap 경로 요청
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
                        // UI 스레드에서 시뮬레이션 시작
                        runOnUiThread(() -> {
                            startPathSimulation(path, totalDistance);
                        });
                    } else {
                        // 경로가 유효하지 않으면 직선 경로로 시뮬레이션
                        runOnUiThread(() -> {
                            List<LatLng> straightPath = new ArrayList<>();
                            straightPath.add(start);
                            straightPath.add(end);
                            startPathSimulation(straightPath, totalDistance);
                        });
                    }
                } else {
                    // API 오류 시 직선 경로로 시뮬레이션
                    runOnUiThread(() -> {
                        List<LatLng> straightPath = new ArrayList<>();
                        straightPath.add(start);
                        straightPath.add(end);
                        startPathSimulation(straightPath, totalDistance);
                    });
                }

            } catch (Exception e) {
                Log.e("TMapSimulation", "경로 요청 실패", e);
                // 예외 발생 시 직선 경로로 시뮬레이션
                runOnUiThread(() -> {
                    List<LatLng> straightPath = new ArrayList<>();
                    straightPath.add(start);
                    straightPath.add(end);
                    startPathSimulation(straightPath, totalDistance);
                });
            }
        }).start();
    }

    // 경로를 따라 시뮬레이션
    private void startPathSimulation(List<LatLng> path, double totalDistance) {
        if (path.size() < 2) return;

        // 누적 거리 초기화
        accumulatedDistance = 0.0;
        lastMovedPosition = new LatLng(path.get(0).latitude, path.get(0).longitude);

        // 경로 표시 (초록색)
        PathOverlay pathOverlay = new PathOverlay();
        pathOverlay.setCoords(path);
        pathOverlay.setColor(0xFF00FF00);
        pathOverlay.setWidth(12);
        pathOverlay.setMap(naverMap);
        pathOverlays.add(pathOverlay);

        // 현재 위치를 시작점으로 설정
        final int[] currentPathIndex = {0};
        final LatLng[] currentPosition = {new LatLng(path.get(0).latitude, path.get(0).longitude)};
        
        // 현재 위치 마커 생성
        Marker currentLocationMarker = new Marker();
        currentLocationMarker.setPosition(currentPosition[0]);
        currentLocationMarker.setCaptionText("현재위치");
        currentLocationMarker.setMap(naverMap);
        activeMarkers.add(currentLocationMarker);
        
        // 시뮬레이션 실행
        Handler simulationHandler = new Handler(Looper.getMainLooper());
        Runnable simulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSimulationRunning) {
                    // 시뮬레이션이 중지된 경우 마커 제거
                    currentLocationMarker.setMap(null);
                    activeMarkers.remove(currentLocationMarker);
                    return;
                }
                
                // 경로의 마지막 지점에 도달했는지 확인
                if (currentPathIndex[0] >= path.size() - 1) {
                    // 시뮬레이션 완료
                    isSimulationRunning = false;
                    Toast.makeText(MapsActivity.this, "시뮬레이션이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                    
                    // 현재 위치 마커 제거
                    currentLocationMarker.setMap(null);
                    activeMarkers.remove(currentLocationMarker);
                    return;
                }
                
                // 다음 경로 지점으로 이동
                LatLng nextPoint = path.get(currentPathIndex[0] + 1);
                double distanceToNext = calculateDistance(currentPosition[0], nextPoint);
                
                if (distanceToNext <= 2.0) { // 2미터 이내에 도달
                    // 다음 경로 지점으로 이동
                    currentPathIndex[0]++;
                    currentPosition[0] = new LatLng(nextPoint.latitude, nextPoint.longitude);
                } else {
                    // 다음 지점 방향으로 조금씩 이동 (더 부드럽게)
                    double moveDistance = Math.min(distanceToNext * 0.1, 2.0); // 최대 2m씩 이동하여 더 부드럽게
                    double bearing = calculateBearing(currentPosition[0], nextPoint);
                    LatLng newPosition = calculateNewPosition(currentPosition[0], bearing, moveDistance);
                    currentPosition[0] = newPosition;
                }
                
                // 실제 이동한 거리 누적
                if (lastMovedPosition != null) {
                    double actualDistanceMoved = calculateDistance(lastMovedPosition, currentPosition[0]);
                    accumulatedDistance += actualDistanceMoved;
                }
                lastMovedPosition = new LatLng(currentPosition[0].latitude, currentPosition[0].longitude);
                
                // 현재 위치 마커 업데이트
                currentLocationMarker.setPosition(currentPosition[0]);
                
                // 지도 중심을 새로운 위치로 이동
                naverMap.moveCamera(CameraUpdate.scrollTo(currentPosition[0]));
                
                // 실시간 만보기 패널 업데이트 (누적된 실제 거리 사용)
                updateStepCounterWithAccumulatedDistance();
                
                // 다음 이동 예약 (더 빠르게 이동하여 부드럽게)
                simulationHandler.postDelayed(this, 100); // 100ms마다 이동
            }
        };
        
        simulationHandler.post(simulationRunnable);
    }

    // 누적된 실제 거리로 만보기 업데이트
    private void updateStepCounterWithAccumulatedDistance() {
        // 누적된 실제 이동 거리 사용
        double distanceTraveled = accumulatedDistance;
        
        // 거리 기반 걸음 수 추정 (평균 보폭 0.7m 가정)
        int estimatedSteps = (int) (distanceTraveled / 0.7);
        
        // 칼로리 계산 (걸음당 0.04kcal)
        float calories = estimatedSteps * 0.04f;
        
        // UI 업데이트
        runOnUiThread(() -> {
            TextView tvStepsMain = findViewById(R.id.tv_steps_bottom);
            TextView tvDistanceMain = findViewById(R.id.tv_distance_bottom);
            TextView tvCaloriesMain = findViewById(R.id.tv_calories_bottom);

            if (tvStepsMain != null && tvDistanceMain != null && tvCaloriesMain != null) {
                tvStepsMain.setText(String.valueOf(estimatedSteps));
                tvDistanceMain.setText(String.format("%.2f", distanceTraveled / 1000.0)); // 미터를 km로 변환
                tvCaloriesMain.setText(String.format("%.1f", calories));
            }
        });
    }

    // 두 지점 간 거리 계산 (미터)
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

    // 두 지점 간 방향 계산 (도)
    private double calculateBearing(LatLng point1, LatLng point2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double dLon = Math.toRadians(point2.longitude - point1.longitude);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                    Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.atan2(y, x);

        // 라디안을 도로 변환 (0~360)
        bearing = Math.toDegrees(bearing);
        if (bearing < 0) {
            bearing += 360;
        }
        return bearing;
    }

    // 새로운 위치 계산 (현재 위치, 방향, 거리)
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

    @Override protected void onStart()   { super.onStart(); mapView.onStart(); }
    @Override protected void onResume()  { super.onResume(); mapView.onResume(); }
    @Override protected void onPause()   { super.onPause(); mapView.onPause(); }
    @Override protected void onStop()    { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory()  { super.onLowMemory(); mapView.onLowMemory(); }




    // 날씨 정보를 표시하는 메서드
    private void showWeatherInfo() {
        // 현재 위치의 날씨 정보를 가져와서 표시
        if (naverMap != null && locationSource != null) {
            Location location = locationSource.getLastLocation();
            if (location != null) {
                LatLng currentLocation = new LatLng(
                    location.getLatitude(),
                    location.getLongitude()
                );
                
                // 날씨 정보를 가져오는 API 호출
                new Thread(() -> {
                    try {
                        String weatherInfo = getWeatherInfo(currentLocation);
                        runOnUiThread(() -> {
                            // 날씨 정보를 다이얼로그로 표시
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("현재 날씨")
                                   .setMessage(weatherInfo)
                                   .setPositiveButton("확인", null)
                                   .show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(this, "날씨 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            } else {
                Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 날씨 정보를 가져오는 메서드
    private String getWeatherInfo(LatLng location) throws Exception {
        // OpenWeatherMap API를 사용하여 날씨 정보 가져오기
        String apiKey = "7a4aa78797771aa887fe9b14a9be94e5"; // OpenWeatherMap API 키
        String url = String.format(
            "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric&lang=kr",
            location.latitude, location.longitude, apiKey
        );

        URL weatherUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) weatherUrl.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // JSON 파싱
        JSONObject json = new JSONObject(response.toString());
        JSONObject main = json.getJSONObject("main");
        JSONArray weather = json.getJSONArray("weather");
        JSONObject weatherInfo = weather.getJSONObject(0);

        double temp = main.getDouble("temp");
        String description = weatherInfo.getString("description");
        int humidity = main.getInt("humidity");

        return String.format(
            "현재 날씨: %s\n기온: %.1f°C\n습도: %d%%",
            description, temp, humidity
        );
    }

    // 코스 선택 처리
    private void selectCourse(int courseIndex) {
        // 남한산성으로 부드럽게 이동
        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(
            new LatLng(37.478046, 127.184021), 14)
            .animate(CameraAnimation.Easing, 1500);
        naverMap.moveCamera(cameraUpdate);
        
        // 선택된 코스만 표시
        displaySingleCourse(courseIndex);
        
        // 코스 정보 표시
        showCourseInfo(courseIndex);
        
        String courseName = courseInfos[courseIndex].title;
        Toast.makeText(this, "📍 " + courseName + " 마커와 경로를 표시합니다.\n🔄 실제 보행 경로를 불러오는 중...", Toast.LENGTH_LONG).show();
    }

    // 위치 업데이트 시 위험 구역 체크
    private void checkDangerZone(LatLng location) {
        // if (dangerZoneManager != null) {
        //     dangerZoneManager.checkUserInDangerZone(this, location);
        // }
        // 위험 구역 체크 기능은 향후 구현 예정
    }

    /*
    // 마커 클릭 시 상세 정보 표시
    private void showMarkerDetail(String title, String description, String imageUrl, String type) {
        // MarkerDetailFragment fragment = MarkerDetailFragment.newInstance(title, description, imageUrl, type);
        // fragment.show(getSupportFragmentManager(), "marker_detail");
        Toast.makeText(this, title + " 상세 정보", Toast.LENGTH_SHORT).show();
    }
    */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인된 경우
                if (locationSource != null) {
                    locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
                // 위치 추적 모드 설정
                if (naverMap != null) {
                    naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
                }
            } else {
                // 권한이 거부된 경우
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupLocationSource() {
        if (naverMap != null) {
            locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
            naverMap.setLocationSource(locationSource);
            naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        }
    }

    private void moveToCurrentLocation() {
        if (naverMap != null && locationSource != null) {
            Location location = locationSource.getLastLocation();
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                // 내 위치로 카메라 확대 이동 (줌 16)
                CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(currentLocation, 16)
                    .animate(CameraAnimation.Easing, 1200);
                naverMap.moveCamera(cameraUpdate);
                Toast.makeText(this, "📍 내 위치로 이동합니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "지도가 준비되지 않았습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadWeatherData() {
        // 현재 위치 기반으로 날씨 정보 로드
        if (naverMap != null && naverMap.getLocationSource() != null) {
            // 서울 기본 위치로 날씨 정보 로드 (실제로는 현재 위치 사용)
            LatLng seoulLocation = new LatLng(37.5665, 126.9780);
            updateWeatherWidget(seoulLocation);
        }
    }
    
    private void updateWeatherWidget(LatLng location) {
        new Thread(() -> {
            try {
                // OpenWeatherMap API를 사용하여 날씨 정보 가져오기
                String apiKey = "7a4aa78797771aa887fe9b14a9be94e5"; // OpenWeatherMap API 키
                String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric&lang=kr",
                    location.latitude, location.longitude, apiKey
                );

                URL weatherUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) weatherUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                double temperature = json.getJSONObject("main").getDouble("temp");
                JSONArray weather = json.getJSONArray("weather");
                JSONObject weatherInfo = weather.getJSONObject(0);
                String weatherMain = weatherInfo.getString("main");
                String weatherDescription = weatherInfo.getString("description");

                // UI 업데이트는 메인 스레드에서
                runOnUiThread(() -> {
                    ImageView ivWeatherIcon = findViewById(R.id.iv_weather_icon);
                    TextView tvTemperature = findViewById(R.id.tv_temperature);
                    
                    // 온도 업데이트
                    tvTemperature.setText(String.format("%.0f°", temperature));
                    
                    // 날씨 아이콘 업데이트
                    int weatherIconRes = getWeatherIconResource(weatherMain);
                    ivWeatherIcon.setImageResource(weatherIconRes);
                });

            } catch (Exception e) {
                Log.e("WeatherAPI", "날씨 정보 로드 실패", e);
                // 실패 시 기본값으로 설정
                runOnUiThread(() -> {
                    TextView tvTemperature = findViewById(R.id.tv_temperature);
                    tvTemperature.setText("23°");
                });
            }
        }).start();
    }
    
    private int getWeatherIconResource(String weatherMain) {
        switch (weatherMain.toLowerCase()) {
            case "clear":
                return R.drawable.ic_weather_clear;
            case "clouds":
                return R.drawable.ic_weather_cloudy;
            case "rain":
                return R.drawable.ic_weather_rainy;
            case "snow":
                return R.drawable.ic_weather_rainy; // 눈 아이콘이 없으므로 비 아이콘 사용
            case "thunderstorm":
                return R.drawable.ic_weather_rainy; // 천둥 아이콘이 없으므로 비 아이콘 사용
            default:
                return R.drawable.ic_weather_clear;
        }
    }

    /**
     * 아이콘 버튼들 초기화 및 클릭 이벤트 설정
     */
    private void setupIconButtons() {
        // 홈 버튼 (원위치)
        View homeBtn = findViewById(R.id.btn_home);
        if (homeBtn != null) {
            homeBtn.setOnClickListener(v -> {
                // 대한민국 전체가 보이도록 카메라 이동
                showMainMarkers();
                LatLngBounds koreaBounds = new LatLngBounds(
                    new LatLng(33.0, 124.0), // 남서쪽
                    new LatLng(43.0, 132.0)  // 북동쪽
                );
                CameraUpdate cameraUpdate = CameraUpdate.fitBounds(koreaBounds, 0)
                    .animate(CameraAnimation.Easing, 1200);
                naverMap.moveCamera(cameraUpdate);
                Toast.makeText(this, "🏠 홈으로 돌아갑니다", Toast.LENGTH_SHORT).show();
            });
        }

        // 커뮤니티 버튼
        View communityBtn = findViewById(R.id.btn_community);
        if (communityBtn != null) {
            communityBtn.setOnClickListener(v -> {
                Intent communityIntent = new Intent(this, com.example.gps.community.CommunityActivity.class);
                startActivity(communityIntent);
            });
        }

        // 만보기 버튼
        View stepsBtn = findViewById(R.id.btn_steps);
        if (stepsBtn != null) {
            stepsBtn.setOnClickListener(v -> {
                toggleStepCounterPanel();
            });
        }

        // 즐겨찾기 버튼
        View favoritesBtn = findViewById(R.id.btn_favorites);
        if (favoritesBtn != null) {
            favoritesBtn.setOnClickListener(v -> {
                Intent favoritesIntent = new Intent(this, FavoritesActivity.class);
                startActivity(favoritesIntent);
            });
        }

        // 상점 버튼
        View shopBtn = findViewById(R.id.btn_shop);
        if (shopBtn != null) {
            shopBtn.setOnClickListener(v -> {
                Intent shopIntent = new Intent(this, com.example.gps.gacha.ShopActivity.class);
                startActivity(shopIntent);
            });
        }

        // 마이페이지 버튼
        View myPageBtn = findViewById(R.id.btn_my_page);
        if (myPageBtn != null) {
            myPageBtn.setOnClickListener(v -> {
                // 로그인 상태 확인 후 적절한 화면으로 이동
                showMyPageOptions();
            });
        }
    }

    /**
     * 마이페이지 옵션 표시 (로그인/회원가입 또는 사용자 정보)
     */
    private void showMyPageOptions() {
        // SharedPreferences에서 로그인 상태 확인
        android.content.SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        
        if (isLoggedIn) {
            // 로그인된 경우 사용자 정보 표시
            String username = prefs.getString("username", "사용자");
            showUserInfoDialog(username);
        } else {
            // 로그인되지 않은 경우 로그인/회원가입 옵션 표시
            showLoginOptionsDialog();
        }
    }

    /**
     * 로그인 옵션 다이얼로그 표시
     */
    private void showLoginOptionsDialog() {
        String[] options = {"로그인", "회원가입", "게스트 모드"};
        
        new AlertDialog.Builder(this)
            .setTitle("마이페이지")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 로그인
                        Intent loginIntent = new Intent(this, LoginActivity.class);
                        startActivity(loginIntent);

                        break;
                    case 1: // 회원가입
                        Intent signupIntent = new Intent(this, RegisterActivity.class);
                        startActivity(signupIntent);
                        break;
                    case 2: // 게스트 모드
                        Intent guestIntent = new Intent(this, GuestMain.class);
                        startActivity(guestIntent);
                        break;
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 사용자 정보 다이얼로그 표시
     */
    private void showUserInfoDialog(String username) {
        String[] options = {"내 정보", "설정", "로그아웃"};
        
        new AlertDialog.Builder(this)
            .setTitle("마이페이지 - " + username)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 내 정보
                        Toast.makeText(this, "내 정보 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
                        break;
                    case 1: // 설정
                        Toast.makeText(this, "설정 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
                        break;
                    case 2: // 로그아웃
                        logout();
                        break;
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 로그아웃 처리
     */
    private void logout() {
        android.content.SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("username");
        editor.apply();
        
        Toast.makeText(this, "로그아웃되었습니다", Toast.LENGTH_SHORT).show();
    }

    /**
     * 바텀시트 초기화 (일반 View 애니메이션 사용)
     */
    private void setupBottomSheet() {
        bottomSheetView = findViewById(R.id.bottom_sheet_step_counter);
        if (bottomSheetView == null) {
            Log.w("BottomSheet", "바텀시트 뷰를 찾을 수 없습니다");
            return;
        }
        
        // 초기에는 숨김 상태로 설정
        bottomSheetView.setVisibility(View.GONE);
        isBottomSheetVisible = false;
        
        // 바텀시트 내부 버튼들 초기화
        setupBottomSheetButtons();
    }
    
    /**
     * 바텀시트 내부 버튼들 초기화
     */
    private void setupBottomSheetButtons() {
        // 닫기 버튼
        View closeBtn = findViewById(R.id.btn_close_bottom_sheet);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                hideBottomSheet();
            });
        }
        
        // 상세보기 버튼
        View detailBtn = findViewById(R.id.btn_step_counter_detail);
        if (detailBtn != null) {
            detailBtn.setOnClickListener(v -> {
                // Intent stepCounterIntent = new Intent(this, StepCounterActivity.class);
                // startActivity(stepCounterIntent);
                Toast.makeText(this, "만보기 상세 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
            });
        }
        
        // 시작 버튼
        View startBtn = findViewById(R.id.btn_start_bottom);
        if (startBtn != null) {
            startBtn.setOnClickListener(v -> {
                startStepCounter();
            });
        }
        
        // 중지 버튼
        View stopBtn = findViewById(R.id.btn_stop_bottom);
        if (stopBtn != null) {
            stopBtn.setOnClickListener(v -> {
                stopStepCounter();
            });
        }
    }

    /**
     * 만보기 바텀시트 토글 (일반 View 애니메이션 사용)
     */
    private void toggleStepCounterPanel() {
        if (bottomSheetView == null) {
            Toast.makeText(this, "바텀시트가 초기화되지 않았습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isBottomSheetVisible) {
            // 바텀시트가 숨겨진 상태면 표시
            showBottomSheet();
        } else {
            // 바텀시트가 보이는 상태면 숨김
            hideBottomSheet();
        }
    }
    
    /**
     * 바텀시트 표시
     */
    private void showBottomSheet() {
        if (bottomSheetView == null) return;
        
        bottomSheetView.setVisibility(View.VISIBLE);
        isBottomSheetVisible = true;
        Toast.makeText(this, "만보기 패널을 표시했습니다", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 바텀시트 숨김
     */
    private void hideBottomSheet() {
        if (bottomSheetView == null) return;
        
        bottomSheetView.setVisibility(View.GONE);
        isBottomSheetVisible = false;
        Toast.makeText(this, "만보기 패널을 숨겼습니다", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 날씨 하단 패널 표시
     */
    private void showWeatherBottomSheet() {
        // 현재 위치 가져오기
        if (locationSource != null && naverMap != null) {
            Location currentLocation = locationSource.getLastLocation();
            if (currentLocation != null) {
                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();
                
                WeatherBottomSheetFragment weatherFragment = WeatherBottomSheetFragment.newInstance(latitude, longitude);
                weatherFragment.show(getSupportFragmentManager(), "weather_bottom_sheet");
            } else {
                // 위치를 가져올 수 없는 경우 서울 좌표 사용
                WeatherBottomSheetFragment weatherFragment = WeatherBottomSheetFragment.newInstance(37.5665, 126.9780);
                weatherFragment.show(getSupportFragmentManager(), "weather_bottom_sheet");
            }
        } else {
            // 위치 서비스가 없는 경우 서울 좌표 사용
            WeatherBottomSheetFragment weatherFragment = WeatherBottomSheetFragment.newInstance(37.5665, 126.9780);
            weatherFragment.show(getSupportFragmentManager(), "weather_bottom_sheet");
        }
    }
    
    /**
     * 만보기 시작
     */
    private void startStepCounter() {
        if (isStepCounterRunning) {
            Toast.makeText(this, "만보기가 이미 실행 중입니다", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isStepCounterRunning = true;
        stepCounterStartTime = System.currentTimeMillis();
        currentSteps = 0;
        currentDistance = 0.0;
        currentCalories = 0.0;
        
        Toast.makeText(this, "만보기를 시작합니다", Toast.LENGTH_SHORT).show();
        
        // 만보기 시뮬레이션 시작 (실제 센서 대신 가상의 걸음 수 증가)
        stepCounterRunnable = new Runnable() {
            @Override
            public void run() {
                if (isStepCounterRunning) {
                    // 가상의 걸음 수 증가 (실제로는 센서에서 받아와야 함)
                    currentSteps += (int)(Math.random() * 3) + 1; // 1-3걸음씩 랜덤 증가
                    currentDistance = currentSteps * 0.7; // 평균 보폭 0.7m
                    currentCalories = currentSteps * 0.04; // 걸음당 0.04kcal
                    
                    // UI 업데이트
                    updateStepCounterUI();
                    
                    // 1초마다 업데이트
                    stepCounterHandler.postDelayed(this, 1000);
                }
            }
        };
        
        stepCounterHandler.post(stepCounterRunnable);
    }
    
    /**
     * 만보기 중지
     */
    private void stopStepCounter() {
        if (!isStepCounterRunning) {
            Toast.makeText(this, "만보기가 실행 중이 아닙니다", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isStepCounterRunning = false;
        
        if (stepCounterRunnable != null) {
            stepCounterHandler.removeCallbacks(stepCounterRunnable);
        }
        
        long duration = System.currentTimeMillis() - stepCounterStartTime;
        long minutes = duration / (1000 * 60);
        
        Toast.makeText(this, 
            String.format("만보기를 중지했습니다\n총 %d걸음, %.2fkm, %.1fkcal\n소요시간: %d분", 
                currentSteps, currentDistance / 1000.0, currentCalories, minutes), 
            Toast.LENGTH_LONG).show();
    }
    
    /**
     * 만보기 UI 업데이트
     */
    private void updateStepCounterUI() {
        TextView tvSteps = findViewById(R.id.tv_steps_bottom);
        TextView tvDistance = findViewById(R.id.tv_distance_bottom);
        TextView tvCalories = findViewById(R.id.tv_calories_bottom);
        
        if (tvSteps != null) {
            tvSteps.setText(String.valueOf(currentSteps));
        }
        if (tvDistance != null) {
            tvDistance.setText(String.format("%.2f", currentDistance / 1000.0));
        }
        if (tvCalories != null) {
            tvCalories.setText(String.format("%.1f", currentCalories));
        }
    }

    /**
     * 검색 기능 초기화
     */
    private void initializeSearch() {
        etSearch = findViewById(R.id.et_search);
        ivSearchIcon = findViewById(R.id.iv_search_icon);
        rvSearchResults = findViewById(R.id.rv_search_results);

        // 검색 결과 어댑터 초기화
        searchResultAdapter = new SearchResultAdapter();
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchResultAdapter);

        // 검색 아이콘 클릭 이벤트
        ivSearchIcon.setOnClickListener(v -> performSearch());

        // 검색 결과 아이템 클릭 이벤트
        searchResultAdapter.setOnItemClickListener(searchResult -> {
            // 검색 결과 클릭 시 해당 위치로 지도 이동
            moveToSearchResult(searchResult);
            // 검색 결과 숨기기
            hideSearchResults();
        });

        // EditText에서 엔터키 처리
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    /**
     * 검색 실행
     */
    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 키보드 숨기기
        hideKeyboard();

        // 검색 실행
        searchPlacesWithNaverAPI(query);
    }

    /**
     * 네이버 지도 API를 사용한 장소 검색
     */
    private void searchPlacesWithNaverAPI(String query) {
        // API 키가 설정되지 않은 경우 샘플 데이터 사용
        if (NAVER_CLIENT_ID.equals("YOUR_NCP_CLIENT_ID") || NAVER_CLIENT_SECRET.equals("YOUR_NCP_CLIENT_SECRET")) {
            Log.i("SearchAPI", "네이버 API 키가 설정되지 않아 샘플 데이터를 사용합니다.");
            showSearchResults(getSampleSearchResults(query));
            return;
        }
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                // 네이버 지도 API 장소 검색 URL
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                String urlString = String.format(
                    "https://naveropenapi.apigw.ntruss.com/map-place/v1/search?query=%s&coordinate=127.027619,37.497950&radius=10000&page=1&size=10",
                    encodedQuery
                );
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", NAVER_CLIENT_ID);
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", NAVER_CLIENT_SECRET);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject json = new JSONObject(response.toString());
                    List<SearchResult> results = parseNaverSearchResults(json);
                    
                    handler.post(() -> {
                        if (results.isEmpty()) {
                            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            showSearchResults(results);
                        }
                    });
                } else {
                    // API 오류 시 샘플 데이터 사용
                    Log.e("SearchAPI", "API 응답 코드: " + responseCode);
                    handler.post(() -> {
                        Toast.makeText(this, "API 인증 오류로 샘플 데이터를 표시합니다.", Toast.LENGTH_SHORT).show();
                        showSearchResults(getSampleSearchResults(query));
                    });
                }
                
            } catch (Exception e) {
                Log.e("SearchAPI", "장소 검색 실패", e);
                handler.post(() -> {
                    Toast.makeText(this, "검색 중 오류가 발생했습니다. 샘플 데이터를 표시합니다.", Toast.LENGTH_SHORT).show();
                    // 오류 시 샘플 데이터 표시
                    showSearchResults(getSampleSearchResults(query));
                });
            }
        });
    }

    /**
     * 네이버 API 검색 결과 파싱
     */
    private List<SearchResult> parseNaverSearchResults(JSONObject json) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        
        if (json.has("places")) {
            JSONArray places = json.getJSONArray("places");
            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                
                String title = place.optString("name", "");
                String address = place.optString("roadAddress", place.optString("address", ""));
                String category = place.optString("category", "일반");
                
                // 좌표 정보
                JSONObject location = place.optJSONObject("location");
                double latitude = 0.0;
                double longitude = 0.0;
                if (location != null) {
                    latitude = location.optDouble("y", 0.0);
                    longitude = location.optDouble("x", 0.0);
                }
                
                results.add(new SearchResult(title, address, category, latitude, longitude, ""));
            }
        }
        
        return results;
    }

    /**
     * 샘플 검색 결과 생성 (API 오류 시 사용)
     */
    private List<SearchResult> getSampleSearchResults(String query) {
        List<SearchResult> results = new ArrayList<>();
        
        // 쿼리에 따른 샘플 결과 생성
        if (query.contains("공원") || query.contains("산책")) {
            results.add(new SearchResult("한강공원", "서울특별시 영등포구 여의도동", "공원", 37.5219, 126.9240, "한강을 따라 산책할 수 있는 공원"));
            results.add(new SearchResult("올림픽공원", "서울특별시 송파구 올림픽로 424", "공원", 37.5211, 127.1213, "올림픽 기념 공원"));
            results.add(new SearchResult("북한산국립공원", "경기도 고양시 덕양구", "국립공원", 37.6584, 126.9996, "북한산 등산로"));
            results.add(new SearchResult("남산공원", "서울특별시 용산구 남산동", "공원", 37.5512, 126.9882, "남산 타워가 있는 공원"));
        } else if (query.contains("카페")) {
            results.add(new SearchResult("스타벅스 강남점", "서울특별시 강남구 테헤란로 152", "카페", 37.5665, 126.9780, "스타벅스 강남점"));
            results.add(new SearchResult("투썸플레이스", "서울특별시 강남구 역삼동", "카페", 37.5000, 127.0000, "투썸플레이스"));
            results.add(new SearchResult("이디야커피", "서울특별시 서초구 서초동", "카페", 37.4947, 127.0276, "이디야커피 서초점"));
        } else if (query.contains("병원") || query.contains("의원")) {
            results.add(new SearchResult("서울대병원", "서울특별시 종로구 대학로 101", "병원", 37.5796, 126.9990, "서울대학교병원"));
            results.add(new SearchResult("삼성서울병원", "서울특별시 강남구 일원로 81", "병원", 37.4881, 127.0856, "삼성서울병원"));
        } else if (query.contains("학교") || query.contains("대학")) {
            results.add(new SearchResult("서울대학교", "서울특별시 관악구 관악로 1", "대학교", 37.4596, 126.9516, "서울대학교"));
            results.add(new SearchResult("연세대학교", "서울특별시 서대문구 연세로 50", "대학교", 37.5640, 126.9369, "연세대학교"));
        } else if (query.contains("지하철") || query.contains("역")) {
            results.add(new SearchResult("강남역", "서울특별시 강남구 강남대로 396", "지하철역", 37.4979, 127.0276, "2호선 강남역"));
            results.add(new SearchResult("홍대입구역", "서울특별시 마포구 양화로 188", "지하철역", 37.5563, 126.9226, "2호선, 6호선 홍대입구역"));
        } else {
            // 일반적인 검색 결과
            results.add(new SearchResult(query + " 검색결과 1", "서울특별시 강남구", "일반", 37.5665, 126.9780, "검색된 장소"));
            results.add(new SearchResult(query + " 검색결과 2", "서울특별시 서초구", "일반", 37.4947, 127.0276, "검색된 장소"));
            results.add(new SearchResult(query + " 검색결과 3", "서울특별시 송파구", "일반", 37.5145, 127.1050, "검색된 장소"));
        }
        
        return results;
    }

    /**
     * 검색 결과 표시
     */
    private void showSearchResults(List<SearchResult> results) {
        searchResultAdapter.updateResults(results);
        rvSearchResults.setVisibility(View.VISIBLE);
    }

    /**
     * 검색 결과 숨기기
     */
    private void hideSearchResults() {
        rvSearchResults.setVisibility(View.GONE);
        etSearch.clearFocus();
    }

    /**
     * 검색 결과로 지도 이동
     */
    private void moveToSearchResult(SearchResult result) {
        if (naverMap != null) {
            LatLng location = new LatLng(result.getLatitude(), result.getLongitude());
            
            // 지도 카메라 이동
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(location)
                    .animate(CameraAnimation.Easing, 1000);
            naverMap.moveCamera(cameraUpdate);
            
            // 마커 추가
            Marker marker = new Marker();
            marker.setPosition(location);
            marker.setMap(naverMap);
            
            Toast.makeText(this, result.getTitle() + "로 이동했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 키보드 숨기기
     */
    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }
}
