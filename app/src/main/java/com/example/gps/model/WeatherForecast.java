package com.example.gps.model;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class WeatherForecast {
    @SerializedName("list")
    private List<ForecastItem> forecastItems;
    @SerializedName("city")
    private City city;

    public List<ForecastItem> getForecastItems() {
        return forecastItems;
    }

    public City getCity() {
        return city;
    }

    public static class ForecastItem {
        @SerializedName("dt")
        private long timestamp;
        @SerializedName("main")
        private Main main;
        @SerializedName("weather")
        private List<Weather> weather;
        @SerializedName("wind")
        private Wind wind;
        @SerializedName("dt_txt")
        private String dateText;

        public long getTimestamp() {
            return timestamp;
        }

        public Main getMain() {
            return main;
        }

        public List<Weather> getWeather() {
            return weather;
        }

        public Wind getWind() {
            return wind;
        }

        public String getDateText() {
            return dateText;
        }
    }

    public static class City {
        @SerializedName("name")
        private String name;
        @SerializedName("country")
        private String country;

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }
    }

    public static class Main {
        @SerializedName("temp")
        private double temp;
        @SerializedName("humidity")
        private int humidity;
        @SerializedName("temp_min")
        private double tempMin;
        @SerializedName("temp_max")
        private double tempMax;

        public double getTemp() {
            return temp;
        }

        public int getHumidity() {
            return humidity;
        }

        public double getTempMin() {
            return tempMin;
        }

        public double getTempMax() {
            return tempMax;
        }
    }

    public static class Weather {
        @SerializedName("description")
        private String description;
        @SerializedName("icon")
        private String icon;

        public String getDescription() {
            return description;
        }

        public String getIcon() {
            return icon;
        }
    }

    public static class Wind {
        @SerializedName("speed")
        private double speed;

        public double getSpeed() {
            return speed;
        }
    }
}