package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.adapters.GroupListAdapter;
import com.example.gps.api.ApiClient;
import com.example.gps.api.GroupApiService; // GroupApiService 추가
import com.example.gps.dto.GroupListResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; // Map import 추가 (서버 삭제 응답 처리용)
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Firebase imports
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyGroupsActivity extends AppCompatActivity implements GroupListAdapter.OnGroupClickListener {

    private RecyclerView rvGroups;
    private GroupListAdapter adapter;
    private String loggedInUsername;

    private DatabaseReference firebaseDatabase;
    private List<GroupListResponse> activeGroups = new ArrayList<>();
    private int pendingFirebaseChecks = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_groups);

        loggedInUsername = getIntent().getStringExtra("username");

        // ⭐ Firebase Database 초기화
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();

        rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        fetchMyGroups();
    }

    // =========================================================
    //  ⭐ GroupListAdapter.OnGroupClickListener 구현 시작
    // =========================================================

    // 1. 그룹 항목 클릭 시 (설정 화면 이동)
    @Override
    public void onGroupClick(Long groupId, String groupName) {
        Intent intent = new Intent(MyGroupsActivity.this, GroupSharingSettingsActivity.class);

        intent.putExtra("groupId", groupId);
        intent.putExtra("username", loggedInUsername);
        intent.putExtra("groupName", groupName);

        startActivity(intent);
    }

    // 2. ⭐ [추가] 삭제 버튼 클릭 시
    // (GroupListAdapter.java에 onDeleteClick이 추가되었다고 가정)
    @Override
    public void onDeleteClick(Long groupId, int position) {
        // 1. 서버 API 호출 (선택 사항: 서버 데이터 삭제)
        deleteGroupOnServer(groupId);

        // 2. Firebase 데이터 삭제 및 UI 업데이트 (핵심 로직)
        deleteGroupFromFirebase(groupId, position);
    }

    // =========================================================
    //  ⭐ 그룹 삭제 로직
    // =========================================================

    /**
     * 서버 API를 호출하여 그룹 정보를 삭제합니다. (선택적)
     */
    private void deleteGroupOnServer(Long groupId) {
        GroupApiService apiService = ApiClient.getGroupApiService(this);
        Call<Map<String, String>> call = apiService.deleteGroup(groupId);

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    // 서버 삭제 성공
                } else {
                    Toast.makeText(MyGroupsActivity.this, "서버 그룹 삭제 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Toast.makeText(MyGroupsActivity.this, "서버 네트워크 오류 (삭제): " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Firebase Realtime Database에서 그룹의 목적지 및 위치 정보를 삭제합니다.
     */
    private void deleteGroupFromFirebase(Long groupId, final int position) {
        String groupIdStr = String.valueOf(groupId);

        // 1. 삭제할 모든 경로를 담을 HashMap 생성
        Map<String, Object> deletionMap = new HashMap<>();

        // ⭐ [핵심 수정] group_destinations/{groupId} 경로에 null 설정 (삭제)
        deletionMap.put("group_destinations/" + groupIdStr, null);

        // ⭐ [핵심 수정] group_locations/{groupId} 경로에 null 설정 (삭제)
        deletionMap.put("group_locations/" + groupIdStr, null);

        // 2. Firebase Database 루트에서 일괄 업데이트 (삭제) 실행
        firebaseDatabase.updateChildren(deletionMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MyGroupsActivity.this, "그룹 " + groupId + " 삭제 완료 (Firebase).", Toast.LENGTH_SHORT).show();

                    // 3. UI 업데이트: RecyclerView에서 항목 제거
                    if (position >= 0 && position < activeGroups.size()) {
                        activeGroups.remove(position);
                        // 어댑터가 null이 아님을 가정하고 업데이트 실행
                        if (adapter != null) {
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, activeGroups.size());
                        } else {
                            // 어댑터가 null이면 전체 목록 재조회 (안전 장치)
                            fetchMyGroups();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // 삭제 실패 시, 사용자에게 명확히 알림
                    Toast.makeText(MyGroupsActivity.this, "그룹 삭제 실패: 권한 부족 또는 네트워크 오류.", Toast.LENGTH_LONG).show();
                    Log.e("MyGroupsActivity", "Firebase 일괄 삭제 실패: " + e.getMessage(), e);
                });
    }

    // =========================================================
    //  ⭐ 그룹 목록 조회 및 필터링 로직
    // =========================================================

    private void fetchMyGroups() {
        Call<List<GroupListResponse>> call = ApiClient.getGroupApiService(this).getMyGroups();
        call.enqueue(new Callback<List<GroupListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<GroupListResponse>> call, @NonNull Response<List<GroupListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GroupListResponse> allGroups = response.body();

                    // ⭐ [핵심] Firebase 필터링 로직 시작
                    filterGroupsByFirebaseExistence(allGroups);

                } else {
                    Toast.makeText(MyGroupsActivity.this, "그룹 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GroupListResponse>> call, @NonNull Throwable t) {
                Toast.makeText(MyGroupsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 서버에서 가져온 그룹 목록을 Firebase Realtime Database의 'group_destinations' 경로를 확인하여 필터링합니다.
     */
    private void filterGroupsByFirebaseExistence(List<GroupListResponse> allGroups) {
        if (allGroups.isEmpty()) {
            updateRecyclerView();
            return;
        }

        activeGroups.clear();
        pendingFirebaseChecks = allGroups.size();

        for (final GroupListResponse group : allGroups) {
            // Firebase의 group_destinations/{groupId} 경로 참조
            DatabaseReference groupDestinationRef = firebaseDatabase
                    .child("group_destinations")
                    .child(String.valueOf(group.getGroupId()));

            groupDestinationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // ⭐ DataSnapshot.exists()를 통해 해당 경로에 데이터가 있는지 확인
                    if (snapshot.exists()) {
                        // 데이터가 존재한다면, 활성 그룹으로 간주하고 리스트에 추가
                        activeGroups.add(group);
                    }

                    // 모든 Firebase 비동기 확인 작업이 완료되었는지 체크
                    checkCompletionAndUpdateUI();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Firebase 접근 오류 발생 시에도 완료 카운트 감소
                    checkCompletionAndUpdateUI();
                    Toast.makeText(MyGroupsActivity.this, "Firebase 확인 오류: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 모든 비동기 작업이 완료되면 RecyclerView를 업데이트합니다.
     */
    private void checkCompletionAndUpdateUI() {
        pendingFirebaseChecks--;

        if (pendingFirebaseChecks <= 0) {
            updateRecyclerView();
        }
    }

    /**
     * RecyclerView에 최종 필터링된 그룹 목록을 표시합니다.
     */
    private void updateRecyclerView() {
        adapter = new GroupListAdapter(
                activeGroups, // 필터링된 활성 그룹 리스트 사용
                MyGroupsActivity.this,
                loggedInUsername,
                MyGroupsActivity.this
        );
        rvGroups.setAdapter(adapter);

        if (activeGroups.isEmpty()) {
            Toast.makeText(MyGroupsActivity.this, "현재 활성 상태인 그룹이 없습니다.", Toast.LENGTH_LONG).show();
        }
    }
}