package com.example.gps.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gps.R;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.api.WeatherApi;
import com.example.gps.model.WeatherInfo;
import com.example.gps.model.WeatherForecast;
import com.example.gps.adapters.WeatherForecastAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {

    private TextView tvDate, tvTemperature, tvWeatherStatus, tvHumidity, tvWindSpeed, tvRecommendation;
    private RecyclerView rvForecast;
    private WeatherForecastAdapter forecastAdapter;
    private static final String WEATHER_API_KEY = "7a4aa78797771aa887fe9b14a9be94e5";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("날씨 정보");

        // 뷰 초기화
        tvDate = findViewById(R.id.tvDate);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvWeatherStatus = findViewById(R.id.tvWeatherStatus);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        tvRecommendation = findViewById(R.id.tvRecommendation);
        rvForecast = findViewById(R.id.rvForecast);

        // 현재 날짜 설정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREA);
        tvDate.setText(dateFormat.format(new Date()));

        // RecyclerView 설정
        rvForecast.setLayoutManager(new LinearLayoutManager(this));
        forecastAdapter = new WeatherForecastAdapter();
        rvForecast.setAdapter(forecastAdapter);

        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
            
        WeatherApi weatherApi = retrofit.create(WeatherApi.class);
        
        // 현재 날씨 정보 요청
        Call<WeatherInfo> currentWeatherCall = weatherApi.getWeather("Seoul", WEATHER_API_KEY);
        currentWeatherCall.enqueue(new Callback<WeatherInfo>() {
            @Override
            public void onResponse(Call<WeatherInfo> call, Response<WeatherInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherInfo weather = response.body();
                    updateWeatherUI(weather);
                } else {
                    Toast.makeText(WeatherActivity.this, "날씨 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<WeatherInfo> call, Throwable t) {
                Toast.makeText(WeatherActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 5일 예보 정보 요청
        Call<WeatherForecast> forecastCall = weatherApi.getWeatherForecast("Seoul", WEATHER_API_KEY);
        forecastCall.enqueue(new Callback<WeatherForecast>() {
            @Override
            public void onResponse(Call<WeatherForecast> call, Response<WeatherForecast> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherForecast forecast = response.body();
                    // forecastAdapter.updateData(forecast.getForecastItems());
                } else {
                    Toast.makeText(WeatherActivity.this, "예보 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherForecast> call, Throwable t) {
                Toast.makeText(WeatherActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWeatherUI(WeatherInfo weather) {
        double tempCelsius = weather.getMain().getTemp() - 273.15;
        tvTemperature.setText(String.format("%.1f°C", tempCelsius));
        tvWeatherStatus.setText(weather.getWeather().get(0).getDescription());
        tvHumidity.setText(String.format("습도: %d%%", weather.getMain().getHumidity()));
        tvWindSpeed.setText(String.format("풍속: %.1f m/s", weather.getWind().getSpeed()));
        
        // 산책 추천 메시지
        String recommendation = getWeatherRecommendation(tempCelsius, weather.getWeather().get(0).getDescription());
        tvRecommendation.setText(recommendation);
    }
    
    private String getWeatherRecommendation(double temperature, String weatherDescription) {
        StringBuilder recommendation = new StringBuilder("🌿 산책 추천: ");
        
        if (temperature < 0) {
            recommendation.append("매우 추운 날씨입니다. 따뜻하게 입고 짧은 산책을 권장합니다.");
        } else if (temperature < 10) {
            recommendation.append("쌀쌀한 날씨입니다. 겉옷을 챙기고 산책하세요.");
        } else if (temperature < 20) {
            recommendation.append("산책하기 좋은 날씨입니다. 가벼운 옷차림으로 나가세요.");
        } else if (temperature < 30) {
            recommendation.append("따뜻한 날씨입니다. 충분한 수분 섭취를 잊지 마세요.");
        } else {
            recommendation.append("더운 날씨입니다. 오전이나 저녁 시간대 산책을 권장합니다.");
        }
        
        if (weatherDescription.contains("rain") || weatherDescription.contains("비")) {
            recommendation.append("\n☔ 비가 오고 있습니다. 우산을 챙기세요.");
        } else if (weatherDescription.contains("snow") || weatherDescription.contains("눈")) {
            recommendation.append("\n❄️ 눈이 내리고 있습니다. 미끄러지지 않도록 주의하세요.");
        } else if (weatherDescription.contains("clear") || weatherDescription.contains("맑음")) {
            recommendation.append("\n☀️ 맑은 날씨입니다. 자외선 차단제를 바르고 산책하세요.");
        }
        
        return recommendation.toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 