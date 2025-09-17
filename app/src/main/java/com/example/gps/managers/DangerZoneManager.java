package com.example.gps.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.widget.Toast;

import com.example.gps.R;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.CircleOverlay;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DangerZoneManager {
    private final List<CircleOverlay> dangerOverlays = new ArrayList<>();
    private final List<JSONObject> dangerZoneData = new ArrayList<>();

    private MediaPlayer mediaPlayer;

    public void displayDangerZones(Context context, NaverMap map, JSONArray dangerZones) {
        dangerOverlays.clear();
        dangerZoneData.clear();

        try {
            for (int i = 0; i < dangerZones.length(); i++) {
                JSONObject zone = dangerZones.getJSONObject(i);
                LatLng center = new LatLng(zone.getDouble("lat"), zone.getDouble("lng"));
                int radius = zone.getInt("radius");
                String name = zone.getString("name");
                String description = zone.getString("description");

                CircleOverlay circle = new CircleOverlay();
                circle.setCenter(center);
                circle.setRadius(radius);
                circle.setColor(0x44FF0000);
                circle.setOutlineColor(Color.RED);
                circle.setOutlineWidth(2);
                circle.setMap(map);

                circle.setOnClickListener(overlay -> {
                    playWarningSound(context);
                    showAlert(context, name, description);
                    return true;
                });

                dangerOverlays.add(circle);
                dangerZoneData.add(zone);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "위험지역 로딩 중 오류 발생", Toast.LENGTH_SHORT).show();
        }
    }

    public void clearDangerZones() {
        for (CircleOverlay circle : dangerOverlays) {
            circle.setMap(null);
        }
        dangerOverlays.clear();
        dangerZoneData.clear();
    }

    private void playWarningSound(Context context) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer = MediaPlayer.create(context, R.raw.warning_sound);
        mediaPlayer.start();
    }

    private void showAlert(Context context, String name, String description) {
        new AlertDialog.Builder(context)
                .setTitle("⚠ 위험 경고: " + name)
                .setMessage(description)
                .setPositiveButton("확인", null)
                .show();
    }

    // 위험지역 진입 여부 확인 및 경고 표시 (사용자 위치 기준)
    public void checkUserInDangerZone(Context context, LatLng userLocation) {
        for (JSONObject zone : dangerZoneData) {
            double lat = zone.optDouble("lat");
            double lng = zone.optDouble("lng");
            int radius = zone.optInt("radius");
            String name = zone.optString("name");
            String description = zone.optString("description");

            double distance = distanceBetween(lat, lng, userLocation.latitude, userLocation.longitude);

            if (distance < radius) {
                playWarningSound(context);
                showAlert(context, name, description);
                Toast.makeText(context, "⚠ 위험 지역에 진입하셨습니다. 즉시 경로를 변경하세요.", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    // 거리 계산 (미터 단위, Haversine 공식)
    private double distanceBetween(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
