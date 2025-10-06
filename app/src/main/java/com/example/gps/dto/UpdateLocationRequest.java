package com.example.gps.dto;

public class UpdateLocationRequest {

    private Double latitude;
    private Double longitude;

    // Setter
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}