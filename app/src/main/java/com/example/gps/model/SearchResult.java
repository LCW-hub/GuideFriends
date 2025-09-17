package com.example.gps.model;

public class SearchResult {
    private String title;
    private String address;
    private String category;
    private double latitude;
    private double longitude;
    private String description;

    public SearchResult(String title, String address, String category, double latitude, double longitude, String description) {
        this.title = title;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
