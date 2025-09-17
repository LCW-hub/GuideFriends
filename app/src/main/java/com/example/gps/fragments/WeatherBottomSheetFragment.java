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
import com.example.gps.adapters.ForecastAdapter;
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

    private TextView tvCurrentTemp, tvWeatherDesc, tvHumidity, tvWindSpeed;
    private ImageView ivWeatherIcon;
    private View loadingIndicator;
    private RecyclerView forecastRecyclerView;
    private ForecastAdapter forecastAdapter;

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
        if (getArguments() != null) {
            double lat = getArguments().getDouble(ARG_LAT);
            double lon = getArguments().getDouble(ARG_LON);
            fetchWeatherData(lat, lon);
        }
    }

    private void initializeViews(View view) {
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvWeatherDesc = view.findViewById(R.id.tv_weather_desc);
        tvHumidity = view.findViewById(R.id.tv_humidity);
        tvWindSpeed = view.findViewById(R.id.tv_wind_speed);
        ivWeatherIcon = view.findViewById(R.id.iv_weather_icon_large);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        forecastRecyclerView = view.findViewById(R.id.forecast_recycler_view);
        forecastAdapter = new ForecastAdapter();
        forecastRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        forecastRecyclerView.setAdapter(forecastAdapter);
    }

    private void fetchWeatherData(double lat, double lon) {
        loadingIndicator.setVisibility(View.VISIBLE);
        forecastRecyclerView.setVisibility(View.GONE);
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
                    loadingIndicator.setVisibility(View.GONE);
                    forecastRecyclerView.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                Log.e("WeatherAPI", "날씨 정보 로드 실패", e);
                handler.post(() -> loadingIndicator.setVisibility(View.GONE));
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
        if (fullData == null) return;
        WeatherData current = fullData.current;
        tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f°", current.temperature));
        tvWeatherDesc.setText(current.description);
        tvHumidity.setText(String.format(Locale.getDefault(), "%d%%", current.humidity));
        tvWindSpeed.setText(String.format(Locale.getDefault(), "%.1f m/s", current.windSpeed));
        ivWeatherIcon.setImageResource(ForecastAdapter.ViewHolder.getWeatherIconResource(current.weatherMain));
        forecastAdapter.updateData(fullData.forecast);
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
