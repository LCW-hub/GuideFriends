package com.example.gps.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;
public class SearchResult implements Parcelable {
    private String title;
    private String address;
    private String category;
    private double latitude;
    private double longitude;
    private String description;
    private String imageUrl; // [추가] 이미지 URL을 저장할 변수

    public SearchResult(String title, String address, String category, double latitude, double longitude, String description, String imageUrl) {
        this.title = title;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.imageUrl = imageUrl; // [추가]
    }

    // --- Parcelable 관련 코드 수정 ---
    protected SearchResult(Parcel in) {
        title = in.readString();
        address = in.readString();
        category = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        description = in.readString();
        imageUrl = in.readString(); // [추가]
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(address);
        dest.writeString(category);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(description);
        dest.writeString(imageUrl); // [추가]
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SearchResult> CREATOR = new Creator<SearchResult>() {
        @Override
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        @Override
        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    // --- 기존 Getter, Setter + imageUrl Getter/Setter ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; } // [추가]
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; } // [추가]
}