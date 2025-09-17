package com.example.gps.managers;

import android.content.Context;
import android.graphics.Color;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.CircleOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class FacilityManager {
    private final List<Marker> facilityMarkers = new ArrayList<>();
    private final List<CircleOverlay> facilityCircles = new ArrayList<>();

    public void displayFacilities(Context context, NaverMap map, JSONArray facilities) {
        clearFacilities();

        try {
            for (int i = 0; i < facilities.length(); i++) {
                JSONObject f = facilities.getJSONObject(i);
                LatLng pos = new LatLng(f.getDouble("lat"), f.getDouble("lng"));
                String type = f.getString("type");
                String name = f.getString("name");

                if ("화장실".equals(type) || "휴게소".equals(type)) {
                    // 원형 오버레이로 표시
                    CircleOverlay circle = new CircleOverlay();
                    circle.setCenter(pos);
                    circle.setRadius(20);
                    circle.setColor(0x4400FF00);
                    circle.setOutlineColor(Color.GREEN);
                    circle.setOutlineWidth(2);
                    circle.setMap(map);
                    facilityCircles.add(circle);
                } else {
                    // 마커로 표시
                    Marker marker = new Marker();
                    marker.setPosition(pos);
                    marker.setCaptionText(name);
                    marker.setMap(map);
                    facilityMarkers.add(marker);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearFacilities() {
        for (Marker marker : facilityMarkers) {
            marker.setMap(null);
        }
        for (CircleOverlay circle : facilityCircles) {
            circle.setMap(null);
        }
        facilityMarkers.clear();
        facilityCircles.clear();
    }
} 