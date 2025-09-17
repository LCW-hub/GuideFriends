package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import com.example.gps.model.WeatherForecast;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.ForecastViewHolder> {
    private List<WeatherForecast.ForecastItem> forecastItems = new ArrayList<>();
    private Map<String, WeatherForecast.ForecastItem> dailyForecasts = new HashMap<>();

    public void setForecastItems(List<WeatherForecast.ForecastItem> items) {
        this.forecastItems = items;
        processDailyForecasts();
        notifyDataSetChanged();
    }

    private void processDailyForecasts() {
        dailyForecasts.clear();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

        for (WeatherForecast.ForecastItem item : forecastItems) {
            try {
                Date date = inputFormat.parse(item.getDateText());
                String dayKey = outputFormat.format(date);
                
                // 각 날짜별로 첫 번째 데이터만 저장
                if (!dailyForecasts.containsKey(dayKey)) {
                    dailyForecasts.put(dayKey, item);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        List<String> sortedDays = new ArrayList<>(dailyForecasts.keySet());
        sortedDays.sort(String::compareTo);
        
        if (position < sortedDays.size()) {
            String dayKey = sortedDays.get(position);
            WeatherForecast.ForecastItem item = dailyForecasts.get(dayKey);
            
            if (item != null) {
                // 날짜 포맷팅
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM월 dd일 (E)", Locale.KOREA);
                try {
                    Date date = inputFormat.parse(dayKey);
                    holder.tvDateTime.setText(outputFormat.format(date));
                } catch (ParseException e) {
                    holder.tvDateTime.setText(dayKey);
                }

                // 온도 (켈빈에서 섭씨로 변환)
                double tempCelsius = item.getMain().getTemp() - 273.15;
                holder.tvTemperature.setText(String.format("%.1f°C", tempCelsius));

                // 날씨 상태와 아이콘
                if (!item.getWeather().isEmpty()) {
                    String weatherDescription = item.getWeather().get(0).getDescription();
                    String weatherIcon = item.getWeather().get(0).getIcon();
                    holder.tvWeatherStatus.setText(weatherDescription);
                    
                    // 날씨 아이콘 설정
                    int iconResource = getWeatherIconResource(weatherIcon);
                    holder.ivWeatherIcon.setImageResource(iconResource);
                }

                // 습도
                holder.tvHumidity.setText(String.format("습도: %d%%", item.getMain().getHumidity()));
            }
        }
    }

    private int getWeatherIconResource(String weatherIcon) {
        // OpenWeatherMap 아이콘 코드에 따른 리소스 매핑
        switch (weatherIcon) {
            case "01d": // 맑음 (낮)
            case "01n": // 맑음 (밤)
                return R.drawable.ic_weather_clear;
            case "02d": // 구름 조금 (낮)
            case "02n": // 구름 조금 (밤)
            case "03d": // 구름 많음
            case "03n":
            case "04d": // 흐림
            case "04n":
                return R.drawable.ic_weather_cloudy;
            case "09d": // 소나기
            case "09n":
            case "10d": // 비
            case "10n":
                return R.drawable.ic_weather_rainy;
            case "11d": // 천둥번개
            case "11n":
                return R.drawable.ic_weather_rainy;
            case "13d": // 눈
            case "13n":
                return R.drawable.ic_weather_cloudy;
            case "50d": // 안개
            case "50n":
                return R.drawable.ic_weather_cloudy;
            default:
                return R.drawable.ic_weather_clear;
        }
    }

    @Override
    public int getItemCount() {
        return dailyForecasts.size();
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvTemperature, tvWeatherStatus, tvHumidity;
        ImageView ivWeatherIcon;

        ForecastViewHolder(View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvTemperature = itemView.findViewById(R.id.tvTemperature);
            tvWeatherStatus = itemView.findViewById(R.id.tvWeatherStatus);
            tvHumidity = itemView.findViewById(R.id.tvHumidity);
            ivWeatherIcon = itemView.findViewById(R.id.ivWeatherIcon);
        }
    }
}