package com.example.gps.api;

import com.example.gps.model.CourseDetail;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CourseApi {
    private static final String BASE_URL = "https://your-api-server.com/api";
    
    // 코스 상세 정보 가져오기
    public static CourseDetail getCourseDetail(int courseId) {
        try {
            URL url = new URL(BASE_URL + "/courses/" + courseId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONObject json = new JSONObject(response.toString());
            return parseCourseDetail(json);
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    // 유동인구 정보 업데이트
    public static boolean updateCrowdLevel(int courseId, int crowdLevel) {
        try {
            URL url = new URL(BASE_URL + "/courses/" + courseId + "/crowd");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            JSONObject body = new JSONObject();
            body.put("crowdLevel", crowdLevel);
            
            conn.getOutputStream().write(body.toString().getBytes());
            
            return conn.getResponseCode() == 200;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static CourseDetail parseCourseDetail(JSONObject json) {
        try {
            CourseDetail course = new CourseDetail();
            course.setCourseId(json.getInt("courseId"));
            course.setCourseName(json.getString("courseName"));
            course.setDescription(json.getString("description"));
            course.setDistance(json.getDouble("distance"));
            course.setDuration(json.getInt("duration"));
            course.setSteps(json.getInt("steps"));
            course.setPetFriendly(json.getBoolean("petFriendly"));
            course.setCalories(json.getDouble("calories"));
            course.setWaterIntake(json.getDouble("waterIntake"));
            course.setDifficulty(json.getString("difficulty"));
            course.setCrowdLevel(json.getInt("crowdLevel"));
            course.setRating(json.getDouble("rating"));
            course.setReviewCount(json.getInt("reviewCount"));
            
            return course;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    
} 