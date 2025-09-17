package com.example.gps.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gps.R;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private FusedLocationProviderClient fusedLocationClient;
    
    private TextView tvSteps;
    private TextView tvDistance;
    private TextView tvCalories;
    private TextView tvTime;
    private TextView tvPace;
    private Button btnStart;
    private Button btnStop;
    private Button btnReset;
    
    private int stepCount = 0;
    private float totalDistance = 0f;
    private long startTime = 0;
    private boolean isTracking = false;
    
    private Location lastLocation = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float AVERAGE_STEP_LENGTH = 0.7f; // 평균 보폭 (미터)
    private static final float CALORIES_PER_STEP = 0.04f; // 걸음당 소모 칼로리
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTimeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_counter);
        
        initViews();
        initSensors();
        initLocationServices();
        setupButtons();
    }
    
    private void initViews() {
        tvSteps = findViewById(R.id.tv_steps);
        tvDistance = findViewById(R.id.tv_distance);
        tvCalories = findViewById(R.id.tv_calories);
        tvTime = findViewById(R.id.tv_time);
        tvPace = findViewById(R.id.tv_pace);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnReset = findViewById(R.id.btn_reset);
        
        updateDisplay();
    }
    
    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepSensor == null) {
                // 만보기 센서가 없는 경우 가속도계로 대체
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Toast.makeText(this, "만보기 센서를 찾을 수 없어 가속도계를 사용합니다", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }
    
    private void setupButtons() {
        btnStart.setOnClickListener(v -> startTracking());
        btnStop.setOnClickListener(v -> stopTracking());
        btnReset.setOnClickListener(v -> resetTracking());
        
        // 초기 상태 설정
        btnStop.setEnabled(false);
    }
    
    private void startTracking() {
        if (checkLocationPermission()) {
            isTracking = true;
            startTime = System.currentTimeMillis();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            
            // 센서 등록
            if (stepSensor != null) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            
            // 위치 업데이트 시작
            startLocationUpdates();
            
            // 시간 업데이트 시작
            startTimeUpdates();
            
            Toast.makeText(this, "산책 추적을 시작합니다", Toast.LENGTH_SHORT).show();
        } else {
            requestLocationPermission();
        }
    }
    
    private void stopTracking() {
        isTracking = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        
        // 센서 해제
        sensorManager.unregisterListener(this);
        
        // 위치 업데이트 중지
        stopLocationUpdates();
        
        // 시간 업데이트 중지
        stopTimeUpdates();
        
        Toast.makeText(this, "산책 추적을 중지했습니다", Toast.LENGTH_SHORT).show();
    }
    
    private void resetTracking() {
        stepCount = 0;
        totalDistance = 0f;
        startTime = 0;
        lastLocation = null;
        updateDisplay();
        
        Toast.makeText(this, "데이터를 초기화했습니다", Toast.LENGTH_SHORT).show();
    }
    
    private void startTimeUpdates() {
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking) {
                    updateTimeDisplay();
                    handler.postDelayed(this, 1000); // 1초마다 업데이트
                }
            }
        };
        handler.post(updateTimeRunnable);
    }
    
    private void stopTimeUpdates() {
        if (updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
    }
    
    private void updateTimeDisplay() {
        if (startTime > 0) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long seconds = elapsedTime / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            long hours = minutes / 60;
            minutes = minutes % 60;
            
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
            tvTime.setText("시간: " + timeStr);
            
            // 페이스 계산 (분/km)
            if (totalDistance > 0) {
                float paceMinutes = (float) elapsedTime / (1000 * 60 * totalDistance / 1000);
                String paceStr = String.format(Locale.getDefault(), "페이스: %.1f 분/km", paceMinutes);
                tvPace.setText(paceStr);
            }
        }
    }
    
    private void startLocationUpdates() {
        if (checkLocationPermission()) {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .build();
            
            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null && lastLocation != null) {
                        float distance = lastLocation.distanceTo(location);
                        totalDistance += distance;
                        updateDisplay();
                    }
                    lastLocation = location;
                }
            };
            
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void stopLocationUpdates() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(new LocationCallback() {});
        }
    }
    
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
            LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "위치 권한이 승인되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // 만보기 센서
            stepCount = (int) event.values[0];
            updateDisplay();
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 가속도계로 걸음 감지 (간단한 구현)
            float[] values = event.values;
            float acceleration = (float) Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);
            
            // 임계값을 넘으면 걸음으로 인식
            if (acceleration > 12.0f) {
                stepCount++;
                updateDisplay();
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변경 시 처리
    }
    
    private void updateDisplay() {
        runOnUiThread(() -> {
            tvSteps.setText("걸음 수: " + stepCount + " 걸음");
            tvDistance.setText(String.format(Locale.getDefault(), "거리: %.2f km", totalDistance / 1000));
            tvCalories.setText(String.format(Locale.getDefault(), "칼로리: %.1f kcal", stepCount * CALORIES_PER_STEP));
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (isTracking && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        stopTimeUpdates();
    }
}
