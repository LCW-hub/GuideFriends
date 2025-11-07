package com.example.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.adapters.MemberSharingAdapter;
import com.example.gps.api.ApiClient;
import com.example.gps.api.GroupApiService;
import com.example.gps.api.UserApi; // ⭐ [수정] UserApiService 대신 UserApi 임포트
import com.example.gps.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class GroupSharingSettingsActivity extends AppCompatActivity {

    private static final String TAG = "SharingSettingsActivity";
    private RecyclerView rvMembers;
    private Button btnSave;
    private TextView tvTitle;
    private MemberSharingAdapter adapter;

    private Long currentGroupId = -1L;
    private Long loggedInUserId = -1L;
    private String loggedInUsername = null;
    private String groupName = null;

    // 현재 내가 설정한 규칙 상태를 저장
    private Map<Long, Boolean> currentOutgoingRules = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_sharing_settings);

        tvTitle = findViewById(R.id.tv_group_name_title);
        rvMembers = findViewById(R.id.rv_group_members_settings);
        btnSave = findViewById(R.id.btn_save_sharing_settings);

        // 1. Intent에서 그룹 ID 및 사용자 이름 가져오기
        if (getIntent() != null) {
            currentGroupId = getIntent().getLongExtra("groupId", -1L);
            loggedInUsername = getIntent().getStringExtra("username");
            groupName = getIntent().getStringExtra("groupName");
        }

        Log.d(TAG, "onCreate - GroupId: " + currentGroupId + ", Username: " + loggedInUsername);

        // 2. 유효성 검사
        if (currentGroupId == -1L || loggedInUsername == null) {
            Toast.makeText(this, "그룹 또는 사용자 정보가 유효하지 않습니다. (로그인 사용자 이름 누락)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvTitle.setText("위치 공유 설정 (" + groupName + ")");
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // 3. 어댑터 초기화
        adapter = new MemberSharingAdapter(new ArrayList<>(), loggedInUserId);
        rvMembers.setAdapter(adapter);

        // 4. username을 이용해 userId를 조회 후, 멤버 목록 로드를 시작합니다.
        fetchLoggedInUserId();

        // 5. 저장 버튼은 userId 획득 후 enable될 때까지 비활성화
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> saveSharingSettings());
    }

    /**
     * 현재 로그인된 사용자의 Username을 이용해 UserId를 서버에서 조회합니다. (API 필수)
     */
    private void fetchLoggedInUserId() {
        // ⭐ [수정] UserApiService -> UserApi로 변경하고, ApiClient를 통해 Retrofit 서비스 생성
        UserApi apiService = ApiClient.getRetrofit(this).create(UserApi.class);
        Call<Map<String, Long>> call = apiService.getUserIdByUsername(loggedInUsername);

        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Long>> call, @NonNull Response<Map<String, Long>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Long userId = response.body().get("userId");
                    if (userId != null && userId != -1L) {
                        loggedInUserId = userId;
                        Log.d(TAG, "사용자 ID 획득 성공: " + loggedInUserId);
                        adapter.setLoggedInUserId(loggedInUserId);
                        btnSave.setEnabled(true);
                        fetchGroupAllMembersAndRules(); // ID 획득 후 다음 단계 호출
                        return;
                    }
                }
                Log.e(TAG, "❌ 사용자 ID 획득 실패. 응답 코드: " + response.code());
                Toast.makeText(GroupSharingSettingsActivity.this, "사용자 ID 획득 실패. 설정을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Long>> call, @NonNull Throwable t) {
                Log.e(TAG, "사용자 ID 네트워크 오류", t);
                Toast.makeText(GroupSharingSettingsActivity.this, "사용자 ID 네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }


    /**
     * 그룹 멤버 목록을 로드하고 (API), Firebase에서 현재 규칙을 로드합니다.
     */
    private void fetchGroupAllMembersAndRules() {
        if (loggedInUserId == -1L) {
            Log.e(TAG, "UserId가 유효하지 않아 멤버 로드 중단.");
            return;
        }

        // ⭐ [수정] ApiClient.getGroupApiService(this) 그대로 사용
        GroupApiService apiService = ApiClient.getGroupApiService(this);

        // 1. 멤버 목록 로드 (API 사용)
        Call<List<User>> membersCall = apiService.getAllGroupMembers(currentGroupId);
        membersCall.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> membersCall, @NonNull Response<List<User>> membersResponse) {
                if (membersResponse.isSuccessful() && membersResponse.body() != null) {
                    List<User> allMembers = membersResponse.body();
                    adapter.updateMembers(allMembers);
                    Log.d(TAG, "멤버 로드 성공. 수신 멤버 수: " + allMembers.size());

                    // 2. 멤버 로드 성공 후, Firebase에서 현재 규칙 로드를 시작합니다.
                    loadFirebaseRulesForMe();
                } else {
                    Log.e(TAG, "❌ 멤버 로드 실패. 응답 코드: " + membersResponse.code());
                    Toast.makeText(GroupSharingSettingsActivity.this, "멤버 목록 로드 실패 (코드: " + membersResponse.code() + ")", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<User>> membersCall, @NonNull Throwable t) {
                Log.e(TAG, "멤버 로드 네트워크 오류", t);
                Toast.makeText(GroupSharingSettingsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * 내 위치 마커의 표시/숨김 상태를 Firebase에 저장합니다.
     */
    private void updateUserMarkerStatus(boolean isSharingAllowed) {
        if (loggedInUserId == -1L) return;

        // 경로: user_status/{userId}/is_marker_visible
        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("user_status")
                .child(String.valueOf(loggedInUserId))
                .child("is_marker_visible");

        statusRef.setValue(isSharingAllowed) // true 또는 false를 저장
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "내 마커 상태 Firebase 업데이트 성공: " + isSharingAllowed);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 내 마커 상태 업데이트 실패", e);
                });
    }

    /**
     * Firebase에서 내가 설정한 현재의 Outgoing 규칙을 읽어와 Adapter에 적용합니다.
     */
    private void loadFirebaseRulesForMe() {
        DatabaseReference myRulesRef = FirebaseDatabase.getInstance()
                .getReference("sharing_permissions")
                .child(String.valueOf(loggedInUserId)); // 경로: sharing_permissions/{myUserId}

        myRulesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentOutgoingRules.clear();
                Map<Long, Boolean> rulesFromFirebase = new HashMap<>();

                // Firebase에서 내가 설정한 모든 규칙을 읽어와 맵에 저장
                for (DataSnapshot ruleSnapshot : snapshot.getChildren()) {
                    try {
                        Long targetId = Long.parseLong(ruleSnapshot.getKey());
                        Boolean isAllowed = ruleSnapshot.getValue(Boolean.class);
                        if (isAllowed != null) {
                            rulesFromFirebase.put(targetId, isAllowed);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Firebase 규칙 키 파싱 오류: " + ruleSnapshot.getKey(), e);
                    }
                }
                currentOutgoingRules.putAll(rulesFromFirebase);
                Log.d(TAG, "Firebase 규칙 로드 성공. Outgoing 규칙 수: " + rulesFromFirebase.size());

                // Adapter에 초기 규칙을 적용하여 체크박스 상태를 설정
                adapter.setInitialSharingRules(rulesFromFirebase);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase 규칙 로드 실패", error.toException());
                Toast.makeText(GroupSharingSettingsActivity.this, "규칙 로드 실패. 기본값(차단)으로 설정합니다.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * ⭐️ [핵심 수정]: 체크박스 상태를 읽어 Firebase에 내가 상대방에게 보내는 규칙을 저장합니다.
     * saveSharingSettings 내부의 지역 변수 사용 오류를 해결하기 위해 로직을 통합했습니다.
     */
    private void saveSharingSettings() {
        if (loggedInUserId == -1L) {
            Toast.makeText(this, "사용자 ID가 유효하지 않아 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 자기 자신을 제외한 멤버 목록을 준비합니다. (콜백에서 접근할 수 있도록 final로 간주)
        final List<User> membersToUpdate = new ArrayList<>();
        for (User member : adapter.getMembers()) {
            if (!member.getId().equals(loggedInUserId)) {
                membersToUpdate.add(member);
            }
        }

        if (membersToUpdate.isEmpty()) {
            Toast.makeText(this, "그룹에 다른 멤버가 없어 저장할 설정이 없습니다.", Toast.LENGTH_SHORT).show();
            finishAndReturnToMaps();
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        btnSave.setEnabled(false);

        final int totalRequests = membersToUpdate.size();
        final int[] completedRequests = {0};
        final List<String> failedMembers = new ArrayList<>();

        // ⭐️ [Firebase 쓰기 시작]
        for (User member : membersToUpdate) {
            // 람다 내부에서 사용되는 member는 final이거나 effectively final이어야 합니다.
            final User finalMember = member;
            boolean allowSharing = adapter.isUserChecked(member.getId());
            final boolean finalAllowSharing = allowSharing;

            // 1. Firebase 경로 설정: sharing_permissions/{sharerId}/{targetId}
            DatabaseReference ruleRef = FirebaseDatabase.getInstance()
                    .getReference("sharing_permissions")
                    .child(String.valueOf(loggedInUserId))
                    .child(String.valueOf(finalMember.getId()));

            ruleRef.setValue(finalAllowSharing)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "규칙 업데이트 성공 (Outgoing): 대상=" + finalMember.getUsername() + ", 허용=" + finalAllowSharing + " (Firebase)");
                        // ⭐️ [로직 통합] 완료 체크
                        completedRequests[0]++;
                        if (completedRequests[0] == totalRequests) {
                            handleFinalCompletion(failedMembers, totalRequests);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ 규칙 업데이트 실패 (Outgoing): 대상=" + finalMember.getUsername() + ", 오류=" + e.getMessage());
                        failedMembers.add(finalMember.getUsername());
                        // ⭐️ [로직 통합] 완료 체크
                        completedRequests[0]++;
                        if (completedRequests[0] == totalRequests) {
                            handleFinalCompletion(failedMembers, totalRequests);
                        }
                    });
        }
    }

    /**
     * ⭐️ [새 함수]: 모든 요청 완료 후 최종 처리를 담당합니다. (마커 상태 제어 로직 포함)
     */
    private void handleFinalCompletion(List<String> failedMembers, int totalMembersCount) {

        // ⭐️ [내 마커 상태 업데이트]: 실패한 요청이 전체 요청과 같지 않다면 (성공이 1개라도 있다면) 마커를 켬
        boolean isSharingToAnyone = failedMembers.size() < totalMembersCount;
        updateUserMarkerStatus(isSharingToAnyone);

        // 1. 최종 메시지 표시
        if (failedMembers.isEmpty()) {
            Toast.makeText(GroupSharingSettingsActivity.this, "위치 공유 설정 저장이 완료되었습니다.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(GroupSharingSettingsActivity.this,
                    "일부 설정 실패: " + failedMembers.size() + "/" + totalMembersCount + "명. 로그를 확인하세요.",
                    Toast.LENGTH_LONG).show();
        }

        // 2. MapsActivity로 돌아가기
        finishAndReturnToMaps();
        btnSave.setEnabled(true);
    }


    /**
     * 저장 완료 후 MapsActivity로 돌아가기 위한 헬퍼 함수입니다.
     */
    private void finishAndReturnToMaps() {
        Intent intent = new Intent(GroupSharingSettingsActivity.this, com.example.gps.activities.MapsActivity.class);
        intent.putExtra("groupId", currentGroupId);
        intent.putExtra("username", loggedInUsername);

        intent.putExtra("RULES_UPDATED", true);

        // MapsActivity의 기존 인스턴스를 재활용하고 onNewIntent()를 호출하도록 강제
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        finish();
    }
}