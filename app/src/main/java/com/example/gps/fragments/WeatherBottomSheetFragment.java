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
import com.example.gps.adapters.DailyForecastAdapter;
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
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_LAT = "latitude";
    private static final String ARG_LON = "longitude";
    private static final String WEATHER_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";

    private TextView tvLocationName, tvCurrentTemp, tvWeatherStatus;
    private TextView tvTabHourly, tvTabDaily, tvUpdateTime;
    private ImageView ivCloseButton, ivCurrentWeatherIcon;

    private RecyclerView rvHourlyForecast;
    private RecyclerView rvDailyForecast;
    private WeatherForecastAdapter hourlyAdapter;
    private DailyForecastAdapter dailyAdapter;

    private final List<WeatherForecastAdapter.WeatherData> hourlyForecastList = new ArrayList<>();
    private final List<DailyForecastAdapter.DailyData> dailyForecastList = new ArrayList<>();


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
        } else {
            fetchWeatherData(37.5665, 126.9780);
        }
    }

    private void initializeViews(View view) {
        tvLocationName = view.findViewById(R.id.tv_location_name);
        ivCloseButton = view.findViewById(R.id.iv_close_button);
        tvCurrentTemp = view.findViewById(R.id.tv_current_temperature);
        tvWeatherStatus = view.findViewById(R.id.tv_current_weather_status);
        ivCurrentWeatherIcon = view.findViewById(R.id.iv_current_weather_icon);
        tvTabHourly = view.findViewById(R.id.tv_tab_hourly);
        tvTabDaily = view.findViewById(R.id.tv_tab_daily);
        tvUpdateTime = view.findViewById(R.id.tv_update_time);

        rvHourlyForecast = view.findViewById(R.id.rv_hourly_forecast);
        rvHourlyForecast.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        hourlyAdapter = new WeatherForecastAdapter(hourlyForecastList);
        rvHourlyForecast.setAdapter(hourlyAdapter);

        rvDailyForecast = view.findViewById(R.id.rv_daily_forecast);
        rvDailyForecast.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        dailyAdapter = new DailyForecastAdapter(dailyForecastList);
        rvDailyForecast.setAdapter(dailyAdapter);

        showHourlyForecast();
    }

    private void setupClickListeners() {
        ivCloseButton.setOnClickListener(v -> dismiss());
        tvTabHourly.setOnClickListener(v -> showHourlyForecast());
        tvTabDaily.setOnClickListener(v -> showDailyForecast());
    }

    private void showHourlyForecast() {
        tvTabHourly.setTextColor(getResources().getColor(android.R.color.black));
        tvTabHourly.setBackgroundResource(R.drawable.tab_selected_indicator);
        tvTabDaily.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tvTabDaily.setBackground(null);
        rvHourlyForecast.setVisibility(View.VISIBLE);
        rvDailyForecast.setVisibility(View.GONE);
    }

    private void showDailyForecast() {
        tvTabDaily.setTextColor(getResources().getColor(android.R.color.black));
        tvTabDaily.setBackgroundResource(R.drawable.tab_selected_indicator);
        tvTabHourly.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tvTabHourly.setBackground(null);
        rvHourlyForecast.setVisibility(View.GONE);
        rvDailyForecast.setVisibility(View.VISIBLE);
    }

    private void fetchWeatherData(double lat, double lon) {
        fetchLocationName(lat, lon);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                String urlString = String.format(
                        "https://api.openweathermap.org/data/3.0/onecall?lat=%f&lon=%f&exclude=minutely,alerts&appid=%s&units=metric&lang=kr",
                        lat, lon, WEATHER_API_KEY
                );
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    JSONObject json = new JSONObject(response.toString());
                    handler.post(() -> parseAndSetData(json));
                } else {
                    Log.e("WeatherBottomSheet", "HTTP 오류: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("WeatherBottomSheet", "날씨 정보 로드 실패", e);
            }
        });
    }

    private void parseAndSetData(JSONObject json) {
        try {
            JSONObject current = json.getJSONObject("current");
            double currentTemp = current.getDouble("temp");
            JSONObject weatherInfo = current.getJSONArray("weather").getJSONObject(0);
            String description = weatherInfo.getString("description");
            String weatherMain = weatherInfo.getString("main");
            tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f°", currentTemp));
            tvWeatherStatus.setText(description);
            ivCurrentWeatherIcon.setImageResource(getWeatherIconResource(weatherMain));
            hourlyForecastList.clear();
            JSONArray hourly = json.getJSONArray("hourly");
            for (int i = 0; i < 24; i++) {
                JSONObject item = hourly.getJSONObject(i);
                hourlyForecastList.add(new WeatherForecastAdapter.WeatherData(
                        formatTime(item.getLong("dt"), "a h시"),
                        getWeatherIconResource(item.getJSONArray("weather").getJSONObject(0).getString("main")),
                        String.format(Locale.getDefault(), "%.0f°", item.getDouble("temp"))
                ));
            }
            hourlyAdapter.notifyDataSetChanged();
            dailyForecastList.clear();
            JSONArray daily = json.getJSONArray("daily");
            for (int i = 0; i < daily.length(); i++) {
                JSONObject item = daily.getJSONObject(i);
                dailyForecastList.add(new DailyForecastAdapter.DailyData(
                        formatTime(item.getLong("dt"), "E요일"),
                        getWeatherIconResource(item.getJSONArray("weather").getJSONObject(0).getString("main")),
                        String.format(Locale.getDefault(), "%.0f°", item.getJSONObject("temp").getDouble("min")),
                        String.format(Locale.getDefault(), "%.0f°", item.getJSONObject("temp").getDouble("max"))
                ));
            }
            dailyAdapter.notifyDataSetChanged();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd. a hh:mm", Locale.KOREA);
            tvUpdateTime.setText("업데이트 " + sdf.format(new Date()));
        } catch (Exception e) {
            Log.e("WeatherBottomSheet", "JSON 파싱 실패", e);
        }
    }

    private String formatTime(long unixSeconds, String format) {
        Date date = new Date(unixSeconds * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.KOREA);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return sdf.format(date);
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

    private void fetchLocationName(double lat, double lon) {
        android.location.Geocoder geocoder = new android.location.Geocoder(getContext(), Locale.KOREAN);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address address = addresses.get(0);

                    // ★★★ 상세 주소(동, 도로명) 정보 추가 ★★★
                    String locality = address.getLocality() != null ? address.getLocality() : ""; // 시
                    String subLocality = address.getSubLocality() != null ? address.getSubLocality() : ""; // 구
                    String thoroughfare = address.getThoroughfare() != null ? address.getThoroughfare() : ""; // 동 또는 도로명

                    // ★★★ 조합 방식 변경 ★★★
                    final String locationName = (locality + " " + subLocality + " " + thoroughfare).trim();

                    handler.post(() -> {
                        if (!locationName.isEmpty()) {
                            tvLocationName.setText("📍 " + locationName);
                        } else {
                            tvLocationName.setText("📍 위치 이름 확인 불가");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("Geocoder", "주소 변환 실패", e);
                handler.post(() -> tvLocationName.setText("📍 위치 이름 확인 불가"));
            }
        });
    }
}