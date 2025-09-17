package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import com.example.gps.fragments.WeatherBottomSheetFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private final List<WeatherBottomSheetFragment.WeatherData> forecastList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherBottomSheetFragment.WeatherData data = forecastList.get(position);
        holder.bind(data);
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    public void updateData(List<WeatherBottomSheetFragment.WeatherData> newData) {
        forecastList.clear();
        forecastList.addAll(newData);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTime;
        private final ImageView ivIcon;
        private final TextView tvTemp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_forecast_time);
            ivIcon = itemView.findViewById(R.id.iv_forecast_icon);
            tvTemp = itemView.findViewById(R.id.tv_forecast_temp);
        }

        public void bind(WeatherBottomSheetFragment.WeatherData data) {
            tvTime.setText(data.time);
            ivIcon.setImageResource(getWeatherIconResource(data.weatherMain));
            tvTemp.setText(String.format(Locale.getDefault(), "%.0f°", data.temperature));
        }

        public static int getWeatherIconResource(String weatherMain) {
            if (weatherMain == null) return R.drawable.ic_weather_clear;
            switch (weatherMain.toLowerCase()) {
                case "clear": return R.drawable.ic_weather_clear;
                case "clouds": return R.drawable.ic_weather_cloudy;
                case "rain": case "drizzle": case "thunderstorm":
                    return R.drawable.ic_weather_rainy;
                case "snow": return R.drawable.ic_weather_rainy;
                case "mist": case "fog": case "haze":
                    return R.drawable.ic_weather_cloudy;
                default: return R.drawable.ic_weather_clear;
            }
        }
    }
}
