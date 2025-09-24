package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import com.example.gps.fragments.WeatherBottomSheetFragment.WeatherData;
import java.util.ArrayList;
import java.util.List;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.ViewHolder> {

    private List<WeatherData> forecastList = new ArrayList<>();

    public void updateData(List<WeatherData> newForecastList) {
        this.forecastList = newForecastList != null ? newForecastList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherData weatherData = forecastList.get(position);
        holder.bind(weatherData);
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTime, tvPrecipitation, tvTemperature;
        private ImageView ivWeatherIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPrecipitation = itemView.findViewById(R.id.tv_precipitation);
            tvTemperature = itemView.findViewById(R.id.tv_temperature);
            ivWeatherIcon = itemView.findViewById(R.id.iv_weather_icon);
        }

        public void bind(WeatherData weatherData) {
            tvTime.setText(weatherData.time);
            tvPrecipitation.setText(weatherData.description);
            tvTemperature.setText(String.format("%.0f°", weatherData.temperature));
            ivWeatherIcon.setImageResource(getWeatherIconResource(weatherData.weatherMain));
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
    }
}