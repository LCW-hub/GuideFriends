package com.example.gps.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.gps.R;
import com.example.gps.adapters.WeatherForecastAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_LAT = "latitude";
    private static final String ARG_LON = "longitude";
    private static final String WEATHER_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";

    private TextView tvLocationName, tvCurrentTemp, tvWeatherStatus, tvFineDustStatus, tvUltrafineDustStatus;
    private TextView tvTabHourly, tvTabDaily, tvUpdateTime, tvWeatherSource, tvMoreInfo;
    private ImageView ivCloseButton, ivCurrentWeatherIcon;
    private RecyclerView rvHourlyForecast;
    private WeatherForecastAdapter forecastAdapter;

    public static WeatherBottomSheetFragment newInstance(double latitude, double longitude) {
        WeatherBottomSheetFragment fragment = new WeatherBottomSheetFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, latitude);
        args.putDouble(ARG_LON, longitude);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupClickListeners();
        if (getArguments() != null) {
            double lat = getArguments().getDouble(ARG_LAT);
            double lon = getArguments().getDouble(ARG_LON);
            fetchWeatherData(lat, lon);
        }
    }

    private void initializeViews(View view) {
        // 위치 및 닫기 버튼
        tvLocationName = view.findViewById(R.id.tv_location_name);
        ivCloseButton = view.findViewById(R.id.iv_close_button);
        
        // 현재 날씨 정보
        tvCurrentTemp = view.findViewById(R.id.tv_current_temperature);
        tvWeatherStatus = view.findViewById(R.id.tv_current_weather_status);
        ivCurrentWeatherIcon = view.findViewById(R.id.iv_current_weather_icon);
        tvFineDustStatus = view.findViewById(R.id.tv_fine_dust_status);
        tvUltrafineDustStatus = view.findViewById(R.id.tv_ultrafine_dust_status);
        
        // 탭
        tvTabHourly = view.findViewById(R.id.tv_tab_hourly);
        tvTabDaily = view.findViewById(R.id.tv_tab_daily);
        
        // 시간별 예보
        rvHourlyForecast = view.findViewById(R.id.rv_hourly_forecast);
        rvHourlyForecast.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        forecastAdapter = new WeatherForecastAdapter();
        rvHourlyForecast.setAdapter(forecastAdapter);
        
        // 하단 정보
        tvUpdateTime = view.findViewById(R.id.tv_update_time);
        tvWeatherSource = view.findViewById(R.id.tv_weather_source);
        tvMoreInfo = view.findViewById(R.id.tv_more_info);
    }
    
    private void setupClickListeners() {
        ivCloseButton.setOnClickListener(v -> dismiss());
        
        tvTabHourly.setOnClickListener(v -> {
            tvTabHourly.setTextColor(getResources().getColor(android.R.color.black));
            tvTabHourly.setBackground(getResources().getDrawable(R.drawable.tab_selected_indicator));
            tvTabDaily.setTextColor(getResources().getColor(R.color.textColorSecondary));
            tvTabDaily.setBackground(null);
        });
        
        tvTabDaily.setOnClickListener(v -> {
            tvTabDaily.setTextColor(getResources().getColor(android.R.color.black));
            tvTabDaily.setBackground(getResources().getDrawable(R.drawable.tab_selected_indicator));
            tvTabHourly.setTextColor(getResources().getColor(R.color.textColorSecondary));
            tvTabHourly.setBackground(null);
        });
    }

    private void fetchWeatherData(double lat, double lon) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                String urlString = String.format(
                        "https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s&units=metric&lang=kr&cnt=8",
                        lat, lon, WEATHER_API_KEY
                );
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject json = new JSONObject(response.toString());
                final FullWeatherData fullData = parseForecastData(json);
                handler.post(() -> {
                    updateUI(fullData);
                });
            } catch (Exception e) {
                Log.e("WeatherAPI", "날씨 정보 로드 실패", e);
                handler.post(() -> {
                    // 기본 데이터로 UI 업데이트
                    updateUIWithDefaultData();
                });
            }
        });
    }

    private FullWeatherData parseForecastData(JSONObject json) throws Exception {
        JSONArray list = json.getJSONArray("list");
        if (list.length() == 0) return null;
        WeatherData current = jsonToWeatherData(list.getJSONObject(0));
        List<WeatherData> forecast = new ArrayList<>();
        for (int i = 1; i < list.length(); i++) {
            forecast.add(jsonToWeatherData(list.getJSONObject(i)));
        }
        return new FullWeatherData(current, forecast);
    }

    private WeatherData jsonToWeatherData(JSONObject item) throws Exception {
        JSONObject main = item.getJSONObject("main");
        JSONObject wind = item.getJSONObject("wind");
        JSONArray weatherArray = item.getJSONArray("weather");
        JSONObject weatherInfo = weatherArray.getJSONObject(0);
        String dt_txt = item.getString("dt_txt");
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
        SimpleDateFormat outputFormat = new SimpleDateFormat("a h시", Locale.KOREA);
        Date date = inputFormat.parse(dt_txt);
        String formattedTime = (date != null) ? outputFormat.format(date) : "";
        return new WeatherData(
                main.getDouble("temp"),
                weatherInfo.getString("description"),
                weatherInfo.getString("main"),
                main.getInt("humidity"),
                wind.getDouble("speed"),
                formattedTime
        );
    }

    private void updateUI(FullWeatherData fullData) {
        if (fullData == null) {
            updateUIWithDefaultData();
            return;
        }
        WeatherData current = fullData.current;
        tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f°", current.temperature));
        tvWeatherStatus.setText(current.description + ". 어제보다 3° 낮아요");
        ivCurrentWeatherIcon.setImageResource(getWeatherIconResource(current.weatherMain));
        forecastAdapter.updateData(fullData.forecast);
        
        // 업데이트 시간 설정
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd. a hh:mm", Locale.KOREA);
        tvUpdateTime.setText("업데이트 " + sdf.format(new Date()));
    }
    
    private void updateUIWithDefaultData() {
        tvLocationName.setText("부천시 소사구 괴안동");
        tvCurrentTemp.setText("24.9°");
        tvWeatherStatus.setText("흐림. 어제보다 3° 낮아요");
        tvFineDustStatus.setText("좋음");
        tvUltrafineDustStatus.setText("좋음");
        
        // 기본 예보 데이터
        List<WeatherData> defaultForecast = new ArrayList<>();
        defaultForecast.add(new WeatherData(25, "20%", "Clouds", 0, 0, "16시"));
        defaultForecast.add(new WeatherData(24, "30%", "Clouds", 0, 0, "17시"));
        defaultForecast.add(new WeatherData(23, "30%", "Rain", 0, 0, "18시"));
        defaultForecast.add(new WeatherData(22, "40%", "Rain", 0, 0, "19시"));
        defaultForecast.add(new WeatherData(21, "20%", "Clouds", 0, 0, "20시"));
        defaultForecast.add(new WeatherData(20, "10%", "Clouds", 0, 0, "21시"));
        defaultForecast.add(new WeatherData(19, "5%", "Clear", 0, 0, "22시"));
        
        forecastAdapter.updateData(defaultForecast);
        
        // 업데이트 시간 설정
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd. a hh:mm", Locale.KOREA);
        tvUpdateTime.setText("업데이트 " + sdf.format(new Date()));
    }
    
    private int getWeatherIconResource(String weatherMain) {
        switch (weatherMain.toLowerCase()) {
            case "clear":
                return R.drawable.ic_weather_clear;
            case "clouds":
                return R.drawable.ic_cloudy;
            case "rain":
                return R.drawable.ic_weather_rainy;
            case "snow":
                return R.drawable.ic_weather_snow;
            default:
                return R.drawable.ic_cloudy;
        }
    }

    public static class WeatherData {
        public final double temperature;
        public final String description;
        public final String weatherMain;
        public final int humidity;
        public final double windSpeed;
        public final String time;

        WeatherData(double temp, String desc, String main, int humidity, double windSpeed, String time) {
            this.temperature = temp;
            this.description = desc;
            this.weatherMain = main;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.time = time;
        }
    }

    public static class FullWeatherData {
        final WeatherData current;
        final List<WeatherData> forecast;

        FullWeatherData(WeatherData current, List<WeatherData> forecast) {
            this.current = current;
            this.forecast = forecast;
        }
    }
}
