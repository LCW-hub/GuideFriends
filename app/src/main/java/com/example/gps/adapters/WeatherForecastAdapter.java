package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import java.util.List;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.WeatherViewHolder> {

    private final List<WeatherData> forecastList;

    public WeatherForecastAdapter(List<WeatherData> forecastList) {
        this.forecastList = forecastList;
    }

    // ... (onCreateViewHolder, onBindViewHolder, getItemCount, WeatherViewHolder 코드는 동일)
    @NonNull
    @Override
    public WeatherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weather_forecast, parent, false);
        return new WeatherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherViewHolder holder, int position) {
        WeatherData data = forecastList.get(position);
        holder.tvTime.setText(data.time);
        holder.ivWeatherIcon.setImageResource(data.iconRes);
        holder.tvTemperature.setText(data.temperature);
    }

    @Override
    public int getItemCount() { return forecastList.size(); }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTemperature;
        ImageView ivWeatherIcon;
        public WeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivWeatherIcon = itemView.findViewById(R.id.iv_weather_icon);
            tvTemperature = itemView.findViewById(R.id.tv_temperature);
        }
    }


    // ★★★★★ '빨간 글씨'의 원인 ★★★★★
    // WeatherData 클래스는 반드시 이 파일 안에, 'public static'으로 있어야 합니다.
    public static class WeatherData {
        public final String time;
        public final int iconRes;
        public final String temperature;

        public WeatherData(String time, int iconRes, String temperature) {
            this.time = time;
            this.iconRes = iconRes;
            this.temperature = temperature;
        }
    }
}