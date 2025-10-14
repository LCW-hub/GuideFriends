package com.example.gps.model;

import java.util.List;

public class WeatherInfo {
    private String name; // 지역 이름
    private Main main;
    private List<Weather> weather;
    private Wind wind;
    
    public String getName() {
        return name;
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
    
    public static class Main {
        private double temp;
        private int humidity;
        
        public double getTemp() {
            return temp;
        }
        
        public int getHumidity() {
            return humidity;
        }
    }
    
    public static class Weather {
        private String description;
        
        public String getDescription() {
            return description;
        }
    }
    
    public static class Wind {
        private double speed;
        
        public double getSpeed() {
            return speed;
        }
    }
}