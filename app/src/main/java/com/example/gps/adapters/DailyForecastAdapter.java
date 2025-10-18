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

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.DailyViewHolder> {

    private final List<DailyData> dailyDataList;

    public DailyForecastAdapter(List<DailyData> dailyDataList) {
        this.dailyDataList = dailyDataList;
    }

    @NonNull
    @Override
    public DailyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_forecast, parent, false);
        return new DailyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyViewHolder holder, int position) {
        DailyData data = dailyDataList.get(position);
        holder.tvDay.setText(data.day);
        holder.ivWeatherIcon.setImageResource(data.iconRes);
        holder.tvMinTemp.setText(data.minTemp);
        holder.tvMaxTemp.setText(data.maxTemp);
    }

    @Override
    public int getItemCount() {
        return dailyDataList.size();
    }

    static class DailyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvMinTemp, tvMaxTemp;
        ImageView ivWeatherIcon;

        public DailyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            ivWeatherIcon = itemView.findViewById(R.id.iv_daily_weather_icon);
            tvMinTemp = itemView.findViewById(R.id.tv_min_temp);
            tvMaxTemp = itemView.findViewById(R.id.tv_max_temp);
        }
    }

    public static class DailyData {
        public final String day;
        public final int iconRes;
        public final String minTemp;
        public final String maxTemp;

        public DailyData(String day, int iconRes, String minTemp, String maxTemp) {
            this.day = day;
            this.iconRes = iconRes;
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
        }
    }
}