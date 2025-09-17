package com.example.gps.model;

import java.util.List;

public class CourseDetail {
    private int courseId;
    private String courseName;
    private String description;
    private double distance; // km
    private int duration; // minutes
    private int steps; // 걸음수
    private boolean petFriendly; // 애완동물 동반 가능여부
    private double calories; // 소모 칼로리
    private double waterIntake; // 수분 섭취량 (ml)
    private String difficulty; // 난이도
    private List<String> seasons; // 추천 계절
    private List<String> weatherConditions; // 추천 날씨
    private int crowdLevel; // 유동인구 수준 (1-5)
    private List<String> facilities; // 주변 편의시설
    private List<String> publicTransport; // 대중교통 정보
    private List<String> photoSpots; // 포토스팟
    private double rating; // 평점
    private int reviewCount; // 리뷰 수

    public CourseDetail() {}

    public CourseDetail(int courseId, String courseName, String description, double distance, 
                       int duration, int steps, boolean petFriendly, double calories, 
                       double waterIntake, String difficulty) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.description = description;
        this.distance = distance;
        this.duration = duration;
        this.steps = steps;
        this.petFriendly = petFriendly;
        this.calories = calories;
        this.waterIntake = waterIntake;
        this.difficulty = difficulty;
    }

    // Getters and Setters
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    public boolean isPetFriendly() { return petFriendly; }
    public void setPetFriendly(boolean petFriendly) { this.petFriendly = petFriendly; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public double getWaterIntake() { return waterIntake; }
    public void setWaterIntake(double waterIntake) { this.waterIntake = waterIntake; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public List<String> getSeasons() { return seasons; }
    public void setSeasons(List<String> seasons) { this.seasons = seasons; }

    public List<String> getWeatherConditions() { return weatherConditions; }
    public void setWeatherConditions(List<String> weatherConditions) { this.weatherConditions = weatherConditions; }

    public int getCrowdLevel() { return crowdLevel; }
    public void setCrowdLevel(int crowdLevel) { this.crowdLevel = crowdLevel; }

    public List<String> getFacilities() { return facilities; }
    public void setFacilities(List<String> facilities) { this.facilities = facilities; }

    public List<String> getPublicTransport() { return publicTransport; }
    public void setPublicTransport(List<String> publicTransport) { this.publicTransport = publicTransport; }

    public List<String> getPhotoSpots() { return photoSpots; }
    public void setPhotoSpots(List<String> photoSpots) { this.photoSpots = photoSpots; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
} 