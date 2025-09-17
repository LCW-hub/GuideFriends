package com.example.gps.api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TransportApi {
    private static final String BASE_URL = "https://your-api-server.com/api";
    
    // 주변 대중교통 정보 가져오기
    public static List<TransportInfo> getNearbyTransport(double latitude, double longitude, double radius) {
        try {
            String params = String.format("?lat=%f&lng=%f&radius=%f", latitude, longitude, radius);
            URL url = new URL(BASE_URL + "/transport/nearby" + params);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONArray jsonArray = new JSONArray(response.toString());
            List<TransportInfo> transports = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                transports.add(parseTransportInfo(json));
            }
            
            return transports;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // 경로별 대중교통 정보 가져오기
    public static List<TransportInfo> getTransportForCourse(int courseId) {
        try {
            URL url = new URL(BASE_URL + "/transport/course/" + courseId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONArray jsonArray = new JSONArray(response.toString());
            List<TransportInfo> transports = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                transports.add(parseTransportInfo(json));
            }
            
            return transports;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private static TransportInfo parseTransportInfo(JSONObject json) {
        try {
            TransportInfo transport = new TransportInfo();
            transport.setType(json.getString("type"));
            transport.setName(json.getString("name"));
            transport.setDistance(json.getDouble("distance"));
            transport.setWalkingTime(json.getInt("walkingTime"));
            transport.setDescription(json.getString("description"));
            
            return transport;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 대중교통 정보를 담는 내부 클래스
    public static class TransportInfo {
        private String type; // "BUS", "SUBWAY", "TRAIN"
        private String name;
        private double distance; // km
        private int walkingTime; // minutes
        private String description;
        
        public TransportInfo() {}
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        
        public int getWalkingTime() { return walkingTime; }
        public void setWalkingTime(int walkingTime) { this.walkingTime = walkingTime; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
} 