package com.example.gps.model;

import java.util.Date;

public class SOSAlert {
    private String alertId;
    private String userId;
    private String userName;
    private double latitude;
    private double longitude;
    private String location;
    private String message;
    private Date timestamp;
    private String status; // "ACTIVE", "RESOLVED", "CANCELLED"
    private int responseCount;
    private String emergencyType; // "MEDICAL", "SAFETY", "ACCIDENT", "OTHER"

    public SOSAlert() {}

    public SOSAlert(String alertId, String userId, String userName, double latitude, 
                   double longitude, String location, String message, String emergencyType) {
        this.alertId = alertId;
        this.userId = userId;
        this.userName = userName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = location;
        this.message = message;
        this.emergencyType = emergencyType;
        this.timestamp = new Date();
        this.status = "ACTIVE";
        this.responseCount = 0;
    }

    // Getters and Setters
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getResponseCount() { return responseCount; }
    public void setResponseCount(int responseCount) { this.responseCount = responseCount; }

    public String getEmergencyType() { return emergencyType; }
    public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }
} 