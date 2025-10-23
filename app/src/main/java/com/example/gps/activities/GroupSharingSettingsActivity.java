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
import com.example.gps.api.UserApiService;
import com.example.gps.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        // 3. 어댑터 초기화 (userId가 -1L인 상태로 초기화)
        adapter = new MemberSharingAdapter(new ArrayList<>(), loggedInUserId);
        rvMembers.setAdapter(adapter);

        // 4. username을 이용해 userId를 조회 후, 멤버 목록 로드를 시작합니다.
        fetchLoggedInUserId();

        // 5. 저장 버튼은 userId 획득 후 enable될 때까지 비활성화
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> saveSharingSettings());
    }

    /**
     * 현재 로그인된 사용자의 Username을 이용해 UserId를 서버에서 조회합니다.
     */
    private void fetchLoggedInUserId() {
        UserApiService apiService = ApiClient.getUserApiService(this);
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
                        fetchGroupAllMembersAndRules(); // ⭐️ ID 획득 후 다음 단계 호출
                        return;
                    }
                }
                Log.e(TAG, "❌ 사용자 ID 획득 실패. 응답 코드: " + response.code());
                Toast.makeText(GroupSharingSettingsActivity.this, "사용자 ID 획득 실패. 설정을 사용할 수 없습니다. (코드: " + response.code() + ")", Toast.LENGTH_LONG).show();
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
     * ⭐️ [개선] 그룹 멤버 목록과 현재 위치 공유 규칙을 서버에서 병렬로 로드합니다.
     */
    private void fetchGroupAllMembersAndRules() {
        if (loggedInUserId == -1L) {
            Log.e(TAG, "UserId가 유효하지 않아 멤버 로드 중단.");
            return;
        }

        GroupApiService apiService = ApiClient.getGroupApiService(this);

        // ⭐️ 1. 병렬 처리를 위한 변수 초기화
        final List<User> allMembersHolder = new ArrayList<>();
        final Map<Long, Boolean> rulesHolder = new HashMap<>();
        final boolean[] membersLoaded = {false};
        final boolean[] rulesLoaded = {false};
        final boolean[] loadFailed = {false};

        // 로딩 실패 시 처리를 위한 헬퍼 함수
        Runnable checkCompletion = () -> {
            if (loadFailed[0]) return; // 이미 실패했으면 종료

            if (membersLoaded[0] && rulesLoaded[0]) {
                // ⭐️ 2. 모든 데이터 로드 완료
                adapter.updateMembers(allMembersHolder);
                adapter.setInitialSharingRules(rulesHolder);
                Toast.makeText(GroupSharingSettingsActivity.this, "멤버 목록 및 현재 규칙 로드 완료.", Toast.LENGTH_SHORT).show();
            }
        };


        // 3. 멤버 목록 로드 (비동기 시작)
        Call<List<User>> membersCall = apiService.getAllGroupMembers(currentGroupId);
        membersCall.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> membersCall, @NonNull Response<List<User>> membersResponse) {
                if (membersResponse.isSuccessful() && membersResponse.body() != null) {
                    allMembersHolder.addAll(membersResponse.body());
                    membersLoaded[0] = true;
                    Log.d(TAG, "멤버 로드 성공. 수신 멤버 수: " + allMembersHolder.size());
                    checkCompletion.run();
                } else {
                    Log.e(TAG, "❌ 멤버 로드 실패. 응답 코드: " + membersResponse.code());
                    loadFailed[0] = true;
                    Toast.makeText(GroupSharingSettingsActivity.this, "멤버 목록 로드 실패 (코드: " + membersResponse.code() + ")", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<User>> membersCall, @NonNull Throwable t) {
                Log.e(TAG, "멤버 로드 네트워크 오류", t);
                loadFailed[0] = true;
                Toast.makeText(GroupSharingSettingsActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });

        // 4. 규칙 로드 (비동기 시작)
        // 내가 Sharer일 때, 다른 멤버에게 허용 여부 규칙을 가져옵니다.
        // 💡 GroupApiService에 getSharingRulesForSharer(groupId, sharerId)가 정의되어 있어야 합니다.
        Call<Map<Long, Boolean>> rulesCall = apiService.getSharingRulesForSharer(currentGroupId, loggedInUserId);
        rulesCall.enqueue(new Callback<Map<Long, Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<Map<Long, Boolean>> rulesCall, @NonNull Response<Map<Long, Boolean>> rulesResponse) {
                if (rulesResponse.isSuccessful() && rulesResponse.body() != null) {
                    rulesHolder.putAll(rulesResponse.body());
                    rulesLoaded[0] = true;
                    Log.d(TAG, "규칙 로드 성공. 로드된 규칙 수: " + rulesHolder.size());
                    checkCompletion.run();
                } else {
                    Log.e(TAG, "❌ 규칙 로드 실패. 응답 코드: " + rulesResponse.code());
                    // ⭐️ 규칙 로드 실패는 치명적이지 않으므로, 기본값 True로 표시하고 계속 진행
                    rulesLoaded[0] = true; // 실패했지만, 규칙 로드 시도는 완료됨으로 표시
                    Toast.makeText(GroupSharingSettingsActivity.this, "규칙 로드 실패. 기본값(공유 허용)으로 설정합니다.", Toast.LENGTH_LONG).show();
                    checkCompletion.run();
                }
            }
            @Override
            public void onFailure(@NonNull Call<Map<Long, Boolean>> rulesCall, @NonNull Throwable t) {
                Log.e(TAG, "규칙 로드 네트워크 오류", t);
                rulesLoaded[0] = true; // 실패했지만, 규칙 로드 시도는 완료됨으로 표시
                Toast.makeText(GroupSharingSettingsActivity.this, "규칙 로드 네트워크 오류 발생. 기본값으로 설정합니다.", Toast.LENGTH_LONG).show();
                checkCompletion.run();
            }
        });
    }

    /**
     * ⭐️ [개선] 체크박스 상태를 읽어 서버에 공유 규칙 요청을 보냅니다. (자신 제외 및 완료 로직 강화)
     */
    private void saveSharingSettings() {
        if (loggedInUserId == -1L) {
            Toast.makeText(this, "사용자 ID가 유효하지 않아 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        GroupApiService apiService = ApiClient.getGroupApiService(this);

        // 1. 자기 자신을 제외한 멤버 목록을 준비합니다.
        List<User> membersToUpdate = new ArrayList<>();
        for (User member : adapter.getMembers()) {
            // ⭐️ 자기 자신(로그인된 사용자)은 업데이트 대상에서 제외합니다.
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

        for (User member : membersToUpdate) {
            boolean isChecked = adapter.isUserChecked(member.getId());
            boolean allowSharing = isChecked;

            apiService.updateSharingRule(currentGroupId, member.getId(), allowSharing).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "규칙 업데이트 성공: 대상=" + member.getUsername() + ", 허용=" + allowSharing);
                    } else {
                        Log.e(TAG, "❌ 규칙 업데이트 실패: 대상=" + member.getUsername() + ", 코드=" + response.code());
                        failedMembers.add(member.getUsername());
                    }

                    checkCompletionAndFinish();
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "❌ 네트워크 실패: " + t.getMessage());
                    failedMembers.add(member.getUsername() + " (네트워크)");
                    checkCompletionAndFinish();
                }

                // 모든 요청 완료 체크 및 최종 처리 도우미 메서드
                private void checkCompletionAndFinish() {
                    completedRequests[0]++;
                    if (completedRequests[0] == totalRequests) { // ⭐️ totalRequests를 사용
                        // 1. 최종 메시지 표시
                        if (failedMembers.isEmpty()) {
                            Toast.makeText(GroupSharingSettingsActivity.this, "위치 공유 설정 저장이 완료되었습니다.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(GroupSharingSettingsActivity.this,
                                    "일부 설정 실패: " + failedMembers.size() + "명. 로그를 확인하세요.",
                                    Toast.LENGTH_LONG).show();
                        }

                        // 2. MapsActivity로 돌아가기
                        finishAndReturnToMaps();
                    }
                }
            });
        }
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