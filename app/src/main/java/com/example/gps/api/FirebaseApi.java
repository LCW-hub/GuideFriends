package com.example.gps.api;

import com.example.gps.model.CourseDetail;
import com.example.gps.model.SOSAlert;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FirebaseApi {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    // 코스 상세 정보 가져오기
    public static CompletableFuture<CourseDetail> getCourseDetail(int courseId) {
        CompletableFuture<CourseDetail> future = new CompletableFuture<>();
        
        db.collection("courses").document(String.valueOf(courseId))
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    CourseDetail course = new CourseDetail();
                    course.setCourseId(document.getLong("courseId").intValue());
                    course.setCourseName(document.getString("courseName"));
                    course.setDescription(document.getString("description"));
                    course.setDistance(document.getDouble("distance"));
                    course.setDuration(document.getLong("duration").intValue());
                    course.setSteps(document.getLong("steps").intValue());
                    course.setPetFriendly(document.getBoolean("petFriendly"));
                    course.setCalories(document.getDouble("calories"));
                    course.setWaterIntake(document.getDouble("waterIntake"));
                    course.setDifficulty(document.getString("difficulty"));
                    course.setCrowdLevel(document.getLong("crowdLevel").intValue());
                    course.setRating(document.getDouble("rating"));
                    course.setReviewCount(document.getLong("reviewCount").intValue());
                    
                    future.complete(course);
                } else {
                    future.complete(null);
                }
            })
            .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    
    // SOS 알림 전송
    public static CompletableFuture<Boolean> sendSOSAlert(SOSAlert alert) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("userId", alert.getUserId());
        alertData.put("userName", alert.getUserName());
        alertData.put("location", new GeoPoint(alert.getLatitude(), alert.getLongitude()));
        alertData.put("locationName", alert.getLocation());
        alertData.put("message", alert.getMessage());
        alertData.put("emergencyType", alert.getEmergencyType());
        alertData.put("timestamp", alert.getTimestamp());
        alertData.put("status", "ACTIVE");
        alertData.put("responseCount", 0);
        
        db.collection("sos_alerts")
            .add(alertData)
            .addOnSuccessListener(documentReference -> {
                alert.setAlertId(documentReference.getId());
                future.complete(true);
            })
            .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    // 주변 SOS 알림 가져오기
    public static CompletableFuture<List<SOSAlert>> getNearbySOSAlerts(double latitude, double longitude, double radius) {
        CompletableFuture<List<SOSAlert>> future = new CompletableFuture<>();
        
        // Firestore는 지리적 쿼리를 직접 지원하지 않으므로
        // 간단한 구현으로 대체 (실제로는 더 정교한 지리적 쿼리 필요)
        db.collection("sos_alerts")
            .whereEqualTo("status", "ACTIVE")
            .limit(20)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<SOSAlert> alerts = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    GeoPoint location = doc.getGeoPoint("location");
                    if (location != null) {
                        // 간단한 거리 계산 (실제로는 Haversine 공식 사용)
                        double distance = Math.sqrt(
                            Math.pow(location.getLatitude() - latitude, 2) +
                            Math.pow(location.getLongitude() - longitude, 2)
                        ) * 111; // 대략적인 km 변환
                        
                        if (distance <= radius) {
                            SOSAlert alert = new SOSAlert();
                            alert.setAlertId(doc.getId());
                            alert.setUserId(doc.getString("userId"));
                            alert.setUserName(doc.getString("userName"));
                            alert.setLatitude(location.getLatitude());
                            alert.setLongitude(location.getLongitude());
                            alert.setLocation(doc.getString("locationName"));
                            alert.setMessage(doc.getString("message"));
                            alert.setEmergencyType(doc.getString("emergencyType"));
                            alert.setStatus(doc.getString("status"));
                            alert.setResponseCount(doc.getLong("responseCount").intValue());
                            
                            alerts.add(alert);
                        }
                    }
                }
                future.complete(alerts);
            })
            .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    // 유동인구 정보 업데이트
    public static CompletableFuture<Boolean> updateCrowdLevel(int courseId, int crowdLevel) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("crowdLevel", crowdLevel);
        updates.put("lastUpdated", System.currentTimeMillis());
        
        db.collection("courses").document(String.valueOf(courseId))
            .update(updates)
            .addOnSuccessListener(aVoid -> future.complete(true))
            .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
} 