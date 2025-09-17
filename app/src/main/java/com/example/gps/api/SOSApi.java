package com.example.gps.api;

import com.example.gps.model.SOSAlert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SOSApi {
    private static final String BASE_URL = "https://your-api-server.com/api";
    
    // SOS 알림 전송
    public static boolean sendSOSAlert(SOSAlert alert) {
        try {
            URL url = new URL(BASE_URL + "/sos/alerts");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            JSONObject body = new JSONObject();
            body.put("userId", alert.getUserId());
            body.put("userName", alert.getUserName());
            body.put("latitude", alert.getLatitude());
            body.put("longitude", alert.getLongitude());
            body.put("location", alert.getLocation());
            body.put("message", alert.getMessage());
            body.put("emergencyType", alert.getEmergencyType());
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }
            
            return conn.getResponseCode() == 200;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 주변 SOS 알림 가져오기
    public static List<SOSAlert> getNearbySOSAlerts(double latitude, double longitude, double radius) {
        try {
            String params = String.format("?lat=%f&lng=%f&radius=%f", latitude, longitude, radius);
            URL url = new URL(BASE_URL + "/sos/alerts/nearby" + params);
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
            List<SOSAlert> alerts = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                alerts.add(parseSOSAlert(json));
            }
            
            return alerts;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // SOS 알림 응답
    public static boolean respondToSOSAlert(String alertId, String responderId, String message) {
        try {
            URL url = new URL(BASE_URL + "/sos/alerts/" + alertId + "/respond");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            JSONObject body = new JSONObject();
            body.put("responderId", responderId);
            body.put("message", message);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }
            
            return conn.getResponseCode() == 200;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // SOS 알림 해결
    public static boolean resolveSOSAlert(String alertId) {
        try {
            URL url = new URL(BASE_URL + "/sos/alerts/" + alertId + "/resolve");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            
            return conn.getResponseCode() == 200;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static SOSAlert parseSOSAlert(JSONObject json) {
        try {
            SOSAlert alert = new SOSAlert();
            alert.setAlertId(json.getString("alertId"));
            alert.setUserId(json.getString("userId"));
            alert.setUserName(json.getString("userName"));
            alert.setLatitude(json.getDouble("latitude"));
            alert.setLongitude(json.getDouble("longitude"));
            alert.setLocation(json.getString("location"));
            alert.setMessage(json.getString("message"));
            alert.setEmergencyType(json.getString("emergencyType"));
            alert.setStatus(json.getString("status"));
            alert.setResponseCount(json.getInt("responseCount"));
            
            return alert;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
} 